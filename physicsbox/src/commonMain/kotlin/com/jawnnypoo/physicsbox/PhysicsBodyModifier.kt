package com.jawnnypoo.physicsbox

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density

internal data class PhysicsParentData(
    val config: PhysicsConfig,
)

private data class PhysicsBodyParentDataModifier(
    val data: PhysicsParentData,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = data
}

/**
 * Sets custom physics properties for a child inside [PhysicsBox].
 *
 * If not applied, child bodies use [PhysicsConfig] defaults.
 */
fun Modifier.physicsBody(
    shape: Shape = Shape.RECTANGLE,
    bodyType: PhysicsBodyType = PhysicsBodyType.DYNAMIC,
    friction: Float = 0.3f,
    restitution: Float = 0.2f,
    density: Float = 0.2f,
    fixedRotation: Boolean = false,
): Modifier {
    val config = PhysicsConfig(
        shape = shape,
        bodyType = bodyType,
        friction = friction,
        restitution = restitution,
        density = density,
        fixedRotation = fixedRotation,
    )
    return then(PhysicsBodyParentDataModifier(PhysicsParentData(config)))
}
