package com.jawnnypoo.physicsbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Compose equivalent of PhysicsLayout for View-based UIs.
 *
 * `PhysicsBox` hosts child composables in a Box2D world and continuously updates
 * their x/y position + rotation.
 */
@Composable
fun PhysicsBox(
    modifier: Modifier = Modifier,
    state: PhysicsBoxState = rememberPhysicsBoxState(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    LaunchedEffect(state) {
        var lastFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                val deltaNanos = if (lastFrameNanos == 0L) 0L else frameNanos - lastFrameNanos
                lastFrameNanos = frameNanos
                // Convert to seconds, cap at 1/20s to avoid physics explosion after pause/resume
                val deltaSec = (deltaNanos / 1_000_000_000f).coerceAtMost(0.05f)
                state.step(deltaSec)
            }
        }
    }

    DisposableEffect(state) {
        onDispose {
            state.dispose()
        }
    }

    Layout(
        modifier = modifier.then(
            Modifier.pointerInput(state, state.isFlingEnabled) {
                if (!state.isFlingEnabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val hitIndex = state.findBodyIndexAt(down.position.x, down.position.y)
                        ?: return@awaitEachGesture

                    state.onDragStart(hitIndex, down.position.x, down.position.y)

                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    var activePointerId: PointerId = down.id
                    var released = false

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Main)

                        val activeChange =
                            event.changes.firstOrNull { it.id == activePointerId }
                                ?: event.changes.firstOrNull { it.pressed }
                                    ?.also { activePointerId = it.id }

                        if (activeChange == null) {
                            break
                        }

                        velocityTracker.addPosition(activeChange.uptimeMillis, activeChange.position)

                        if (!activeChange.pressed) {
                            val velocity = velocityTracker.calculateVelocity()
                            state.onDragEnd(hitIndex, velocity.x, velocity.y)
                            activeChange.consume()
                            released = true
                            break
                        }

                        if (activeChange.positionChange() != Offset.Zero) {
                            state.onDragMove(
                                hitIndex,
                                activeChange.position.x,
                                activeChange.position.y,
                            )
                            activeChange.consume()
                        }
                    }

                    if (!released) {
                        state.onDragEnd(hitIndex, 0f, 0f)
                    }
                }
            },
        ),
        content = content,
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }

        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            (placeables.maxOfOrNull { it.width } ?: 0).coerceAtLeast(constraints.minWidth)
        }

        val initialPositions = flowInitialPositions(placeables, width)

        val contentHeight = initialPositions.lastOrNull()?.let { lastPos ->
            val lastPlaceable = placeables.lastOrNull()
            if (lastPlaceable != null) {
                lastPos.y + lastPlaceable.height
            } else {
                0
            }
        } ?: 0

        val height = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            contentHeight.coerceIn(constraints.minHeight, constraints.maxHeightOrInfinity())
        }

        val childSpecs = placeables.mapIndexed { index, placeable ->
            val config = (measurables[index].parentData as? PhysicsParentData)?.config ?: PhysicsConfig()
            val initial = initialPositions[index]
            ChildSpec(
                width = placeable.width,
                height = placeable.height,
                initialX = initial.x,
                initialY = initial.y,
                config = config,
            )
        }

        state.syncWorld(
            containerWidth = width,
            containerHeight = height,
            children = childSpecs,
            density = density.density,
        )

        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val bodyState = state.bodyStateAt(index)
                val initial = initialPositions[index]

                val x = (bodyState?.x ?: initial.x.toFloat()).roundToInt()
                val y = (bodyState?.y ?: initial.y.toFloat()).roundToInt()
                val rotation = bodyState?.rotation ?: 0f

                placeable.placeWithLayer(x, y) {
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    rotationZ = rotation
                }
            }
        }
    }
}

/**
 * Remembers a [PhysicsBoxState] for use with [PhysicsBox].
 */
@Composable
fun rememberPhysicsBoxState(): PhysicsBoxState = remember { PhysicsBoxState() }

private fun flowInitialPositions(
    placeables: List<androidx.compose.ui.layout.Placeable>,
    containerWidth: Int,
): List<IntOffset> {
    if (placeables.isEmpty()) return emptyList()

    val positions = ArrayList<IntOffset>(placeables.size)

    var x = 0
    var y = 0
    var rowHeight = 0

    placeables.forEach { placeable ->
        if (x > 0 && x + placeable.width > containerWidth) {
            x = 0
            y += rowHeight
            rowHeight = 0
        }

        positions += IntOffset(x, y)

        x += placeable.width
        rowHeight = max(rowHeight, placeable.height)
    }

    return positions
}

private fun Constraints.maxHeightOrInfinity(): Int {
    return if (hasBoundedHeight) maxHeight else Int.MAX_VALUE
}
