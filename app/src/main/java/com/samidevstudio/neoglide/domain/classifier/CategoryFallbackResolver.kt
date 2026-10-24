package com.samidevstudio.neoglide.domain.classifier

import android.content.Context
import com.samidevstudio.neoglide.data.repository.CategoryBarType
import com.samidevstudio.neoglide.domain.model.AppCategory

object CategoryFallbackResolver {

    const val MAX_SLOTS = 7
    const val MIN_APPS = 3
    const val DEFAULT_MIN_SCORE = 0.5f

    /**
     * Dynamically calculates max rail slots based on screen dimensions and CategoryBarType.
     */
    fun getMaxRailSlots(context: Context, type: CategoryBarType): Int {
        if (type == CategoryBarType.NONE) return Int.MAX_VALUE
        val displayMetrics = context.resources.displayMetrics
        val isVertical = type == CategoryBarType.LEFT || type == CategoryBarType.RIGHT
        val totalAvailable = if (isVertical) {
            (displayMetrics.heightPixels / displayMetrics.density) * 0.9f
        } else {
            (displayMetrics.widthPixels / displayMetrics.density) - 32
        }
        // 24dp icon + 2dp padding = 26dp per tab
        return ((totalAvailable + 2) / 26).toInt().coerceAtLeast(1)
    }

    /**
     * One-hop RELATED categories lookup table, preference-ordered.
     */
    val RELATED_CATEGORIES: Map<AppCategory, List<AppCategory>> by lazy {
        mapOf(
            AppCategory.SOCIAL to listOf(AppCategory.COMMUNICATION, AppCategory.ENTERTAINMENT),
            AppCategory.COMMUNICATION to listOf(AppCategory.SOCIAL, AppCategory.PRODUCTIVITY),
            AppCategory.PRODUCTIVITY to listOf(AppCategory.BUSINESS, AppCategory.TOOLS),
            AppCategory.BUSINESS to listOf(AppCategory.PRODUCTIVITY, AppCategory.FINANCE),
            AppCategory.ENTERTAINMENT to listOf(AppCategory.MUSIC, AppCategory.SOCIAL),
            AppCategory.GAMING to listOf(AppCategory.ENTERTAINMENT),
            AppCategory.FINANCE to listOf(AppCategory.WALLET, AppCategory.BUSINESS),
            AppCategory.WALLET to listOf(AppCategory.FINANCE, AppCategory.SHOPPING),
            AppCategory.SHOPPING to listOf(AppCategory.FINANCE, AppCategory.LIFESTYLE),
            AppCategory.PHOTOGRAPHY to listOf(AppCategory.ENTERTAINMENT, AppCategory.TOOLS),
            AppCategory.HEALTH to listOf(AppCategory.FITNESS, AppCategory.LIFESTYLE),
            AppCategory.FITNESS to listOf(AppCategory.HEALTH, AppCategory.SPORTS),
            AppCategory.MAPS to listOf(AppCategory.NAVIGATION, AppCategory.TRAVEL),
            AppCategory.NAVIGATION to listOf(AppCategory.MAPS, AppCategory.TRAVEL),
            AppCategory.FOOD to listOf(AppCategory.LIFESTYLE, AppCategory.SHOPPING),
            AppCategory.EDUCATION to listOf(AppCategory.BOOKS, AppCategory.PRODUCTIVITY, AppCategory.KIDS, AppCategory.TOOLS),
            AppCategory.TOOLS to listOf(AppCategory.UTILITIES, AppCategory.PRODUCTIVITY),
            AppCategory.UTILITIES to listOf(AppCategory.TOOLS),
            AppCategory.WEATHER to listOf(AppCategory.NEWS, AppCategory.UTILITIES),
            AppCategory.NEWS to listOf(AppCategory.BOOKS, AppCategory.ENTERTAINMENT),
            AppCategory.MUSIC to listOf(AppCategory.ENTERTAINMENT),
            AppCategory.TRAVEL to listOf(AppCategory.MAPS, AppCategory.LIFESTYLE),
            AppCategory.LIFESTYLE to listOf(AppCategory.SHOPPING, AppCategory.HEALTH),
            AppCategory.BOOKS to listOf(AppCategory.EDUCATION, AppCategory.ENTERTAINMENT),
            AppCategory.SPORTS to listOf(AppCategory.FITNESS, AppCategory.NEWS),
            AppCategory.HOME to listOf(AppCategory.TOOLS, AppCategory.LIFESTYLE),
            AppCategory.KIDS to listOf(AppCategory.EDUCATION, AppCategory.ENTERTAINMENT),
            AppCategory.BROWSER to listOf(AppCategory.TOOLS, AppCategory.UTILITIES),
            AppCategory.SECURITY to listOf(AppCategory.TOOLS, AppCategory.UTILITIES),
            AppCategory.PERSONALIZATION to listOf(AppCategory.TOOLS, AppCategory.UTILITIES),
            AppCategory.DEVELOPER to listOf(AppCategory.TOOLS, AppCategory.PRODUCTIVITY)
        )
    }

