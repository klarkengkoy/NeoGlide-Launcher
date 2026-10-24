package com.samidevstudio.neoglide.domain.classifier

import com.samidevstudio.neoglide.domain.model.AppCategory

/**
 * Highest-precision tier: well-known packages (exact match or package-segment prefix matching).
 */
internal object KnownPackages {
    private val ENTRIES: Map<String, AppCategory> = mapOf(
        "com.whatsapp" to AppCategory.COMMUNICATION,
        "org.telegram.messenger" to AppCategory.COMMUNICATION,
        "com.discord" to AppCategory.COMMUNICATION,
        "com.facebook.orca" to AppCategory.COMMUNICATION,
        "us.zoom.videomeetings" to AppCategory.COMMUNICATION,
        "com.google.android.gm" to AppCategory.COMMUNICATION,
        "com.facebook.katana" to AppCategory.SOCIAL,
        "com.instagram.android" to AppCategory.SOCIAL,
        "com.twitter.android" to AppCategory.SOCIAL,
        "com.reddit.frontpage" to AppCategory.SOCIAL,
        "com.zhiliaoapp.musically" to AppCategory.SOCIAL,
        "com.ss.android.ugc.trill" to AppCategory.SOCIAL,
        "com.google.android.youtube" to AppCategory.ENTERTAINMENT,
        "com.netflix.mediaclient" to AppCategory.ENTERTAINMENT,
        "com.spotify.music" to AppCategory.MUSIC,
        "com.google.android.apps.youtube.music" to AppCategory.MUSIC,
        "com.google.android.apps.maps" to AppCategory.MAPS,
        "com.waze" to AppCategory.MAPS,
        "com.ubercab" to AppCategory.NAVIGATION,
        "com.grabtaxi.passenger" to AppCategory.NAVIGATION,
        "com.google.android.apps.photos" to AppCategory.PHOTOGRAPHY,
        "com.google.android.apps.docs" to AppCategory.PRODUCTIVITY,
        "com.google.android.calendar" to AppCategory.PRODUCTIVITY,
        "com.google.android.keep" to AppCategory.PRODUCTIVITY,
        "com.google.android.apps.bard" to AppCategory.PRODUCTIVITY,
        "com.openai.chatgpt" to AppCategory.PRODUCTIVITY,
        "com.anthropic.claude" to AppCategory.PRODUCTIVITY,
        "com.Slack" to AppCategory.BUSINESS,
        "com.microsoft.teams" to AppCategory.BUSINESS,
        "com.google.android.deskclock" to AppCategory.UTILITIES,
        "com.google.android.calculator" to AppCategory.UTILITIES,
        "com.android.settings" to AppCategory.UTILITIES,
        "com.android.chrome" to AppCategory.BROWSER,
        "org.mozilla.firefox" to AppCategory.BROWSER,
        "com.google.android.apps.walletnfcrel" to AppCategory.WALLET,
        "com.paypal.android.p2pmobile" to AppCategory.WALLET,
        "com.shopee" to AppCategory.SHOPPING,
        "com.lazada.android" to AppCategory.SHOPPING,
        "com.amazon.mShop.android.shopping" to AppCategory.SHOPPING,
        "com.duolingo" to AppCategory.EDUCATION,
        "com.strava" to AppCategory.FITNESS,
        "com.airbnb.android" to AppCategory.TRAVEL,
        "com.booking" to AppCategory.TRAVEL
    )

    fun lookup(packageName: String): AppCategory? {
        ENTRIES[packageName]?.let { return it }
        var prefix = packageName
        while (true) {
            val dot = prefix.lastIndexOf('.')
            if (dot <= 0) return null
            prefix = prefix.substring(0, dot)
            ENTRIES[prefix]?.let { return it }
        }
    }
}
