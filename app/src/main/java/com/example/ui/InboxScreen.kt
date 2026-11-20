package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.remote.MessageSummaryDto
import com.example.data.repository.ActiveMailbox
import com.example.data.repository.MailFormatters
import com.example.ui.theme.MonoAddressStyle

@Composable
fun InboxScreen(
  uiState: MailVaultUiState,
  onCopyAddress: (String) -> Unit,
  onRotateAddress: () -> Unit,
  onRefresh: () -> Unit,
  onRetryBootstrap: () -> Unit,
  onOpenMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .testTag("inbox_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item(key = "header") {
        InboxTopHeader(
          isRefreshing = uiState.isRefreshingInbox,
          onRefresh = onRefresh,
        )
      }

      item(key = "hero_mailbox") {
        ActiveMailboxHeroCard(
          mailbox = uiState.mailbox,
          isProvisioning = uiState.isProvisioning,
          onCopy = { uiState.mailbox?.address?.let(onCopyAddress) },
          onRotate = onRotateAddress,
        )
      }

      if (uiState.errorBanner != null) {
        item(key = "error_banner") {
          ErrorBannerCard(
            message = uiState.errorBanner,
            onRetry = if (uiState.mailbox == null) onRetryBootstrap else onRefresh,
          )
        }
      }

      item(key = "messages_section_header") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = stringResource(R.string.messages_header),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = uiState.messages.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
              )
            }
          }
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
              )
              Text(
                text = stringResource(R.string.auto_sync_active),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }

      if (uiState.messages.isEmpty() && !uiState.isProvisioning) {
        item(key = "empty_state") {
          EmptyInboxCard(onRefresh = onRefresh)
        }
      } else {
        items(items = uiState.messages, key = { it.id }) { message ->
          MessageRowCard(
            message = message,
            onClick = { onOpenMessage(message.id) },
          )
        }
      }

      item(key = "bottom_spacer") {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun InboxTopHeader(
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = stringResource(R.string.inbox_title),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
        }
      }
      Column {
        Text(
          text = stringResource(R.string.inbox_title),
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = stringResource(R.string.inbox_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    IconButton(
      onClick = onRefresh,
      enabled = !isRefreshing,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .testTag("refresh_inbox_button"),
    ) {
      if (isRefreshing) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          strokeWidth = 2.5.dp,
          color = MaterialTheme.colorScheme.primary,
        )
      } else {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = stringResource(R.string.refresh_inbox),
          tint = MaterialTheme.colorScheme.onBackground,
        )
      }
    }
  }
}

@Composable
private fun ActiveMailboxHeroCard(
  mailbox: ActiveMailbox?,
  isProvisioning: Boolean,
  onCopy: () -> Unit,
  onRotate: () -> Unit,
) {
  val gradientBrush = Brush.linearGradient(
    colors = listOf(
      MaterialTheme.colorScheme.surface,
      MaterialTheme.colorScheme.surfaceVariant,
    )
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("active_mailbox_card"),
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(gradientBrush)
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .size(9.dp)
              .clip(CircleShape)
              .background(
                if (mailbox != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary
              )
          )
          Text(
            text = stringResource(R.string.active_address_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Text(
          text = "api.mail.tm",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(end = 4.dp),
        )
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
          if (isProvisioning) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = stringResource(R.string.creating_mailbox),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            SelectionContainer {
              Text(
                text = mailbox?.address ?: "—",
                style = MonoAddressStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("active_address_text"),
              )
            }
          }
        }
      }

      AnimatedVisibility(
        visible = mailbox != null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        mailbox?.let { mb ->
          val ratio = if (mb.quotaBytes > 0) {
            (mb.usedBytes.toFloat() / mb.quotaBytes.toFloat()).coerceIn(0f, 1f)
          } else {
            0f
          }
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Quota Used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${MailFormatters.formatBytes(mb.usedBytes)} / ${MailFormatters.formatBytes(mb.quotaBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            LinearProgressIndicator(
              progress = { ratio },
              modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onCopy,
          enabled = mailbox != null && !isProvisioning,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("copy_address_button"),
          shape = MaterialTheme.shapes.small,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        ) {
          Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.copy_address),
            style = MaterialTheme.typography.labelLarge,
          )
        }

        FilledTonalButton(
          onClick = onRotate,
          enabled = !isProvisioning,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("rotate_address_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(
            imageVector = Icons.Filled.Autorenew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.rotate_address),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun ErrorBannerCard(
  message: String,
  onRetry: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("error_banner_card"),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.WarningAmber,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onErrorContainer,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.weight(1f),
      )
      FilledTonalButton(
        onClick = onRetry,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("retry_button"),
      ) {
        Text(text = stringResource(R.string.retry_action))
      }
    }
  }
}

@Composable
private fun EmptyInboxCard(onRefresh: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("empty_inbox_card"),
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_inbox_1795178213429),
        contentDescription = stringResource(R.string.empty_inbox_title),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(172.dp)
          .clip(MaterialTheme.shapes.medium),
      )
      Text(
        text = stringResource(R.string.empty_inbox_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.empty_inbox_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      FilledTonalButton(
        onClick = onRefresh,
        modifier = Modifier
          .height(48.dp)
          .testTag("empty_state_refresh_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.refresh_inbox))
      }
    }
  }
}

@Composable
private fun MessageRowCard(
  message: MessageSummaryDto,
  onClick: () -> Unit,
) {
  val senderName = message.from.name.ifBlank {
    message.from.address.ifBlank { stringResource(R.string.unknown_sender) }
  }
  val monogram = senderName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
  val subject = message.subject.ifBlank { stringResource(R.string.no_subject) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .testTag("message_card_${message.id}"),
    shape = MaterialTheme.shapes.medium,
    border = BorderStroke(
      width = if (!message.seen) 1.5.dp else 1.dp,
      color = if (!message.seen) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
      } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
      },
    ),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Surface(
        shape = CircleShape,
        color = if (!message.seen) {
          MaterialTheme.colorScheme.primaryContainer
        } else {
          MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = monogram,
            style = MaterialTheme.typography.titleMedium,
            color = if (!message.seen) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = senderName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = MailFormatters.formatIsoTimestamp(message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (!message.seen) {
            Icon(
              imageVector = Icons.Filled.MarkEmailUnread,
              contentDescription = stringResource(R.string.unread_badge),
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(14.dp),
            )
          }
          Text(
            text = subject,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = if (!message.seen) FontWeight.Bold else FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (message.intro.isNotBlank()) {
          Text(
            text = message.intro,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(
          modifier = Modifier.padding(top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = MailFormatters.formatBytes(message.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (message.hasAttachments) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.AttachFile,
                contentDescription = stringResource(R.string.attachments_label),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = stringResource(R.string.attachments_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
          }
        }
      }
    }
  }
}
