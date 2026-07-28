package com.orbit.browser.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.Dp

/**
 * Orbit Browser Animation Tokens matching Figma design (`orbit-browser.html`).
 *
 * CSS Specs:
 * --spring:  cubic-bezier(0.34, 1.56, 0.64, 1)
 * --smooth:  cubic-bezier(0.0, 0.0, 0.2, 1)
 * --ease:    cubic-bezier(0.4, 0.0, 0.2, 1)
 * --bounce:  cubic-bezier(0.68, -0.45, 0.27, 1.55)
 */
object OBEasing {
    val FigmaSpring = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)   // Exact iOS system transition curve
    val FigmaSmooth = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)   // iOS fluid curve
    val FigmaEase   = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)   // iOS smooth curve
    val FigmaBounce = CubicBezierEasing(0.28f, 0.8f, 0.24f, 1.0f)

    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val Accelerate = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val Standard   = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    val Emphasized = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
}

object OBDuration {
    const val Instant = 100
    const val Fast    = 220
    const val Normal  = 300
    const val Medium  = 340
    const val Slow    = 380
    const val Slower  = 420
    const val Sheet   = 380
}

object OBSpring {
    val Island = spring<Float>(dampingRatio = 0.62f, stiffness = 380f)
    val IslandDp = spring<Dp>(dampingRatio = 0.62f, stiffness = 380f)
    val Panel = spring<Float>(dampingRatio = 0.72f, stiffness = 440f)
    val TabCard = spring<Float>(dampingRatio = 0.68f, stiffness = 320f)
    val Icon = spring<Float>(dampingRatio = 0.75f, stiffness = 520f)
    val Overlay = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 340f)
    val PageScale = spring<Float>(dampingRatio = 0.80f, stiffness = 360f)
    val Badge = spring<Float>(dampingRatio = 0.45f, stiffness = 600f)
}

fun staggerDelay(index: Int, baseDelay: Int = 40, maxDelay: Int = 240): Int =
    (index * baseDelay).coerceAtMost(maxDelay)

val islandSizeSpec: AnimationSpec<Dp> = tween(durationMillis = OBDuration.Slower, easing = OBEasing.FigmaSpring)
val islandAlphaSpec: AnimationSpec<Float> = tween(durationMillis = OBDuration.Fast, easing = OBEasing.FigmaEase)
val overlayAlphaSpec: AnimationSpec<Float> = tween(durationMillis = OBDuration.Normal, easing = OBEasing.FigmaEase)
val pageScaleSpec: AnimationSpec<Float> = tween(durationMillis = OBDuration.Sheet, easing = OBEasing.FigmaSpring)
val cardEntranceSpec: AnimationSpec<Float> = tween(durationMillis = OBDuration.Slow, easing = OBEasing.FigmaSpring)

