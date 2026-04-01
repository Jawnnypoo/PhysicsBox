package com.jawnnypoo.physicsbox.sample

import androidx.compose.runtime.Composable
import com.jawnnypoo.physicsbox.PhysicsBoxState

// TODO: Could use CoreMotion for accelerometer support on iOS
actual val isTiltSupported: Boolean = false

@Composable
actual fun PlatformGravityEffect(enabled: Boolean, state: PhysicsBoxState) {
    // No sensor support on iOS yet
}
