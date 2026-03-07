package com.bettermingle.app.ui.component

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .build()
            )
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
                .padding(Spacing.sm)
        ) {
            val ad = nativeAd!!

            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    val dp = ctx.resources.displayMetrics.density

                    val adView = NativeAdView(ctx)
                    adView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    // Horizontal layout: icon on left, text content on right
                    val rootLayout = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // Icon (40x40dp — meets 32x32dp minimum)
                    val iconView = ImageView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            (40 * dp).toInt(),
                            (40 * dp).toInt()
                        ).apply {
                            marginEnd = (10 * dp).toInt()
                        }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    adView.iconView = iconView
                    rootLayout.addView(iconView)

                    // Right side: headline + body + CTA
                    val textColumn = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }

                    val headlineView = TextView(ctx).apply {
                        textSize = 14f
                        setTextColor(ctx.getColor(android.R.color.black))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    adView.headlineView = headlineView
                    textColumn.addView(headlineView)

                    val bodyView = TextView(ctx).apply {
                        textSize = 12f
                        setTextColor(ctx.getColor(android.R.color.darker_gray))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = (2 * dp).toInt()
                        }
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    adView.bodyView = bodyView
                    textColumn.addView(bodyView)

                    val ctaView = TextView(ctx).apply {
                        textSize = 12f
                        setTextColor(android.graphics.Color.WHITE)
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(0xFF4285F4.toInt())
                            cornerRadius = 16 * dp
                        }
                        gravity = Gravity.CENTER
                        setPadding(
                            (12 * dp).toInt(),
                            (6 * dp).toInt(),
                            (12 * dp).toInt(),
                            (6 * dp).toInt()
                        )
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = (4 * dp).toInt()
                        }
                    }
                    adView.callToActionView = ctaView
                    textColumn.addView(ctaView)

                    rootLayout.addView(textColumn)
                    adView.addView(rootLayout)
                    adView
                },
                update = { adView ->
                    (adView.headlineView as? TextView)?.text = ad.headline ?: ""
                    (adView.bodyView as? TextView)?.text = ad.body ?: ""

                    val icon = ad.icon
                    val iconImageView = adView.iconView as? ImageView
                    if (icon != null) {
                        iconImageView?.setImageDrawable(icon.drawable)
                        iconImageView?.visibility = View.VISIBLE
                    } else {
                        iconImageView?.visibility = View.GONE
                    }

                    (adView.callToActionView as? TextView)?.text = ad.callToAction ?: ""

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
