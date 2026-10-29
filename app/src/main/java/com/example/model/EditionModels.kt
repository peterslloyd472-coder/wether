package com.example.model

enum class AppEdition(
    val displayName: String,
    val shortLabel: String,
    val isFoss: Boolean,
    val badgeColorHex: Long,
    val tagline: String
) {
    FOSS(
        displayName = "F-Droid FOSS Edition",
        shortLabel = "F-Droid FOSS",
        isFoss = true,
        badgeColorHex = 0xFF10B981,
        tagline = "100% Free & Open Source • Zero Ads • Native GPS"
    ),
    GOOGLE(
        displayName = "Google Play Edition",
        shortLabel = "Google Edition",
        isFoss = false,
        badgeColorHex = 0xFF0284C7,
        tagline = "Google Services & Cloud Sign-In Support"
    )
}
