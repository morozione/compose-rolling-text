package io.github.morozione.rollingtext

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Animation timing
private const val DEFAULT_DEBOUNCE_MS = 20L
private const val ANIMATION_DURATION_MS = 500

// Font size auto-scaling
private val DEFAULT_MIN_FONT_SIZE = 10.sp
private const val FONT_SIZE_SEARCH_PRECISION = 0.5f

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

private val DefaultRollingAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = ANIMATION_DURATION_MS, easing = RollingEasing)

/**
 * Resolves the effective text color from the provided color and style.
 * Priority: explicit color > style color > default black.
 */
internal fun resolveTextColor(color: Color, style: TextStyle): Color = when {
    color != Color.Unspecified -> color
    style.color != Color.Unspecified -> style.color
    else -> DEFAULT_TEXT_COLOR
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
 * Digit slots are keyed by their offset from the end of the string, so a change like
 * 99 → 100 rolls the ones and tens columns while the new hundreds digit appears, matching
 * mechanical odometer behavior.
 *
 * Each digit slot is given the width of the widest digit in the font, so the layout stays
 * stable while digits roll even with proportional fonts.
 *
 * @param text The text to display. Digits will be animated when they change.
 *   Converted with [toString]; any [androidx.compose.ui.text.AnnotatedString] styling is ignored.
 * @param modifier Modifier to be applied to the composable.
 * @param style The text style to use. Defaults to [TextStyle.Default].
 * @param color The text color. Defaults to [Color.Unspecified] which uses the color from [style].
 *   Note: when neither is specified the text is drawn in black — this component does not read
 *   Material's LocalContentColor, so pass an explicit color when using dark themes.
 * @param animateChanges Enable/disable rolling animation. Defaults to true.
 * @param debounceMs Debounce delay to avoid flickering on rapid updates. Defaults to 20ms.
 * @param autoSize Enable automatic font size adjustment to fit available space. Defaults to true.
 * @param minFontSize Lower bound for [autoSize] shrinking. Defaults to 10.sp.
 * @param animationSpec Animation used for the rolling transition. Defaults to a 500ms tween
 *   with Material's emphasized easing.
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
    minFontSize: TextUnit = DEFAULT_MIN_FONT_SIZE,
    animationSpec: AnimationSpec<Float> = DefaultRollingAnimationSpec,
) {
    var displayedValue by remember { mutableStateOf(text.toString()) }
    val resolvedColor = resolveTextColor(color, style)

    LaunchedEffect(text.toString()) {
        val newValue = text.toString()
        if (newValue == displayedValue) return@LaunchedEffect

        if (debounceMs > 0) {
            delay(debounceMs)
        }
        displayedValue = newValue
    }

    @Composable
    fun RollingRow(textStyle: TextStyle, rowModifier: Modifier = Modifier) {
        val coloredStyle = remember(textStyle, resolvedColor) {
            textStyle.copy(color = resolvedColor)
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val length = displayedValue.length
            displayedValue.forEachIndexed { index, char ->
                // Key by distance from the end so the ones/tens/hundreds columns stay
                // stable when the string grows or shrinks (e.g. 99 -> 100)
                key(length - index) {
                    RollingCharacter(
                        char = char,
                        textStyle = coloredStyle,
                        animate = animateChanges,
                        animationSpec = animationSpec,
                    )
                }
            }
        }
    }

    if (autoSize) {
        BoxWithConstraints(modifier = modifier) {
            val textMeasurer = rememberTextMeasurer()
            val maxWidth = constraints.maxWidth

            val adjustedStyle = remember(displayedValue, maxWidth, style, minFontSize) {
                if (maxWidth > 0 && maxWidth != Constraints.Infinity) {
                    calculateFontSizeToFit(
                        text = displayedValue,
                        style = style,
                        maxWidth = maxWidth,
                        minFontSize = minFontSize,
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
 * Calculates the optimal font size to fit text within the given width.
 * Binary-searches between [minFontSize] and the style's font size.
 *
 * Width is computed the same way the text is rendered: digits occupy the width of the
 * widest digit, other characters their natural width.
 */
private fun calculateFontSizeToFit(
    text: String,
    style: TextStyle,
    maxWidth: Int,
    minFontSize: TextUnit,
    textMeasurer: TextMeasurer,
): TextStyle {
    if (text.isEmpty()) return style
    // Without a concrete sp starting size there is nothing to shrink from
    if (style.fontSize.type != TextUnitType.Sp) return style

    val maxFontSize = style.fontSize.value
    val minSize = minFontSize.value
    if (maxFontSize <= minSize) return style

    if (measureRowWidth(text, style, textMeasurer) < maxWidth) return style
    if (measureRowWidth(text, style.copy(fontSize = minSize.sp), textMeasurer) >= maxWidth) {
        return style.copy(fontSize = minSize.sp)
    }

    var fits = minSize
    var overflows = maxFontSize
    while (overflows - fits > FONT_SIZE_SEARCH_PRECISION) {
        val mid = (fits + overflows) / 2f
        val width = measureRowWidth(text, style.copy(fontSize = mid.sp), textMeasurer)
        if (width < maxWidth) fits = mid else overflows = mid
    }

    return style.copy(fontSize = fits.sp)
}

/**
 * Measures the rendered row width: digits use the widest digit's width (matching the
 * fixed-width slots used at render time), other characters their own measured width.
 */
private fun measureRowWidth(
    text: String,
    style: TextStyle,
    textMeasurer: TextMeasurer,
): Float {
    val digitWidth = if (text.any { it.isDigit() }) {
        ('0'..'9').maxOf { digit ->
            textMeasurer.measure(text = digit.toString(), style = style, maxLines = 1).size.width
        }.toFloat()
    } else {
        0f
    }

    var width = 0f
    for (char in text) {
        width += if (char.isDigit()) {
            digitWidth
        } else {
            textMeasurer.measure(text = char.toString(), style = style, maxLines = 1)
                .size.width.toFloat()
        }
    }
    return width
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
    animationSpec: AnimationSpec<Float>,
) {
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

    // Widest digit defines the slot size so the layout doesn't shift while rolling,
    // and all characters share the same height so baselines stay aligned
    val slotSize = remember(internalStyle) {
        var maxWidth = 0
        var maxHeight = 0
        for (digit in '0'..'9') {
            val layout = textMeasurer.measure(
                text = digit.toString(),
                style = internalStyle,
                maxLines = 1
            )
            if (layout.size.width > maxWidth) maxWidth = layout.size.width
            if (layout.size.height > maxHeight) maxHeight = layout.size.height
        }
        IntSize(maxWidth, maxHeight)
    }
    val lineHeight = slotSize.height.toFloat()

    var previousChar by remember { mutableStateOf(char) }

    if (!char.isDigit()) {
        // Keep previousChar in sync so a later digit in this slot snaps instead of
        // animating from a stale value
        LaunchedEffect(char) { previousChar = char }
        Box(
            modifier = Modifier.height(with(density) { lineHeight.toDp() }),
            contentAlignment = Alignment.TopCenter
        ) {
            BasicText(
                text = char.toString(),
                style = internalStyle,
            )
        }
        return
    }

    var drumText by remember { mutableStateOf(char.toString()) }
    var linePositions by remember { mutableStateOf<List<Float>>(emptyList()) }
    var isIncreasing by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    val progress = remember { Animatable(ANIMATION_PROGRESS_START) }

    LaunchedEffect(char) {
        val skipAnimation = shouldSkipAnimation(
            previousChar = previousChar,
            currentChar = char,
            animationEnabled = animate
        )

        if (skipAnimation) {
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
                animationSpec = animationSpec
            )
        } finally {
            drumText = char.toString()
            previousChar = char
            isAnimating = false
            linePositions = emptyList()
        }
    }

    val linesCount = drumText.count { it == '\n' } + 1

    Box(
        modifier = Modifier
            .width(with(density) { slotSize.width.toDp() })
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
                    // progress is read here, inside the layer block, so animation frames
                    // only invalidate this layer instead of recomposing the composable
                    translationY = if (isAnimating) {
                        val adjustedProgress =
                            if (isIncreasing) 1f - progress.value else progress.value
                        calculateVerticalOffset(
                            progress = adjustedProgress,
                            linePositions = linePositions,
                            lineHeight = lineHeight,
                            linesCount = linesCount,
                        )
                    } else {
                        0f
                    }
                }
        )
    }
}

/**
 * Determines if animation should be skipped for this character transition.
 */
internal fun shouldSkipAnimation(
    previousChar: Char,
    currentChar: Char,
    animationEnabled: Boolean,
): Boolean = !previousChar.isDigit() || currentChar == previousChar || !animationEnabled

/**
 * Builds the vertical "drum" text containing all digits to scroll through.
 * Digits are ordered top-to-bottom from highest to lowest.
 */
internal fun buildDrumText(
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
internal fun calculateVerticalOffset(
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
