package com.kushalsrinivas.phones.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kushalsrinivas.phones.process.ProcessManager
import com.kushalsrinivas.phones.ui.components.TerminalComposeView
import com.kushalsrinivas.phones.viewmodel.DashboardViewModel

@Composable
fun AgentTerminalScreen(viewModel: DashboardViewModel) {
    val session = viewModel.processManager.getSession(ProcessManager.AGENT_ID)

    if (session == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Agent not running. Start it from the Dashboard.",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    } else {
        TerminalComposeView(
            session = session,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
    }
}
