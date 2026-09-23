package com.vaibhav.relive.ui.screens

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import com.vaibhav.relive.ui.theme.ReliveTheme
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The layered-sheet mechanism shared by every surface built as *a backdrop with a timeline riding
 * over it*: Home over its welcome block and Rediscover row (ADR-0061), and custom timeline detail
 * over its cover photo (ADR-0062).
 *
 * Such a surface has three resting places along one axis, and the sheet never stops between them:
 *
 *   expanded  the backdrop owns the whole viewport, the feed parked below the bottom edge
 *   resting   the backdrop on top, the feed starting below it
 *   focused   the feed covering everything
 *
 * Only the middle-to-bottom stretch is list scrolling. Above `resting` the list is already at its
 * top, so the leftover pull drives [BackdropExpansionState.expansionPx] instead: the feed is pushed
 * off the bottom, and the next downward gesture slides it back up from that edge. There is still
 * exactly one scroll container and one scroll position; expansion is a layer offset on top.
 */
@Stable
class BackdropExpansionState(
    val behavior: BackdropBehavior = BackdropBehavior.Standard,
) {
    /** How far the sheet is pushed past the bottom edge. Zero at `resting`, max when `expanded`. */
    var expansionPx: Float by mutableFloatStateOf(0f)

    /** Height of the content area the sheet travels inside, measured by the backdrop. */
    var viewportHeightPx: Int by mutableIntStateOf(0)

    /** Height of the backdrop at rest: the welcome block on Home, the cover photo on a timeline. */
    var backdropHeightPx: Int by mutableIntStateOf(0)

    /** Home's in-flight spring. A new user drag cancels it before changing [expansionPx]. */
    internal var activeSettlement: Job? = null

    /** Expanding past this would push the backdrop's own bottom edge off screen. */
    val maxExpansionPx: Float
        get() = (viewportHeightPx - backdropHeightPx).coerceAtLeast(0).toFloat()

    /** Current expansion independent of viewport size: zero at rest and one fully expanded. */
    val progress: Float
        get() = if (maxExpansionPx == 0f) {
            0f
        } else {
            (expansionPx / maxExpansionPx).coerceIn(0f, 1f)
        }
}

/** Selects Home's stretch visuals and physics; Standard preserves the original behavior. */
enum class BackdropBehavior {
    Standard,
    HomeStretch,
}

/** Smooth interpolation with zero velocity at both ends. */
internal fun smoothstep(value: Float): Float {
    val progress = value.coerceIn(0f, 1f)
    return progress * progress * (3f - 2f * progress)
}

/** Backdrop opacity for a normalized expansion [progress]. */
internal fun backdropOpacity(
    progress: Float,
    behavior: BackdropBehavior = BackdropBehavior.Standard,
): Float = when (behavior) {
    BackdropBehavior.Standard -> 1f
    BackdropBehavior.HomeStretch -> 1f - 0.18f * smoothstep(progress)
}

/** Backdrop scale for a normalized expansion [progress]. */
internal fun backdropScale(
    progress: Float,
    behavior: BackdropBehavior = BackdropBehavior.Standard,
): Float = when (behavior) {
    BackdropBehavior.Standard -> 1f
    BackdropBehavior.HomeStretch -> 1f + 0.04f * smoothstep(progress)
}

/** Normalized backdrop position for a normalized expansion [progress]. */
internal fun backdropPosition(
    progress: Float,
    behavior: BackdropBehavior = BackdropBehavior.Standard,
): Float = when (behavior) {
    BackdropBehavior.Standard -> progress.coerceIn(0f, 1f)
    BackdropBehavior.HomeStretch -> smoothstep(progress)
}

@Composable
internal fun rememberBackdropExpansionState(): BackdropExpansionState =
    remember { BackdropExpansionState() }

/** Fling speed, in pixels per second, past which the gesture's direction decides where it lands. */
private const val SETTLE_VELOCITY = 320f

/**
 * How much slower the backdrop travels than the sheet riding over it. Zero would pin it outright,
 * which reads as a static wallpaper; a small amount keeps it feeling like a layer of the same
 * surface that the timeline is covering.
 */
internal const val BACKDROP_PARALLAX = 0.35f

/**
 * Drives [state] from whatever the feed could not consume at its own top edge, and settles to one
 * end or the other when the gesture finishes.
 *
 * Deliberately remembered against the state object alone: every changing quantity is read live from
 * snapshot state inside the callbacks, so a mid-gesture measurement change cannot swap the
 * connection out from under the fling.
 */
