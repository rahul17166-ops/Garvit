package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionPreferences
import com.example.data.remote.DomainDto
import com.example.data.remote.MailTmNetworkModule
import com.example.data.remote.MessageDetailDto
import com.example.data.remote.MessageSummaryDto
import com.example.data.repository.ActiveMailbox
import com.example.data.repository.ApiProbeResult
import com.example.data.repository.MailTmRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainDestination {
  INBOX,
  IDENTITY,
  DIAGNOSTICS,
}

data class MailVaultUiState(
  val selectedTab: MainDestination = MainDestination.INBOX,
  val isProvisioning: Boolean = true,
  val isRefreshingInbox: Boolean = false,
  val mailbox: ActiveMailbox? = null,
  val domains: List<DomainDto> = emptyList(),
  val messages: List<MessageSummaryDto> = emptyList(),
  val selectedMessageId: String? = null,
  val selectedMessageDetail: MessageDetailDto? = null,
  val isLoadingDetail: Boolean = false,
  val rawMimeSource: String? = null,
  val isLoadingSource: Boolean = false,
  val isProbingApi: Boolean = false,
  val probeResult: ApiProbeResult? = null,
  val errorBanner: String? = null,
  val snackbarMessage: String? = null,
  val lastSyncedTimeMs: Long? = null,
)

