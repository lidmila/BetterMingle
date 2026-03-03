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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.theme.AccentGold
import com.bettermingle.app.ui.theme.AccentOrange
import com.bettermingle.app.ui.theme.AccentPink
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
    var selectedPlan by remember { mutableIntStateOf(0) } // 0 = Pro, 1 = Business
    var isYearly by remember { mutableStateOf(true) }

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
                title = { Text("Premium", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentGold, AccentOrange, AccentPink)
                        )
                    ),
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
                        text = "Odemkni plný potenciál",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Neomezené akce, funkce a žádné reklamy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Plan selector
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 250)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 250)) { it / 3 }
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedPlan == 0,
                        onClick = { selectedPlan = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Mingle Pro")
                    }
                    SegmentedButton(
                        selected = selectedPlan == 1,
                        onClick = { selectedPlan = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Business")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Billing period toggle
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 300)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 300)) { it / 3 }
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isYearly,
                        onClick = { isYearly = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Měsíčně")
                    }
                    SegmentedButton(
                        selected = isYearly,
                        onClick = { isYearly = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Ročně (ušetři 32 %)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Price card
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 350)) +
                        slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 350)) { it / 3 }
            ) {
                val priceScale = remember { Animatable(0.8f) }
                LaunchedEffect(selectedPlan, isYearly) {
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
                        containerColor = PrimaryBlue
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val (price, period) = when {
                            selectedPlan == 0 && isYearly -> "649 Kč" to "/ rok"
                            selectedPlan == 0 -> "79 Kč" to "/ měsíc"
                            selectedPlan == 1 && isYearly -> "2 990 Kč" to "/ rok"
                            else -> "299 Kč" to "/ měsíc"
                        }
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
                    FeatureItem(Icons.Default.Star, "Neomezené akce a ankety"),
                    FeatureItem(Icons.Default.Groups, "Až 100 účastníků"),
                    FeatureItem(Icons.Default.PhotoLibrary, "Galerie v plném rozlišení"),
                    FeatureItem(Icons.Default.TableChart, "Export výdajů (CSV/PDF)"),
                    FeatureItem(Icons.Default.Palette, "Vlastní vizuál akce"),
                    FeatureItem(Icons.Default.AutoAwesome, "Bez reklam")
                )
            } else {
                listOf(
                    FeatureItem(Icons.Default.Star, "Vše z Pro"),
                    FeatureItem(Icons.Default.Groups, "Až 500 účastníků"),
                    FeatureItem(Icons.Default.Poll, "Více spoluorganizátorů"),
                    FeatureItem(Icons.Default.Palette, "Firemní branding"),
                    FeatureItem(Icons.Default.TableChart, "Reporty a statistiky"),
                    FeatureItem(Icons.Default.AutoAwesome, "API přístup a SSO")
                )
            }

            features.forEachIndexed { index, feature ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 400 + index * 60)) +
                            slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 400 + index * 60)) { it / 3 }
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
                BetterMingleButton(
                    text = if (selectedPlan == 0) "Získat Mingle Pro" else "Získat Business",
                    onClick = {
                        val productId = if (selectedPlan == 0) "mingle_pro" else "mingle_business"
                        onSubscribe(productId, isYearly)
                    },
                    isCta = true
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "Předplatné se automaticky obnovuje. Zrušit můžeš kdykoli v Google Play.",
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
