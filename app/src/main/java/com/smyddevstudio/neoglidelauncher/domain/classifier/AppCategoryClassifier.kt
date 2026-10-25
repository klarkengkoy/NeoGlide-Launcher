package com.smyddevstudio.neoglidelauncher.domain.classifier

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.smyddevstudio.neoglidelauncher.domain.model.AppCategory

class AppCategoryClassifier(
    private val enabledCategories: Set<String> = emptySet(),
) {

    data class ClassificationResult(
        val natural: AppCategory?,
        val heuristic: AppCategory?,
    )

    /**
     * Classifies a single app into a category.
     */
    fun classify(packageName: String, context: Context): AppCategory {
        val packageManager = context.packageManager

        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
            val label = packageManager.getApplicationLabel(appInfo).toString()
            val appCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appInfo.category else -1

            classify(packageName, label, appCategory, permissions)
        } catch (_: Exception) {
            AppCategory.OTHER
        }
    }

    /**
     * Pure logic for classification, returning the final decided category.
     */
    fun classify(
        packageName: String,
        label: String,
        appInfoCategory: Int,
        permissions: List<String>
    ): AppCategory {
        val detailed = classifyDetailed(packageName, label, appInfoCategory, permissions)
        
        detailed.natural?.let { return it }

        if ((detailed.heuristic != null) && (enabledCategories.isEmpty() || detailed.heuristic.name in enabledCategories)) {
            return detailed.heuristic
        }

        return AppCategory.OTHER
    }

    /**
     * Computes the winning category along with top category scores for an app.
     */
    fun classifyWithScores(
        packageName: String,
        label: String,
        appInfoCategory: Int,
        permissions: List<String>
    ): Pair<AppCategory, Map<AppCategory, Float>> {
        val scores = mutableMapOf<AppCategory, Float>()

        // Pre-Step 0: Known Packages
        KnownPackages.lookup(packageName)?.let { knownCat ->
            scores[knownCat] = 1.0f
            return knownCat to scores
        }

        // Step 1: System Tag
        mapApplicationInfoCategory(appInfoCategory)?.let { sysCat ->
            scores[sysCat] = (scores[sysCat] ?: 0f) + 1.0f
        }

        // Step 2: Keyword Heuristic v2
        val kwScores = KeywordHeuristic.score(packageName, label)
        kwScores.forEach { (cat, score) ->
            scores[cat] = (scores[cat] ?: 0f) + score
        }

        // Step 3: High Confidence Permissions
        HIGH_CONFIDENCE_PERMISSION_MAP.forEach { (perm, cat) ->
            if (permissions.contains(perm)) {
                scores[cat] = (scores[cat] ?: 0f) + 0.5f
            }
        }

        val winner = classify(packageName, label, appInfoCategory, permissions)
        if (winner !in scores) {
            scores[winner] = 0.5f
        }

        // Top 3 scores sorted descending
        val top3 = scores.entries
            .sortedByDescending { it.value }
            .take(3)
            .associate { it.key to it.value }

        return winner to top3
    }

    /**
     * Checks if an app matches any signal for a specific target category.
     */
    fun fitsCategory(
        packageName: String,
        label: String,
        appInfoCategory: Int,
        permissions: List<String>,
        target: AppCategory
    ): Boolean {
        val detailed = classifyDetailed(packageName, label, appInfoCategory, permissions)
        
        if (detailed.natural?.name == target.name) return true
        if (detailed.heuristic?.name == target.name) return true
        
        return false
    }

    /**
     * Returns both the natural and heuristic matches using the 4-stage waterfall.
     */
    fun classifyDetailed(
        packageName: String,
        label: String,
        appInfoCategory: Int,
        permissions: List<String>
    ): ClassificationResult {
        // Pre-Step 0: Known Packages
        KnownPackages.lookup(packageName)?.let { knownCat ->
            return ClassificationResult(natural = knownCat, heuristic = null)
        }

        // Step 1: System Tag
        val systemCategory = mapApplicationInfoCategory(appInfoCategory)
        if (systemCategory == AppCategory.GAMING) {
            return ClassificationResult(natural = systemCategory, heuristic = null)
        }

        // Step 2: Keyword Heuristic v2 with Clear Winner Check
        val keywordScores = KeywordHeuristic.score(packageName, label)
        val keywordMatch = evaluateKeywordScores(keywordScores)

        // If Step 1 system tag is corroborated or keyword match was clear
        if (systemCategory != null && (keywordMatch == null || keywordMatch == systemCategory)) {
            return ClassificationResult(natural = systemCategory, heuristic = keywordMatch)
        }

        // Step 3: High-Confidence Permissions Only
        val permissionMatch = if (keywordMatch == null) scoreHighConfidencePermissions(permissions) else null

        return ClassificationResult(
            natural = systemCategory,
            heuristic = keywordMatch ?: permissionMatch
        )
    }

    private fun evaluateKeywordScores(scores: Map<AppCategory, Float>): AppCategory? {
        if (scores.isEmpty()) return null

        val sorted = scores.entries.sortedByDescending { it.value }
        val top = sorted[0]
        val runnerUp = sorted.getOrNull(1)?.value ?: 0f

        if (top.value >= MIN_KEYWORD_THRESHOLD && (top.value - runnerUp) >= MARGIN_THRESHOLD) {
            return top.key
        }

        return null
    }

    private fun mapApplicationInfoCategory(category: Int): AppCategory? {
        return when (category) {
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMING
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.PHOTOGRAPHY
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.MUSIC
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.NAVIGATION
            else -> null
        }
    }

    private fun scoreHighConfidencePermissions(permissions: List<String>): AppCategory? {
        val scores = mutableMapOf<AppCategory, Int>()

        HIGH_CONFIDENCE_PERMISSION_MAP.forEach { (perm, category) ->
            if (permissions.contains(perm)) {
                scores[category] = scores.getOrDefault(category, 0) + 1
            }
        }

        if (scores.isEmpty()) return null

        val maxScore = scores.values.maxOrNull() ?: 0
        if (maxScore == 0) return null

        val candidates = scores.filter { it.value == maxScore }.keys
        if (candidates.size == 1) return candidates.first()

        return CATEGORY_PRIORITY.firstOrNull { it in candidates }
    }

    companion object {
        private const val MIN_KEYWORD_THRESHOLD = 0.8f
        private const val MARGIN_THRESHOLD = 0.3f

        private val HIGH_CONFIDENCE_PERMISSION_MAP = mapOf(
            "android.permission.ACTIVITY_RECOGNITION" to AppCategory.HEALTH,
            "android.permission.BODY_SENSORS" to AppCategory.HEALTH,
            "com.google.android.gms.permission.ACTIVITY_RECOGNITION" to AppCategory.HEALTH,

            "android.permission.SEND_SMS" to AppCategory.COMMUNICATION,
            "android.permission.RECEIVE_SMS" to AppCategory.COMMUNICATION,

            "android.permission.READ_CALENDAR" to AppCategory.PRODUCTIVITY,
            "android.permission.WRITE_CALENDAR" to AppCategory.PRODUCTIVITY,

            "android.permission.PACKAGE_USAGE_STATS" to AppCategory.TOOLS,
            "android.permission.BIND_ACCESSIBILITY_SERVICE" to AppCategory.TOOLS
        )

        private val CATEGORY_PRIORITY = listOf(
            AppCategory.COMMUNICATION,
            AppCategory.SOCIAL,
            AppCategory.WALLET,
            AppCategory.FINANCE,
            AppCategory.PRODUCTIVITY,
            AppCategory.BUSINESS,
            AppCategory.MAPS,
            AppCategory.NAVIGATION,
            AppCategory.GAMING,
            AppCategory.ENTERTAINMENT,
            AppCategory.MUSIC,
            AppCategory.PHOTOGRAPHY,
            AppCategory.HEALTH,
            AppCategory.FITNESS,
            AppCategory.FOOD,
            AppCategory.EDUCATION,
            AppCategory.BOOKS,
            AppCategory.WEATHER,
            AppCategory.NEWS,
            AppCategory.SPORTS,
            AppCategory.HOME,
            AppCategory.LIFESTYLE,
            AppCategory.KIDS,
            AppCategory.TRAVEL,
            AppCategory.TOOLS,
            AppCategory.UTILITIES,
            AppCategory.BROWSER,
            AppCategory.SECURITY,
            AppCategory.PERSONALIZATION,
            AppCategory.DEVELOPER,
            AppCategory.OTHER
        )
    }
}
