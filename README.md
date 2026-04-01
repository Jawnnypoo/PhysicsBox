# PhysicsBox
Compose Multiplatform physics-enabled layout powered by [boks2d](https://github.com/joaomcl/boks2d). Simply add composables, enable physics, and watch them fall! The Compose equivalent of [PhysicsLayout](https://github.com/Jawnnypoo/PhysicsLayout).

[![Maven Central](https://img.shields.io/maven-central/v/com.jawnnypoo/physicsbox)](https://central.sonatype.com/artifact/com.jawnnypoo/physicsbox)

## Supported Platforms
- Android
- JVM (Desktop)
- iOS (arm64, simulator arm64)

## Gradle

### Multiplatform
For Compose Multiplatform projects, add the dependency in your `commonMain` source set:
```kotlin
commonMain.dependencies {
    implementation("com.jawnnypoo:physicsbox:latest.release.here")
}
```

### Android
For Android-only projects:
```kotlin
dependencies {
    implementation("com.jawnnypoo:physicsbox-android:latest.release.here")
}
```

### JVM (Desktop)
For JVM-only projects:
```kotlin
dependencies {
    implementation("com.jawnnypoo:physicsbox-jvm:latest.release.here")
}
```

## Basic Usage
Wrap your composables in a `PhysicsBox` and they'll start falling with gravity:
```kotlin
val state = rememberPhysicsBoxState()

PhysicsBox(
    modifier = Modifier
        .fillMaxWidth()
        .height(320.dp),
    state = state,
) {
    Text("Hello world, I have physics!")
}
```

## PhysicsBoxState Configuration
You can customize the behavior of your `PhysicsBox` through `PhysicsBoxState`:
```kotlin
val state = rememberPhysicsBoxState().apply {
    isPhysicsEnabled = true      // Determines if physics will be applied (Default true)
    isFlingEnabled = true        // Enables drag + fling interactions (Default true)
    hasBounds = true             // Determines if the layout has bounds on edges (Default true)
    gravityX = 0f                // X gravity — positive is right, negative is left (Default 0)
    gravityY = PhysicsBoxState.EARTH_GRAVITY  // Y gravity — positive is down, negative is up (Default 9.8)
    subStepCount = 4             // boks2d sub-steps per step — higher is more accurate (Default 4)
}
```

Gravity constants are provided for convenience:
- `PhysicsBoxState.NO_GRAVITY` — 0.0
- `PhysicsBoxState.MOON_GRAVITY` — 1.6
- `PhysicsBoxState.EARTH_GRAVITY` — 9.8
- `PhysicsBoxState.JUPITER_GRAVITY` — 24.8

## Custom Physics Configuration
Each child composable can have its own physics configuration via `Modifier.physicsBody(...)`. This defines its collision shape, mass, bounciness, and other physics properties:
```kotlin
Box(
    modifier = Modifier
        .size(48.dp)
        .physicsBody(
            shape = Shape.CIRCLE,
            bodyType = PhysicsBodyType.DYNAMIC,
            friction = 0.3f,
            restitution = 0.2f,
            density = 0.2f,
            fixedRotation = false,
        )
        .background(Color.Red, CircleShape)
)
```

- `shape` — Collision shape: `Shape.RECTANGLE` or `Shape.CIRCLE` (Default RECTANGLE)
- `bodyType` — `PhysicsBodyType.DYNAMIC`, `STATIC`, or `KINEMATIC` (Default DYNAMIC)
- `friction` — Coulomb friction coefficient, 0 = ice, 1 = rough (Default 0.3)
- `restitution` — Bounciness, 0 = no bounce, 1 = perfect bounce (Default 0.2)
- `density` — Mass per unit area, higher = heavier (Default 0.2)
- `fixedRotation` — If true, the body will not rotate from collisions (Default false)

This is especially useful for circular composables, since the default collision shape for all children is `RECTANGLE`. For most rectangular views, the defaults work fine.

## Tilt / Gimbal Gravity
Hook your device's sensor data into gravity for a tilt-controlled experience:
```kotlin
state.setGravity(x = sensorX, y = sensorY)
```
This updates boks2d gravity in real time.

## Sample App
Check out the sample modules to see these features in action:
- **Android**: Build and run the `:androidApp` module
- **Desktop**: `./gradlew :composeApp:run`

The sample includes gravity sliders, bounds toggle, fling/drag interactions, and mixed shapes. On Android, tilt/gimbal control is also available via the device accelerometer.

## Making a Game?
This library was designed with the intention of allowing for playful physics animations within normal apps. It is not built to be a game engine or meant to compete with the likes. If you are looking to do more intense mobile games, we recommend libraries such as [libGDX](https://libgdx.badlogicgames.com/) or [Unity](https://unity3d.com/)

License
--------

    Copyright 2026 John Carlson

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
