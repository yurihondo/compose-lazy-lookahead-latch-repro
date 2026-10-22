# compose-lazy-lookahead-latch-repro

`LazyListState.hasLookaheadOccurred` (androidx.compose.foundation 1.12.1, `LazyListState.kt:190` declaration, only assignment at `:625`) is a one-way latch: once a `LazyColumn` driven by a `LazyListState` has been measured inside a `LookaheadScope`, it stays `true` forever.
If the same state later drives a `LazyColumn` that is not inside any `LookaheadScope`, every measure is a non-lookahead pass and `applyMeasureResult` (`:611`) takes the approach-only branch, so `layoutInfo`, `canScrollForward`, `scrollToBeConsumed` and the scroll position are never updated and the list freezes.
This project reproduces that with a hoisted state and with `movableContentOf`, plus controls.

## Host modes (`Host`)

- `INSIDE_LOOKAHEAD`: inside a `LookaheadScope`, half width.
- `OUTSIDE`: outside any `LookaheadScope`, full width.
- `OUTSIDE_WITH_OWN_SCOPE`: outside the original scope, but the host wraps the content in its own `LookaheadScope` (workaround).

## Content modes (`ContentMode`)

- `RECREATE`: the `LazyColumn` composition is recreated on a host swap; the `LazyListState` is hoisted.
- `MOVABLE`: the composition (and the state created inside it) is moved with `movableContentOf`.

## Run

```
./gradlew :app:connectedDebugAndroidTest -PfoundationVersion=1.12.1 --continue
./gradlew :app:connectedDebugAndroidTest -PfoundationVersion=1.13.0-beta02 --continue
```

Each test asserts the correct behaviour, so a failure demonstrates the bug.
The app (`MainActivity`) logs a status line under the tag `LookaheadLatch` after every host or content change.

## Results (observed)

| Test | Pixel Fold (Android 14), 1.12.1 | Pixel 9a (Android 17), 1.12.1 |
|---|---|---|
| scrollByIsConsumedAfterLeavingLookaheadScope | FAIL (java.lang.AssertionError: expected:<300.0> but was:<-0.0>) | FAIL (java.lang.AssertionError: expected:<300.0> but was:<-0.0>) |
| layoutInfoReflectsNewHostAfterLeavingLookaheadScope | FAIL (java.lang.AssertionError: expected:<2208> but was:<1104>) | FAIL (java.lang.AssertionError: expected:<1080> but was:<540>) |
| scrollByIsConsumedWhenListStartsOutside | PASS | PASS |
| scrollByIsConsumedAfterEnteringLookaheadScope | PASS | PASS |
| scrollByIsConsumedAfterLeavingLookaheadScopeWhenHostHasOwnScope | PASS | PASS |
| scrollByIsConsumedAfterMovableContentLeavesLookaheadScope | FAIL (java.lang.AssertionError: expected:<300.0> but was:<-0.0>) | FAIL (java.lang.AssertionError: expected:<300.0> but was:<-0.0>) |

Notes:

- `ActivityScenario` requires the "Don't keep activities" developer option to be off on the device.
- foundation 1.13.0-beta02 was not run because its AAR metadata requires compileSdk 37.1, which is not installed here. The latch code is unchanged on androidx-main at 5901d750e2 (2026-10-21): `LazyListState.kt:221` declaration, `:665` branch, `:679-680` only assignment.
