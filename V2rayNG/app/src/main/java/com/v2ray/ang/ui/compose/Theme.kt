package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ---- Matixray palette: light-gray page, white cards, sky-blue accent (the look of V2Box). The Xf* names of the older
// palettes are kept so every file that uses them keeps compiling; only the values changed ----
val XfBackground = Color(0xFF0B0C0E)
val XfCard = Color(0xFF1C1D21)
val XfPurple = Color(0xFF38BDF8)
val XfPurpleDark = Color(0xFF0EA5E9)
val XfPurpleLight = Color(0xFF7DD3FC)
val XfTextMain = Color(0xFFF2F4F7)
val XfTextSub = Color(0xFF9AA3AF)
val XfTextMuted = Color(0xFF6B7380)
val XfBorder = Color(0x1FFFFFFF)
val XfGreen = Color(0xFF22C55E)
val XfYellow = Color(0xFFFACC15)
/** The light gray of the thin bars (the handle of the connection bar, the edge of the selected config). */
val XfMint = Color(0xFFD5DBE3)
val XfMintDark = Color(0xFF9AA3AF)
/** Old names of the button colors (they were ink and silver once); they are sky blue now. */
val XfInk = Color(0xFF0EA5E9)
val XfGraphite = Color(0xFF0284C7)
val XfSilver = Color(0xFFBAE6FD)
val XfSteel = Color(0xFF7DD3FC)
/** The color of a live connection: the connect button once it is on, the ring while it comes up. */
val XfConnected = Color(0xFF0EA5E9)
val XfConnectedLight = Color(0xFF38BDF8)
/** Old names of the connected color, kept so every file that still uses them follows the new color. */
val XfBlue = Color(0xFF0EA5E9)
val XfBlueLight = Color(0xFF38BDF8)
// Psiphon section: orange, so it reads apart from the main section
val XfOrange = Color(0xFFFF8A1F)
val XfOrangeDark = Color(0xFFE5700A)
val XfOrangeLight = Color(0xFFFFC38A)
val XfRed = Color(0xFFEF4444)
val XfProgressTrack = Color(0xFF26282D)

private val LightColor = lightColorScheme(
    primary = Color(0xFF0EA5E9), // Sky blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE), // Pale sky
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFFf97910), // Orange (Psiphon)
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE8D6),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = Color(0xFF009966), // Green
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA0F2D0),
    onTertiaryContainer = Color(0xFF00201A),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F4F7), // Page
    onBackground = Color(0xFF111418),
    surface = Color(0xFFF2F4F7), // Same as the page, so headers sit flat on it
    onSurface = Color(0xFF111418),
    surfaceVariant = Color(0xFFE8ECF1),
    onSurfaceVariant = Color(0xFF5B6470),
    outline = Color(0xFF7B8794),
    outlineVariant = Color(0xFFE3E7EC),
    inverseSurface = Color(0xFF25272C),
    inverseOnSurface = Color(0xFFF2F4F7),
    inversePrimary = Color(0xFF7DD3FC),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF0EA5E9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FB),
    surfaceContainer = Color(0xFFFFFFFF), // Cards, the bottom bar
    surfaceContainerHigh = Color(0xFFFFFFFF), // Dialogs, menus
    surfaceContainerHighest = Color(0xFFE9EDF2),
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFF38BDF8), // Sky blue
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFFf97910), // Orange (Psiphon)
    onSecondary = Color(0xFF4E2600),
    secondaryContainer = Color(0xFF6F3800),
    onSecondaryContainer = Color(0xFFFFE8D6),
    tertiary = Color(0xFF83D6B5),
    onTertiary = Color(0xFF00382E),
    tertiaryContainer = Color(0xFF005143),
    onTertiaryContainer = Color(0xFFA0F2D0),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B0C0E),
    onBackground = Color(0xFFF2F4F7),
    surface = Color(0xFF0B0C0E),
    onSurface = Color(0xFFF2F4F7),
    surfaceVariant = Color(0xFF2A2C31),
    onSurfaceVariant = Color(0xFFA3ABB8),
    outline = Color(0xFF7C8594),
    outlineVariant = Color(0xFF2E3138),
    inverseSurface = Color(0xFFF2F4F7),
    inverseOnSurface = Color(0xFF111418),
    inversePrimary = Color(0xFF0284C7),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF38BDF8),
    surfaceContainerLowest = Color(0xFF08090A),
    surfaceContainerLow = Color(0xFF131417),
    surfaceContainer = Color(0xFF1C1D21), // Cards, the bottom bar
    surfaceContainerHigh = Color(0xFF25272C), // Dialogs, menus
    surfaceContainerHighest = Color(0xFF2E3036),
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
val colorPingRed = Color(0xFFE5484D) // Soft red (timeout)
val colorConfigType = Color(0xFF0EA5E9) // Sky blue
val colorFabActive = Color(0xFF0EA5E9) // Sky blue
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE9ECF0)
val dividerColorDark = Color(0xFF2A2C31)

// Toast Colors 85%
val toastNormalBgLight = Color(0xD9353A3E) // Dark Gray
val toastNormalBgDark = Color(0xD94A4F54) // Darker Gray
val toastSuccessBg = Color(0xD9388E3C) // Green
val toastErrorBg = Color(0xD9D50000) // Red
val toastInfoBg = Color(0xD90284C7) // Sky blue
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

/**
 * Fill of the main buttons (connect, get configs, add): a flat sky blue, so the button is always the strongest thing on the page. [onMatixButton] is the text color on it.
 */
val MatixButtonBrush: Brush
    @Composable get() = Brush.verticalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF0EA5E9)))

val onMatixButton: Color
    @Composable get() = Color.White

/** The theme the app opens in the first time: "0" follows the system, "1" is light, "2" dark. */
private const val THEME_DEFAULT = "0"

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, THEME_DEFAULT) ?: THEME_DEFAULT
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
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
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
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
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> DarkColor
        else -> LightColor
    }
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
            Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
