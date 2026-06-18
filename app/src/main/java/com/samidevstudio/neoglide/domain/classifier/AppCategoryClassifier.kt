package com.samidevstudio.neoglide.domain.classifier

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.samidevstudio.neoglide.domain.model.AppCategory

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

        // Signal 2/3 (Heuristic) only win if that category is currently enabled
        if ((detailed.heuristic != null) && (detailed.heuristic.name in enabledCategories)) {
            return detailed.heuristic
        }

        return AppCategory.OTHER
    }

    /**
     * Returns both the natural and heuristic matches.
     */
    fun classifyDetailed(
        packageName: String,
        label: String,
        appInfoCategory: Int,
        permissions: List<String>
    ): ClassificationResult {
        val natural = mapApplicationInfoCategory(appInfoCategory)
        
        // For heuristic, we want the best match regardless of whether it's enabled
        // so we can decide to "steal" it later if the category gets enabled.
        val keywordMatch = scoreKeywords(packageName, label)
        val permissionMatch = if (keywordMatch == null) scorePermissions(permissions) else null
        
        return ClassificationResult(
            natural = natural,
            heuristic = keywordMatch ?: permissionMatch
        )
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
            else -> null // CATEGORY_UNDEFINED or others
        }
    }

    private fun scorePermissions(permissions: List<String>): AppCategory? {
        val scores = mutableMapOf<AppCategory, Int>()

        PERMISSION_MAP.forEach { (perm, category) ->
            if (permissions.contains(perm)) {
                scores[category] = scores.getOrDefault(category, 0) + 1
            }
        }

        // Special rule for GAMING: VIBRATE alone is not enough
        val gamingScore = scores.getOrDefault(AppCategory.GAMING, 0)
        if (gamingScore > 0) {
            val hasVibrate = permissions.contains("android.permission.VIBRATE")
            val hasOtherGaming = permissions.contains("com.android.launcher.permission.INSTALL_SHORTCUT")
            if (hasVibrate && !hasOtherGaming) {
                scores[AppCategory.GAMING] = 0
            }
        }

        return getWinner(scores)
    }

    private fun scoreKeywords(packageName: String, label: String): AppCategory? {
        val tokens = (tokenize(packageName) + tokenize(label)).distinct()
        val scores = mutableMapOf<AppCategory, Int>()

        KEYWORD_MAP.forEach { (category, keywords) ->
            keywords.forEach { keyword ->
                if (tokens.contains(keyword)) {
                    scores[category] = scores.getOrDefault(category, 0) + 1
                }
            }
        }

        return getWinner(scores)
    }

    private fun getWinner(scores: Map<AppCategory, Int>): AppCategory? {
        if (scores.isEmpty()) return null

        val maxScore = scores.values.maxOrNull() ?: 0
        if (maxScore == 0) return null

        val candidates = scores.filter { it.value == maxScore }.keys
        if (candidates.size == 1) return candidates.first()

        // Tie-break using priority order
        return CATEGORY_PRIORITY.firstOrNull { it in candidates }
    }

    private fun tokenize(text: String): List<String> {
        return text.split(".", "_", "-")
            .asSequence()
            .flatMap { it.split(Regex("(?<=[a-z])(?=[A-Z])")) }
            .map { it.lowercase() }
            .filter { it.isNotBlank() }
            .toList()
    }

    companion object {
        private val PERMISSION_MAP = mapOf(
            "android.permission.READ_CONTACTS" to AppCategory.SOCIAL,
            "android.permission.WRITE_CONTACTS" to AppCategory.SOCIAL,
            "android.permission.READ_CALL_LOG" to AppCategory.SOCIAL,
            "android.permission.SEND_SMS" to AppCategory.SOCIAL,
            "android.permission.RECEIVE_SMS" to AppCategory.SOCIAL,

            "android.permission.ACTIVITY_RECOGNITION" to AppCategory.HEALTH,
            "android.permission.BODY_SENSORS" to AppCategory.HEALTH,
            "com.google.android.gms.permission.ACTIVITY_RECOGNITION" to AppCategory.HEALTH,

            "android.permission.CAMERA" to AppCategory.PHOTOGRAPHY,

            "android.permission.ACCESS_FINE_LOCATION" to AppCategory.MAPS,
            "android.permission.ACCESS_COARSE_LOCATION" to AppCategory.MAPS,
            "android.permission.ACCESS_BACKGROUND_LOCATION" to AppCategory.MAPS,

            "android.permission.USE_BIOMETRIC" to AppCategory.WALLET,
            "android.permission.USE_FINGERPRINT" to AppCategory.WALLET,
            "com.android.vending.BILLING" to AppCategory.WALLET,

            "android.permission.RECORD_AUDIO" to AppCategory.MUSIC,
            "android.permission.MODIFY_AUDIO_SETTINGS" to AppCategory.MUSIC,

            "android.permission.RECEIVE_BOOT_COMPLETED" to AppCategory.TOOLS,
            "android.permission.WRITE_SETTINGS" to AppCategory.TOOLS,
            "android.permission.WRITE_SECURE_SETTINGS" to AppCategory.TOOLS,
            "android.permission.PACKAGE_USAGE_STATS" to AppCategory.TOOLS,
            "android.permission.BIND_ACCESSIBILITY_SERVICE" to AppCategory.TOOLS,
            "android.permission.SET_ALARM" to AppCategory.TOOLS,

            "android.permission.VIBRATE" to AppCategory.GAMING,
            "com.android.launcher.permission.INSTALL_SHORTCUT" to AppCategory.GAMING,

            "android.permission.READ_CALENDAR" to AppCategory.PRODUCTIVITY,
            "android.permission.WRITE_CALENDAR" to AppCategory.PRODUCTIVITY,
            "android.permission.READ_SYNC_SETTINGS" to AppCategory.PRODUCTIVITY
        )

        private val KEYWORD_MAP = mapOf(
            AppCategory.SOCIAL to listOf("social", "dating", "friend", "network", "community", "share", "post", "facebook", "instagram", "twitter", "linkedin", "tiktok"),
            AppCategory.COMMUNICATION to listOf("chat", "message", "messenger", "talk", "meet", "video", "call", "whatsapp", "telegram", "slack", "discord", "teams", "skype", "zoom"),
            AppCategory.PRODUCTIVITY to listOf("office", "doc", "docs", "note", "notes", "task", "tasks", "todo", "calendar", "email", "mail", "pdf", "scan", "sign", "sheet", "drive", "cloud", "sync"),
            AppCategory.BUSINESS to listOf("work", "business", "crm", "erp", "manage", "project", "teams", "slack", "hustle"),
            AppCategory.ENTERTAINMENT to listOf("video", "watch", "stream", "movie", "film", "tv", "show", "anime", "episode", "drama", "cartoon", "clip", "media", "player", "netflix", "youtube", "hulu", "disney"),
            AppCategory.GAMING to listOf("game", "games", "play", "puzzle", "quiz", "rpg", "battle", "arena", "clash", "craft", "quest", "run", "jump", "shoot", "race", "tower", "arcade", "casino", "poker"),
            AppCategory.FINANCE to listOf("bank", "banking", "money", "cash", "finance", "invest", "stock", "crypto", "budget", "loan", "credit", "tax", "accounting"),
            AppCategory.WALLET to listOf("pay", "payment", "wallet", "card", "transfer", "gpay", "samsungpay", "applepay", "visa", "mastercard", "paypal", "venmo"),
            AppCategory.SHOPPING to listOf("shop", "shopping", "store", "buy", "sell", "market", "mall", "cart", "order", "deal", "coupon", "price", "product", "merchant", "ebay", "amazon", "lazada", "shopee"),
            AppCategory.PHOTOGRAPHY to listOf("photo", "camera", "gallery", "pic", "picture", "image", "selfie", "filter", "edit", "crop", "album", "snapshot", "lens", "portrait", "gallery"),
            AppCategory.HEALTH to listOf("health", "heart", "sleep", "meditat", "doctor", "clinic", "hospital", "pharmacy", "medicine", "period", "tracker"),
            AppCategory.FITNESS to listOf("fitness", "workout", "exercise", "gym", "run", "running", "step", "pedometer", "calorie", "diet", "yoga", "sport", "sports", "training", "weight"),
            AppCategory.MAPS to listOf("map", "maps", "navigate", "navigation", "gps", "route", "direction", "traffic", "transit", "location", "track", "waze", "googlemaps"),
            AppCategory.NAVIGATION to listOf("commute", "uber", "grab", "lyft", "bolt", "taxi", "bike", "scooter", "bus", "train", "flight"),
            AppCategory.FOOD to listOf("food", "recipe", "cook", "cooking", "restaurant", "eat", "meal", "delivery", "order", "menu", "kitchen", "diet", "grocery", "drink", "coffee", "snack", "starbucks", "mcdonalds"),
            AppCategory.EDUCATION to listOf("learn", "learning", "study", "student", "school", "course", "lesson", "tutor", "quiz", "language", "math", "science", "history", "kids", "child", "education", "edu", "duolingo", "khan"),
            AppCategory.TOOLS to listOf("tool", "tools", "cleaner", "clean", "boost", "battery", "cpu", "ram", "storage", "file", "manager", "backup", "antivirus", "vpn", "wifi", "bluetooth", "keyboard", "flashlight", "qr", "barcode", "compress", "zip", "unzip"),
            AppCategory.UTILITIES to listOf("launcher", "clock", "alarm", "util", "system", "setting", "calculator", "calendar"),
            AppCategory.WEATHER to listOf("weather", "forecast", "temp", "temperature", "rain", "snow", "sun", "sunny", "storm", "wind", "humidity"),
            AppCategory.NEWS to listOf("news", "headline", "article", "feed", "rss", "read", "blog", "media", "press", "daily", "report", "bulletin", "magazine", "journal", "nyt", "bbc", "cnn"),
            AppCategory.MUSIC to listOf("music", "song", "songs", "audio", "sound", "radio", "podcast", "playlist", "beat", "dj", "spotify", "listen", "ringtone", "instrument", "guitar", "piano", "karaoke"),
            AppCategory.TRAVEL to listOf("travel", "hotel", "trip", "tour", "vacation", "holiday", "airline", "airport", "visa", "passport", "airbnb", "booking", "expedia", "agoda", "hostel"),
            AppCategory.LIFESTYLE to listOf("lifestyle", "style", "home", "garden", "decor", "fashion", "beauty", "wedding", "dating", "zodiac", "horoscope"),
            AppCategory.BOOKS to listOf("book", "books", "read", "reader", "ebook", "kindle", "audiobook", "novel", "comic", "manga", "library"),
            AppCategory.SPORTS to listOf("sport", "sports", "soccer", "football", "basketball", "baseball", "tennis", "golf", "score", "nfl", "nba", "fifa"),
            AppCategory.HOME to listOf("home", "house", "smart", "iot", "light", "security", "camera", "lock", "nest", "hue", "ring"),
            AppCategory.KIDS to listOf("kids", "child", "baby", "parent", "toy", "story", "cartoon", "disney", "education", "game")
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
            AppCategory.OTHER
        )
    }
}
