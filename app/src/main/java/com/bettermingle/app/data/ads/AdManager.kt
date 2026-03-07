package com.bettermingle.app.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.bettermingle.app.data.preferences.PremiumTier
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9561317089977080/6458752356"
    const val NATIVE_AD_UNIT_ID = "ca-app-pub-9561317089977080/7915304250"

    private const val MIN_INTERSTITIAL_INTERVAL_MS = 3 * 60 * 1000L // 3 minutes

    private var interstitialAd: InterstitialAd? = null
    private var lastInterstitialShownAt: Long = 0L

    fun hasAds(tier: PremiumTier): Boolean = tier == PremiumTier.FREE

    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.d(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownAt < MIN_INTERSTITIAL_INTERVAL_MS) {
            onDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShownAt = System.currentTimeMillis()
                    loadInterstitial(activity)
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissed()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitial(activity)
            onDismissed()
        }
    }
}
