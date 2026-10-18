package com.v2ray.ang.ui

import android.content.Intent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.v2ray.ang.ui.compose.GlassPillShape
import com.v2ray.ang.ui.compose.PrimaryGradient
import com.v2ray.ang.ui.compose.pressScale
import android.os.Bundle
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.NavigationBarsSpacer
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.VersionInfoBlock
import com.v2ray.ang.ui.compose.liquidGlass
import com.v2ray.ang.util.Utils

class AboutActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        AboutScreen(
            onBackClick = { finish() },
            onTranslatorsClick = {
                startActivity(Intent(this, TranslatorsActivity::class.java))
            }
        )
    }
}

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onTranslatorsClick: () -> Unit
) {
    val context = LocalContext.current
    var showOssDialog by remember { mutableStateOf(false) }

    val versionText = "Matixray v${BuildConfig.VERSION_NAME.trimStart('v', 'V')}"
    val appIdText = BuildConfig.APPLICATION_ID

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_about),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            AboutBismillah()
            Text(
                text = stringResource(R.string.mx_about_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .liquidGlass()
                    .padding(16.dp)
            )
            TelegramButton(onClick = { Utils.openUri(context, AppConfig.TG_CHANNEL_URL) })
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_source_code_24dp),
                title = stringResource(R.string.title_source_code),
                onClick = { Utils.openUri(context, AppConfig.APP_URL) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.license_24px),
                title = stringResource(R.string.title_oss_license),
                onClick = { showOssDialog = true }
            )
            // The Aether binary is not a Gradle dependency, so the generated license report
            // cannot list it; its AGPL-3.0 notice and source link live here instead.
            SettingsMenuItem(
                icon = painterResource(R.drawable.license_24px),
                title = stringResource(R.string.title_aether_core),
                subtitle = stringResource(R.string.summary_aether_core),
                onClick = { Utils.openUri(context, AppConfig.AETHER_URL) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_translate_24dp),
                title = stringResource(R.string.title_translators),
                onClick = onTranslatorsClick
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_feedback_24dp),
                title = stringResource(R.string.title_pref_feedback),
                onClick = { Utils.openUri(context, AppConfig.APP_ISSUES_URL) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_privacy_24dp),
                title = stringResource(R.string.title_privacy_policy),
                onClick = { Utils.openUri(context, AppConfig.APP_PRIVACY_POLICY) }
            )
            VersionInfoBlock(
                versionText = versionText,
                appIdText = appIdText
            )
            NavigationBarsSpacer()
        }
    }

    if (showOssDialog) {
        AlertDialog(
            onDismissRequest = { showOssDialog = false },
            title = { Text(stringResource(R.string.title_oss_license)) },
            text = {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            loadUrl("file:///android_asset/open_source_licenses.html")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showOssDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(bottom = 60.dp)
        )
    }
}

/** The Telegram channel as a button with the Telegram icon: a gradient pill that sits under the description. */
@Composable
private fun TelegramButton(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .pressScale(source)
            .clip(GlassPillShape)
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF2AABEE), Color(0xFF1C7ED6))))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_telegram_24dp),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.mx_telegram_button),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}


/**
 * Matixray: "In the name of God" opens the About page inside a glass frame with a shining gradient edge. The lettering is
 * big, with a soft glow, and its colors slowly drift from side to side; a small ornament between two fading lines sits
 * above and below it.
 */
@Composable
private fun AboutBismillah() {
    val scheme = MaterialTheme.colorScheme
    val violet = Color(0xFF8B5CF6)
    val teal = Color(0xFF2DD4BF)
    val gold = Color(0xFFF5C26B)
    val shape = RoundedCornerShape(28.dp)

    // The colors of the lettering and of the frame move slowly, so the title seems to shine.
    val drift by rememberInfiniteTransition(label = "bismillahDrift").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bismillahDriftValue",
    )
    val textBrush = Brush.linearGradient(
        colors = listOf(scheme.primary, violet, teal, gold),
        start = Offset(-300f + 600f * drift, 0f),
        end = Offset(600f + 600f * drift, 120f),
        tileMode = TileMode.Mirror,
    )
    val frameBrush = Brush.sweepGradient(listOf(scheme.primary, violet, teal, gold, scheme.primary))
    val fadingLine = Brush.horizontalGradient(
        listOf(Color.Transparent, scheme.primary.copy(alpha = 0.75f), Color.Transparent)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .liquidGlass(shape)
            .border(1.5.dp, frameBrush, shape)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BismillahOrnament(fadingLine, gold, "\u2726")
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.xf_splash_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = textBrush,
                    shadow = Shadow(color = scheme.primary.copy(alpha = 0.40f), offset = Offset(0f, 2f), blurRadius = 22f),
                ),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            BismillahOrnament(fadingLine, violet, "\u2756")
        }
    }
}

/** Two fading lines with a small star between them. */
@Composable
private fun BismillahOrnament(line: Brush, color: Color, glyph: String) {
    Row(
        modifier = Modifier.fillMaxWidth(0.8f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.weight(1f).height(1.5.dp).background(line))
        Text(text = glyph, color = color, fontSize = 14.sp)
        Box(modifier = Modifier.weight(1f).height(1.5.dp).background(line))
    }
}
