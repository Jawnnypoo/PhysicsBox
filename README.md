# PhysicsBox

Compose-native physics layout powered by Box2D.

`PhysicsBox` is the Compose equivalent of [PhysicsLayout](https://github.com/Jawnnypoo/PhysicsLayout):
- Drop any composables inside a parent container
- Enable gravity and bounds
- Drag/fling children around
- Configure body shape + physics properties per child

## Installation

```kotlin
dependencies {
    implementation("com.jawnnypoo:physicsbox:<version>")
}
```

## Basic Usage

```kotlin
val state = rememberPhysicsBoxState()

PhysicsBox(
    modifier = Modifier
        .fillMaxWidth()
        .height(320.dp),
    state = state,
) {
    repeat(12) { index ->
        Box(
            modifier = Modifier
                .size(56.dp)
                .physicsBody(
                    shape = if (index % 3 == 0) Shape.CIRCLE else Shape.RECTANGLE,
                    restitution = 0.35f,
                    friction = 0.4f,
                    density = 0.3f,
                )
                .background(Color.Red, CircleShape)
        )
    }
}
```

## API

### PhysicsBoxState

```kotlin
val state = rememberPhysicsBoxState().apply {
    isPhysicsEnabled = true
    isFlingEnabled = true
    hasBounds = true

    gravityX = 0f
    gravityY = PhysicsBoxState.EARTH_GRAVITY

    velocityIterations = 8
    positionIterations = 3

    // Optional overrides
    // pixelsPerMeter = 20f * density
    // boundsSizePx = 20f * density
}
```

### Child configuration

Use `Modifier.physicsBody(...)` on each child to customize behavior:

```kotlin
Modifier.physicsBody(
    shape = Shape.RECTANGLE,
    bodyType = PhysicsBodyType.DYNAMIC,
    friction = 0.3f,
    restitution = 0.2f,
    density = 0.2f,
    fixedRotation = false,
)
```

## Tilt / gimbal gravity

Hook your sensor data to gravity:

```kotlin
state.setGravity(x = tiltX, y = tiltY)
```

This updates Box2D gravity in real time.

## Sample app

This repo includes `:sample` demonstrating:
- gravity sliders
- bounds toggle
- physics toggle
- fling/drag interactions
- mixed circle + rectangle bodies

## License

Apache-2.0
