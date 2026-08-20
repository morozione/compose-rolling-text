package io.github.morozione.rollingtext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests running [RollingAnimatedText] on a device/emulator.
 *
 * Each character renders as its own text node; while a digit is rolling its node
 * contains the whole multi-line drum (e.g. "5\n4\n3"), so exact-text matchers only
 * succeed once the animation has settled on the final value.
 */
@RunWith(AndroidJUnit4::class)
class RollingAnimatedTextE2eTest {

    @get:Rule
    val rule = createComposeRule()

    private val timeoutMs = 5_000L

    @Test
    fun displaysInitialText() {
        rule.setContent {
            RollingAnimatedText(text = "$42")
        }

        rule.onNodeWithText("$").assertIsDisplayed()
        rule.onNodeWithText("4").assertIsDisplayed()
        rule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun digitChangeSettlesOnNewValue() {
        var value by mutableStateOf("5")
        rule.setContent {
            RollingAnimatedText(text = value)
        }
        rule.onNodeWithText("5").assertIsDisplayed()

        rule.runOnIdle { value = "8" }

        rule.waitUntil(timeoutMs) {
            rule.onAllNodesWithText("8").fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithText("5").assertDoesNotExist()
    }

    @Test
    fun growingNumberRollsLikeOdometer() {
        var value by mutableStateOf("99")
        rule.setContent {
            RollingAnimatedText(text = value)
        }
        rule.waitUntil(timeoutMs) {
            rule.onAllNodesWithText("9").fetchSemanticsNodes().size == 2
        }

        rule.runOnIdle { value = "100" }

        // Both existing columns roll 9 -> 0 and a new "1" column appears on the left
        rule.waitUntil(timeoutMs) {
            rule.onAllNodesWithText("0").fetchSemanticsNodes().size == 2 &&
                rule.onAllNodesWithText("1").fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithText("9").assertDoesNotExist()
    }

    @Test
    fun animateChangesDisabledUpdatesInstantly() {
        var value by mutableStateOf("1")
        rule.setContent {
            RollingAnimatedText(text = value, animateChanges = false, debounceMs = 0)
        }

        rule.runOnIdle { value = "9" }

        rule.waitUntil(timeoutMs) {
            rule.onAllNodesWithText("9").fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithText("1").assertDoesNotExist()
    }

    @Test
    fun nonDigitCharactersAreReplacedWithoutAnimation() {
        var value by mutableStateOf("1.5")
        rule.setContent {
            RollingAnimatedText(text = value)
        }
        rule.onNodeWithText(".").assertIsDisplayed()

        rule.runOnIdle { value = "1,5" }

        rule.waitUntil(timeoutMs) {
            rule.onAllNodesWithText(",").fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithText(".").assertDoesNotExist()
    }

    @Test
    fun autoSizeRendersLongTextInNarrowContainer() {
        rule.setContent {
            Box(modifier = Modifier.width(120.dp)) {
                RollingAnimatedText(
                    text = "1234567890",
                    style = TextStyle(fontSize = 32.sp),
                )
            }
        }

        // 10 digits at 32.sp need ~180dp, so auto-size must shrink the font;
        // 120dp is wide enough for the shrunk row, so every digit stays visible
        rule.onNodeWithText("1").assertIsDisplayed()
        rule.onNodeWithText("5").assertIsDisplayed()
        rule.onNodeWithText("0").assertIsDisplayed()
    }
}
