package com.bettermingle.app.ui.screen.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bettermingle.app.R
import com.bettermingle.app.data.preferences.PremiumTier
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onNavigateBack: () -> Unit,
    onSubscribe: (productId: String, isYearly: Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = null)
    val currentTier = settings?.premiumTier ?: PremiumTier.FREE
    val premiumUntil = settings?.premiumUntil
    val isLifetime = currentTier != PremiumTier.FREE && premiumUntil == null
    val hasActivePlan = currentTier != PremiumTier.FREE

    var visible by remember { mutableStateOf(false) }
    var isYearly by remember { mutableStateOf(true) }
    var selectedPlan by remember { mutableIntStateOf(0) } // 0 = Pro, 1 = Business

    val crownScale = remember { Animatable(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    val shimmerRotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    LaunchedEffect(Unit) {
        visible = true
        crownScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.upgrade_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated crown icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(crownScale.value)
                    .rotate(shimmerRotation)
                    .clip(CircleShape)
                    .background(if (selectedPlan == 0) PrimaryBlue else AccentGold),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = TextOnColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 150)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 150)) { it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.upgrade_headline),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.upgrade_subheadline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Active plan banner
            if (hasActivePlan) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 170)) +
                            slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 170)) { it / 3 }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLifetime) AccentGold.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.12f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isLifetime) AccentGold else PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isLifetime) stringResource(R.string.upgrade_has_lifetime)
                                           else stringResource(R.string.upgrade_has_plan, currentTier.name),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!isLifetime && premiumUntil != null) {
                                    val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                                        .format(java.util.Date(premiumUntil))
                                    Text(
                                        text = stringResource(R.string.upgrade_expires, dateStr),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            if (isLifetime) {
                // Lifetime users see only the banner above
                Spacer(modifier = Modifier.height(Spacing.lg))
                Text(
                    text = stringResource(R.string.upgrade_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }

            if (!isLifetime) {
            // Plan selector: Pro / Business
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 200)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 200)) { it / 3 }
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedPlan == 0,
                        onClick = { selectedPlan = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.upgrade_plan_pro))
                    }
                    SegmentedButton(
                        selected = selectedPlan == 1,
                        onClick = { selectedPlan = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.upgrade_plan_business))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Billing period toggle
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 250)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 250)) { it / 3 }
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isYearly,
                        onClick = { isYearly = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.upgrade_billing_monthly))
                    }
                    SegmentedButton(
                        selected = isYearly,
                        onClick = { isYearly = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.upgrade_billing_yearly))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Price card
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 300)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 300)) { it / 3 }
            ) {
                val priceScale = remember { Animatable(0.8f) }
                LaunchedEffect(isYearly, selectedPlan) {
                    priceScale.snapTo(0.8f)
                    priceScale.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(priceScale.value),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPlan == 0) PrimaryBlue else AccentGold
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val (price, period) = if (selectedPlan == 0) {
                            if (isYearly) stringResource(R.string.upgrade_price_pro_yearly) to stringResource(R.string.upgrade_period_yearly)
                            else stringResource(R.string.upgrade_price_pro_monthly) to stringResource(R.string.upgrade_period_monthly)
                        } else {
                            if (isYearly) stringResource(R.string.upgrade_price_business_yearly) to stringResource(R.string.upgrade_period_yearly)
                            else stringResource(R.string.upgrade_price_business_monthly) to stringResource(R.string.upgrade_period_monthly)
                        }
                        Text(
                            text = if (selectedPlan == 0) stringResource(R.string.upgrade_card_title_pro) else stringResource(R.string.upgrade_card_title_business),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextOnColor
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = price,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextOnColor
                        )
                        Text(
                            text = period,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextOnColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Features list
            val features = if (selectedPlan == 0) {
                listOf(
                    FeatureItem(Icons.Default.Star, stringResource(R.string.upgrade_feature_pro_events)),
                    FeatureItem(Icons.Default.Groups, stringResource(R.string.upgrade_feature_pro_participants)),
                    FeatureItem(Icons.Default.HowToVote, stringResource(R.string.upgrade_feature_pro_polls)),
                    FeatureItem(Icons.Default.Assessment, stringResource(R.string.upgrade_feature_organizer_overview)),
                    FeatureItem(Icons.Default.PersonAdd, stringResource(R.string.upgrade_feature_multi_organizers)),
                    FeatureItem(Icons.Default.Restaurant, stringResource(R.string.upgrade_feature_premium_modules)),
                    FeatureItem(Icons.Default.AutoAwesome, stringResource(R.string.upgrade_feature_no_ads))
                )
            } else {
                listOf(
                    FeatureItem(Icons.Default.AllInclusive, stringResource(R.string.upgrade_feature_business_events)),
                    FeatureItem(Icons.Default.Groups, stringResource(R.string.upgrade_feature_business_participants)),
                    FeatureItem(Icons.Default.HowToVote, stringResource(R.string.upgrade_feature_business_polls)),
                    FeatureItem(Icons.Default.Star, stringResource(R.string.upgrade_feature_business_templates)),
                    FeatureItem(Icons.Default.Assessment, stringResource(R.string.upgrade_feature_organizer_overview)),
                    FeatureItem(Icons.Default.PersonAdd, stringResource(R.string.upgrade_feature_multi_organizers)),
                    FeatureItem(Icons.Default.Restaurant, stringResource(R.string.upgrade_feature_premium_modules)),
                    FeatureItem(Icons.Default.AutoAwesome, stringResource(R.string.upgrade_feature_no_ads))
                )
            }

            features.forEachIndexed { index, feature ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 350 + index * 60)) +
                            slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 350 + index * 60)) { it / 3 }
                ) {
                    FeatureRow(feature = feature)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Subscribe button
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 700)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 700)) { it / 2 }
            ) {
                val productId = if (selectedPlan == 0) "mingle_pro" else "mingle_business"
                val selectedTier = if (selectedPlan == 0) PremiumTier.PRO else PremiumTier.BUSINESS
                val isCurrentPlan = currentTier == selectedTier
                val isDowngrade = (currentTier == PremiumTier.BUSINESS && selectedTier == PremiumTier.PRO)

                val buttonText = when {
                    isCurrentPlan -> stringResource(R.string.upgrade_active_badge)
                    isDowngrade -> stringResource(R.string.upgrade_downgrade)
                    selectedPlan == 0 -> stringResource(R.string.upgrade_button_pro)
                    else -> stringResource(R.string.upgrade_button_business)
                }

                BetterMingleButton(
                    text = buttonText,
                    onClick = {
                        if (!isCurrentPlan && !isDowngrade) {
                            onSubscribe(productId, isYearly)
                        }
                    },
                    isCta = !isCurrentPlan && !isDowngrade,
                    enabled = !isCurrentPlan && !isDowngrade
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Lifetime card
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 750)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 750)) { it / 3 }
            ) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.upgrade_lifetime_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = stringResource(R.string.upgrade_lifetime_price),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.upgrade_lifetime_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        BetterMingleButton(
                            text = stringResource(R.string.upgrade_lifetime_button),
                            onClick = {
                                onSubscribe("mingle_lifetime", false)
                            },
                            isCta = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = stringResource(R.string.upgrade_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.lg))
            } // end if (!isLifetime)

            // ── Compare all plans table ──
            Spacer(modifier = Modifier.height(Spacing.md))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 800)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 800)) { it / 3 }
            ) {
                ComparisonTable()
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

// ── Comparison table data ──

private sealed class CellValue {
    data class TextValue(val text: String) : CellValue()
    data class BoolValue(val enabled: Boolean) : CellValue()
}

private data class ComparisonRow(
    val label: String,
    val free: CellValue,
    val pro: CellValue,
    val business: CellValue
)

@Composable
private fun ComparisonTable() {
    val unlimited = stringResource(R.string.upgrade_compare_unlimited)

    val rows = listOf(
        // ── Limits ──
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_events),
            free = CellValue.TextValue(stringResource(R.string.upgrade_compare_events_free)),
            pro = CellValue.TextValue(stringResource(R.string.upgrade_compare_events_pro)),
            business = CellValue.TextValue(unlimited)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_participants),
            free = CellValue.TextValue(stringResource(R.string.upgrade_compare_participants_free)),
            pro = CellValue.TextValue(stringResource(R.string.upgrade_compare_participants_pro)),
            business = CellValue.TextValue(unlimited)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_polls),
            free = CellValue.TextValue(stringResource(R.string.upgrade_compare_polls_free)),
            pro = CellValue.TextValue(stringResource(R.string.upgrade_compare_polls_pro)),
            business = CellValue.TextValue(unlimited)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_avatars),
            free = CellValue.TextValue(stringResource(R.string.upgrade_compare_avatars_free)),
            pro = CellValue.TextValue(stringResource(R.string.upgrade_compare_avatars_premium)),
            business = CellValue.TextValue(stringResource(R.string.upgrade_compare_avatars_premium))
        ),
        // ── Premium features ──
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_templates),
            free = CellValue.BoolValue(false),
            pro = CellValue.BoolValue(false),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_ads),
            free = CellValue.BoolValue(false),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_export_summary),
            free = CellValue.BoolValue(false),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_co_organizers),
            free = CellValue.BoolValue(false),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_repeat_event),
            free = CellValue.BoolValue(false),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        // ── Modules (all tiers) ──
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_chat),
            free = CellValue.BoolValue(true),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_expenses),
            free = CellValue.BoolValue(true),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_carpool),
            free = CellValue.BoolValue(false),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_rooms),
            free = CellValue.BoolValue(false),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_schedule),
            free = CellValue.BoolValue(true),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_tasks),
            free = CellValue.BoolValue(true),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_packing),
            free = CellValue.BoolValue(true),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_wishlist),
            free = CellValue.BoolValue(true),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
        ComparisonRow(
            label = stringResource(R.string.upgrade_compare_catering),
            free = CellValue.BoolValue(false),
            pro = CellValue.BoolValue(true),
            business = CellValue.BoolValue(true)
        ),
    )

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = stringResource(R.string.upgrade_compare_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small
                    )
                    .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.upgrade_compare_feature),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = stringResource(R.string.upgrade_compare_free),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = stringResource(R.string.upgrade_compare_pro),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = PrimaryBlue,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = stringResource(R.string.upgrade_compare_business),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = AccentGold,
                    modifier = Modifier.weight(0.8f)
                )
            }

            rows.forEachIndexed { index, row ->
                ComparisonRowItem(row = row, isEven = index % 2 == 0)
            }
        }
    }
}

@Composable
private fun ComparisonRowItem(row: ComparisonRow, isEven: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEven) Modifier.background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) else Modifier
            )
            .padding(vertical = 6.dp, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.4f)
        )
        ComparisonCell(value = row.free, modifier = Modifier.weight(0.8f))
        ComparisonCell(value = row.pro, modifier = Modifier.weight(0.8f))
        ComparisonCell(value = row.business, modifier = Modifier.weight(0.8f))
    }
}

@Composable
private fun ComparisonCell(value: CellValue, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (value) {
            is CellValue.TextValue -> Text(
                text = value.text,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            is CellValue.BoolValue -> Icon(
                imageVector = if (value.enabled) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (value.enabled) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class FeatureItem(
    val icon: ImageVector,
    val text: String
)

@Composable
private fun FeatureRow(feature: FeatureItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = feature.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = AccentGold,
            modifier = Modifier.size(20.dp)
        )
    }
}
