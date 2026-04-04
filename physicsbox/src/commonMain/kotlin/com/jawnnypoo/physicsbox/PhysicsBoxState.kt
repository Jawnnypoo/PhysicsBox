package com.jawnnypoo.physicsbox

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.joaomcl.boks2d.core.Body
import io.github.joaomcl.boks2d.core.BodyDef
import io.github.joaomcl.boks2d.core.BodyType
import io.github.joaomcl.boks2d.core.World
import io.github.joaomcl.boks2d.core.WorldDef
import io.github.joaomcl.boks2d.math.Rot
import io.github.joaomcl.boks2d.math.Transform
import io.github.joaomcl.boks2d.math.Vec2
import io.github.joaomcl.boks2d.shapes.Circle
import io.github.joaomcl.boks2d.shapes.Polygon
import io.github.joaomcl.boks2d.shapes.ShapeDef
import kotlin.math.PI
import kotlin.math.max

/**
 * State holder for [PhysicsBox].
 *
 * Controls gravity, bounds, fling behavior, and simulation tuning.
 */
@Stable
class PhysicsBoxState {

    companion object {
        const val NO_GRAVITY = 0.0f
        const val MOON_GRAVITY = 1.6f
        const val EARTH_GRAVITY = 9.8f
        const val JUPITER_GRAVITY = 24.8f

        private const val DEFAULT_BOUNDS_DP = 20f
        private const val DEFAULT_PIXELS_PER_METER_DP = 20f
        private const val DEFAULT_SUB_STEP_COUNT = 4
    }

    /** Enables/disables physics processing. */
    var isPhysicsEnabled by mutableStateOf(true)

    /** Enables/disables drag + fling interactions. */
    var isFlingEnabled by mutableStateOf(true)

    /** Enables/disables world bounds around the container. */
    var hasBounds by mutableStateOf(true)

    /** X gravity (positive right, negative left). */
    var gravityX by mutableFloatStateOf(0f)

    /** Y gravity (positive down, negative up). */
    var gravityY by mutableFloatStateOf(EARTH_GRAVITY)

    /** Number of boks2d sub-steps per step. */
    var subStepCount: Int = DEFAULT_SUB_STEP_COUNT

    /**
     * Pixels per meter conversion factor.
     * If <= 0, defaults to 20dp converted to pixels.
     */
    var pixelsPerMeter by mutableFloatStateOf(0f)

    /**
     * Bounds thickness in pixels.
     * If <= 0, defaults to 20dp converted to pixels.
     */
    var boundsSizePx by mutableFloatStateOf(0f)

    private var currentPixelsPerMeter = 20f
    private var world: World? = null

    private val bodyStates = mutableListOf<BodyUiState>()

    // Tracked for incremental sync — only full-recreate when these change.
    private var lastContainerWidth = -1
    private var lastContainerHeight = -1
    private var lastHasBounds = true
    private var lastResolvedBoundsSize = 0f
    private var lastResolvedPPM = 0f

    private var draggingIndex: Int? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var dragOriginalType: BodyType? = null

    /** Current number of physics bodies in the world. */
    val bodyCount: Int get() = bodyStates.size

    internal fun bodyStateAt(index: Int): BodyUiState? = bodyStates.getOrNull(index)

    fun setGravity(x: Float, y: Float) {
        gravityX = x
        gravityY = y
        world?.gravity = Vec2(x, y)
    }

    internal fun syncWorld(
        containerWidth: Int,
        containerHeight: Int,
        children: List<ChildSpec>,
        density: Float,
    ) {
        val resolvedPixelsPerMeter =
            if (pixelsPerMeter > 0f) pixelsPerMeter else DEFAULT_PIXELS_PER_METER_DP * density
        val resolvedBoundsSize = if (boundsSizePx > 0f) boundsSizePx else DEFAULT_BOUNDS_DP * density

        currentPixelsPerMeter = resolvedPixelsPerMeter

        val needsFullRecreation = world == null
                || containerWidth != lastContainerWidth
                || containerHeight != lastContainerHeight
                || hasBounds != lastHasBounds
                || resolvedBoundsSize != lastResolvedBoundsSize
                || resolvedPixelsPerMeter != lastResolvedPPM

        lastContainerWidth = containerWidth
        lastContainerHeight = containerHeight
        lastHasBounds = hasBounds
        lastResolvedBoundsSize = resolvedBoundsSize
        lastResolvedPPM = resolvedPixelsPerMeter

        if (needsFullRecreation) {
            fullRecreate(containerWidth, containerHeight, children, resolvedPixelsPerMeter, resolvedBoundsSize)
            return
        }

        // --- Incremental sync: preserve existing bodies, add/remove as needed ---
        world?.gravity = Vec2(gravityX, gravityY)

        // Cancel any drag if body count changes
        if (children.size != bodyStates.size) {
            draggingIndex = null
            dragOriginalType = null
        }

        // Remove excess bodies from the end
        val currentWorld = world ?: return
        while (bodyStates.size > children.size) {
            val removed = bodyStates.removeAt(bodyStates.lastIndex)
            currentWorld.destroyBody(removed.body)
        }

        // Add new bodies (children beyond current count)
        for (i in bodyStates.size until children.size) {
            val spec = children[i]
            val body = createBody(
                world = currentWorld,
                spec = spec,
                pixelsPerMeter = resolvedPixelsPerMeter,
            )
            bodyStates += BodyUiState(
                body = body,
                width = spec.width,
                height = spec.height,
                x = spec.initialX.toFloat(),
                y = spec.initialY.toFloat(),
                rotation = radiansToDegrees(body.angle),
            )
        }
    }