class MailVaultViewModel(
  private val repository: MailTmRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(MailVaultUiState())
  val uiState: StateFlow<MailVaultUiState> = _uiState.asStateFlow()

  private var pollingJob: Job? = null

  init {
    bootstrapSession()
  }

  fun bootstrapSession() {
    viewModelScope.launch {
      _uiState.update { it.copy(isProvisioning = true, errorBanner = null) }
      val domainsResult = repository.fetchActiveDomains()
      val domains = domainsResult.getOrDefault(emptyList())
      _uiState.update { it.copy(domains = domains) }

      val mailboxResult = repository.restoreOrCreateMailbox()
      mailboxResult.fold(
        onSuccess = { mailbox ->
          _uiState.update {
            it.copy(
              isProvisioning = false,
              mailbox = mailbox,
              errorBanner = null,
            )
          }
          refreshInbox(silent = false)
          startAutoPolling()
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isProvisioning = false,
              errorBanner = error.localizedMessage ?: "Unable to connect to Mail.tm API",
            )
          }
        },
      )
    }
  }

  fun selectTab(destination: MainDestination) {
    _uiState.update { it.copy(selectedTab = destination) }
    if (destination == MainDestination.DIAGNOSTICS && _uiState.value.probeResult == null) {
      runApiProbe()
    }
    if (destination == MainDestination.IDENTITY && _uiState.value.domains.isEmpty()) {
      viewModelScope.launch {
        repository.fetchActiveDomains().onSuccess { domains ->
          _uiState.update { it.copy(domains = domains) }
        }
      }
    }
  }

  fun refreshInbox(silent: Boolean = false) {
    val mailbox = _uiState.value.mailbox ?: return
    viewModelScope.launch {
      if (!silent) {
        _uiState.update { it.copy(isRefreshingInbox = true, errorBanner = null) }
      }
      repository.fetchMessages(mailbox.token).fold(
        onSuccess = { msgs ->
          _uiState.update {
            it.copy(
              isRefreshingInbox = false,
              messages = msgs,
              lastSyncedTimeMs = System.currentTimeMillis(),
              errorBanner = null,
            )
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isRefreshingInbox = false,
              errorBanner = if (silent) it.errorBanner else err.localizedMessage,
            )
          }
        },
      )
      repository.refreshAccountQuota(mailbox.token).onSuccess { account ->
        _uiState.update { state ->
          state.copy(
            mailbox = state.mailbox?.copy(
              quotaBytes = account.quota,
              usedBytes = account.used,
            )
          )
        }
      }
    }
  }

  fun openMessage(messageId: String) {
    val mailbox = _uiState.value.mailbox ?: return
    _uiState.update {
      it.copy(
        selectedMessageId = messageId,
        selectedMessageDetail = null,
        rawMimeSource = null,
        isLoadingDetail = true,
        errorBanner = null,
      )
    }
    viewModelScope.launch {
      repository.fetchMessageDetail(mailbox.token, messageId).fold(
        onSuccess = { detail ->
          _uiState.update { state ->
            state.copy(
              isLoadingDetail = false,
              selectedMessageDetail = detail,
              messages = state.messages.map { msg ->
                if (msg.id == messageId) msg.copy(seen = true) else msg
              },
            )
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isLoadingDetail = false,
              snackbarMessage = err.localizedMessage ?: "Unable to load message",
            )
          }
        },
      )
    }
  }

  fun loadMessageSource(messageId: String) {
    val mailbox = _uiState.value.mailbox ?: return
    if (_uiState.value.rawMimeSource != null || _uiState.value.isLoadingSource) return
    _uiState.update { it.copy(isLoadingSource = true) }
    viewModelScope.launch {
      repository.fetchMessageSource(mailbox.token, messageId).fold(
        onSuccess = { source ->
          _uiState.update {
            it.copy(isLoadingSource = false, rawMimeSource = source.data)
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isLoadingSource = false,
              rawMimeSource = "// Failed to fetch MIME source: ${err.localizedMessage}",
            )
          }
        },
      )
    }
  }

  fun closeMessage() {
    _uiState.update {
      it.copy(
        selectedMessageId = null,
        selectedMessageDetail = null,
        rawMimeSource = null,
        isLoadingDetail = false,
        isLoadingSource = false,
      )
    }
  }

  fun deleteMessage(messageId: String) {
    val mailbox = _uiState.value.mailbox ?: return
    viewModelScope.launch {
      repository.deleteMessage(mailbox.token, messageId).fold(
        onSuccess = {
          _uiState.update { state ->
            state.copy(
              messages = state.messages.filterNot { it.id == messageId },
              selectedMessageId = if (state.selectedMessageId == messageId) null else state.selectedMessageId,
              selectedMessageDetail = if (state.selectedMessageId == messageId) null else state.selectedMessageDetail,
              snackbarMessage = "Message deleted",
            )
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(snackbarMessage = err.localizedMessage ?: "Delete failed") }
        },
      )
    }
  }

  fun rotateRandomMailbox() {
    viewModelScope.launch {
      _uiState.update { it.copy(isProvisioning = true, errorBanner = null) }
      repository.createRandomMailbox().fold(
        onSuccess = { mailbox ->
          _uiState.update {
            it.copy(
              isProvisioning = false,
              mailbox = mailbox,
              messages = emptyList(),
              selectedMessageId = null,
              selectedMessageDetail = null,
              selectedTab = MainDestination.INBOX,
              snackbarMessage = "New disposable address ready",
            )
          }
          refreshInbox(silent = false)
          startAutoPolling()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isProvisioning = false,
              snackbarMessage = err.localizedMessage ?: "Could not rotate mailbox",
            )
          }
        },
      )
    }
  }

  fun createCustomMailbox(username: String, domain: String, password: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProvisioning = true, errorBanner = null) }
      repository.createCustomMailbox(username, domain, password).fold(
        onSuccess = { mailbox ->
          _uiState.update {
            it.copy(
              isProvisioning = false,
              mailbox = mailbox,
              messages = emptyList(),
              selectedTab = MainDestination.INBOX,
              snackbarMessage = "Provisioned ${mailbox.address}",
            )
          }
          refreshInbox(silent = false)
          startAutoPolling()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isProvisioning = false,
              snackbarMessage = err.localizedMessage ?: "Mailbox creation failed",
            )
          }
        },
      )
    }
  }

  fun signInExisting(address: String, password: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isProvisioning = true, errorBanner = null) }
      repository.signIn(address, password).fold(
        onSuccess = { mailbox ->
          _uiState.update {
            it.copy(
              isProvisioning = false,
              mailbox = mailbox,
              messages = emptyList(),
              selectedTab = MainDestination.INBOX,
              snackbarMessage = "Signed in as ${mailbox.address}",
            )
          }
          refreshInbox(silent = false)
          startAutoPolling()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isProvisioning = false,
              snackbarMessage = err.localizedMessage ?: "Sign-in failed",
            )
          }
        },
      )
    }
  }

  fun burnCurrentMailbox() {
    val mailbox = _uiState.value.mailbox ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isProvisioning = true) }
      repository.burnMailbox(mailbox.token, mailbox.accountId).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              mailbox = null,
              messages = emptyList(),
              snackbarMessage = "Disposable mailbox burned",
            )
          }
          rotateRandomMailbox()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isProvisioning = false,
              snackbarMessage = err.localizedMessage ?: "Failed to burn mailbox",
            )
          }
        },
      )
    }
  }

  fun runApiProbe() {
    viewModelScope.launch {
      _uiState.update { it.copy(isProbingApi = true) }
      val result = repository.probeApiHealth(_uiState.value.mailbox?.token)
      _uiState.update { it.copy(isProbingApi = false, probeResult = result) }
    }
  }

  fun generatePasswordSuggestion(): String = repository.generateRandomPassword()

  fun generateUsernameSuggestion(): String = repository.generateRandomUsername()

  fun showSnackbar(message: String) {
    _uiState.update { it.copy(snackbarMessage = message) }
  }

  fun clearSnackbar() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }

  private fun startAutoPolling() {
    pollingJob?.cancel()
    pollingJob = viewModelScope.launch {
      while (isActive) {
        delay(15_000L)
        if (_uiState.value.selectedTab == MainDestination.INBOX && _uiState.value.mailbox != null) {
          refreshInbox(silent = true)
        }
      }
    }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          val repo = MailTmRepository(
            api = MailTmNetworkModule.createApi(),
            sessionPrefs = SessionPreferences(context.applicationContext),
          )
          return MailVaultViewModel(repo) as T
        }
      }
  }
}
