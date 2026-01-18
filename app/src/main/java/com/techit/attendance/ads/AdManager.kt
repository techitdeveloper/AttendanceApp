package com.techit.attendance.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {

    companion object {
        private const val TAG = "AdManager"

        // TEST AD UNITS - Replace with your real ad unit IDs before publishing
        const val BANNER_AD_UNIT_ID = "ca-app-pub-1670302520389848/7780052473" // test ad
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1670302520389848/7586295565" //tes ad
        // Ad frequency control
        private var lastInterstitialTime: Long = 0
        private const val INTERSTITIAL_COOLDOWN_MS = 30000L // 30 seconds minimum between ads
        private var interstitialShowCount = 0
        private const val MAX_INTERSTITIALS_PER_SESSION = 5 // Max 5 interstitials per app session
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    var canShowAds = false
        private set

    /**
     * Call this after consent is granted to enable ad loading
     */
    fun enableAds() {
        canShowAds = true
        Log.d(TAG, "Ads enabled")
    }

    /**
     * Call this if consent is revoked to disable ads
     */
    fun disableAds() {
        canShowAds = false
        interstitialAd = null
        isLoadingInterstitial = false
        Log.d(TAG, "Ads disabled")
    }

    /**
     * Create a banner ad for Compose
     */
    fun createBannerAdView(): AdView? {
        if (!canShowAds) {
            Log.w(TAG, "Cannot show ads - consent not granted")
            return null
        }

        if (!ConsentManager.isMobileAdsInitialized()) {
            Log.w(TAG, "Cannot show ads - MobileAds not initialized yet")
            return null
        }

        return AdView(context).apply {
            adUnitId = BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)

            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Banner ad failed to load: ${error.message}")
                }

                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded successfully")
                }
            }

            loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * Load interstitial ad in advance
     */
    fun loadInterstitialAd() {
        if (!canShowAds) {
            Log.w(TAG, "Cannot load ads - consent not granted")
            return
        }

        if (!ConsentManager.isMobileAdsInitialized()) {
            Log.w(TAG, "Cannot load ads - MobileAds not initialized yet")
            return
        }

        if (isLoadingInterstitial || interstitialAd != null) {
            Log.d(TAG, "Interstitial already loading or loaded")
            return // Already loading or loaded
        }

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()

        Log.d(TAG, "Loading interstitial ad...")
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    setupInterstitialCallbacks()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    private fun setupInterstitialCallbacks() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                interstitialAd = null
                // Preload next ad
                loadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${error.message}")
                interstitialAd = null
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad shown")
                interstitialAd = null
            }
        }
    }

    /**
     * Show interstitial ad with frequency capping
     * Returns true if ad was shown, false otherwise
     */
    fun showInterstitialAd(activity: Activity): Boolean {
        if (!canShowAds) {
            Log.w(TAG, "Cannot show ads - consent not granted")
            return false
        }

        if (!ConsentManager.isMobileAdsInitialized()) {
            Log.w(TAG, "Cannot show ads - MobileAds not initialized yet")
            return false
        }

        val currentTime = System.currentTimeMillis()

        // Check frequency cap
        if (currentTime - lastInterstitialTime < INTERSTITIAL_COOLDOWN_MS) {
            Log.d(TAG, "Interstitial cooldown active")
            return false // Too soon since last ad
        }

        // Check session limit
        if (interstitialShowCount >= MAX_INTERSTITIALS_PER_SESSION) {
            Log.d(TAG, "Max interstitials per session reached")
            return false // Too many ads this session
        }

        interstitialAd?.let { ad ->
            ad.show(activity)
            lastInterstitialTime = currentTime
            interstitialShowCount++
            Log.d(TAG, "Showing interstitial ad (count: $interstitialShowCount)")
            return true
        }

        // If ad not ready, try to load for next time
        if (!isLoadingInterstitial) {
            Log.d(TAG, "No interstitial ready, loading one")
            loadInterstitialAd()
        }

        return false
    }

    /**
     * Check if enough time has passed to show another interstitial
     */
    fun canShowInterstitial(): Boolean {
        val currentTime = System.currentTimeMillis()
        return canShowAds
                && ConsentManager.isMobileAdsInitialized()
                && (currentTime - lastInterstitialTime >= INTERSTITIAL_COOLDOWN_MS)
                && (interstitialShowCount < MAX_INTERSTITIALS_PER_SESSION)
                && (interstitialAd != null)
    }
}