    private fun fullRecreate(
        containerWidth: Int,
        containerHeight: Int,
        children: List<ChildSpec>,
        resolvedPixelsPerMeter: Float,
        resolvedBoundsSize: Float,
    ) {
        draggingIndex = null
        dragOriginalType = null

        // Destroy old world if it exists
        world?.destroy()

        val newWorld = World(WorldDef(gravity = Vec2(gravityX, gravityY)))
        if (hasBounds) {
            createBounds(
                world = newWorld,
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                boundsSizePx = resolvedBoundsSize,
                pixelsPerMeter = resolvedPixelsPerMeter,
            )
        }

        bodyStates.clear()
        children.forEach { child ->
            val body = createBody(
                world = newWorld,
                spec = child,
                pixelsPerMeter = resolvedPixelsPerMeter,
            )
            bodyStates += BodyUiState(
                body = body,
                width = child.width,
                height = child.height,
                x = child.initialX.toFloat(),
                y = child.initialY.toFloat(),
                rotation = radiansToDegrees(body.angle),
            )
        }

        world = newWorld
    }

    fun step(deltaSeconds: Float) {
        val world = world ?: return
        if (!isPhysicsEnabled || deltaSeconds <= 0f) return

        world.gravity = Vec2(gravityX, gravityY)
        world.step(deltaSeconds, subStepCount)

        bodyStates.forEachIndexed { index, bodyUiState ->
            if (draggingIndex == index) return@forEachIndexed

            val body = bodyUiState.body
            bodyUiState.x = metersToPixels(body.position.x, currentPixelsPerMeter) - bodyUiState.width / 2f
            bodyUiState.y = metersToPixels(body.position.y, currentPixelsPerMeter) - bodyUiState.height / 2f
            bodyUiState.rotation = radiansToDegrees(body.angle)
        }
    }

    internal fun findBodyIndexAt(x: Float, y: Float): Int? {
        for (i in bodyStates.indices.reversed()) {
            val state = bodyStates[i]
            if (state.body.type == BodyType.Static) continue

            val left = state.x
            val top = state.y
            val right = left + state.width
            val bottom = top + state.height
            if (x in left..right && y in top..bottom) {
                return i
            }
        }
        return null
    }

    internal fun onDragStart(index: Int, touchX: Float, touchY: Float) {
        val state = bodyStates.getOrNull(index) ?: return
        val body = state.body
        if (body.type == BodyType.Static) return

        draggingIndex = index
        dragOriginalType = body.type

        val centerX = state.x + state.width / 2f
        val centerY = state.y + state.height / 2f
        dragOffsetX = touchX - centerX
        dragOffsetY = touchY - centerY

        body.type = BodyType.Kinematic
        body.linearVelocity = Vec2(0f, 0f)
        body.angularVelocity = 0f
    }

    internal fun onDragMove(index: Int, touchX: Float, touchY: Float) {
        if (draggingIndex != index) return

        val state = bodyStates.getOrNull(index) ?: return
        val body = state.body

        val centerX = touchX - dragOffsetX
        val centerY = touchY - dragOffsetY

        body.transform = Transform(
            position = Vec2(
                pixelsToMeters(centerX, currentPixelsPerMeter),
                pixelsToMeters(centerY, currentPixelsPerMeter),
            ),
            rotation = body.rotation,
        )

        state.x = centerX - state.width / 2f
        state.y = centerY - state.height / 2f
    }

    internal fun onDragEnd(index: Int, velocityXPxPerSec: Float, velocityYPxPerSec: Float) {
        if (draggingIndex != index) return

        val state = bodyStates.getOrNull(index) ?: return
        val body = state.body

        val restoredType = dragOriginalType ?: BodyType.Dynamic
        body.type = restoredType

        if (restoredType == BodyType.Dynamic) {
            body.linearVelocity = Vec2(
                pixelsToMeters(velocityXPxPerSec, currentPixelsPerMeter),
                pixelsToMeters(velocityYPxPerSec, currentPixelsPerMeter),
            )
        }

        body.isAwake = true

        draggingIndex = null
        dragOriginalType = null
        dragOffsetX = 0f
        dragOffsetY = 0f
    }

