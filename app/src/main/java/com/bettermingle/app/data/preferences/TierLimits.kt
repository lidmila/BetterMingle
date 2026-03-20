package com.bettermingle.app.data.preferences

import com.bettermingle.app.data.model.EventModule
import com.bettermingle.app.ui.component.AVATAR_COUNT

object TierLimits {
    val PREMIUM_MODULES = setOf(EventModule.CATERING, EventModule.ROOMS, EventModule.CARPOOL)
    val BUSINESS_MODULES = setOf(EventModule.BUDGET)

    fun canUseModule(tier: PremiumTier, module: EventModule): Boolean = when {
        module in BUSINESS_MODULES -> tier == PremiumTier.BUSINESS
        module in PREMIUM_MODULES -> tier != PremiumTier.FREE
        else -> true
    }

    fun maxEvents(tier: PremiumTier) = when (tier) {
        PremiumTier.FREE -> 1
        PremiumTier.PRO -> 3
        PremiumTier.BUSINESS -> Int.MAX_VALUE
    }

    fun maxParticipants(tier: PremiumTier) = when (tier) {
        PremiumTier.FREE -> 20
        PremiumTier.PRO -> 100
        PremiumTier.BUSINESS -> Int.MAX_VALUE
    }

    fun maxPolls(tier: PremiumTier) = when (tier) {
        PremiumTier.FREE -> 1
        PremiumTier.PRO -> 3
        PremiumTier.BUSINESS -> Int.MAX_VALUE
    }

    fun canAddCoOrganizers(tier: PremiumTier) = tier == PremiumTier.PRO || tier == PremiumTier.BUSINESS

    fun hasAds(tier: PremiumTier) = tier == PremiumTier.FREE

    fun maxAvatars(tier: PremiumTier) = when (tier) {
        PremiumTier.FREE -> 15
        else -> AVATAR_COUNT
    }

    fun canRepeatEvent(tier: PremiumTier) = tier != PremiumTier.FREE

    fun canExportSummary(tier: PremiumTier) = tier != PremiumTier.FREE
}
