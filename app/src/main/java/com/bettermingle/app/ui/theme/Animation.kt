package com.bettermingle.app.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

object BetterMingleMotion {
    // Durations
    const val QUICK = 150
    const val STANDARD = 300
    const val EMPHASIZE = 500

    // Navigation transitions
    val screenEnter: EnterTransition = fadeIn(tween(STANDARD)) +
            slideInHorizontally(tween(STANDARD, easing = FastOutSlowInEasing)) { it / 4 }

    val screenExit: ExitTransition = fadeOut(tween(QUICK)) +
            slideOutHorizontally(tween(STANDARD, easing = FastOutSlowInEasing)) { -it / 4 }

    val screenPopEnter: EnterTransition = fadeIn(tween(STANDARD)) +
            slideInHorizontally(tween(STANDARD, easing = FastOutSlowInEasing)) { -it / 4 }

    val screenPopExit: ExitTransition = fadeOut(tween(QUICK)) +
            slideOutHorizontally(tween(STANDARD, easing = FastOutSlowInEasing)) { it / 4 }

    // Bottom sheet / dialog
    val bottomSheetEnter: EnterTransition = fadeIn(tween(STANDARD)) +
            slideInVertically(tween(STANDARD, easing = FastOutSlowInEasing)) { it / 3 }

    val bottomSheetExit: ExitTransition = fadeOut(tween(QUICK)) +
            slideOutVertically(tween(STANDARD)) { it / 3 }

    // List item animations
    val listItemEnter: EnterTransition = fadeIn(tween(STANDARD)) +
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) { it / 2 }

    // FAB
    val fabEnter: EnterTransition = fadeIn(tween(STANDARD)) +
            scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                initialScale = 0.6f
            )

    val fabExit: ExitTransition = fadeOut(tween(QUICK)) +
            scaleOut(tween(QUICK), targetScale = 0.6f)

    // Card press effect
    fun <S> cardContentTransform(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (fadeIn(tween(STANDARD)) + scaleIn(tween(STANDARD), initialScale = 0.95f))
            .togetherWith(fadeOut(tween(QUICK)) + scaleOut(tween(QUICK), targetScale = 0.95f))
    }

    // Success/celebration
    val celebrationEnter: EnterTransition = fadeIn(tween(EMPHASIZE)) +
            scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialScale = 0.3f
            )
}
