package com.v2ray.ang.ui.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ---- Matixray palette: black, gray and white. The Xf* names of the old violet palette are kept so every user of them
// stays as it is; the accent (XfPurple*) is now white in the dark theme and near-black in the light one ----
val XfBackground = Color(0xFF0B0B0C)
val XfCard = Color(0xFF161617)
val XfPurple = Color(0xFFE9E9EB)
val XfPurpleDark = Color(0xFF8E8E93)
val XfPurpleLight = Color(0xFFF5F5F7)
val XfTextMain = Color(0xFFF5F5F7)
val XfTextSub = Color(0xFF9A9AA0)
val XfTextMuted = Color(0xFF6A6A70)
val XfBorder = Color(0x33FFFFFF)
val XfGreen = Color(0xFF22C55E)
val XfYellow = Color(0xFFFACC15)
/** The light gray of the thin bars (the handle of the connection bar, the edge of the selected config). */
val XfMint = Color(0xFFD0D0D4)
val XfMintDark = Color(0xFF9A9AA0)
/** Matixray ink, graphite and silver: the metal of the logo. Buttons are cut from these. */
val XfInk = Color(0xFF0E0E10)
val XfGraphite = Color(0xFF2B2B2F)
val XfSilver = Color(0xFFE8E8EC)
val XfSteel = Color(0xFF8E8E96)
/** The color of a live connection: the connect button once it is on, the ring while it comes up, a healthy ping. */
val XfConnected = Color(0xFF16A34A)
val XfConnectedLight = Color(0xFF4ADE80)
/** Old names of the connected color, kept so every file that still uses them follows the new color. */
val XfBlue = XfConnected
val XfBlueLight = XfConnectedLight
// Psiphon section: orange, so it reads apart from the main section
val XfOrange = Color(0xFFFF8A1F)
val XfOrangeDark = Color(0xFFE5700A)
val XfOrangeLight = Color(0xFFFFC38A)
val XfRed = Color(0xFFEF4444)
val XfProgressTrack = Color(0xFF262628)

private val LightColor = lightColorScheme(
    primary = Color(0xFF1C1C1E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5EA),
    onPrimaryContainer = Color(0xFF1C1C1E),
    secondary = Color(0xFF636366),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E5EA),
    onSecondaryContainer = Color(0xFF1C1C1E),
    tertiary = Color(0xFF16A34A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC9F4D7),
    onTertiaryContainer = Color(0xFF05301A),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F2F4),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE9E9ED),
    onSurfaceVariant = Color(0xFF636366),
    outline = Color(0xFF9A9AA0),
    outlineVariant = Color(0x1F000000),
    inverseSurface = Color(0xFF2C2C2E),
    inverseOnSurface = Color(0xFFF5F5F7),
    inversePrimary = Color(0xFFE9E9EB),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF1C1C1E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F8FA),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEDEDF0),
    surfaceContainerHighest = Color(0xFFE3E3E8),
)

private val DarkColor = darkColorScheme(
    primary = XfPurple,
    onPrimary = Color(0xFF111113),
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = Color(0xFFF2F2F7),
    secondary = XfMint,
    onSecondary = Color(0xFF111113),
    secondaryContainer = Color(0xFF2A2A2C),
    onSecondaryContainer = Color(0xFFE0E0E5),
    tertiary = XfGreen,
    onTertiary = Color(0xFF03210F),
    tertiaryContainer = Color(0xFF0F3D22),
    onTertiaryContainer = Color(0xFFBBF7D0),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF4A0F0F),
    onErrorContainer = Color(0xFFFECACA),
    background = XfBackground,
    onBackground = XfTextMain,
    surface = Color(0xFF121214),
    onSurface = XfTextMain,
    surfaceVariant = Color(0xFF252528),
    onSurfaceVariant = XfTextSub,
    outline = XfTextMuted,
    outlineVariant = Color(0xFF2E2E31),
    inverseSurface = XfTextMain,
    inverseOnSurface = XfBackground,
    inversePrimary = XfPurpleDark,
    scrim = Color(0xFF000000),
    surfaceTint = XfPurple,
    surfaceContainerLowest = Color(0xFF080809),
    surfaceContainerLow = Color(0xFF0F0F10),
    surfaceContainer = XfCard,
    surfaceContainerHigh = Color(0xFF1F1F21),
    surfaceContainerHighest = Color(0xFF28282B),
)

/** Vazirmatn (static weights in res/font), used for every text style of the app. */
@Composable
fun rememberAppFontFamily(): FontFamily = remember {
    FontFamily(
        Font(R.font.vazirmatn_regular, FontWeight.Normal),
        Font(R.font.vazirmatn_medium, FontWeight.Medium),
        Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
        Font(R.font.vazirmatn_bold, FontWeight.Bold),
    )
}

private fun Typography.withFontFamily(family: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    titleMedium = titleMedium.copy(fontFamily = family, fontWeight = FontWeight.Medium),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

// Semantic Colors
val colorPing = Color(0xFF009966) // Green
val colorPingRed = Color(0xFFFF4D6D) // Red
val colorConfigType = Color(0xFFA1A1AA) // Steel
val colorFabActive = Color(0xFF6B6B73) // Graphite
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

// Toast Colors 85%
val toastNormalBgLight = Color(0xD9353A3E) // Dark Gray
val toastNormalBgDark = Color(0xD94A4F54) // Darker Gray
val toastSuccessBg = Color(0xD9388E3C) // Green
val toastErrorBg = Color(0xD9D50000) // Red
val toastInfoBg = Color(0xD93A3A40) // Graphite
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

/**
 * Fill of the main buttons (connect, get configs, add): a vertical metal gradient, graphite on the light theme and
 * silver on the dark one, so the button is always the strongest thing on the page. [onMatixButton] is the text color on it.
 */
val MatixButtonBrush: Brush
    @Composable get() = if (LocalDarkTheme.current) {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD4D4DA)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF3A3A3F), Color(0xFF131315)))
    }

val onMatixButton: Color
    @Composable get() = if (LocalDarkTheme.current) XfInk else Color.White

/** Matixray: the app opens in the light (white and violet) look the first time; "1" is light, "2" dark, "0" the system. */
private const val THEME_DEFAULT = "1"

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, THEME_DEFAULT) ?: THEME_DEFAULT
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, THEME_DEFAULT) ?: THEME_DEFAULT
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val useDark = darkTheme
    val colorScheme = if (useDark) DarkColor else LightColor
    val fontFamily = rememberAppFontFamily()
    val typography = remember(fontFamily) { Typography().withFontFamily(fontFamily) }
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides useDark,
        LocalGlassStyle provides if (useDark) DarkGlassStyle else LightGlassStyle,
        LocalAppSnackbar provides snackbarController,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppBackdrop()
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
