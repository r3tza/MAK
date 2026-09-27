package dev.retza.mak.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.retza.mak.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLocaleTest {
    @Test
    fun mainActivityUsesPolishResourcesWhateverThePhoneLanguage() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("pl", activity.resources.configuration.locales[0].language)
            }
        }
    }
}
