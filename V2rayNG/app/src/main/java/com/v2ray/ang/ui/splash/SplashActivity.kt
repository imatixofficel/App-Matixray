package com.v2ray.ang.ui.splash

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.v2ray.ang.ui.compose.BouncingDots
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.GlassCardShape
import com.v2ray.ang.ui.compose.XfGreen
import com.v2ray.ang.ui.compose.liquidGlass
import com.v2ray.ang.ui.main.MainActivity
import java.util.Locale

/** Launcher entry: the opening words (always in Persian) fade in over the bouncing dots, in under a second, then hands over to [MainActivity]. */
class SplashActivity : BaseComponentActivity() {

    @Composable
    override fun ScreenContent() {
        // Nothing to go back to: the user is either waiting or leaving the app.
        BackHandler { finish() }
        SplashScreen(onFinished = {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        })
    }
}

private const val FADE_IN_MS = 700
private const val LOAD_MS = 2300
private const val HOLD_MS = 350L
private val splashStatusRes = intArrayOf(
    R.string.xf_splash_status_0,
    R.string.xf_splash_status_1,
    R.string.xf_splash_status_2,
    R.string.xf_splash_status_3,
    R.string.xf_splash_status_4,
    R.string.xf_splash_status_5,
)

/** Status line for a percentage: five stages while loading, the last one only at 100 %. */
internal fun splashStatusIndex(percent: Int): Int =
    if (percent >= 100) splashStatusRes.lastIndex else (percent.coerceAtLeast(0) * 5 / 100)

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // The opening words are Persian from the very first frame, whatever the language of the phone is: the title is read
    // from a Persian copy of the resources, so it does not wait for (or depend on) the app language being applied.
    val base = LocalContext.current
    val persianContext = remember(base) {
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag("fa"))
        base.createConfigurationContext(config)
    }
    val title = remember(persianContext) { persianContext.getString(R.string.xf_splash_title) }
    // The words rise and fade in with a glow behind them, a thin line fills while the status words change, then the
    // app opens: about 2.7 s in all.
    val fade = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { fade.animateTo(1f, tween(durationMillis = FADE_IN_MS, easing = FastOutSlowInEasing)) }
            progress.animateTo(1f, tween(durationMillis = LOAD_MS, easing = LinearEasing))
        }
        kotlinx.coroutines.delay(HOLD_MS)
        onFinished()
    }
    val percent = (progress.value * 100).toInt()

    val transition = rememberInfiniteTransition(label = "splash")
    val breathe by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "ring",
    )
    val glowColor = scheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Halo and two rings spreading out behind the words.
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = fade.value }) {
            val c = center.copy(y = size.height * 0.45f)
            val base = size.minDimension * 0.32f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.30f), Color.Transparent),
                    center = c,
                    radius = base * 1.5f * breathe,
                ),
                radius = base * 1.5f * breathe,
                center = c,
            )
            for (i in 0..1) {
                val t = (ring + i * 0.5f) % 1f
                drawCircle(
                    color = glowColor.copy(alpha = (1f - t) * 0.35f),
                    radius = base * (0.6f + 1.1f * t),
                    center = c,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = scheme.onBackground,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    this.alpha = fade.value
                    val scale = 0.82f + 0.18f * fade.value
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - fade.value) * 24.dp.toPx()
                },
            )
            Spacer(Modifier.height(36.dp))
            BouncingDots(
                color = scheme.primary,
                dotSize = 14.dp,
                modifier = Modifier.graphicsLayer { this.alpha = fade.value },
            )
            Spacer(Modifier.height(28.dp))
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50)),
                color = scheme.primary,
                trackColor = scheme.primary.copy(alpha = 0.16f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(splashStatusRes[splashStatusIndex(percent)]),
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { this.alpha = fade.value },
            )
        }
        Text(
            text = stringResource(R.string.xf_brand),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .graphicsLayer { this.alpha = fade.value },
            color = scheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 0.2.em,
        )
    }
}
