package com.jawnnypoo.physicsbox.sample

import androidx.compose.runtime.Composable
import com.jawnnypoo.physicsbox.PhysicsBoxState

/** Whether this platform supports device tilt/accelerometer for gravity control. */
expect val isTiltSupported: Boolean

/** Platform-specific gravity effect. No-op on platforms without sensors. */
@Composable
expect fun PlatformGravityEffect(enabled: Boolean, state: PhysicsBoxState)
