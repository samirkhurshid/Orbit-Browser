package com.orbit.browser.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.Dp

/**
 * Orbit Browser iOS-Style Steady & Gradual Animation Tokens
 */
object OBEasing {
    // iOS standard system bezier curve (steady, gradual, ultra-smooth)
    val IosCurve    = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    val FigmaSpring = IosCurve
    val FigmaSmooth = IosCurve
    val FigmaEase   = IosCurve
    val FigmaBounce = CubicBezierEasing(0.28f, 0.8f, 0.24f, 1.0f)

    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val Accelerate = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val Standard   = IosCurve
    val Emphasized = IosCurve
}

object OBDuration {
    const val Instant = 100
    const val Fast    = 200
    const val Normal  = 280
    const val Medium  = 300
    const val Slow    = 320
    const val Slower  = 350
    const val Sheet   = 320
}

object OBSpring {
    // iOS steady & gradual spring response (0.85f damping = no harsh bounce, steady gradual flow)
    val IosSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 300f)
    val IosSpringDp = spring<Dp>(dampingRatio = 0.85f, stiffness = 300f)
    val Island = IosSpring
    val IslandDp = IosSpringDp
    val Panel = spring<Float>(dampingRatio = 0.85f, stiffness = 320f)
    val TabCard = spring<Float>(dampingRatio = 0.85f, stiffness = 300f)
    val Icon = spring<Float>(dampingRatio = 0.82f, stiffness = 400f)
    val Overlay = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 320f)
    val PageScale = spring<Float>(dampingRatio = 0.85f, stiffness = 300f)
    val Badge = spring<Float>(dampingRatio = 0.75f, stiffness = 450f)
}

fun staggerDelay(index: Int, baseDelay: Int = 30, maxDelay: Int = 180): Int =
    (index * baseDelay).coerceAtMost(maxDelay)

val islandSizeSpec: AnimationSpec<Dp> = tween(durationMillis = OBDuration.Normal, easing = OBEasing.IosCurve)
val islandAlphaSpec: AnimationSpec<Float> = tween(durationMillis = OBDuration.Fast, easing = OBEasing.IosCurve)
val overlayAlphaSpec: AnimationSpec<Float> = tween(durationMillis = OBDuration.Normal, easing = OBEasing.IosCurve)
val pageScaleSpec: AnimationSpec<Float> = tween(durationMillis = OBDuration.Normal, easing = OBEasing.IosCurve)
val cardEntranceSpec: AnimationSpec<Float> = tween(durationMillis = OBDuration.Normal, easing = OBEasing.IosCurve)

