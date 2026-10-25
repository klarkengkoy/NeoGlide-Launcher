package com.smyddevstudio.neoglidelauncher.domain.classifier

import com.smyddevstudio.neoglidelauncher.domain.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryFallbackResolverTest {

    @Test
    fun `UnionBank case - falls back to FINANCE when BANKING not in rail`() {
        val banking = AppCategory("BANKING")
        val scores = mapOf(
            banking to 1.0f,
            AppCategory.FINANCE to 0.8f,
            AppCategory.WALLET to 0.6f
        )
        val railWithFinance = setOf(AppCategory.FINANCE, AppCategory.TOOLS)

        val resolved = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = false,
            isUserAssigned = false,
            storedCategory = banking,
            scores = scores,
            rail = railWithFinance
        )

        assertEquals(AppCategory.FINANCE, resolved)
    }

    @Test
    fun `UnionBank case - falls back to WALLET when neither BANKING nor FINANCE in rail`() {
        val banking = AppCategory("BANKING")
        val scores = mapOf(
            banking to 1.0f,
            AppCategory.FINANCE to 0.8f,
            AppCategory.WALLET to 0.6f
        )
        val railWithWallet = setOf(AppCategory.WALLET, AppCategory.TOOLS)

        val resolved = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = false,
            isUserAssigned = false,
            storedCategory = banking,
            scores = scores,
            rail = railWithWallet
        )

        assertEquals(AppCategory.WALLET, resolved)
    }

    @Test
    fun `UnionBank case - falls back to anchor when none are in rail`() {
        val banking = AppCategory("BANKING")
        val scores = mapOf(
            banking to 1.0f,
            AppCategory.FINANCE to 0.8f,
            AppCategory.WALLET to 0.6f
        )
        val railWithAnchorOnly = setOf(AppCategory.TOOLS)

        val resolved = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = false,
            isUserAssigned = false,
            storedCategory = banking,
            scores = scores,
            rail = railWithAnchorOnly,
            anchor = AppCategory.TOOLS
        )

        assertEquals(AppCategory.TOOLS, resolved)
    }

    @Test
    fun `Runner-up score below minScore is ignored`() {
        val scores = mapOf(
            AppCategory.SPORTS to 0.9f,
            AppCategory.GAMING to 0.3f // below minScore = 0.5f
        )
        // GAMING is in rail, but its score is 0.3f (< minScore).
        // SPORTS is not in rail, and its related categories (FITNESS, NEWS) are not in rail.
        val railWithGamingOnly = setOf(AppCategory.GAMING, AppCategory.TOOLS)

        val resolved = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = false,
            isUserAssigned = false,
            storedCategory = AppCategory.SPORTS,
            scores = scores,
            rail = railWithGamingOnly,
            anchor = AppCategory.TOOLS,
            minScore = 0.5f
        )

        // GAMING score (0.3f) is below threshold, so it falls through to ANCHOR (TOOLS)
        assertEquals(AppCategory.TOOLS, resolved)
    }

    @Test
    fun `Hidden apps stay in Hidden even when their category is added to the rail`() {
        val scores = mapOf(AppCategory.SOCIAL to 1.0f)
        val railWithSocial = setOf(AppCategory.SOCIAL, AppCategory.TOOLS)

        val resolved = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = true,
            isUserAssigned = false,
            storedCategory = AppCategory.SOCIAL,
            scores = scores,
            rail = railWithSocial
        )

        assertEquals(AppCategory.HIDDEN, resolved)
    }

    @Test
    fun `Manual assignment wins over classification and fallback`() {
        val scores = mapOf(AppCategory.GAMING to 1.0f)
        val railWithProductivity = setOf(AppCategory.PRODUCTIVITY, AppCategory.TOOLS)

        val resolved = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = false,
            isUserAssigned = true,
            storedCategory = AppCategory.PRODUCTIVITY,
            scores = scores,
            rail = railWithProductivity
        )

        assertEquals(AppCategory.PRODUCTIVITY, resolved)
    }

    @Test
    fun `Adding a category moves fallback apps into it without touching hidden or manual apps`() {
        val scores = mapOf(AppCategory.FINANCE to 1.0f)

        // 1. Initial state: FINANCE not in rail -> falls back to ANCHOR (TOOLS)
        val initialRail = setOf(AppCategory.TOOLS)
        val initialDisplay = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = false,
            isUserAssigned = false,
            storedCategory = AppCategory.FINANCE,
            scores = scores,
            rail = initialRail
        )
        assertEquals(AppCategory.TOOLS, initialDisplay)

        // 2. FINANCE added to rail -> app automatically pulls into FINANCE
        val updatedRail = setOf(AppCategory.FINANCE, AppCategory.TOOLS)
        val updatedDisplay = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = false,
            isUserAssigned = false,
            storedCategory = AppCategory.FINANCE,
            scores = scores,
            rail = updatedRail
        )
        assertEquals(AppCategory.FINANCE, updatedDisplay)

        // 3. Hidden app remains in HIDDEN
        val hiddenDisplay = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = true,
            isUserAssigned = false,
            storedCategory = AppCategory.FINANCE,
            scores = scores,
            rail = updatedRail
        )
        assertEquals(AppCategory.HIDDEN, hiddenDisplay)

        // 4. Manually assigned app remains in assigned category
        val manualDisplay = CategoryFallbackResolver.resolveDisplayCategory(
            isHidden = false,
            isUserAssigned = true,
            storedCategory = AppCategory.PRODUCTIVITY,
            scores = scores,
            rail = updatedRail
        )
        assertEquals(AppCategory.PRODUCTIVITY, manualDisplay)
    }

    @Test
    fun `Rail never exceeds MAX_SLOTS and anchor is always present`() {
        val primaryCounts = mapOf(
            AppCategory.SOCIAL to 10,
            AppCategory.COMMUNICATION to 8,
            AppCategory.FINANCE to 6,
            AppCategory.ENTERTAINMENT to 5,
            AppCategory.GAMING to 5,
            AppCategory.NEWS to 4,
            AppCategory.SHOPPING to 4,
            AppCategory.FOOD to 4
        )

        val rail = CategoryFallbackResolver.computeRailCategories(
            primaryAppCounts = primaryCounts,
            currentRail = emptyList(),
            userAdded = emptySet(),
            userRemoved = emptySet(),
            anchor = AppCategory.TOOLS,
            maxSlots = 7,
            minApps = 3
        )

        assertTrue(rail.size <= 7)
        assertTrue(rail.contains(AppCategory.TOOLS))
    }

    @Test
    fun `Rail order stays stable across app installs and uninstalls`() {
        val initialCounts = mapOf(
            AppCategory.SOCIAL to 5,
            AppCategory.FINANCE to 4
        )

        val initialRail = CategoryFallbackResolver.computeRailCategories(
            primaryAppCounts = initialCounts,
            currentRail = listOf(AppCategory.SOCIAL, AppCategory.FINANCE, AppCategory.TOOLS),
            userAdded = emptySet(),
            userRemoved = emptySet(),
            anchor = AppCategory.TOOLS,
            maxSlots = 7,
            minApps = 3
        )

        // New app installed -> GAMING qualifies
        val updatedCounts = mapOf(
            AppCategory.SOCIAL to 5,
            AppCategory.FINANCE to 4,
            AppCategory.GAMING to 6
        )

        val updatedRail = CategoryFallbackResolver.computeRailCategories(
            primaryAppCounts = updatedCounts,
            currentRail = initialRail,
            userAdded = emptySet(),
            userRemoved = emptySet(),
            anchor = AppCategory.TOOLS,
            maxSlots = 7,
            minApps = 3
        )

        // Existing categories preserve their order (SOCIAL, FINANCE, TOOLS) and new qualifying category goes to the end
        assertEquals(AppCategory.SOCIAL, updatedRail[0])
        assertEquals(AppCategory.FINANCE, updatedRail[1])
        assertEquals(AppCategory.TOOLS, updatedRail[2])
        assertEquals(AppCategory.GAMING, updatedRail[3])
    }
}
