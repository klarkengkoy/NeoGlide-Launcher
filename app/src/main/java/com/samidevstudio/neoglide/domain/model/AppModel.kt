package com.samidevstudio.neoglide.domain.model

data class AppModel(
    val packageName: String,
    val label: String,
    val category: AppCategory,
    val isFavorite: Boolean = false,
    val lastUsedTime: Long = 0L,
    val installTime: Long = 0L,
    val dominantHue: Float = 0f
)

data class AppShortcut(
    val id: String,
    val label: String,
    val packageName: String,
    val icon: Any? = null
)

data class AppCategory(
    val name: String, 
    val isCustom: Boolean = false,
    val iconName: String? = null,
    val label: String? = null
) {
    val displayName: String get() = label ?: name.lowercase().replaceFirstChar { it.uppercase() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppCategory) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {
        val SOCIAL = AppCategory("SOCIAL", iconName = "chat")
        val PRODUCTIVITY = AppCategory("PRODUCTIVITY", iconName = "build")
        val ENTERTAINMENT = AppCategory("ENTERTAINMENT", iconName = "play_circle")
        val GAMING = AppCategory("GAMING", iconName = "sports_esports")
        val FINANCE = AppCategory("FINANCE", iconName = "account_balance")
        val SHOPPING = AppCategory("SHOPPING", iconName = "shopping_cart")
        val PHOTOGRAPHY = AppCategory("PHOTOGRAPHY", iconName = "camera_alt")
        val FITNESS = AppCategory("FITNESS", iconName = "favorite")
        val NAVIGATION = AppCategory("NAVIGATION", iconName = "directions_car")
        val FOOD = AppCategory("FOOD", iconName = "restaurant")
        val EDUCATION = AppCategory("EDUCATION", iconName = "school")
        val UTILITIES = AppCategory("UTILITIES", iconName = "settings")
        val NEWS = AppCategory("NEWS", iconName = "newspaper")
        val MUSIC = AppCategory("MUSIC", iconName = "music_note")
        val TRAVEL = AppCategory("TRAVEL", iconName = "flight")
        val COMMUNICATION = AppCategory("COMMUNICATION", iconName = "chat_bubble")
        val HEALTH = AppCategory("HEALTH", iconName = "health_and_safety")
        val WEATHER = AppCategory("WEATHER", iconName = "wb_sunny")
        val LIFESTYLE = AppCategory("LIFESTYLE", iconName = "style")
        val BOOKS = AppCategory("BOOKS", iconName = "menu_book")
        val BUSINESS = AppCategory("BUSINESS", iconName = "business_center")
        val TOOLS = AppCategory("TOOLS", iconName = "handyman")
        val MAPS = AppCategory("MAPS", iconName = "map")
        val SPORTS = AppCategory("SPORTS", iconName = "sports_soccer")
        val HOME = AppCategory("HOME", iconName = "home")
        val KIDS = AppCategory("KIDS", iconName = "child_care")
        val WALLET = AppCategory("WALLET", iconName = "account_balance_wallet")
        val OTHER = AppCategory("OTHER", iconName = "apps")
        
        // Internal launcher categories
        val HIDDEN = AppCategory("HIDDEN", iconName = "visibility_off")
        val FOLDER = AppCategory("FOLDER", iconName = "folder")

        val builtInValues = listOf(
            SOCIAL, COMMUNICATION, PRODUCTIVITY, BUSINESS, ENTERTAINMENT, GAMING, 
            FINANCE, WALLET, SHOPPING, PHOTOGRAPHY, HEALTH, FITNESS, 
            NAVIGATION, MAPS, FOOD, EDUCATION, TOOLS, UTILITIES, 
            WEATHER, NEWS, MUSIC, TRAVEL, LIFESTYLE, BOOKS, 
            SPORTS, HOME, KIDS, OTHER
        )

        fun fromString(value: String): AppCategory {
            val builtIn = builtInValues.find { it.name == value }
            if (builtIn != null) return builtIn
            return when (value) {
                "HIDDEN" -> HIDDEN
                "FOLDER" -> FOLDER
                else -> AppCategory(value, isCustom = true)
            }
        }
    }

    override fun toString(): String = name
}