@Composable
internal fun rememberBackdropExpansionConnection(
    state: BackdropExpansionState,
): NestedScrollConnection {
    val motion = ReliveTheme.motion
    val settleDurationMillis = motion.durations.standardMillis
    val settleEasing = motion.easings.standard

    // Give back anything the backdrop can no longer hold, so a rotation or an inset change cannot
    // strand the feed below the bottom edge.
    LaunchedEffect(state) {
        snapshotFlow { state.maxExpansionPx }.collect { max ->
            if (state.expansionPx > max) state.expansionPx = max
        }
    }

    return remember(state, settleDurationMillis, settleEasing) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) state.cancelActiveSettlement()
                // Parking the feed again comes before it moves at all, so one continuous downward
                // gesture closes the backdrop and then scrolls the timeline.
                if (available.y >= 0f || state.expansionPx <= 0f) return Offset.Zero
                val consumed = min(-available.y, state.expansionPx)
                state.expansionPx -= consumed
                return Offset(0f, -consumed)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput) state.cancelActiveSettlement()
                // Only reached once the list itself has nothing left to give at the top, which is
                // exactly the "scrolling more upward" this state answers.
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                val response = if (state.behavior == BackdropBehavior.HomeStretch) {
                    homeExpansionResponseRatio(state.progress)
                } else {
                    1f
                }
                val applied = min(available.y * response, state.maxExpansionPx - state.expansionPx)
                if (applied <= 0f) return Offset.Zero
                state.expansionPx += applied
                // Home owns the full overscroll even though resistance makes its rendered travel
                // smaller. Returning only the rendered delta would leak the remainder upward.
                return Offset(0f, if (state.behavior == BackdropBehavior.HomeStretch) available.y else applied)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val max = state.maxExpansionPx
                if (state.expansionPx <= 0f || max <= 0f) return Velocity.Zero
                val target = settleTargetFor(
                    expansionPx = state.expansionPx,
                    maxExpansionPx = max,
                    velocityY = available.y,
                )
                if (state.behavior == BackdropBehavior.HomeStretch) {
                    animateHomeExpansionTo(
                        state = state,
                        target = target,
                        initialVelocity = homeRenderedVelocity(available.y, state.progress),
                    )
                } else {
                    animateExpansionTo(
                        state = state,
                        target = target,
                        durationMillis = settleDurationMillis,
                        easing = settleEasing,
                    )
                }
                // Consume the fling: the gesture ended on this layer, not in the feed.
                return available
            }
        }
    }
}

/** Direct until 80%, then eased resistance down to 30% response at full expansion. */
internal fun homeExpansionResponseRatio(progress: Float): Float =
    1f - (1f - HOME_LIMIT_RESPONSE) * smoothstep(
        (progress - HOME_RESISTANCE_START) / (1f - HOME_RESISTANCE_START),
    )

/** Collapse velocity stays direct; only outward velocity inherits the current resistance. */
internal fun homeRenderedVelocity(velocityY: Float, progress: Float): Float =
    if (velocityY > 0f) velocityY * homeExpansionResponseRatio(progress) else velocityY

private const val HOME_RESISTANCE_START = 0.8f
private const val HOME_LIMIT_RESPONSE = 0.3f

internal fun BackdropExpansionState.cancelActiveSettlement() {
    activeSettlement?.cancel()
    activeSettlement = null
}

/**
 * Which end of the expansion range a lifted gesture lands on. A decisive fling wins outright;
 * otherwise the sheet goes wherever it is already closer to.
 */
internal fun settleTargetFor(
    expansionPx: Float,
    maxExpansionPx: Float,
    velocityY: Float,
): Float = when {
    velocityY > SETTLE_VELOCITY -> maxExpansionPx
    velocityY < -SETTLE_VELOCITY -> 0f
    expansionPx >= maxExpansionPx / 2f -> maxExpansionPx
    else -> 0f
}

/** Runs [state] to [target] on the app's standard settle curve. */
internal suspend fun animateExpansionTo(
    state: BackdropExpansionState,
    target: Float,
    durationMillis: Int,
    easing: androidx.compose.animation.core.Easing,
) {
    animate(
        initialValue = state.expansionPx,
        targetValue = target,
        animationSpec = tween(durationMillis, easing = easing),
    ) { value, _ -> state.expansionPx = value }
}

/** Home's velocity-preserving, critically damped settlement. */
private suspend fun animateHomeExpansionTo(
    state: BackdropExpansionState,
    target: Float,
    initialVelocity: Float,
) = coroutineScope {
    state.cancelActiveSettlement()
    val settlement = launch {
        animate(
            initialValue = state.expansionPx,
            targetValue = target,
            initialVelocity = initialVelocity,
            animationSpec = spring(
                dampingRatio = 1f,
                // Slower than the standard sheet settle so the stretched Home header has time to
                // read as an elastic surface while still ending without a bounce.
                stiffness = 260f,
            ),
        ) { value, _ -> state.expansionPx = value.coerceIn(0f, state.maxExpansionPx) }
    }
    state.activeSettlement = settlement
    try {
        settlement.join()
    } finally {
        if (state.activeSettlement === settlement) state.activeSettlement = null
    }
}

