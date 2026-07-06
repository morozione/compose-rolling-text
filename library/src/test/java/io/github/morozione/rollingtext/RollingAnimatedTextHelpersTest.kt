package io.github.morozione.rollingtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildDrumTextTest {

    @Test
    fun `increasing digits ordered top to bottom from target to start`() {
        assertEquals("5\n4\n3\n2", buildDrumText(from = '2', to = '5'))
    }

    @Test
    fun `decreasing digits ordered top to bottom from start to target`() {
        assertEquals("7\n6\n5\n4\n3", buildDrumText(from = '7', to = '3'))
    }

    @Test
    fun `adjacent increase produces two lines`() {
        assertEquals("1\n0", buildDrumText(from = '0', to = '1'))
    }

    @Test
    fun `full range roll`() {
        assertEquals("9\n8\n7\n6\n5\n4\n3\n2\n1\n0", buildDrumText(from = '0', to = '9'))
    }
}

class CalculateVerticalOffsetTest {

    @Test
    fun `uses measured line positions when available`() {
        val positions = listOf(0f, 100f, 200f)

        val offset = calculateVerticalOffset(
            progress = 0.5f,
            linePositions = positions,
            lineHeight = 90f,
            linesCount = 3,
        )

        // Distance between first and last line tops is 200, scaled by progress
        assertEquals(-100f, offset, 0.001f)
    }

    @Test
    fun `falls back to uniform line height when positions missing`() {
        val offset = calculateVerticalOffset(
            progress = 1f,
            linePositions = emptyList(),
            lineHeight = 50f,
            linesCount = 3,
        )

        assertEquals(-100f, offset, 0.001f)
    }

    @Test
    fun `returns zero when nothing measured`() {
        val offset = calculateVerticalOffset(
            progress = 1f,
            linePositions = emptyList(),
            lineHeight = 0f,
            linesCount = 3,
        )

        assertEquals(0f, offset, 0.001f)
    }

    @Test
    fun `zero progress produces zero offset`() {
        val offset = calculateVerticalOffset(
            progress = 0f,
            linePositions = listOf(0f, 100f),
            lineHeight = 90f,
            linesCount = 2,
        )

        assertEquals(0f, offset, 0.001f)
    }
}

class ShouldSkipAnimationTest {

    @Test
    fun `skips when previous char is not a digit`() {
        assertTrue(shouldSkipAnimation(previousChar = '$', currentChar = '5', animationEnabled = true))
    }

    @Test
    fun `skips when char unchanged`() {
        assertTrue(shouldSkipAnimation(previousChar = '5', currentChar = '5', animationEnabled = true))
    }

    @Test
    fun `skips when animation disabled`() {
        assertTrue(shouldSkipAnimation(previousChar = '2', currentChar = '5', animationEnabled = false))
    }

    @Test
    fun `animates digit to digit change`() {
        assertFalse(shouldSkipAnimation(previousChar = '2', currentChar = '5', animationEnabled = true))
    }
}

class ResolveTextColorTest {

    @Test
    fun `explicit color wins over style color`() {
        val resolved = resolveTextColor(Color.Red, TextStyle(color = Color.Blue))

        assertEquals(Color.Red, resolved)
    }

    @Test
    fun `style color used when explicit color unspecified`() {
        val resolved = resolveTextColor(Color.Unspecified, TextStyle(color = Color.Blue))

        assertEquals(Color.Blue, resolved)
    }

    @Test
    fun `defaults to black when nothing specified`() {
        val resolved = resolveTextColor(Color.Unspecified, TextStyle.Default)

        assertEquals(Color.Black, resolved)
    }
}
