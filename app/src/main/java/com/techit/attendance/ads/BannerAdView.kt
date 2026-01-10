package com.techit.attendance.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BannerAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    if (!adManager.canShowAds) {
        // Don't show ad if consent not granted
        return
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            adManager.createBannerAdView() ?: return@AndroidView android.view.View(context)
        }
    )
}