/**
 * How far the sheet has ridden up over the backdrop, in pixels, from the list's own scroll
 * position. Once the first item — the transparent window reserving the backdrop's space — has
 * scrolled away entirely, the backdrop is fully covered.
 */
@Composable
internal fun rememberScrolledIntoBackdrop(
    listState: LazyListState,
    backdropHeightPx: () -> Int,
): State<Int> = remember(listState) {
    derivedStateOf {
        if (listState.firstVisibleItemIndex == 0) {
            listState.firstVisibleItemScrollOffset
        } else {
            backdropHeightPx()
        }
    }
}

/**
 * Below `resting` the sheet obeys the same rule as above it: whichever direction the gesture was
 * travelling when it ended wins, so the sheet never stops half over the backdrop.
 */
@Composable
internal fun BackdropSettleEffect(
    listState: LazyListState,
    backdropHeightPx: Int,
    scrolledIntoBackdrop: () -> Int,
) {
    val motion = ReliveTheme.motion
    var settleTowardFocused by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var previous = scrolledIntoBackdrop()
        snapshotFlow(scrolledIntoBackdrop).collect { current ->
            if (current != previous) settleTowardFocused = current > previous
            previous = current
        }
    }
    LaunchedEffect(listState, backdropHeightPx) {
        if (backdropHeightPx <= 0) return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling || listState.firstVisibleItemIndex != 0) return@collect
                val offset = listState.firstVisibleItemScrollOffset
                if (offset <= 0 || offset >= backdropHeightPx) return@collect
                listState.animateScrollBy(
                    value = ((if (settleTowardFocused) backdropHeightPx else 0) - offset).toFloat(),
                    animationSpec = tween(
                        durationMillis = motion.durations.standardMillis,
                        easing = motion.easings.standard,
                    ),
                )
            }
    }
}

/**
 * Hands Home's partially focused sheet straight from the finger into its final resting place.
 * Intercepting before the list starts a separate fling avoids the stopped frame that would
 * otherwise appear between LazyColumn's fling and [BackdropSettleEffect]'s endpoint correction.
 */
@Composable
internal fun rememberHomeFocusSettleConnection(
    listState: LazyListState,
    backdropHeightPx: () -> Int,
): NestedScrollConnection = remember(listState) {
    object : NestedScrollConnection {
        private var lastDragDeltaY = 0f

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && available.y != 0f) {
                lastDragDeltaY = available.y
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val height = backdropHeightPx()
            val offset = listState.firstVisibleItemScrollOffset
            if (height <= 0 || listState.firstVisibleItemIndex != 0 || offset <= 0 || offset >= height) {
                return Velocity.Zero
            }
            val target = focusedSettleTarget(
                offsetPx = offset,
                backdropHeightPx = height,
                velocityY = available.y,
                lastDragDeltaY = lastDragDeltaY,
            )
            animateHomeFocusTo(
                listState = listState,
                targetOffsetPx = target,
                initialVelocityPxPerSecond = -available.y,
            )
            return available
        }
    }
}

/** Finger direction wins, including a very small upward pull; distance is only a zero-motion fallback. */
internal fun focusedSettleTarget(
    offsetPx: Int,
    backdropHeightPx: Int,
    velocityY: Float,
    lastDragDeltaY: Float,
): Int = when {
    velocityY < 0f || (velocityY == 0f && lastDragDeltaY < 0f) -> backdropHeightPx
    velocityY > 0f || (velocityY == 0f && lastDragDeltaY > 0f) -> 0
    offsetPx >= backdropHeightPx / 2 -> backdropHeightPx
    else -> 0
}

/** A velocity-preserving, no-bounce continuation of Home's focus gesture. */
private suspend fun animateHomeFocusTo(
    listState: LazyListState,
    targetOffsetPx: Int,
    initialVelocityPxPerSecond: Float,
) {
    listState.scroll {
        var previous = listState.firstVisibleItemScrollOffset.toFloat()
        animate(
            initialValue = previous,
            targetValue = targetOffsetPx.toFloat(),
            initialVelocity = initialVelocityPxPerSecond,
            animationSpec = spring(dampingRatio = 1f, stiffness = 420f),
        ) { value, _ ->
            val consumed = scrollBy(value - previous)
            previous += consumed
        }
    }
}

/**
 * Lets a child ignore an ancestor's horizontal padding so the sheet's edge and shadow — and the
 * backdrop behind it — reach the screen edges. Without it the sheet reads as a floating card rather
 * than a surface being covered.
 */
internal fun Modifier.bleedHorizontal(padding: Dp): Modifier = layout { measurable, constraints ->
    val extra = padding.roundToPx() * 2
    val widened = constraints.copy(
        minWidth = constraints.maxWidth + extra,
        maxWidth = constraints.maxWidth + extra,
    )
    val placeable = measurable.measure(widened)
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-padding.roundToPx(), 0)
    }
}
