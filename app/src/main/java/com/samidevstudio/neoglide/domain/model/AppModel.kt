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

enum class AppCategory {
    COMMUNICATION, GAMES, MEDIA, UTILITIES, SOCIAL, SHOPPING, EDUCATION, LIFESTYLE, OTHER, SYSTEM, HIDDEN, FOLDER;

    companion object {
        fun fromString(value: String): AppCategory {
            return try {
                valueOf(value)
            } catch (_: Exception) {
                OTHER
            }
        }
    }
}
