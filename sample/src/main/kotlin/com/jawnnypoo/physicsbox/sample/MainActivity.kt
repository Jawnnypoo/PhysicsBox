package com.jawnnypoo.physicsbox.sample

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jawnnypoo.physicsbox.PhysicsBox
import com.jawnnypoo.physicsbox.PhysicsBodyType
import com.jawnnypoo.physicsbox.PhysicsBoxState
import com.jawnnypoo.physicsbox.Shape
import com.jawnnypoo.physicsbox.physicsBody
import com.jawnnypoo.physicsbox.rememberPhysicsBoxState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DemoScreen()
            }
        }
    }
}

/** Describes a single physics body added by the user. */
private data class BodyDef(
    val id: Int,
    val label: String,
    val shape: Shape,
    val width: Dp,
    val height: Dp,
    val color: Color,
    val restitution: Float,
    val density: Float,
    val emoji: String? = null,
)

private val bodyTemplates = listOf(
    // Small bouncy circle
    { id: Int ->
        BodyDef(id, "$id", Shape.CIRCLE, 28.dp, 28.dp, Color(0xFF1E88E5), 0.7f, 0.2f)
    },
    // Small rectangle
    { id: Int ->
        BodyDef(id, "$id", Shape.RECTANGLE, 36.dp, 24.dp, Color(0xFF43A047), 0.3f, 0.4f)
    },
    // Medium heavy circle
    { id: Int ->
        BodyDef(id, "$id", Shape.CIRCLE, 36.dp, 36.dp, Color(0xFFF4511E), 0.2f, 0.8f)
    },
    // Tiny super-bouncy ball
    { id: Int ->
        BodyDef(id, "$id", Shape.CIRCLE, 20.dp, 20.dp, Color(0xFFFFB300), 0.9f, 0.1f)
    },
    // Short plank
    { id: Int ->
        BodyDef(id, "$id", Shape.RECTANGLE, 48.dp, 18.dp, Color(0xFF8E24AA), 0.25f, 0.5f)
    },
    // Small bouncy square
    { id: Int ->
        BodyDef(id, "$id", Shape.RECTANGLE, 26.dp, 26.dp, Color(0xFF00ACC1), 0.6f, 0.3f)
    },
)

@Composable
private fun DemoScreen() {
    val physicsState = rememberPhysicsBoxState()

    var gravityX by remember { mutableFloatStateOf(0f) }
    var gravityY by remember { mutableFloatStateOf(PhysicsBoxState.EARTH_GRAVITY) }
    var physicsEnabled by remember { mutableStateOf(true) }
    var boundsEnabled by remember { mutableStateOf(true) }
    var flingEnabled by remember { mutableStateOf(true) }
    var tiltEnabled by remember { mutableStateOf(false) }

    val bodies = remember { mutableStateListOf<BodyDef>() }
    var nextId by remember { mutableIntStateOf(1) }

    physicsState.isPhysicsEnabled = physicsEnabled
    physicsState.hasBounds = boundsEnabled
    physicsState.isFlingEnabled = flingEnabled

    if (!tiltEnabled) {
        physicsState.setGravity(gravityX, gravityY)
    }

    SensorGravityEffect(enabled = tiltEnabled, state = physicsState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101217))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "PhysicsBox Sample",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        // ── Add / Clear row ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    val template = bodyTemplates[nextId % bodyTemplates.size]
                    bodies.add(template(nextId))
                    nextId++
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                modifier = Modifier.weight(1f),
            ) {
                Text("+ Add")
            }

            Button(
                onClick = {
                    repeat(5) {
                        val template = bodyTemplates[nextId % bodyTemplates.size]
                        bodies.add(template(nextId))
                        nextId++
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                modifier = Modifier.weight(1f),
            ) {
                Text("+ Add 5")
            }

            OutlinedButton(
                onClick = {
                    bodies.clear()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear", color = Color.White)
            }
        }

        Text(
            text = "Bodies: ${bodies.size}",
            color = Color(0xFF8899AA),
            style = MaterialTheme.typography.bodySmall,
        )

        // ── Toggles ──────────────────────────────────────
        ControlRow("Physics", physicsEnabled) { physicsEnabled = it }
        ControlRow("Bounds", boundsEnabled) { boundsEnabled = it }
        ControlRow("Fling", flingEnabled) { flingEnabled = it }
        ControlRow("Tilt / Gimbal", tiltEnabled) { tiltEnabled = it }

        AnimatedVisibility(visible = !tiltEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "Gravity X: ${"%.1f".format(gravityX)}", color = Color.White)
                Slider(
                    value = gravityX,
                    valueRange = -24.8f..24.8f,
                    onValueChange = { gravityX = it },
                )
                Text(text = "Gravity Y: ${"%.1f".format(gravityY)}", color = Color.White)
                Slider(
                    value = gravityY,
                    valueRange = -24.8f..24.8f,
                    onValueChange = { gravityY = it },
                )
            }
        }

        AnimatedVisibility(visible = tiltEnabled) {
            Text(
                text = "Tilt your device to control gravity",
                color = Color(0xFF8899AA),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Physics playground ────────────────────────────
        PhysicsBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color(0xFF2A3445), RoundedCornerShape(16.dp))
                .background(Color(0xFF151A23), RoundedCornerShape(16.dp))
                .padding(12.dp),
            state = physicsState,
        ) {
            bodies.forEach { bodyDef ->
                val isCircle = bodyDef.shape == Shape.CIRCLE
                Box(
                    modifier = Modifier
                        .size(bodyDef.width, bodyDef.height)
                        .physicsBody(
                            shape = bodyDef.shape,
                            restitution = bodyDef.restitution,
                            friction = 0.4f,
                            density = bodyDef.density,
                        )
                        .background(
                            color = bodyDef.color,
                            shape = if (isCircle) CircleShape else RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = bodyDef.label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Feeds device accelerometer / gravity sensor data directly into [PhysicsBoxState.setGravity].
 */
@Composable
private fun SensorGravityEffect(
    enabled: Boolean,
    state: PhysicsBoxState,
) {
    val context = LocalContext.current

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                state.setGravity(
                    x = -event.values[0],
                    y = event.values[1],
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

@Composable
private fun ControlRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Color.White)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
