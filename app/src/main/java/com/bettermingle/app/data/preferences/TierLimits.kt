package com.bettermingle.app.data.preferences

import com.bettermingle.app.ui.component.AVATAR_COUNT

object TierLimits {
    fun maxEvents(tier: PremiumTier) = when (tier) {
        PremiumTier.FREE -> 2
        PremiumTier.PRO -> 5
        PremiumTier.BUSINESS -> Int.MAX_VALUE
    }

    fun maxParticipants(tier: PremiumTier) = when (tier) {
        PremiumTier.FREE -> 8
        PremiumTier.PRO -> 100
        PremiumTier.BUSINESS -> Int.MAX_VALUE
    }

    fun maxPolls(tier: PremiumTier) = when (tier) {
        PremiumTier.FREE -> 1
        PremiumTier.PRO -> Int.MAX_VALUE
        PremiumTier.BUSINESS -> Int.MAX_VALUE
    }

    fun canExportExpenses(tier: PremiumTier) = tier == PremiumTier.PRO || tier == PremiumTier.BUSINESS

    fun canAddCoOrganizers(tier: PremiumTier) = tier == PremiumTier.PRO || tier == PremiumTier.BUSINESS

    fun hasAds(tier: PremiumTier) = tier == PremiumTier.FREE

    fun maxAvatars(tier: PremiumTier) = when (tier) {
        PremiumTier.FREE -> 15
        else -> AVATAR_COUNT
    }

    fun canUseAllTemplates(tier: PremiumTier) = tier != PremiumTier.FREE

    fun canRepeatEvent(tier: PremiumTier) = tier != PremiumTier.FREE

    fun canExportSummary(tier: PremiumTier) = tier != PremiumTier.FREE

    fun canUseWidget(tier: PremiumTier) = tier != PremiumTier.FREE
}
