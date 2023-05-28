package cn.qhplus.emo.scheme.impl

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import cn.qhplus.emo.scheme.SchemeTransition

object SchemeTransitionProviders {
    private val push = PushSchemeTransitionProvider()
    private val map = mutableMapOf<Int, SchemeTransitionProvider>().apply {
        put(SchemeTransition.PUSH, push)
        put(SchemeTransition.PRESENT, PresentSchemeTransitionProvider())
        put(SchemeTransition.SCALE, ScaleSchemeTransitionProvider())
        put(SchemeTransition.PUSH_THEN_STILL, PushThenStillSchemeTransitionProvider())
    }
    fun put(type: Int, converter: SchemeTransitionProvider){
        if(type <= 0){
            throw RuntimeException("type must be a positive number.")
        }
        map[type] = converter
    }

    fun get(type: Int): SchemeTransitionProvider {
        return map[type] ?: push
    }
}

@OptIn(ExperimentalAnimationApi::class)
interface SchemeTransitionProvider {
    fun activityEnterRes(): Int
    fun activityExitRes(): Int
    fun enterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)?
    fun exitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)?
    fun popEnterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)?
    fun popExitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)?
}

@OptIn(ExperimentalAnimationApi::class)
val SlideInRight: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition by lazy {
    {
        slideIn(tween(durationMillis = 300)) { IntOffset(it.width, 0) }
    }
}
@OptIn(ExperimentalAnimationApi::class)
val SlideInLeft: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition by lazy {
    {
        slideIn(tween(durationMillis = 300)) { IntOffset(-it.width, 0) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val SlideInBottom: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition by lazy {
    {
        slideIn(tween(durationMillis = 300)) { IntOffset(0, it.height) }
    }
}
@OptIn(ExperimentalAnimationApi::class)
val SlideInTop: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition by lazy {
    {
        slideIn(tween(durationMillis = 300)) { IntOffset(0, -it.height) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val ScaleIn: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition by lazy {
    {
        scaleIn(tween(durationMillis = 300), 0.8f) + fadeIn(tween(durationMillis = 300), 0f)
    }
}

@OptIn(ExperimentalAnimationApi::class)
val FadeIn: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition by lazy {
    {
        fadeIn(tween(durationMillis = 300), 0f)
    }
}

@OptIn(ExperimentalAnimationApi::class)
val StillIn: AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition by lazy {
    {
        scaleIn(tween(durationMillis = 300), 1f)
    }
}

@OptIn(ExperimentalAnimationApi::class)
val SlideOutLeft: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(-it.width, 0) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val SlideOutRight: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(it.width, 0) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val SlideOutTop: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(0, -it.height) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
val ScaleOut: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        scaleOut(
            tween(durationMillis = 300, delayMillis = 50), 0.8f
        ) + fadeOut(tween(durationMillis = 300, delayMillis = 50), 0f)
    }
}

@OptIn(ExperimentalAnimationApi::class)
val FadeOut: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        fadeOut(tween(durationMillis = 300, delayMillis = 50))
    }
}

@OptIn(ExperimentalAnimationApi::class)
val StillOut: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        scaleOut(tween(durationMillis = 300, delayMillis = 50), 1f)
    }
}

@OptIn(ExperimentalAnimationApi::class)
val SlideOutBottom: AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition by lazy {
    {
        slideOut(tween(durationMillis = 300, delayMillis = 50)) { IntOffset(0, it.height) }
    }
}

@OptIn(ExperimentalAnimationApi::class)
open class PushSchemeTransitionProvider : SchemeTransitionProvider {

    override fun activityEnterRes(): Int {
        return R.anim.slide_in_right
    }

    override fun activityExitRes(): Int {
        return R.anim.slide_out_left
    }

    override fun enterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? {
        return SlideInRight
    }

    override fun exitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? {
        return SlideOutLeft
    }

    override fun popEnterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? {
        return SlideInLeft
    }

    override fun popExitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? {
        return SlideOutRight
    }
}

@OptIn(ExperimentalAnimationApi::class)
open class PushThenStillSchemeTransitionProvider : SchemeTransitionProvider {

    override fun activityEnterRes(): Int {
        return R.anim.slide_in_right
    }

    override fun activityExitRes(): Int {
        return R.anim.slide_out_left
    }

    override fun enterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? {
        return SlideInRight
    }

    override fun exitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? {
        return StillOut
    }

    override fun popEnterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? {
        return StillIn
    }

    override fun popExitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? {
        return SlideOutRight
    }
}

@OptIn(ExperimentalAnimationApi::class)
open class PresentSchemeTransitionProvider : SchemeTransitionProvider {

    override fun activityEnterRes(): Int {
        return R.anim.slide_in_bottom
    }

    override fun activityExitRes(): Int {
        return R.anim.slide_still
    }


    override fun enterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? {
        return SlideInBottom
    }

    override fun exitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? {
        return StillOut
    }

    override fun popEnterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? {
        return StillIn
    }

    override fun popExitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? {
        return SlideOutBottom
    }
}

@OptIn(ExperimentalAnimationApi::class)
open class ScaleSchemeTransitionProvider : SchemeTransitionProvider {

    override fun activityEnterRes(): Int {
        return R.anim.scale_enter
    }

    override fun activityExitRes(): Int {
        return R.anim.slide_still
    }


    override fun enterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? {
        return ScaleIn
    }

    override fun exitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? {
        return StillOut
    }

    override fun popEnterTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? {
        return StillIn
    }

    override fun popExitTransition(): (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?)? {
        return ScaleOut
    }
}