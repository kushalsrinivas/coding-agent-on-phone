package com.kushalsrinivas.phones.ui.components

import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

@Composable
fun TerminalComposeView(
    session: TerminalSession?,
    modifier: Modifier = Modifier,
) {
    if (session == null) return

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val terminalView = TerminalView(context, null)
            terminalView.setTextSize(14)
            terminalView.setTypeface(Typeface.MONOSPACE)
            terminalView.setTerminalViewClient(object : TerminalViewClient {
                override fun onScale(scale: Float): Float = scale
                override fun onSingleTapUp(e: MotionEvent) {
                    terminalView.requestFocus()
                }
                override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                override fun shouldEnforceCharBasedInput(): Boolean = true
                override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                override fun isTerminalViewSelected(): Boolean = true
                override fun copyModeChanged(copyMode: Boolean) {}
                override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean = false
                override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false
                override fun onLongPress(event: MotionEvent): Boolean = false
                override fun readControlKey(): Boolean = false
                override fun readAltKey(): Boolean = false
                override fun readShiftKey(): Boolean = false
                override fun readFnKey(): Boolean = false
                override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false
                override fun onEmulatorSet() {}
                override fun logError(tag: String?, message: String?) {}
                override fun logWarn(tag: String?, message: String?) {}
                override fun logInfo(tag: String?, message: String?) {}
                override fun logDebug(tag: String?, message: String?) {}
                override fun logVerbose(tag: String?, message: String?) {}
                override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
                override fun logStackTrace(tag: String?, e: Exception?) {}
            })
            terminalView.attachSession(session)
            terminalView.isFocusable = true
            terminalView.isFocusableInTouchMode = true

            FrameLayout(context).apply {
                addView(terminalView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
            }
        },
        update = { frameLayout ->
            val terminalView = frameLayout.getChildAt(0) as? TerminalView
            terminalView?.attachSession(session)
            terminalView?.invalidate()
        }
    )
}
