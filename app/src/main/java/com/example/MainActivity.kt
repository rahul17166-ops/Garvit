package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DiagnosticsScreen
import com.example.ui.IdentityScreen
import com.example.ui.InboxScreen
import com.example.ui.MailVaultViewModel
import com.example.ui.MainDestination
import com.example.ui.MessageDetailScreen
import com.example.ui.theme.MailVaultTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MailVaultTheme(darkTheme = true) {
        MailVaultApp()
      }
    }
  }
}

private data class NavItemSpec(
  val destination: MainDestination,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

@Composable
fun MailVaultApp(
  viewModel: MailVaultViewModel = viewModel(
    factory = MailVaultViewModel.factory(LocalContext.current)
  ),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val copiedMsg = stringResource(R.string.copied_snackbar)

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  val unreadCount = remember(uiState.messages) {
    uiState.messages.count { !it.seen }
  }

  val navItems = remember {
    listOf(
      NavItemSpec(
        destination = MainDestination.INBOX,
        labelRes = R.string.nav_inbox,
        selectedIcon = Icons.Filled.Inbox,
        unselectedIcon = Icons.Outlined.Inbox,
        testTag = "nav_inbox",
      ),
      NavItemSpec(
        destination = MainDestination.IDENTITY,
        labelRes = R.string.nav_identity,
        selectedIcon = Icons.Filled.AlternateEmail,
        unselectedIcon = Icons.Outlined.AlternateEmail,
        testTag = "nav_identity",
      ),
      NavItemSpec(
        destination = MainDestination.DIAGNOSTICS,
        labelRes = R.string.nav_diagnostics,
        selectedIcon = Icons.Filled.VerifiedUser,
        unselectedIcon = Icons.Outlined.VerifiedUser,
        testTag = "nav_diagnostics",
      ),
    )
  }

  val isViewingDetail = uiState.selectedMessageId != null

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useRail && !isViewingDetail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
          ) {
            navItems.forEach { item ->
              val selected = uiState.selectedTab == item.destination
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(item.destination) },
                icon = {
                  NavIconWithBadge(
                    item = item,
                    selected = selected,
                    unreadCount = if (item.destination == MainDestination.INBOX) unreadCount else 0,
                  )
                },
                label = { Text(text = stringResource(item.labelRes)) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useRail && !isViewingDetail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.windowInsetsPadding(WindowInsets(0)),
          ) {
            navItems.forEach { item ->
              val selected = uiState.selectedTab == item.destination
              NavigationRailItem(
                selected = selected,
                onClick = { viewModel.selectTab(item.destination) },
                icon = {
                  NavIconWithBadge(
                    item = item,
                    selected = selected,
                    unreadCount = if (item.destination == MainDestination.INBOX) unreadCount else 0,
                  )
                },
                label = { Text(text = stringResource(item.labelRes)) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }

        if (isViewingDetail) {
          val msgId = uiState.selectedMessageId!!
          MessageDetailScreen(
            messageId = msgId,
            detail = uiState.selectedMessageDetail,
            isLoadingDetail = uiState.isLoadingDetail,
            rawMimeSource = uiState.rawMimeSource,
            isLoadingSource = uiState.isLoadingSource,
            onBack = { viewModel.closeMessage() },
            onDelete = { viewModel.deleteMessage(it) },
            onLoadSource = { viewModel.loadMessageSource(it) },
            modifier = Modifier.weight(1f),
          )
        } else {
          when (uiState.selectedTab) {
            MainDestination.INBOX -> InboxScreen(
              uiState = uiState,
              onCopyAddress = { address ->
                copyToClipboard(context, address)
                viewModel.showSnackbar(copiedMsg)
              },
              onRotateAddress = { viewModel.rotateRandomMailbox() },
              onRefresh = { viewModel.refreshInbox(silent = false) },
              onRetryBootstrap = { viewModel.bootstrapSession() },
              onOpenMessage = { viewModel.openMessage(it) },
              modifier = Modifier.weight(1f),
            )

            MainDestination.IDENTITY -> IdentityScreen(
              uiState = uiState,
              onGenerateUsername = { viewModel.generateUsernameSuggestion() },
              onGeneratePassword = { viewModel.generatePasswordSuggestion() },
              onCreateCustom = { u, d, p -> viewModel.createCustomMailbox(u, d, p) },
              onCreateRandom = { viewModel.rotateRandomMailbox() },
              onSignIn = { e, p -> viewModel.signInExisting(e, p) },
              onBurnCurrent = { viewModel.burnCurrentMailbox() },
              modifier = Modifier.weight(1f),
            )

            MainDestination.DIAGNOSTICS -> DiagnosticsScreen(
              uiState = uiState,
              onRunProbe = { viewModel.runApiProbe() },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NavIconWithBadge(
  item: NavItemSpec,
  selected: Boolean,
  unreadCount: Int,
) {
  val iconVector = if (selected) item.selectedIcon else item.unselectedIcon
  val label = stringResource(item.labelRes)
  if (unreadCount > 0) {
    BadgedBox(
      badge = {
        Badge(
          containerColor = MaterialTheme.colorScheme.tertiary,
          contentColor = MaterialTheme.colorScheme.onTertiary,
        ) {
          Text(text = if (unreadCount > 99) "99+" else unreadCount.toString())
        }
      }
    ) {
      Icon(imageVector = iconVector, contentDescription = label)
    }
  } else {
    Icon(imageVector = iconVector, contentDescription = label)
  }
}

private fun copyToClipboard(context: Context, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
  clipboard?.setPrimaryClip(ClipData.newPlainText("MailVault Address", text))
}
