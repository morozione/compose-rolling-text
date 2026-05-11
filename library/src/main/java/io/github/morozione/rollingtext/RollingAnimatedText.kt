package io.github.morozione.rollingtext

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Animation timing
private const val DEFAULT_DEBOUNCE_MS = 20L
private const val ANIMATION_DURATION_MS = 500

// Font size auto-scaling
private val MIN_FONT_SIZE = 10.sp
private const val FONT_SIZE_SEARCH_STEP = 1f

// Animation progress bounds
private const val ANIMATION_PROGRESS_START = 0f
private const val ANIMATION_PROGRESS_END = 1f

// Fallback text color when none specified
private val DEFAULT_TEXT_COLOR = Color.Black

/**
 * Material Design's emphasized easing for smooth deceleration.
 * Creates a natural "settling" effect at the end of animations.
 */
@Suppress("MagicNumber")
private val RollingEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * Resolves the effective text color from the provided color and style.
 * Priority: explicit color > style color > default black.
 */
private fun resolveTextColor(color: Color, style: TextStyle): Color = when {
    color != Color.Unspecified -> color
    style.color != Color.Unspecified -> style.color
    else -> DEFAULT_TEXT_COLOR
}

/**
 * Calculates the optimal font size to fit text within the given width.
 * Searches from the style's font size down to [MIN_FONT_SIZE].
 */
private fun calculateFontSizeToFit(
    text: String,
    style: TextStyle,
    maxWidth: Int,
    textMeasurer: TextMeasurer,
): TextStyle {
    if (text.isEmpty()) return style

    val maxFontSize = style.fontSize.value
    val minFontSize = MIN_FONT_SIZE.value

    var currentFontSize = maxFontSize
    while (currentFontSize >= minFontSize) {
        val testStyle = style.copy(fontSize = currentFontSize.sp)
        val layout = textMeasurer.measure(
            text = text,
            style = testStyle,
            maxLines = 1,
        )

        if (layout.size.width < maxWidth) {
            return testStyle
        }
        currentFontSize -= FONT_SIZE_SEARCH_STEP
    }

    return style.copy(fontSize = MIN_FONT_SIZE)
}

/**
 * Animated text component that "rolls" through intermediate digit values like an odometer.
 *
 * When a digit changes, it animates through all intermediate values:
 * - Digit increases (2 → 5): falls down from top through 2 → 3 → 4 → 5
 * - Digit decreases (7 → 3): rises up from bottom through 7 → 6 → 5 → 4 → 3
 *
 * Non-digit characters (spaces, punctuation, currency symbols) are displayed without animation.
 *
 * @param text The text to display. Digits will be animated when they change.
 * @param modifier Modifier to be applied to the composable.
 * @param style The text style to use. Defaults to [TextStyle.Default].
 * @param color The text color. Defaults to [Color.Unspecified] which uses the color from [style].
 * @param animateChanges Enable/disable rolling animation. Defaults to true.
 * @param debounceMs Debounce delay to avoid flickering on rapid updates. Defaults to 20ms.
 * @param autoSize Enable automatic font size adjustment to fit available space. Defaults to true.
 */
@Composable
fun RollingAnimatedText(
    text: CharSequence,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    animateChanges: Boolean = true,
    debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    autoSize: Boolean = true,
) {
    var displayedValue by remember { mutableStateOf(text.toString()) }
    var previousValue by remember { mutableStateOf(text.toString()) }
    val resolvedColor = resolveTextColor(color, style)

    LaunchedEffect(text.toString()) {
        val newValue = text.toString()

        if (debounceMs > 0) {
            delay(debounceMs)
        }

        if (newValue != previousValue) {
            previousValue = newValue
            displayedValue = newValue
        }
    }

    @Composable
    fun RollingRow(textStyle: TextStyle, rowModifier: Modifier = Modifier) {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            displayedValue.forEachIndexed { index, char ->
                key(index) {
                    RollingCharacter(
                        char = char,
                        textStyle = textStyle.copy(color = resolvedColor),
                        animate = animateChanges,
                    )
                }
            }
        }
    }

    if (autoSize) {
        BoxWithConstraints(modifier = modifier) {
            val textMeasurer = rememberTextMeasurer()
            val maxWidth = constraints.maxWidth

            val adjustedStyle = remember(displayedValue, maxWidth, style) {
                if (maxWidth > 0 && maxWidth != Constraints.Infinity) {
                    calculateFontSizeToFit(
                        text = displayedValue,
                        style = style,
                        maxWidth = maxWidth,
                        textMeasurer = textMeasurer,
                    )
                } else {
                    style
                }
            }

            RollingRow(textStyle = adjustedStyle)
        }
    } else {
        RollingRow(textStyle = style, rowModifier = modifier)
    }
}

