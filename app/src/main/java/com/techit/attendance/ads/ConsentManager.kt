package com.techit.attendance.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private var consentForm: ConsentForm? = null

    companion object {
        private const val TAG = "ConsentManager"

        @Volatile
        private var isMobileAdsInitialized = false

        // Thread-safe way to check if ads are initialized
        fun isMobileAdsInitialized(): Boolean = isMobileAdsInitialized
    }

    /**
     * Helper method to check if ads can be requested
     */
    fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }

    /**
     * Helper method to check if privacy options entry point is required
     */
    fun isPrivacyOptionsRequired(): Boolean {
        return consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * Request consent information update and load consent form if needed
     */
    fun gatherConsent(
        activity: Activity,
        onConsentGatheringComplete: (canRequestAds: Boolean) -> Unit
    ) {
        // For testing, you can set debug geography
        // Remove this in production!
        val debugSettings = ConsentDebugSettings.Builder(context)
             .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
             .addTestDeviceHashedId("YOUR_TEST_DEVICE_ID") // Add your test device
            .build()

        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings)
            .build()

        // Request consent information update
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Consent info updated successfully
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.e(TAG, "Form error: ${formError.message}")
                    }

                    // Consent gathering completed
                    // Can now request ads if consent allows
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }

                    onConsentGatheringComplete(consentInformation.canRequestAds())
                }
            },
            { requestConsentError ->
                // Consent gathering failed
                Log.e(TAG, "Consent request error: ${requestConsentError.message}")
                onConsentGatheringComplete(consentInformation.canRequestAds())
            }
        )
    }

    /**
     * Show privacy options form (for users to change their consent)
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onDismiss: (canRequestAds: Boolean) -> Unit
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.e(TAG, "Privacy options form error: ${formError.message}")
            }

            // After user changes consent, check if we can now request ads
            val canRequest = consentInformation.canRequestAds()
            if (canRequest && !isMobileAdsInitialized) {
                initializeMobileAdsSdk()
            }

            onDismiss(canRequest)
        }
    }

    /**
     * Initialize Mobile Ads SDK
     * Only call this after consent has been gathered
     * This is thread-safe and will only initialize once
     */
    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitialized) {
            Log.d(TAG, "Mobile Ads SDK already initialized")
            return
        }

        synchronized(this) {
            if (isMobileAdsInitialized) {
                return
            }

            Log.d(TAG, "Initializing Mobile Ads SDK...")
            MobileAds.initialize(context) { initStatus ->
                Log.d(TAG, "Mobile Ads SDK initialized successfully")
                Log.d(TAG, "Adapter status: ${initStatus.adapterStatusMap}")
            }

            isMobileAdsInitialized = true
        }
    }

    /**
     * Reset consent for testing purposes
     * REMOVE THIS IN PRODUCTION
     */
    fun resetConsentForTesting() {
        consentInformation.reset()
        isMobileAdsInitialized = false
    }
}