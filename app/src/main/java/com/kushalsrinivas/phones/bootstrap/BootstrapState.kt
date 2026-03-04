package com.kushalsrinivas.phones.bootstrap

sealed class BootstrapState {
    data object NotStarted : BootstrapState()
    data class Downloading(val progress: Float) : BootstrapState()
    data object Extracting : BootstrapState()
    data object InstallingPackages : BootstrapState()
    data object Ready : BootstrapState()
    data class Error(val message: String) : BootstrapState()
}
