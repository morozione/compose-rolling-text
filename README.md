# Compose Rolling Text

A Jetpack Compose library for animated rolling/odometer-style text transitions.

When digits change, they animate through all intermediate values like a mechanical counter:
- **Increasing** (2 → 5): digits roll down through 2 → 3 → 4 → 5
- **Decreasing** (7 → 3): digits roll up through 7 → 6 → 5 → 4 → 3
  
<img src="assets/demo.gif" width="300" />

## Installation

Add JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.morozione:compose-rolling-text:v1.1.0")
}
```

## Usage

```kotlin
import io.github.morozione.rollingtext.RollingAnimatedText

@Composable
fun BalanceDisplay() {
    var balance by remember { mutableDoubleStateOf(1234.56) }

    RollingAnimatedText(
        text = "$$balance",
        style = TextStyle(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        ),
        color = Color.Black
    )
}
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `text` | `CharSequence` | required | The text to display. Digits will animate when changed. |
| `modifier` | `Modifier` | `Modifier` | Modifier for the composable. |
| `style` | `TextStyle` | `TextStyle.Default` | Text style (fontSize, fontWeight, etc.). |
| `color` | `Color` | `Color.Unspecified` | Text color. Falls back to style color or Black. |
| `animateChanges` | `Boolean` | `true` | Enable/disable rolling animation. |
| `debounceMs` | `Long` | `20` | Debounce delay to avoid flickering on rapid updates. |
| `autoSize` | `Boolean` | `true` | Auto-adjust font size to fit available width. |
| `minFontSize` | `TextUnit` | `10.sp` | Lower bound for auto-size shrinking. |
| `animationSpec` | `AnimationSpec<Float>` | 500ms emphasized tween | Animation used for the rolling transition. |

## Features

- Smooth Material Design emphasized easing curves (customizable via `animationSpec`)
- Digit columns keyed from the right, so `99 → 100` rolls like a real odometer
- Fixed-width digit slots — no horizontal jitter while digits roll, even with proportional fonts
- Auto-sizing text to fit container width
- Non-digit characters (currency symbols, spaces, punctuation) displayed without animation
- Configurable animation enable/disable
- Debounce support for rapid value changes

## Notes

- When neither `color` nor `style.color` is specified, text is drawn in **black** — the
  component does not read Material's `LocalContentColor`, so pass an explicit color in dark themes.

## Requirements

- Min SDK: 21
- Jetpack Compose BOM: 2026.06.01+

## License

```
Copyright 2024 Ivan Moroz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
