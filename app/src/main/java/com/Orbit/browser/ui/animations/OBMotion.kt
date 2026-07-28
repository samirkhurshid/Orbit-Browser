package com.orbit.browser.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.TransformOrigin

/**
 * Shared transition specs for screen-level and overlay-level motion.
 * Used by AnimatedVisibility / AnimatedContent across Orbit screens.
 * Exactly mirroring timing and curves from Figma (`orbit-browser.html`).
 */
object OBMotion {

    val overlayEnter = fadeIn(animationSpec = tween(OBDuration.Normal, easing = OBEasing.FigmaEase))
    val overlayExit  = fadeOut(animationSpec = tween(OBDuration.Fast, easing = OBEasing.FigmaEase))

    val panelEnterFromTop = slideInVertically(
        initialOffsetY = { -20 },
        animationSpec  = tween(OBDuration.Slow, easing = OBEasing.FigmaSpring),
    ) + fadeIn(tween(OBDuration.Normal, easing = OBEasing.FigmaEase))

    val panelExitToTop = slideOutVertically(
        targetOffsetY = { -20 },
        animationSpec = tween(OBDuration.Fast, easing = OBEasing.FigmaEase),
    ) + fadeOut(tween(OBDuration.Fast, easing = OBEasing.FigmaEase))

    val sheetEnterFromBottom = slideInVertically(
        initialOffsetY = { it },
        animationSpec  = tween(OBDuration.Sheet, easing = OBEasing.FigmaSpring),
    ) + fadeIn(tween(OBDuration.Normal, easing = OBEasing.FigmaEase))

    val sheetExitToBottom = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(OBDuration.Slow, easing = OBEasing.FigmaEase),
    ) + fadeOut(tween(OBDuration.Fast, easing = OBEasing.FigmaEase))

    val menuEnter = scaleIn(
        initialScale    = 0.95f,
        transformOrigin = TransformOrigin(1f, 1f),
        animationSpec   = tween(OBDuration.Normal, easing = OBEasing.FigmaSpring),
    ) + fadeIn(tween(OBDuration.Fast, easing = OBEasing.FigmaEase))

    val menuExit = scaleOut(
        targetScale     = 0.95f,
        transformOrigin = TransformOrigin(1f, 1f),
        animationSpec   = tween(OBDuration.Fast, easing = OBEasing.FigmaEase),
    ) + fadeOut(tween(OBDuration.Fast, easing = OBEasing.FigmaEase))
}

object OBTransitions {
    val cardStagger: (Int) -> AnimationSpec<Float> = { index ->
        tween(durationMillis = OBDuration.Slow, delayMillis = staggerDelay(index), easing = OBEasing.FigmaSpring)
    }
}