    /**
     * Resolves the category under which an app should be displayed in the launcher rail.
     */
    fun resolveDisplayCategory(
        isHidden: Boolean,
        isUserAssigned: Boolean,
        storedCategory: AppCategory,
        scores: Map<AppCategory, Float>,
        rail: Set<AppCategory>,
        anchor: AppCategory = AppCategory.TOOLS,
        minScore: Float = DEFAULT_MIN_SCORE
    ): AppCategory {
        // a. If the app is in Hidden: show only in Hidden.
        if (isHidden) {
            return AppCategory.HIDDEN
        }

        // b. Else if the user manually assigned a category: show there.
        if (isUserAssigned) {
            return storedCategory
        }

        // c. Else resolve with fallback:
        val rankedQualified = scores.entries
            .filter { it.value >= minScore }
            .sortedByDescending { it.value }
            .map { it.key }

        // 1. Show in the first ranked category that is in the rail.
        for (cat in rankedQualified) {
            if (cat in rail) {
                return cat
            }
        }

        // 2. If none are in the rail, check RELATED categories (one hop only) of each ranked category in order.
        for (cat in rankedQualified) {
            val related = RELATED_CATEGORIES[cat] ?: getRelatedForUnknownCategory(cat)
            for (rel in related) {
                if (rel in rail) {
                    return rel
                }
            }
        }

        // Also check related categories for the stored primary category if not present in scores
        if (storedCategory !in rankedQualified) {
            if (storedCategory in rail) return storedCategory
            val related = RELATED_CATEGORIES[storedCategory] ?: getRelatedForUnknownCategory(storedCategory)
            for (rel in related) {
                if (rel in rail) {
                    return rel
                }
            }
        }

        // 3. If still none, use the ANCHOR category.
        return if (anchor in rail) anchor else AppCategory.TOOLS
    }

    private fun getRelatedForUnknownCategory(category: AppCategory): List<AppCategory> {
        if (category.name.equals("BANKING", ignoreCase = true)) {
            return listOf(AppCategory.FINANCE, AppCategory.WALLET)
        }
        return emptyList()
    }

    /**
     * Computes the stable set of rail categories based on primary app counts and user additions/removals.
     */
    fun computeRailCategories(
        primaryAppCounts: Map<AppCategory, Int>,
        currentRail: List<AppCategory>,
        userAdded: Set<AppCategory>,
        userRemoved: Set<AppCategory>,
        anchor: AppCategory = AppCategory.TOOLS,
        maxSlots: Int = MAX_SLOTS,
        minApps: Int = MIN_APPS
    ): List<AppCategory> {
        val result = mutableListOf<AppCategory>()

        // 1. Keep already shown categories in currentRail if not removed by user
        for (cat in currentRail) {
            if (cat !in userRemoved && cat !in result) {
                result.add(cat)
            }
        }

        // 2. Ensure anchor is present
        if (anchor !in result && anchor !in userRemoved) {
            result.add(anchor)
        }

        // 3. Ensure user-added categories are present
        for (cat in userAdded) {
            if (cat !in result && result.size < maxSlots) {
                result.add(cat)
            }
        }

        // 4. Find newly qualifying categories from primary app counts
        if (result.size < maxSlots) {
            val qualifyingCandidates = primaryAppCounts.entries
                .filter { (cat, count) ->
                    count >= minApps && cat !in result && cat !in userRemoved && cat != AppCategory.HIDDEN && cat != AppCategory.OTHER
                }
                .sortedWith(
                    compareByDescending<Map.Entry<AppCategory, Int>> { it.value }
                        .thenBy { AppCategory.builtInValues.indexOf(it.key) }
                )

            for (entry in qualifyingCandidates) {
                if (result.size >= maxSlots) break
                result.add(entry.key)
            }
        }

        // Trim to maxSlots if necessary while guaranteeing anchor is present
        if (result.size > maxSlots) {
            val trimmed = result.take(maxSlots).toMutableList()
            if (anchor !in trimmed) {
                trimmed[trimmed.lastIndex] = anchor
            }
            return trimmed
        }

        return result
    }

    fun serializeScores(scores: Map<AppCategory, Float>): String {
        return scores.entries.joinToString("|") { "${it.key.name}:${it.value}" }
    }

    fun deserializeScores(raw: String?): Map<AppCategory, Float> {
        if (raw.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<AppCategory, Float>()
        raw.split("|").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val cat = AppCategory.fromString(parts[0].trim())
                val score = parts[1].trim().toFloatOrNull() ?: 0f
                result[cat] = score
            }
        }
        return result
    }
}
