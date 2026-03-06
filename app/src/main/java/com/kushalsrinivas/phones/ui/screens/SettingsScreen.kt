package com.kushalsrinivas.phones.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kushalsrinivas.phones.bootstrap.BootstrapState
import com.kushalsrinivas.phones.viewmodel.DashboardViewModel
import com.kushalsrinivas.phones.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dashboardViewModel: DashboardViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val botToken by settingsViewModel.botToken.collectAsState()
    val agentCommand by settingsViewModel.agentCommand.collectAsState()
    val bootstrapState by dashboardViewModel.bootstrapManager.state.collectAsState()

    val allowedUserIds by settingsViewModel.securityConfig.allowedUserIds.collectAsState(initial = emptySet())
    val allowedDirs by settingsViewModel.securityConfig.allowedDirectories.collectAsState(initial = setOf("projects/"))
    val rateLimit by settingsViewModel.securityConfig.rateLimitPerMinute.collectAsState(initial = 30)

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // Telegram Bot
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Telegram Bot", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = botToken,
                    onValueChange = { settingsViewModel.setBotToken(it) },
                    label = { Text("Bot Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        // Commands
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Commands", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = agentCommand,
                    onValueChange = { settingsViewModel.setAgentCommand(it) },
                    label = { Text("Agent startup command") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Text(
                    text = "Bot: managed by pi-tele (npm install -g pi-tele → pi-tele setup → pi-tele start)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Security
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Security", style = MaterialTheme.typography.titleMedium)

                var userIdsText by remember { mutableStateOf(allowedUserIds.joinToString(", ")) }
                OutlinedTextField(
                    value = userIdsText,
                    onValueChange = {
                        userIdsText = it
                        settingsViewModel.setAllowedUserIds(it)
                    },
                    label = { Text("Allowed Telegram User IDs (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                var dirsText by remember { mutableStateOf(allowedDirs.joinToString(", ")) }
                OutlinedTextField(
                    value = dirsText,
                    onValueChange = {
                        dirsText = it
                        settingsViewModel.setAllowedDirectories(it)
                    },
                    label = { Text("Allowed directories (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                var rateLimitText by remember { mutableStateOf(rateLimit.toString()) }
                OutlinedTextField(
                    value = rateLimitText,
                    onValueChange = {
                        rateLimitText = it
                        it.toIntOrNull()?.let { limit -> settingsViewModel.setRateLimit(limit) }
                    },
                    label = { Text("Rate limit (commands/minute)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }

        // Bootstrap
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Environment", style = MaterialTheme.typography.titleMedium)

                val statusText = when (bootstrapState) {
                    is BootstrapState.Ready -> "Ready"
                    is BootstrapState.Error -> "Error: ${(bootstrapState as BootstrapState.Error).message}"
                    is BootstrapState.Downloading -> "Downloading..."
                    is BootstrapState.Extracting -> "Extracting..."
                    is BootstrapState.InstallingPackages -> "Installing packages..."
                    is BootstrapState.NotStarted -> "Not initialized"
                }
                Text("Bootstrap: $statusText", style = MaterialTheme.typography.bodyMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                dashboardViewModel.bootstrapManager.installPackages("nodejs", "python")
                            }
                        },
                        enabled = bootstrapState is BootstrapState.Ready,
                    ) {
                        Text("Install Node + Python")
                    }
                }
            }
        }
    }
}
