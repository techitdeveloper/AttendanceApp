package com.techit.attendance

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.techit.attendance.ads.AdManager
import com.techit.attendance.ads.ConsentManager
import com.techit.attendance.data.database.AppDatabase
import com.techit.attendance.ui.navigation.NavGraph
import com.techit.attendance.ui.theme.AttendanceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var adManager: AdManager
    private lateinit var consentManager: ConsentManager

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(applicationContext)
        adManager = AdManager(this)
        consentManager = ConsentManager(this)

        // Gather consent on app start
        gatherConsent()
    }

    private fun gatherConsent() {
        consentManager.gatherConsent(this) { canRequestAds ->
            Log.d(TAG, "Consent gathering complete. Can request ads: $canRequestAds")

            if (canRequestAds) {
                // Enable ads in AdManager
                adManager.enableAds()

                // Preload interstitial ad
                CoroutineScope(Dispatchers.Main).launch {
                    adManager.loadInterstitialAd()
                }
            } else {
                // User denied consent or consent not required
                adManager.disableAds()
            }

            // Set up the UI
            setContent {
                AttendanceTheme {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        database = database,
                        adManager = adManager,
                        consentManager = consentManager,
                        activity = this
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if consent is still valid when app returns to foreground
        if (consentManager.canRequestAds() && !adManager.canShowAds) {
            adManager.enableAds()
            adManager.loadInterstitialAd()
        }
    }
}