/**
 * Renders a single character with slot-machine style rolling animation for digits.
 *
 * Creates a vertical "drum" of text (e.g., "4\n3\n2") and smoothly scrolls through
 * it to reveal the target digit. Non-digit characters are rendered directly without animation.
 *
 * Animation behavior:
 * - When digit increases (2→4): digits fall down from top
 *   - Drum: "4\n3\n2", starts at "2" (bottom), scrolls up to "4" (top)
 * - When digit decreases (4→2): digits rise up from bottom
 *   - Drum: "4\n3\n2", starts at "4" (top), scrolls down to "2" (bottom)
 */
@Composable
private fun RollingCharacter(
    char: Char,
    textStyle: TextStyle,
    animate: Boolean,
) {
    if (!char.isDigit()) {
        BasicText(
            text = char.toString(),
            style = textStyle,
        )
        return
    }

    var previousChar by remember { mutableStateOf(char) }
    var drumText by remember { mutableStateOf(char.toString()) }
    var linePositions by remember { mutableStateOf<List<Float>>(emptyList()) }
    var isIncreasing by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current

    val textMeasurer = rememberTextMeasurer()
    val internalStyle = remember(textStyle) {
        textStyle.copy(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Proportional,
                trim = LineHeightStyle.Trim.None
            )
        )
    }

    val lineHeight = remember(internalStyle.fontSize) {
        val layout = textMeasurer.measure(
            text = "0",
            style = internalStyle,
            maxLines = 1
        )
        layout.size.height.toFloat()
    }

    LaunchedEffect(char) {
        val shouldSkipAnimation = shouldSkipAnimation(
            previousChar = previousChar,
            currentChar = char,
            animationEnabled = animate
        )

        if (shouldSkipAnimation) {
            progress.snapTo(ANIMATION_PROGRESS_START)
            drumText = char.toString()
            previousChar = char
            isAnimating = false
            linePositions = emptyList()

            return@LaunchedEffect
        }

        isIncreasing = char.digitToInt() > previousChar.digitToInt()

        drumText = buildDrumText(
            from = previousChar,
            to = char
        )


        try {
            isAnimating = true
            progress.snapTo(ANIMATION_PROGRESS_START)
            progress.animateTo(
                targetValue = ANIMATION_PROGRESS_END,
                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS, easing = RollingEasing)
            )
        } finally {
            drumText = char.toString()
            previousChar = char
            isAnimating = false
            linePositions = emptyList()
        }
    }

    val linesCount = drumText.count { it == '\n' } + 1
    // Invert progress for increasing digits so they fall from top
    val adjustedProgress = if (isIncreasing) 1f - progress.value else progress.value

    val verticalOffset = if (isAnimating) {
        calculateVerticalOffset(
            progress = adjustedProgress,
            linePositions = linePositions,
            lineHeight = lineHeight,
            linesCount = linesCount,
        )
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .height(with(density) { lineHeight.toDp() })
            .clipToBounds(),
        contentAlignment = Alignment.TopCenter
    ) {
        BasicText(
            text = drumText,
            style = internalStyle,
            overflow = TextOverflow.Visible,
            maxLines = Int.MAX_VALUE,
            onTextLayout = { layout ->
                linePositions = extractLinePositions(layout)
            },
            modifier = Modifier
                .graphicsLayer {
                    translationY = verticalOffset
                }
        )
    }
}

/**
 * Determines if animation should be skipped for this character transition.
 */
private fun shouldSkipAnimation(
    previousChar: Char,
    currentChar: Char,
    animationEnabled: Boolean,
): Boolean = !previousChar.isDigit() || currentChar == previousChar || !animationEnabled

/**
 * Builds the vertical "drum" text containing all digits to scroll through.
 * Digits are ordered top-to-bottom from highest to lowest.
 */
private fun buildDrumText(
    from: Char,
    to: Char,
): String {
    val start = from.digitToInt()
    val end = to.digitToInt()

    return if (end > start) {
        (end downTo start).joinToString("\n")
    } else {
        (start downTo end).joinToString("\n")
    }
}

/**
 * Calculates the vertical translation offset for the drum animation.
 * Uses measured line positions when available, falls back to uniform line height.
 */
private fun calculateVerticalOffset(
    progress: Float,
    linePositions: List<Float>,
    lineHeight: Float,
    linesCount: Int,
): Float = when {
    linePositions.size >= linesCount -> {
        -(linePositions[linesCount - 1] - linePositions[0]) * progress
    }
    lineHeight > 0f -> {
        -progress * (linesCount - 1) * lineHeight
    }
    else -> 0f
}

/**
 * Extracts the top Y position of each line from a text layout.
 */
private fun extractLinePositions(layout: TextLayoutResult): List<Float> =
    List(layout.lineCount) { lineIndex ->
        layout.getLineTop(lineIndex)
    }
