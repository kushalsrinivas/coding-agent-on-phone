package com.kushalsrinivas.phones.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kushalsrinivas.phones.bootstrap.BootstrapState
import com.kushalsrinivas.phones.process.ProcessManager
import com.kushalsrinivas.phones.ui.components.LogViewer
import com.kushalsrinivas.phones.ui.components.ProcessStatusCard
import com.kushalsrinivas.phones.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    settingsBotToken: String,
    settingsAgentCommand: String,
) {
    val bootstrapState by viewModel.bootstrapManager.state.collectAsState()
    val processes by viewModel.processManager.processes.collectAsState()
    val logLines by viewModel.logLines.collectAsState()
    val uptimes by viewModel.uptimes.collectAsState()

    val agentInfo = processes[ProcessManager.AGENT_ID]
    val botInfo = processes[ProcessManager.BOT_ID]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Bootstrap status
        when (val state = bootstrapState) {
            is BootstrapState.NotStarted -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Initializing...", style = MaterialTheme.typography.bodySmall)
            }
            is BootstrapState.Downloading -> {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Downloading bootstrap: ${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            is BootstrapState.Extracting -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Extracting environment...", style = MaterialTheme.typography.bodySmall)
            }
            is BootstrapState.InstallingPackages -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Installing packages...", style = MaterialTheme.typography.bodySmall)
            }
            is BootstrapState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Bootstrap error: ${state.message}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            is BootstrapState.Ready -> { /* Show nothing, proceed to process cards */ }
        }

        if (bootstrapState is BootstrapState.Ready) {
            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val anyRunning = processes.any { it.value.isRunning }

                Button(
                    onClick = {
                        viewModel.startAgent(settingsAgentCommand)
                        viewModel.startBot(settingsBotToken)
                    },
                    enabled = !anyRunning,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Start Both")
                }

                OutlinedButton(
                    onClick = { viewModel.stopAll() },
                    enabled = anyRunning,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Process status cards
            ProcessStatusCard(
                label = "Coding Agent",
                isRunning = agentInfo?.isRunning == true,
                uptimeText = uptimes[ProcessManager.AGENT_ID] ?: "00:00:00",
                onStart = { viewModel.startAgent(settingsAgentCommand) },
                onStop = { viewModel.stopAgent() },
                onRestart = {
                    viewModel.stopAgent()
                    viewModel.startAgent(settingsAgentCommand)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProcessStatusCard(
                label = "Telegram Bot",
                isRunning = botInfo?.isRunning == true,
                uptimeText = uptimes[ProcessManager.BOT_ID] ?: "00:00:00",
                onStart = { viewModel.startBot(settingsBotToken) },
                onStop = { viewModel.stopBot() },
                onRestart = {
                    viewModel.stopBot()
                    viewModel.startBot(settingsBotToken)
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Log viewer
            Text(
                text = "Logs",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LogViewer(
                lines = logLines,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}