    fun dispose() {
        draggingIndex = null
        dragOriginalType = null
        bodyStates.clear()
        world?.destroy()
        world = null
        lastContainerWidth = -1
        lastContainerHeight = -1
    }

    private fun createBounds(
        world: World,
        containerWidth: Int,
        containerHeight: Int,
        boundsSizePx: Float,
        pixelsPerMeter: Float,
    ) {
        val w = containerWidth.toFloat()
        val h = containerHeight.toFloat()
        val bs = boundsSizePx

        // Top — center sits above the container so inner face is at y=0
        createBound(
            world = world,
            centerXPx = w / 2f,
            centerYPx = -bs / 2f,
            widthPx = w + bs * 2f,
            heightPx = bs,
            pixelsPerMeter = pixelsPerMeter,
        )
        // Bottom — center sits below the container so inner face is at y=h
        createBound(
            world = world,
            centerXPx = w / 2f,
            centerYPx = h + bs / 2f,
            widthPx = w + bs * 2f,
            heightPx = bs,
            pixelsPerMeter = pixelsPerMeter,
        )
        // Left — inner face at x=0
        createBound(
            world = world,
            centerXPx = -bs / 2f,
            centerYPx = h / 2f,
            widthPx = bs,
            heightPx = h + bs * 2f,
            pixelsPerMeter = pixelsPerMeter,
        )
        // Right — inner face at x=w
        createBound(
            world = world,
            centerXPx = w + bs / 2f,
            centerYPx = h / 2f,
            widthPx = bs,
            heightPx = h + bs * 2f,
            pixelsPerMeter = pixelsPerMeter,
        )
    }

    private fun createBound(
        world: World,
        centerXPx: Float,
        centerYPx: Float,
        widthPx: Float,
        heightPx: Float,
        pixelsPerMeter: Float,
    ) {
        val body = world.createBody(
            BodyDef(
                type = BodyType.Static,
                position = Vec2(
                    pixelsToMeters(centerXPx, pixelsPerMeter),
                    pixelsToMeters(centerYPx, pixelsPerMeter),
                ),
            )
        )

        body.createPolygonShape(
            ShapeDef(),
            Polygon.makeBox(
                pixelsToMeters(widthPx / 2f, pixelsPerMeter),
                pixelsToMeters(heightPx / 2f, pixelsPerMeter),
            ),
        )
    }

    private fun createBody(
        world: World,
        spec: ChildSpec,
        pixelsPerMeter: Float,
    ): Body {
        val body = world.createBody(
            BodyDef(
                type = spec.config.bodyType.toEngineType(),
                fixedRotation = spec.config.fixedRotation,
                position = Vec2(
                    pixelsToMeters(spec.initialX + spec.width / 2f, pixelsPerMeter),
                    pixelsToMeters(spec.initialY + spec.height / 2f, pixelsPerMeter),
                ),
            )
        )

        val shapeDef = ShapeDef(
            friction = spec.config.friction,
            restitution = spec.config.restitution,
            density = spec.config.density,
        )

        when (spec.config.shape) {
            Shape.RECTANGLE -> body.createPolygonShape(
                shapeDef,
                Polygon.makeBox(
                    pixelsToMeters(spec.width / 2f, pixelsPerMeter),
                    pixelsToMeters(spec.height / 2f, pixelsPerMeter),
                ),
            )

            Shape.CIRCLE -> body.createCircleShape(
                shapeDef,
                Circle(
                    center = Vec2.Zero,
                    radius = pixelsToMeters(max(spec.width, spec.height) / 2f, pixelsPerMeter),
                ),
            )
        }

        return body
    }

    private fun pixelsToMeters(px: Float, pixelsPerMeter: Float): Float = px / pixelsPerMeter

    private fun metersToPixels(meters: Float, pixelsPerMeter: Float): Float = meters * pixelsPerMeter

    private fun radiansToDegrees(radians: Float): Float = (radians / PI.toFloat()) * 180f
}

private fun PhysicsBodyType.toEngineType(): BodyType = when (this) {
    PhysicsBodyType.DYNAMIC -> BodyType.Dynamic
    PhysicsBodyType.STATIC -> BodyType.Static
    PhysicsBodyType.KINEMATIC -> BodyType.Kinematic
}

@Stable
internal data class ChildSpec(
    val width: Int,
    val height: Int,
    val initialX: Int,
    val initialY: Int,
    val config: PhysicsConfig,
)

internal class BodyUiState(
    val body: Body,
    val width: Int,
    val height: Int,
    x: Float,
    y: Float,
    rotation: Float,
) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var rotation by mutableFloatStateOf(rotation)
}
