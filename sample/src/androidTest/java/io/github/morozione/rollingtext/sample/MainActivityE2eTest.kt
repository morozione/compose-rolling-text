package io.github.morozione.rollingtext.sample

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Full-activity end-to-end tests: launches the real [MainActivity], clicks the demo
 * buttons and verifies the rolling counter settles on the expected value.
 *
 * Assertions are scoped to the "counter" test tag because other demo sections
 * (balance) also render digits.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityE2eTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val timeoutMs = 5_000L

    private fun counterDigit(digit: String) =
        hasText(digit) and hasAnyAncestor(hasTestTag("counter"))

    private fun counterShows(value: String): Boolean =
        value.all { digit ->
            rule.onAllNodes(counterDigit(digit.toString()))
                .fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun counterStartsAtZero() {
        rule.onNodeWithTag("counter").assertExists()
        rule.onNode(counterDigit("0")).assertExists()
    }

    @Test
    fun plusOneRollsCounterToOne() {
        rule.onNodeWithText("+1").performClick()

        rule.waitUntil(timeoutMs) { counterShows("1") }
        rule.onNode(counterDigit("0")).assertDoesNotExist()
    }

    @Test
    fun plusTenGrowsCounterToTwoDigits() {
        rule.onNodeWithText("+10").performClick()

        rule.waitUntil(timeoutMs) { counterShows("10") }
    }

    @Test
    fun minusOneRollsCounterToNegative() {
        rule.onNodeWithText("-1").performClick()

        rule.waitUntil(timeoutMs) { counterShows("-1") }
    }
}
