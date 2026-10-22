package com.example.lookaheadlatchrepro

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ReproScreen() {
    var host by remember { mutableStateOf(Host.INSIDE_LOOKAHEAD) }
    var contentMode by remember { mutableStateOf(ContentMode.RECREATE) }
    var hostSize by remember { mutableStateOf(IntSize.Zero) }
    var lastConsumed by remember { mutableStateOf<Float?>(null) }
    val scope = rememberCoroutineScope()

    val hoistedState = rememberLazyListState()

    // The state lives inside the movable content on purpose: that is how Navigation 3 entries behave.
    val movableStateHolder = remember { mutableStateOf<LazyListState?>(null) }
    val movableContent = remember {
        movableContentOf {
            val s = rememberLazyListState()
            SideEffect { movableStateHolder.value = s }
            ReproList(s)
        }
    }

    val currentState: LazyListState? =
        if (contentMode == ContentMode.RECREATE) hoistedState else movableStateHolder.value

    LaunchedEffect(host, contentMode) {
        withFrameNanos { }
        withFrameNanos { }
        Log.i(LOG_TAG, statusLine(host, contentMode, currentState, hostSize, lastConsumed))
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        BasicText(
            "host: $host  (tap to cycle)",
            Modifier
                .testTag("host-toggle")
                .clickable { host = Host.entries[(host.ordinal + 1) % Host.entries.size] }
                .padding(12.dp),
        )
        BasicText(
            "content: $contentMode  (tap to toggle)",
            Modifier
                .testTag("content-toggle")
                .clickable {
                    contentMode = ContentMode.entries[(contentMode.ordinal + 1) % ContentMode.entries.size]
                }
                .padding(12.dp),
        )
        BasicText(
            "scrollBy(300f)",
            Modifier
                .testTag("scroll")
                .clickable {
                    scope.launch {
                        val s = currentState ?: return@launch
                        val c = s.scrollBy(300f)
                        lastConsumed = c
                        Log.i(LOG_TAG, statusLine(host, contentMode, s, hostSize, c))
                    }
                }
                .padding(12.dp),
        )
        BasicText(
            statusLine(host, contentMode, currentState, hostSize, lastConsumed),
            Modifier.testTag("status").padding(12.dp),
        )
        HostedList(host = host, onHostSize = { hostSize = it }, modifier = Modifier.weight(1f)) {
            if (contentMode == ContentMode.RECREATE) ReproList(hoistedState) else movableContent()
        }
    }
}

private fun statusLine(
    host: Host,
    contentMode: ContentMode,
    state: LazyListState?,
    hostSize: IntSize,
    lastConsumed: Float?,
): String {
    val id = state?.let { Integer.toHexString(System.identityHashCode(it)) } ?: "null"
    val info = state?.layoutInfo
    return "host=$host content=$contentMode state=$id " +
        "first=${state?.firstVisibleItemIndex} offset=${state?.firstVisibleItemScrollOffset} " +
        "viewport=${info?.viewportSize?.width}x${info?.viewportSize?.height} " +
        "hostSize=${hostSize.width}x${hostSize.height} " +
        "canScrollForward=${state?.canScrollForward} consumed=${lastConsumed ?: "null"}"
}
