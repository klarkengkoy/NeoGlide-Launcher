package com.samidevstudio.neoglide.domain.classifier

import android.content.pm.ApplicationInfo
import com.samidevstudio.neoglide.domain.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryClassifierTest {

    private val classifier = AppCategoryClassifier()

    @Test
    fun `Signal 1 - Maps ApplicationInfo category correctly`() {
        val result = classifier.classify(
            packageName = "com.test.app",
            label = "Test App",
            appInfoCategory = ApplicationInfo.CATEGORY_GAME,
            permissions = emptyList()
        )
        assertEquals(AppCategory.GAMING, result)
    }

    @Test
    fun `Signal 2 - Scores permissions correctly (Photography)`() {
        val result = classifier.classify(
            packageName = "com.test.app",
            label = "Test App",
            appInfoCategory = -1, // Undefined
            permissions = listOf("android.permission.CAMERA")
        )
        assertEquals(AppCategory.PHOTOGRAPHY, result)
    }

    @Test
    fun `Signal 2 - Scores permissions correctly (Social)`() {
        val result = classifier.classify(
            packageName = "com.test.app",
            label = "Test App",
            appInfoCategory = -1,
            permissions = listOf("android.permission.READ_CONTACTS", "android.permission.SEND_SMS")
        )
        assertEquals(AppCategory.SOCIAL, result)
    }

    @Test
    fun `Signal 2b - Matches keywords in package name`() {
        val result = classifier.classify(
            packageName = "com.duolingo",
            label = "Duolingo",
            appInfoCategory = -1,
            permissions = emptyList()
        )
        assertEquals(AppCategory.EDUCATION, result)
    }

    @Test
    fun `Signal 2b - Matches keywords in label`() {
        val result = classifier.classify(
            packageName = "com.app.any",
            label = "Shopping Cart",
            appInfoCategory = -1,
            permissions = emptyList()
        )
        assertEquals(AppCategory.SHOPPING, result)
    }

    @Test
    fun `Tie-break - Priority follows UTILITIES over PRODUCTIVITY`() {
        // "calendar" is Productivity, "clock" is Utilities.
        // Priority list: UTILITIES > PRODUCTIVITY
        val result = classifier.classify(
            packageName = "com.app.task.clock",
            label = "Task Clock",
            appInfoCategory = -1,
            permissions = emptyList()
        )
        assertEquals(AppCategory.UTILITIES, result)
    }
}
