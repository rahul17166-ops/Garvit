package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.MailFormatters
import com.example.ui.theme.MonoAddressStyle
import com.example.ui.theme.MonoCodeStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdentityScreen(
  uiState: MailVaultUiState,
  onGenerateUsername: () -> String,
  onGeneratePassword: () -> String,
  onCreateCustom: (username: String, domain: String, password: String) -> Unit,
  onCreateRandom: () -> Unit,
  onSignIn: (address: String, password: String) -> Unit,
  onBurnCurrent: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var customUsername by rememberSaveable { mutableStateOf("") }
  var selectedDomain by rememberSaveable(uiState.domains) {
    mutableStateOf(uiState.domains.firstOrNull()?.domain.orEmpty())
  }
  var customPassword by rememberSaveable { mutableStateOf(onGeneratePassword()) }

  var signInEmail by rememberSaveable { mutableStateOf("") }
  var signInPassword by rememberSaveable { mutableStateOf("") }
  var showCurrentPassword by rememberSaveable { mutableStateOf(false) }
  var showBurnDialog by rememberSaveable { mutableStateOf(false) }

  if (selectedDomain.isBlank() && uiState.domains.isNotEmpty()) {
    selectedDomain = uiState.domains.first().domain
  }

  if (showBurnDialog) {
    AlertDialog(
      onDismissRequest = { showBurnDialog = false },
      title = { Text(text = stringResource(R.string.burn_confirm_title)) },
      text = { Text(text = stringResource(R.string.burn_confirm_body)) },
      confirmButton = {
        Button(
          onClick = {
            showBurnDialog = false
            onBurnCurrent()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
          modifier = Modifier.testTag("confirm_burn_button"),
        ) {
          Text(text = stringResource(R.string.confirm_burn))
        }
      },
      dismissButton = {
        TextButton(onClick = { showBurnDialog = false }) {
          Text(text = stringResource(R.string.cancel_action))
        }
      },
    )
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item(key = "title") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = stringResource(R.string.identity_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Provision disposable addresses or authenticate via POST /accounts and POST /token.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      uiState.mailbox?.let { mb ->
        item(key = "current_session") {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = stringResource(R.string.account_quota_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
              )
              SelectionContainer {
                Text(
                  text = mb.address,
                  style = MonoAddressStyle,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Saved Password",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  SelectionContainer {
                    Text(
                      text = if (showCurrentPassword) mb.password else "••••••••••••••••",
                      style = MonoCodeStyle,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
                IconButton(
                  onClick = { showCurrentPassword = !showCurrentPassword },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(
                    imageVector = if (showCurrentPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Toggle password visibility",
                  )
                }
              }
              Text(
                text = "Account ID: ${mb.accountId}",
                style = MonoCodeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "Storage: ${MailFormatters.formatBytes(mb.usedBytes)} / ${MailFormatters.formatBytes(mb.quotaBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item(key = "create_custom") {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(R.string.create_custom_title),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
              value = customUsername,
              onValueChange = { customUsername = it.filter { ch -> !ch.isWhitespace() } },
              label = { Text(stringResource(R.string.username_label)) },
              placeholder = { Text(stringResource(R.string.username_hint)) },
              leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
              trailingIcon = {
                IconButton(
                  onClick = { customUsername = onGenerateUsername() },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Filled.Casino, contentDescription = "Random username")
                }
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_username_input"),
            )

            Text(
              text = stringResource(R.string.domain_label),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.domains.isEmpty()) {
              Text(
                text = "Fetching live domains from GET /domains…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                uiState.domains.forEach { domainDto ->
                  FilterChip(
                    selected = selectedDomain == domainDto.domain,
                    onClick = { selectedDomain = domainDto.domain },
                    label = { Text(text = "@${domainDto.domain}", style = MonoCodeStyle) },
                  )
                }
              }
            }

            OutlinedTextField(
              value = customPassword,
              onValueChange = { customPassword = it },
              label = { Text(stringResource(R.string.password_label)) },
              leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
              trailingIcon = {
                IconButton(
                  onClick = { customPassword = onGeneratePassword() },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Filled.Autorenew, contentDescription = "Regenerate password")
                }
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_password_input"),
            )

            Button(
              onClick = { onCreateCustom(customUsername, selectedDomain, customPassword) },
              enabled = !uiState.isProvisioning &&
                customUsername.length >= 3 &&
                customPassword.length >= 8 &&
                selectedDomain.isNotBlank(),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("create_custom_button"),
              shape = MaterialTheme.shapes.small,
            ) {
              Text(text = stringResource(R.string.create_address_button))
            }

            OutlinedButton(
              onClick = onCreateRandom,
              enabled = !uiState.isProvisioning,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("quick_random_button"),
              shape = MaterialTheme.shapes.small,
            ) {
              Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = stringResource(R.string.quick_random_button))
            }
          }
        }
      }

      item(key = "sign_in") {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(R.string.sign_in_existing_title),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
              value = signInEmail,
              onValueChange = { signInEmail = it },
              label = { Text(stringResource(R.string.email_address_label)) },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("sign_in_email_input"),
            )
            OutlinedTextField(
              value = signInPassword,
              onValueChange = { signInPassword = it },
              label = { Text(stringResource(R.string.password_label)) },
              visualTransformation = PasswordVisualTransformation(),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("sign_in_password_input"),
            )
            Button(
              onClick = { onSignIn(signInEmail, signInPassword) },
              enabled = !uiState.isProvisioning && signInEmail.contains("@") && signInPassword.isNotBlank(),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sign_in_button"),
              shape = MaterialTheme.shapes.small,
            ) {
              Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = stringResource(R.string.sign_in_button))
            }
          }
        }
      }

      if (uiState.mailbox != null) {
        item(key = "burn_zone") {
          OutlinedButton(
            onClick = { showBurnDialog = true },
            enabled = !uiState.isProvisioning,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("burn_mailbox_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.burn_mailbox_button))
          }
        }
      }

      item(key = "bottom_pad") {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}
