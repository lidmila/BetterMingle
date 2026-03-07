package com.bettermingle.app.ui.component

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bettermingle.app.R
import com.bettermingle.app.data.ads.AdManager
import com.bettermingle.app.ui.theme.Spacing
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adLoaded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, AdManager.NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                nativeAd = ad
                adLoaded = true
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLoaded = false
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    if (adLoaded && nativeAd != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.md)
        ) {
            val ad = nativeAd!!

            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    val adView = NativeAdView(ctx)

                    val headlineView = TextView(ctx).apply {
                        textSize = 16f
                        setTextColor(ctx.getColor(android.R.color.black))
                    }
                    adView.headlineView = headlineView
                    adView.addView(headlineView)

                    val bodyView = TextView(ctx).apply {
                        textSize = 14f
                        setTextColor(ctx.getColor(android.R.color.darker_gray))
                    }
                    adView.bodyView = bodyView
                    adView.addView(bodyView)

                    adView
                },
                update = { adView ->
                    (adView.headlineView as? TextView)?.text = ad.headline ?: ""
                    (adView.bodyView as? TextView)?.text = ad.body ?: ""
                    adView.setNativeAd(ad)
                }
            )

            Text(
                text = stringResource(R.string.ad_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
