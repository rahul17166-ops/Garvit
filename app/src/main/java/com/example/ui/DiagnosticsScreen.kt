package com.example.ui

import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.ApiProbeResult
import com.example.ui.theme.MonoCodeStyle

private data class MatrixRow(
  val label: String,
  val value: String,
  val note: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsScreen(
  uiState: MailVaultUiState,
  onRunProbe: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val toolchainRows = listOf(
    MatrixRow("Gradle Wrapper", BuildConfig.GRADLE_VERSION, "Verified for AGP 9.x & JDK 21"),
    MatrixRow("Android Gradle Plugin", BuildConfig.AGP_VERSION, "Built-in Kotlin support"),
    MatrixRow("Kotlin / Compose Compiler", BuildConfig.KOTLIN_VERSION, "org.jetbrains.kotlin.plugin.compose"),
    MatrixRow("KSP (Moshi Codegen)", BuildConfig.KSP_VERSION, "Compile-time JSON adapters"),
    MatrixRow("Jetpack Compose BOM", BuildConfig.COMPOSE_BOM_VERSION, "Material 3 + UI aligned"),
    MatrixRow("Compile / Target / Min SDK", "36 / 36 / 24", "Running API ${Build.VERSION.SDK_INT}"),
    MatrixRow("Retrofit / Moshi", "${BuildConfig.RETROFIT_VERSION} / ${BuildConfig.MOSHI_VERSION}", "Hydra JSON-LD + merge-patch"),
  )

  val ciSteps = listOf(
    "actions/checkout@v4 + Temurin JDK 21",
    "gradle/actions/setup-gradle@v4 (Gradle 9.3.1 + build cache)",
    "Auto-generates debug.keystore via keytool for debugConfig signing",
    "gradle :app:testDebugUnitTest (Moshi Hydra + Robolectric CUJ)",
    "gradle :app:assembleDebug + :app:lintDebug + artifact upload",
  )

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .testTag("diagnostics_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item(key = "diag_header") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = stringResource(R.string.diagnostics_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = stringResource(R.string.diagnostics_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item(key = "api_probe_card") {
        ApiProbeCard(
          probe = uiState.probeResult,
          isProbing = uiState.isProbingApi,
          onRunProbe = onRunProbe,
        )
      }

      item(key = "toolchain_card") {
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.BuildCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Text(
                text = stringResource(R.string.toolchain_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            toolchainRows.forEachIndexed { index, row ->
              if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = row.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = row.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                  Text(
                    text = row.value,
                    style = MonoCodeStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }
            }
          }
        }
      }

      item(key = "ci_card") {
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Column {
                Text(
                  text = stringResource(R.string.ci_section),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = ".github/workflows/android-ci.yml",
                  style = MonoCodeStyle,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            ciSteps.forEach { step ->
              Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp),
                )
                Text(
                  text = step,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }

      item(key = "maker_credit_card") {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("maker_credit_card"),
          shape = MaterialTheme.shapes.large,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(46.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "GG",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = stringResource(R.string.maker_section_title).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = stringResource(R.string.maker_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = stringResource(R.string.maker_role),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item(key = "diag_bottom_spacer") {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApiProbeCard(
  probe: ApiProbeResult?,
  isProbing: Boolean,
  onRunProbe: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("api_probe_card"),
    shape = MaterialTheme.shapes.large,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
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
          Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = stringResource(R.string.api_probe_section),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Text(
        text = "Endpoint: ${BuildConfig.MAIL_TM_BASE_URL}  •  Rate Limit: 8 QPS/IP",
        style = MonoCodeStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (probe != null) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = if (probe.reachable) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
          } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = if (probe.reachable) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = if (probe.reachable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = if (probe.reachable) {
                  "HTTP ${probe.httpStatus} OK • ${probe.latencyMs} ms"
                } else {
                  probe.errorMessage ?: "Probe failed (HTTP ${probe.httpStatus})"
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text(
              text = "Hydra JSON-LD (hydra:member): ${if (probe.hydraEnvelopeValid) "VERIFIED" else "FAILED"}",
              style = MonoCodeStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Bearer JWT (/me): ${if (probe.authenticatedMeValid) "AUTHENTICATED" else "NOT SIGNED IN"}",
              style = MonoCodeStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (probe.activeDomains.isNotEmpty()) {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                probe.activeDomains.forEach { domain ->
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                  ) {
                    Text(
                      text = "@$domain",
                      style = MonoCodeStyle,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }

      Button(
        onClick = onRunProbe,
        enabled = !isProbing,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("run_probe_button"),
        shape = MaterialTheme.shapes.small,
      ) {
        if (isProbing) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = stringResource(R.string.probing_label))
        } else {
          Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = stringResource(R.string.run_probe_button))
        }
      }
    }
  }
}
