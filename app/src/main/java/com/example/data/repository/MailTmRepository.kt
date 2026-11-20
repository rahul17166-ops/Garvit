package com.example.data.repository

import com.example.data.local.SessionPreferences
import com.example.data.local.StoredSession
import com.example.data.remote.AccountDto
import com.example.data.remote.CredentialsRequest
import com.example.data.remote.DomainDto
import com.example.data.remote.MailTmApi
import com.example.data.remote.MarkSeenRequest
import com.example.data.remote.MessageDetailDto
import com.example.data.remote.MessageSummaryDto
import com.example.data.remote.SourceDto
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.delay
import retrofit2.Response

data class ActiveMailbox(
  val accountId: String,
  val address: String,
  val password: String,
  val token: String,
  val quotaBytes: Long = 40_000_000L,
  val usedBytes: Long = 0L,
)

data class ApiProbeResult(
  val reachable: Boolean,
  val httpStatus: Int,
  val latencyMs: Long,
  val activeDomains: List<String>,
  val hydraEnvelopeValid: Boolean,
  val authenticatedMeValid: Boolean,
  val errorMessage: String? = null,
)

class MailTmRepository(
  private val api: MailTmApi,
  private val sessionPrefs: SessionPreferences,
) {

  private val random = SecureRandom()

  suspend fun restoreOrCreateMailbox(): Result<ActiveMailbox> = runCatching {
    val stored = sessionPrefs.loadSession()
    if (stored != null) {
      val meResp = api.getMe(bearer(stored.token))
      if (meResp.isSuccessful && meResp.body() != null) {
        val account = meResp.body()!!
        return@runCatching ActiveMailbox(
          accountId = account.id,
          address = account.address,
          password = stored.password,
          token = stored.token,
          quotaBytes = account.quota,
          usedBytes = account.used,
        )
      }
      // Attempt token refresh if password was saved and token expired (401)
      if (meResp.code() == 401 && stored.password.isNotBlank()) {
        val refreshed = signIn(stored.address, stored.password)
        if (refreshed.isSuccess) return@runCatching refreshed.getOrThrow()
      }
    }
    createRandomMailbox().getOrThrow()
  }

  suspend fun fetchActiveDomains(): Result<List<DomainDto>> = runCatching {
    val response = api.getDomains(page = 1)
    val body = requireSuccess(response, "Failed to fetch Mail.tm domains")
    val active = body.members.filter { it.isActive }
    if (active.isEmpty()) error("No active Mail.tm domains returned")
    active
  }

  suspend fun createRandomMailbox(): Result<ActiveMailbox> = runCatching {
    val domains = fetchActiveDomains().getOrThrow()
    val domain = domains.first().domain
    val username = generateRandomUsername()
    val password = generateRandomPassword()
    createCustomMailbox(username, domain, password).getOrThrow()
  }

  suspend fun createCustomMailbox(
    username: String,
    domain: String,
    password: String,
  ): Result<ActiveMailbox> = runCatching {
    val cleanUser = username.trim().lowercase(Locale.US)
    require(cleanUser.length >= 3) { "Username must be at least 3 characters" }
    require(password.length >= 8) { "Password must be at least 8 characters" }
    val address = "$cleanUser@$domain"
    val createCredentials = CredentialsRequest(address = address, password = password)

    val accountResp = api.createAccount(createCredentials)
    val account = requireSuccess(accountResp, "Could not create mailbox ($address)")

    // Use the exact address returned by Mail.tm and retry briefly if /token hits replica lag (401)
    val resolvedAddress = account.address.ifBlank { address }
    val tokenCredentials = CredentialsRequest(address = resolvedAddress, password = password)
    val tokenBody = requestTokenWithPropagationRetry(tokenCredentials)

    val session = StoredSession(
      accountId = account.id,
      address = account.address,
      password = password,
      token = tokenBody.token,
    )
    sessionPrefs.saveSession(session)

    ActiveMailbox(
      accountId = account.id,
      address = account.address,
      password = password,
      token = tokenBody.token,
      quotaBytes = account.quota,
      usedBytes = account.used,
    )
  }

  private suspend fun requestTokenWithPropagationRetry(
    credentials: CredentialsRequest,
    retryDelaysMs: List<Long> = listOf(350L, 600L, 1000L, 1500L),
  ): com.example.data.remote.TokenResponse {
    var lastResponse: Response<com.example.data.remote.TokenResponse>? = null
    for (waitMs in retryDelaysMs) {
      delay(waitMs)
      val resp = api.createToken(credentials)
      if (resp.isSuccessful && resp.body() != null) {
        return resp.body()!!
      }
      lastResponse = resp
      // Retry only on transient 401 (newly created account propagation lag) or 429/5xx
      if (resp.code() != 401 && resp.code() != 429 && resp.code() < 500) {
        break
      }
    }
    return requireSuccess(
      requireNotNull(lastResponse),
      "Mailbox created, but /token authentication failed",
    )
  }

  suspend fun signIn(address: String, password: String): Result<ActiveMailbox> = runCatching {
    val normalized = address.trim().lowercase(Locale.US)
    require(normalized.contains("@")) { "Enter a valid email address" }
    require(password.isNotBlank()) { "Password is required" }

    val tokenResp = api.createToken(CredentialsRequest(normalized, password))
    val tokenBody = requireSuccess(tokenResp, "Invalid Mail.tm credentials")

    val meResp = api.getMe(bearer(tokenBody.token))
    val account = requireSuccess(meResp, "Authenticated, but failed to load /me profile")

    val session = StoredSession(
      accountId = account.id,
      address = account.address,
      password = password,
      token = tokenBody.token,
    )
    sessionPrefs.saveSession(session)

    ActiveMailbox(
      accountId = account.id,
      address = account.address,
      password = password,
      token = tokenBody.token,
      quotaBytes = account.quota,
      usedBytes = account.used,
    )
  }

  suspend fun refreshAccountQuota(token: String): Result<AccountDto> = runCatching {
    val resp = api.getMe(bearer(token))
    requireSuccess(resp, "Failed to refresh account quota")
  }

  suspend fun fetchMessages(token: String): Result<List<MessageSummaryDto>> = runCatching {
    val resp = api.getMessages(bearer(token), page = 1)
    val collection = requireSuccess(resp, "Failed to load inbox messages")
    collection.members.filterNot { it.isDeleted }
  }

  suspend fun fetchMessageDetail(token: String, messageId: String): Result<MessageDetailDto> =
    runCatching {
      val resp = api.getMessageDetail(bearer(token), messageId)
      val detail = requireSuccess(resp, "Failed to fetch message body")
      if (!detail.seen) {
        // Best-effort mark seen via API Platform merge-patch
        runCatching { api.markMessageSeen(bearer(token), messageId, MarkSeenRequest(true)) }
      }
      detail.copy(seen = true)
    }

  suspend fun fetchMessageSource(token: String, messageId: String): Result<SourceDto> =
    runCatching {
      val resp = api.getMessageSource(bearer(token), messageId)
      requireSuccess(resp, "Failed to fetch raw MIME source")
    }

  suspend fun deleteMessage(token: String, messageId: String): Result<Unit> = runCatching {
    val resp = api.deleteMessage(bearer(token), messageId)
    if (!resp.isSuccessful) {
      error("Delete message failed (HTTP ${resp.code()})")
    }
  }

  suspend fun burnMailbox(token: String, accountId: String): Result<Unit> = runCatching {
    val resp = api.deleteAccount(bearer(token), accountId)
    if (!resp.isSuccessful && resp.code() != 404) {
      error("Delete account failed (HTTP ${resp.code()})")
    }
    sessionPrefs.clearSession()
  }

  suspend fun probeApiHealth(token: String?): ApiProbeResult {
    var status = 0
    var domains: List<String> = emptyList()
    var hydraValid = false
    var meValid = false
    var errorMsg: String? = null

    val latency = measureTimeMillis {
      try {
        val domainResp = api.getDomains(page = 1)
        status = domainResp.code()
        if (domainResp.isSuccessful) {
          val body = domainResp.body()
          domains = body?.members?.filter { it.isActive }?.map { it.domain }.orEmpty()
          hydraValid = body != null && (body.totalItems >= domains.size)
        } else {
          errorMsg = "HTTP ${domainResp.code()} ${domainResp.message()}"
        }

        if (!token.isNullOrBlank() && domainResp.isSuccessful) {
          val meResp = api.getMe(bearer(token))
          meValid = meResp.isSuccessful && meResp.body() != null
        }
      } catch (e: Exception) {
        errorMsg = e.localizedMessage ?: "Network error contacting api.mail.tm"
      }
    }

    return ApiProbeResult(
      reachable = status in 200..299,
      httpStatus = status,
      latencyMs = latency,
      activeDomains = domains,
      hydraEnvelopeValid = hydraValid,
      authenticatedMeValid = meValid,
      errorMessage = errorMsg,
    )
  }

  fun generateRandomUsername(): String {
    val prefixes = listOf("cipher", "vault", "relay", "shield", "orbit", "prism", "pulse", "nova")
    val prefix = prefixes[random.nextInt(prefixes.size)]
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    val suffix = (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
    return "$prefix$suffix"
  }

  fun generateRandomPassword(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    return (1..16).map { chars[random.nextInt(chars.length)] }.joinToString("")
  }

  private fun bearer(token: String): String = "Bearer $token"

  private fun <T> requireSuccess(response: Response<T>, fallbackMessage: String): T {
    if (response.isSuccessful) {
      val body = response.body()
      if (body != null) return body
    }
    val detail = response.errorBody()?.string()?.take(180)?.takeIf { it.isNotBlank() }
    val code = response.code()
    throw IllegalStateException(
      if (detail != null) "$fallbackMessage (HTTP $code): $detail"
      else "$fallbackMessage (HTTP $code)"
    )
  }
}

object MailFormatters {
  fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
  }

  fun formatIsoTimestamp(iso: String): String {
    if (iso.isBlank()) return ""
    val patterns = listOf(
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )
    for (pattern in patterns) {
      try {
        val parser = SimpleDateFormat(pattern, Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(iso) ?: continue
        val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        return formatter.format(date)
      } catch (_: Exception) {
        // Try next pattern
      }
    }
    return iso.take(16).replace("T", " ")
  }

  fun extractReadableBody(detail: MessageDetailDto): String {
    val plain = detail.text?.trim().orEmpty()
    if (plain.isNotBlank()) return plain
    val htmlJoined = detail.html.joinToString("\n").trim()
    if (htmlJoined.isBlank()) return ""
    return htmlJoined
      .replace(Regex("(?is)<style.*?</style>"), "")
      .replace(Regex("(?is)<script.*?</script>"), "")
      .replace(Regex("(?i)<br\\s*/?>"), "\n")
      .replace(Regex("(?i)</p>|</div>|</tr>|</li>"), "\n")
      .replace(Regex("<[^>]+>"), "")
      .replace("&nbsp;", " ")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace(Regex("\n{3,}"), "\n\n")
      .trim()
  }
}
