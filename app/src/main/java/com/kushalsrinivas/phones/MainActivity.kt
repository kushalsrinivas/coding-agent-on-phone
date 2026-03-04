package com.kushalsrinivas.phones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kushalsrinivas.phones.ui.theme.PhoneAgentTheme
import com.kushalsrinivas.phones.ui.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneAgentTheme {
                AppNavHost()
            }
        }
    }
}
