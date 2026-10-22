package com.example.lookaheadlatchrepro

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LookaheadLatchTest {
    @get:Rule
    val rule = createComposeRule()

    private fun setHosted(
        initial: Host,
        state: LazyListState,
        hostSize: MutableState<IntSize>,
    ): MutableState<Host> {
        val host = mutableStateOf(initial)
        rule.setContent {
            HostedList(host = host.value, onHostSize = { hostSize.value = it }) { ReproList(state) }
        }
        rule.waitForIdle()
        return host
    }

    private fun scroll(state: LazyListState): Float {
        val consumed = rule.runOnIdle { runBlocking { state.scrollBy(300f) } }
        rule.waitForIdle()
        return consumed
    }

    @Test
    fun scrollByIsConsumedAfterLeavingLookaheadScope() {
        val state = LazyListState()
        val host = setHosted(Host.INSIDE_LOOKAHEAD, state, mutableStateOf(IntSize.Zero))
        host.value = Host.OUTSIDE
        rule.waitForIdle()
        val consumed = scroll(state)
        assertEquals(300f, consumed, 0.5f)
        assertTrue(state.firstVisibleItemIndex > 0)
    }

    @Test
    fun layoutInfoReflectsNewHostAfterLeavingLookaheadScope() {
        val state = LazyListState()
        val hostSize = mutableStateOf(IntSize.Zero)
        val host = setHosted(Host.INSIDE_LOOKAHEAD, state, hostSize)
        host.value = Host.OUTSIDE
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(hostSize.value.width, state.layoutInfo.viewportSize.width) }
    }

    @Test
    fun scrollByIsConsumedWhenListStartsOutside() {
        val state = LazyListState()
        setHosted(Host.OUTSIDE, state, mutableStateOf(IntSize.Zero))
        val consumed = scroll(state)
        assertEquals(300f, consumed, 0.5f)
        assertTrue(state.firstVisibleItemIndex > 0)
    }

    @Test
    fun scrollByIsConsumedAfterEnteringLookaheadScope() {
        val state = LazyListState()
        val host = setHosted(Host.OUTSIDE, state, mutableStateOf(IntSize.Zero))
        host.value = Host.INSIDE_LOOKAHEAD
        rule.waitForIdle()
        val consumed = scroll(state)
        assertEquals(300f, consumed, 0.5f)
    }

    @Test
    fun scrollByIsConsumedAfterLeavingLookaheadScopeWhenHostHasOwnScope() {
        val state = LazyListState()
        val host = setHosted(Host.INSIDE_LOOKAHEAD, state, mutableStateOf(IntSize.Zero))
        host.value = Host.OUTSIDE_WITH_OWN_SCOPE
        rule.waitForIdle()
        val consumed = scroll(state)
        assertEquals(300f, consumed, 0.5f)
    }

    @Test
    fun scrollByIsConsumedAfterMovableContentLeavesLookaheadScope() {
        val holder = mutableStateOf<LazyListState?>(null)
        val movable = movableContentOf {
            val s = rememberLazyListState()
            SideEffect { holder.value = s }
            ReproList(s)
        }
        val host = mutableStateOf(Host.INSIDE_LOOKAHEAD)
        rule.setContent {
            HostedList(host = host.value, onHostSize = {}) { movable() }
        }
        rule.waitForIdle()
        val before = holder.value!!
        host.value = Host.OUTSIDE
        rule.waitForIdle()
        val after = holder.value!!
        assertSame(before, after)
        val consumed = scroll(after)
        assertEquals(300f, consumed, 0.5f)
    }
}
