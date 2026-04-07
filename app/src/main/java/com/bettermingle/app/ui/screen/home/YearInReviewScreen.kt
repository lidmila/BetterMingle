package com.bettermingle.app.ui.screen.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bettermingle.app.R
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.Success
import com.bettermingle.app.viewmodel.YearInReviewViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val Purple = Color(0xFF8B7CF6)

private data class ReviewPageData(
    val gradientStart: Color,
    val gradientEnd: Color,
    val icon: ImageVector,
    @DrawableRes val illustration: Int? = null,
    val number: Int? = null,
    val label: String,
    val description: String,
    val subtitle: String = ""
)

@Composable
fun YearInReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: YearInReviewViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val graphicsLayer = rememberGraphicsLayer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Resolve strings in composable scope
    val introLabel = stringResource(R.string.year_review_intro_label)
    val introDesc = stringResource(R.string.year_review_intro_desc)
    val eventsLabel = stringResource(R.string.year_review_events_label)
    val eventsDesc = stringResource(R.string.year_review_events_desc, stats.totalEvents)
    val peopleLabel = stringResource(R.string.year_review_people_label)
    val peopleDesc = stringResource(R.string.year_review_people_desc)
    val daysLabel = stringResource(R.string.year_review_days_label)
    val daysDesc = stringResource(R.string.year_review_days_desc, stats.totalEventDays)
    val longestLabel = stringResource(R.string.year_review_longest_label)
    val longestDesc = stringResource(R.string.year_review_longest_desc, stats.longestEvent, stats.longestEventDays)
    val longestSubtitle = stringResource(R.string.year_review_longest_subtitle)
    val locationDesc = stringResource(R.string.year_review_location_desc)
    val locationSubtitle = stringResource(R.string.year_review_location_subtitle)
    val monthDesc = stringResource(R.string.year_review_month_desc)
    val monthSubtitle = stringResource(R.string.year_review_month_subtitle)
    val avgLabel = stringResource(R.string.year_review_avg_label)
    val avgDesc = stringResource(R.string.year_review_avg_desc, stats.avgParticipantsPerEvent)
    val avgSubtitle = stringResource(R.string.year_review_avg_subtitle)
    val outroLabel = stringResource(R.string.year_review_outro_label)
    val outroDesc = stringResource(R.string.year_review_outro_desc)

    // Build pages dynamically, skip empty ones
    val pages = remember(stats, introLabel) {
        buildList {
            // Intro with logo
            add(ReviewPageData(
                PrimaryBlue, Purple, Icons.Default.Celebration,
                illustration = R.drawable.il_review_intro,

                label = introLabel,
                description = introDesc,
                subtitle = "${stats.year}"
            ))
            // Total events
            if (stats.totalEvents > 0) add(ReviewPageData(
                PrimaryBlue, AccentPink, Icons.Default.Event,
                illustration = R.drawable.il_review_events,
                number = stats.totalEvents,
                label = eventsLabel,
                description = eventsDesc
            ))
            // Total people
            if (stats.totalUniqueParticipants > 0) add(ReviewPageData(
                AccentPink, AccentOrange, Icons.Default.People,
                illustration = R.drawable.il_review_people,
                number = stats.totalUniqueParticipants,
                label = peopleLabel,
                description = peopleDesc
            ))
            // Total days
            if (stats.totalEventDays > 0) add(ReviewPageData(
                AccentOrange, AccentGold, Icons.Default.CalendarMonth,
                illustration = R.drawable.il_review_days,
                number = stats.totalEventDays,
                label = daysLabel,
                description = daysDesc
            ))
            // Longest event
            if (stats.longestEvent.isNotEmpty() && stats.longestEventDays > 1) add(ReviewPageData(
                AccentGold, AccentOrange, Icons.Default.Event,
                number = stats.longestEventDays,
                label = longestLabel,
                description = longestDesc,
                subtitle = longestSubtitle
            ))
            // Most visited location
            if (stats.mostVisitedLocation.isNotEmpty()) add(ReviewPageData(
                AccentGold, Success, Icons.Default.LocationOn,
                illustration = R.drawable.il_review_location,
                label = stats.mostVisitedLocation,
                description = locationDesc,
                subtitle = locationSubtitle
            ))
            // Most active month
            if (stats.mostActiveMonth.isNotEmpty()) add(ReviewPageData(
                Success, PrimaryBlue, Icons.AutoMirrored.Filled.TrendingUp,
                label = stats.mostActiveMonth,
                description = monthDesc,
                subtitle = monthSubtitle
            ))
            // Average participants
            if (stats.avgParticipantsPerEvent > 0) add(ReviewPageData(
                PrimaryBlue, Purple, Icons.Default.People,
                illustration = R.drawable.il_review_people,
                number = stats.avgParticipantsPerEvent,
                label = avgLabel,
                description = avgDesc,
                subtitle = avgSubtitle
            ))
            // Outro with logo
            add(ReviewPageData(
                Purple, AccentPink, Icons.Default.Favorite,
                illustration = R.drawable.il_review_outro,

                label = outroLabel,
                description = outroDesc,
                subtitle = "❤\uFE0F"
            ))
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }
        ) { pageIndex ->
            val page = pages[pageIndex]
            ReviewPage(page = page)
        }

        // Top bar with back + share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = Color.White
                )
            }
            IconButton(onClick = {
                scope.launch {
                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    shareBitmap(context, bitmap)
                }
            }) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.year_review_share),
                    tint = Color.White
                )
            }
        }

        // Bottom section: tagline + dots
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.year_review_tagline),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun ReviewPage(page: ReviewPageData) {
    val illustrationScale = remember { Animatable(0f) }

    LaunchedEffect(page) {
        illustrationScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(page.gradientStart, page.gradientEnd))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Spacing.xl)
        ) {
            // Illustration or icon in semi-transparent circle
            if (page.illustration != null) {
                Image(
                    painter = painterResource(id = page.illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(illustrationScale.value),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(illustrationScale.value)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Animated number or static text
            if (page.number != null) {
                AnimatedCounter(target = page.number)
            }

            Spacer(modifier = Modifier.height(if (page.number != null) 8.dp else 0.dp))

            // Label
            Text(
                text = page.label,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            // Subtitle
            if (page.subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = page.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AnimatedCounter(target: Int) {
    var displayValue by remember(target) { mutableIntStateOf(0) }

    LaunchedEffect(target) {
        if (target <= 0) return@LaunchedEffect
        val steps = 40
        val delayPerStep = 1200L / steps
        for (i in 1..steps) {
            displayValue = (target * i) / steps
            delay(delayPerStep)
        }
        displayValue = target
    }

    Text(
        text = "$displayValue",
        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        color = Color.White
    )
}

private fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cacheDir = File(context.cacheDir, "shared_images")
        cacheDir.mkdirs()
        val file = File(cacheDir, "year_in_review.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.year_review_share_chooser)))
    } catch (_: Exception) {
        // Silently fail if sharing is not possible
    }
}
