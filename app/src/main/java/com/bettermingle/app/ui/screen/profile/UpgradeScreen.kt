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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.BackgroundPrimary
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    onNavigateBack: () -> Unit,
    onSubscribe: (productId: String, isYearly: Boolean) -> Unit = { _, _ -> }
) {
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
                    containerColor = BackgroundPrimary
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
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

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
                    FeatureItem(Icons.Default.TableChart, stringResource(R.string.upgrade_feature_export_csv)),
                    FeatureItem(Icons.Default.PersonAdd, stringResource(R.string.upgrade_feature_multi_organizers)),
                    FeatureItem(Icons.Default.AutoAwesome, stringResource(R.string.upgrade_feature_no_ads))
                )
            } else {
                listOf(
                    FeatureItem(Icons.Default.AllInclusive, stringResource(R.string.upgrade_feature_business_events)),
                    FeatureItem(Icons.Default.Groups, stringResource(R.string.upgrade_feature_business_participants)),
                    FeatureItem(Icons.Default.HowToVote, stringResource(R.string.upgrade_feature_business_polls)),
                    FeatureItem(Icons.Default.TableChart, stringResource(R.string.upgrade_feature_export_csv)),
                    FeatureItem(Icons.Default.PersonAdd, stringResource(R.string.upgrade_feature_multi_organizers)),
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
                BetterMingleButton(
                    text = if (selectedPlan == 0) stringResource(R.string.upgrade_button_pro) else stringResource(R.string.upgrade_button_business),
                    onClick = {
                        onSubscribe(productId, isYearly)
                    },
                    isCta = true
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
                            color = TextSecondary,
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
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.lg))
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
