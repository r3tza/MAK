package dev.retza.mak.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.ui.theme.MAKTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassCardCurrentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun currentLabelIsVisibleAndIncludedInCardDescription() {
        val item = ClassItemUi(
            id = "1:2026-11-11",
            name = "Analiza",
            type = "Wykład",
            courseName = "Informatyka",
            startTime = "08:00",
            endTime = "09:30",
            room = "A1",
            isCurrent = true
        )

        composeTestRule.setContent {
            MAKTheme {
                ClassCard(item)
            }
        }

        composeTestRule.onNodeWithText("Teraz").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("08:00-09:30, Analiza, Teraz, Informatyka, Wykład, A1")
            .assertIsDisplayed()
    }
}
