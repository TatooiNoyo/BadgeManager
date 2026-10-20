package io.github.tatooinoyo.star.badge

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.github.tatooinoyo.star.badge.data.BadgeChannelTarget
import io.github.tatooinoyo.star.badge.data.InstalledChannelProvider
import io.github.tatooinoyo.star.badge.ui.home.BadgeInputForm
import io.github.tatooinoyo.star.badge.ui.theme.BadgeManagerTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OtherAppPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pickerRemainsAvailableWithoutPreloadedAppsAndSelectsInstalledApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val channels = InstalledChannelProvider.discover(context)
        assertTrue(channels.all { it.acceptsBadgeLinks })
        val otherApps = InstalledChannelProvider.discoverOtherApps(
            context, channels.mapTo(HashSet()) { it.packageName },
        )
        assertTrue("Test device must have another launcher app", otherApps.isNotEmpty())
        val target = otherApps.first()
        var selected: BadgeChannelTarget? = channels.first()

        compose.setContent {
            BadgeManagerTheme {
                BadgeInputForm(
                    title = "", onTitleChange = {},
                    remark = "", onRemarkChange = {},
                    link = "", onLinkChange = {},
                    channel = selected, onChannelChange = { selected = it },
                    availableChannels = channels,
                    onExtractSkClick = {}, onTagsChange = {},
                )
            }
        }
        compose.onNodeWithText(channels.first().getLabel(context)).performClick()
        compose.onNodeWithText(context.getString(R.string.channel_choose_other_app))
            .assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(target.packageName).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(target.packageName).performClick()
        compose.runOnIdle { assertEquals(target.packageName, selected?.packageName) }
    }
}
