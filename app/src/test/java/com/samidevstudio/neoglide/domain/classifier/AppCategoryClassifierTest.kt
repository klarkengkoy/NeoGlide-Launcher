package com.samidevstudio.neoglide.domain.classifier

import android.content.pm.ApplicationInfo
import com.samidevstudio.neoglide.domain.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryClassifierTest {

    private val classifier = AppCategoryClassifier()

    @Test
    fun `Pre-Step 0 - KnownPackages matches famous apps directly`() {
        val whatsapp = classifier.classify(
            packageName = "com.whatsapp",
            label = "WhatsApp",
            appInfoCategory = -1,
            permissions = emptyList()
        )
        assertEquals(AppCategory.COMMUNICATION, whatsapp)

        val maps = classifier.classify(
            packageName = "com.google.android.apps.maps",
            label = "Google Maps",
            appInfoCategory = -1,
            permissions = emptyList()
        )
        assertEquals(AppCategory.MAPS, maps)

        val chrome = classifier.classify(
            packageName = "com.android.chrome",
            label = "Chrome",
            appInfoCategory = -1,
            permissions = emptyList()
        )
        assertEquals(AppCategory.BROWSER, chrome)
    }

    @Test
    fun `Step 1 - Trusted ApplicationInfo CATEGORY_GAME matches GAMING`() {
        val result = classifier.classify(
            packageName = "com.test.game",
            label = "Custom Game",
            appInfoCategory = ApplicationInfo.CATEGORY_GAME,
            permissions = emptyList()
        )
        assertEquals(AppCategory.GAMING, result)
    }

    @Test
    fun `Step 2 - Matches clear keyword matches in label or package`() {
        val shopping = classifier.classify(
            packageName = "com.custom.shopping",
            label = "Shopping Cart",
            appInfoCategory = -1,
            permissions = emptyList()
        )
        assertEquals(AppCategory.SHOPPING, shopping)

        val finance = classifier.classify(
            packageName = "com.test.bank",
            label = "My Bank Balance",
            appInfoCategory = -1,
            permissions = emptyList()
        )
        assertEquals(AppCategory.FINANCE, finance)
    }

    @Test
    fun `Step 3 - High confidence permissions match correctly`() {
        val comm = classifier.classify(
            packageName = "org.custom.app",
            label = "Random App",
            appInfoCategory = -1,
            permissions = listOf("android.permission.SEND_SMS", "android.permission.RECEIVE_SMS")
        )
        assertEquals(AppCategory.COMMUNICATION, comm)

        val productivity = classifier.classify(
            packageName = "org.custom.app2",
            label = "Random App 2",
            appInfoCategory = -1,
            permissions = listOf("android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR")
        )
        assertEquals(AppCategory.PRODUCTIVITY, productivity)
    }

    @Test
    fun `Step 3 - Generic permissions like CAMERA alone do not force PHOTOGRAPHY`() {
        val result = classifier.classify(
            packageName = "com.unknown.app",
            label = "My Generic App",
            appInfoCategory = -1,
            permissions = listOf("android.permission.CAMERA")
        )
        // Camera alone is no longer a high-confidence trigger for PHOTOGRAPHY
        assertEquals(AppCategory.OTHER, result)
    }

    @Test
    fun `Fallback - Weak or unknown apps fallback to OTHER`() {
        val result = classifier.classify(
            packageName = "com.xyz.unknown",
            label = "Xyz App",
            appInfoCategory = -1,
            permissions = emptyList()
        )
        assertEquals(AppCategory.OTHER, result)
    }
}
