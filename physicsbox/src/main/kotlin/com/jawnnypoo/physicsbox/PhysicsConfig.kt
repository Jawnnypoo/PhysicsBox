package com.jawnnypoo.physicsbox

/**
 * Configuration for an individual physics body within a [PhysicsBox].
 *
 * Apply to a child composable via [physicsBody]:
 * ```
 * Box(modifier = Modifier.physicsBody(shape = Shape.CIRCLE)) {
 *     // circular collision shape
 * }
 * ```
 *
 * @param shape        Collision shape — [Shape.RECTANGLE] or [Shape.CIRCLE].
 * @param bodyType     How the body participates in the simulation.
 * @param friction     Coulomb friction coefficient (0 = ice, 1 = rough). Default 0.3.
 * @param restitution  Bounciness (0 = no bounce, 1 = perfect bounce). Default 0.2.
 * @param density      Mass per unit area. Higher = heavier. Default 0.2.
 * @param fixedRotation If true, the body will not rotate from collisions.
 */
data class PhysicsConfig(
    val shape: Shape = Shape.RECTANGLE,
    val bodyType: PhysicsBodyType = PhysicsBodyType.DYNAMIC,
    val friction: Float = 0.3f,
    val restitution: Float = 0.2f,
    val density: Float = 0.2f,
    val fixedRotation: Boolean = false,
)
