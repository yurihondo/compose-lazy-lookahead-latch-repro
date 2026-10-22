package com.example.lookaheadlatchrepro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

const val LOG_TAG = "LookaheadLatch"

/** Where the list is hosted. The host swap is the trigger. */
enum class Host {
    /** Inside a LookaheadScope, at half width (stands in for a two-pane scaffold pane). */
    INSIDE_LOOKAHEAD,

    /** Outside any LookaheadScope, at full width (stands in for the single-pane scene). */
    OUTSIDE,

    /** Outside the original scope, but the host wraps the content in its own LookaheadScope (the workaround). */
    OUTSIDE_WITH_OWN_SCOPE,
}

/** Whether the LazyColumn composition is recreated on a host swap (state hoisted) or moved with movableContentOf. */
enum class ContentMode { RECREATE, MOVABLE }

@Composable
fun ReproList(state: LazyListState, modifier: Modifier = Modifier) {
    LazyColumn(state = state, modifier = modifier.fillMaxSize().testTag("list")) {
        items(500) { index ->
            BasicText("Item $index", Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp))
        }
    }
}

/**
 * Hosts [content] according to [host]. The branches are distinct call sites, so a plain [content] lambda is
 * recomposed fresh in each branch while a movableContentOf lambda is moved.
 */
@Composable
fun HostedList(
    host: Host,
    onHostSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (host) {
        Host.INSIDE_LOOKAHEAD -> LookaheadScope {
            Box(
                modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .testTag("host")
                    .onSizeChanged { onHostSize(it) },
            ) { content() }
        }

        Host.OUTSIDE -> Box(
            modifier
                .fillMaxSize()
                .testTag("host")
                .onSizeChanged { onHostSize(it) },
        ) { content() }

        Host.OUTSIDE_WITH_OWN_SCOPE -> Box(
            modifier
                .fillMaxSize()
                .testTag("host")
                .onSizeChanged { onHostSize(it) },
        ) { LookaheadScope { content() } }
    }
}
