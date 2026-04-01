package com.jawnnypoo.physicsbox

/**
 * The shape used for the physics body's collision boundary.
 */
enum class Shape {
    /**
     * A rectangular collision shape matching the composable's width and height.
     */
    RECTANGLE,

    /**
     * A circular collision shape. The radius is derived from the larger of width or height.
     */
    CIRCLE
}
