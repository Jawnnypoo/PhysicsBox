package com.jawnnypoo.physicsbox

import org.jbox2d.dynamics.BodyType

/**
 * Controls how a physics body behaves in the simulation.
 */
enum class PhysicsBodyType {
    /**
     * Fully simulated — affected by gravity, forces, and collisions.
     */
    DYNAMIC,

    /**
     * Immovable — not affected by forces or collisions, but other bodies collide with it.
     * Use for walls, floors, or fixed obstacles.
     */
    STATIC,

    /**
     * Moves only via explicit velocity — not affected by gravity or collisions,
     * but other bodies react to it. Use for platforms or conveyor belts.
     */
    KINEMATIC
}

internal fun PhysicsBodyType.toBox2D(): BodyType = when (this) {
    PhysicsBodyType.DYNAMIC -> BodyType.DYNAMIC
    PhysicsBodyType.STATIC -> BodyType.STATIC
    PhysicsBodyType.KINEMATIC -> BodyType.KINEMATIC
}
