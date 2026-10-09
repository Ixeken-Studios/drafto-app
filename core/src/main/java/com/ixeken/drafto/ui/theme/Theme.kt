package com.ixeken.drafto.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.annotation.StringRes
import androidx.core.view.WindowCompat
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.adaptive.LocalWindowSizeInfo
import com.ixeken.drafto.ui.adaptive.rememberWindowSizeInfo

/**
 * Resuelve recursivamente la [Activity] anfitriona desenvolviendo instancias de [ContextWrapper].
 *
 * En Jetpack Compose, el contexto de la vista suele estar envuelto por decoradores (como
 * Hilt o ContextThemeWrapper), impidiendo el casteo directo a [Activity]. Esta función garantiza
 * el acceso a la [Activity] real para el control de barras del sistema e insets.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Modos de tema visual soportados por Drafto Brain OS.
 */
enum class DraftoThemeMode(val storageKey: String) {
    DARK("Dark"),
    LIGHT("Light"),
    AMOLED("Amoled"),
    KRAFT("Kraft");

    companion object {
        fun fromString(value: String): DraftoThemeMode {
            return entries.firstOrNull {
                it.storageKey.equals(value, ignoreCase = true) ||
                (it == AMOLED && value.equals("AMOLED", ignoreCase = true))
            } ?: DARK
        }
    }
}

/**
 * Opciones de color de acento disponibles en Drafto Brain OS con soporte de duplas tonales
 * adaptables según el modo de tema visual (Oscuro / AMOLED vs Claro).
 */
enum class DraftoAccentColor(
    val storageKey: String,
    val darkColor: Color,
    val lightColor: Color,
    @get:StringRes val titleRes: Int
) {
    MONOCHROME("monochrome", PureWhite, PureBlack, R.string.accent_color_monochrome),
    FRESH_SKY("fresh_sky", DraftoFreshSkyDark, DraftoFreshSkyLight, R.string.accent_color_fresh_sky),
    SCHOOL_BUS("school_bus", DraftoSchoolBusDark, DraftoSchoolBusLight, R.string.accent_color_school_bus),
    MUTED_OLIVE("muted_olive", DraftoMutedOliveDark, DraftoMutedOliveLight, R.string.accent_color_muted_olive),
    AMETHYST("amethyst", DraftoAmethystDark, DraftoAmethystLight, R.string.accent_color_amethyst),
    DARK_ORANGE("dark_orange", DraftoDarkOrangeDark, DraftoDarkOrangeLight, R.string.accent_color_dark_orange),
    FLAG_RED("flag_red", DraftoFlagRedDark, DraftoFlagRedLight, R.string.accent_color_flag_red);

    /**
     * Resuelve el color de acento reactivo al modo oscuro o claro del tema.
     */
    fun resolveColor(isDark: Boolean): Color = if (isDark) darkColor else lightColor

    /**
     * Resuelve el color de contenido (glifos o textos) con contraste WCAG garantizado.
     */
    fun resolveOnColor(isDark: Boolean): Color = resolveColor(isDark).contrastContentColor()

    // Propiedades de conveniencia y retrocompatibilidad
    val color: Color get() = darkColor
    val onColor: Color get() = darkColor.contrastContentColor()

    companion object {
        val DEFAULT = MONOCHROME

        fun fromString(value: String): DraftoAccentColor {
            if (value.equals("pure_white", ignoreCase = true) || value.equals("pure_black", ignoreCase = true)) {
                return MONOCHROME
            }
            return entries.firstOrNull {
                it.storageKey.equals(value, ignoreCase = true) ||
                it.name.equals(value, ignoreCase = true)
            } ?: DEFAULT
        }
    }
}

/**
 * Paleta de colores extendida y reactiva para el sistema de diseño de Drafto.
 */
@Immutable
data class DraftoColors(
    val themeMode: DraftoThemeMode = DraftoThemeMode.DARK,
    val isDark: Boolean,
    val background: Color,
    val onBackground: Color,
    val cardSurface: Color,
    val onCardSurface: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val divider: Color,
    val cardBorder: Color,
    val cardBorderHighlight: Color,
    val navBarSurface: Color,
    val navBarSurfaceTranslucent: Color,
    val navBarContent: Color,
    val navBarIndicator: Color,
    val onNavBarIndicator: Color = PureBlack,
    val navBarActionCard: Color,
    val navBarCloseRed: Color,
    val navBarShadow: Color,
    val searchBarBackground: Color,
    val searchBarPlaceholder: Color,
    val bottomSheetSurface: Color,
    val bottomSheetInnerCard: Color,
    val actionBadgeContainerAlpha: Float = 0.15f,

    // Nodos cognitivos y acentos temáticos
    val electricMint: Color = DraftoElectricMint,
    val electricMintContainer: Color = DraftoElectricMintContainer,
    val aquaCyan: Color = DraftoAquaCyan,
    val aquaCyanContainer: Color = DraftoAquaCyanContainer,
    val cyberViolet: Color = DraftoCyberViolet,
    val cyberVioletContainer: Color = DraftoCyberVioletContainer,
    val warmMango: Color = DraftoWarmMango,
    val warmMangoContainer: Color = DraftoWarmMangoContainer,
    val emerald: Color = DraftoEmerald,
    val emeraldContainer: Color = DraftoEmeraldContainer,
    val neonPink: Color = DraftoNeonPink,
    val neonPinkContainer: Color = DraftoNeonPinkContainer,
    val sunsetCoral: Color = DraftoSunsetCoral,
    val sunsetCoralContainer: Color = DraftoSunsetCoralContainer,
    val indigo: Color = DraftoIndigo,
    val indigoContainer: Color = DraftoIndigoContainer,
    val electricLime: Color = DraftoElectricLime,
    val electricLimeContainer: Color = DraftoElectricLimeContainer,
    val brightOrange: Color = DraftoBrightOrange,
    val brightOrangeContainer: Color = DraftoBrightOrangeContainer,
    val skyBlue: Color = DraftoSkyBlue,
    val skyBlueContainer: Color = DraftoSkyBlueContainer,
    val electricLavender: Color = DraftoElectricLavender,
    val electricLavenderContainer: Color = DraftoElectricLavenderContainer,
    val freshSky: Color = DraftoFreshSky,
    val accent: Color = DraftoFreshSky,
    val onAccent: Color = PureBlack,

    // Tokens semánticos para botones de cancelación en Bottom Sheets
    val sheetCancelButtonBackground: Color = DraftoSheetCancelRed,
    val onSheetCancelButton: Color = PureWhite,

    // Token para glifos e íconos sobre contenedores/badges sólidos
    val badgeGlyph: Color = DraftoDarkGlyph,

    // Tokens semánticos para conmutadores Nothing OS (DraftoSwitch)
    val switchCheckedTrack: Color = accent.copy(alpha = 0.35f),
    val switchCheckedThumb: Color = accent,
    val switchCheckedIcon: Color = onAccent,
    val switchUncheckedTrack: Color = secondaryContainer,
    val switchUncheckedThumb: Color = textMuted,
    val switchUncheckedIcon: Color = cardSurface,
    val switchUncheckedBorder: Color = cardBorder
)

// --- Paletas predefinidas por modo ---

val DraftoDarkColors = DraftoColors(
    themeMode = DraftoThemeMode.DARK,
    isDark = true,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    cardSurface = CardSurfaceDark,
    onCardSurface = TextPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextPlaceholderDark,
    divider = Color(0x1FFFFFFF),
    cardBorder = Color(0x1FFFFFFF),
    cardBorderHighlight = Color(0x3DFFFFFF),
    navBarSurface = NavBarSurfaceDark,
    navBarSurfaceTranslucent = NavBarSurfaceTranslucentDark,
    navBarContent = NavBarContentDark,
    navBarIndicator = AccentColor,
    onNavBarIndicator = PureBlack,
    navBarActionCard = Color(0x1AFFFFFF),
    navBarCloseRed = DraftoNavBarCloseRed,
    navBarShadow = Color(0xCC000000),
    searchBarBackground = SearchBarBackgroundDark,
    searchBarPlaceholder = SearchBarPlaceholderDark,
    bottomSheetSurface = BottomSheetSurfaceDark,
    bottomSheetInnerCard = BottomSheetInnerCardDark,
    actionBadgeContainerAlpha = 0.15f,
    accent = AccentColor,
    freshSky = AccentColor,
    badgeGlyph = DraftoDarkGlyph,
    switchCheckedTrack = AccentColor.copy(alpha = 0.35f),
    switchCheckedThumb = AccentColor,
    switchCheckedIcon = PureBlack,
    switchUncheckedTrack = SecondaryContainerDark,
    switchUncheckedThumb = TextPlaceholderDark,
    switchUncheckedIcon = CardSurfaceDark,
    switchUncheckedBorder = Color(0x1FFFFFFF)
)

val DraftoAmoledColors = DraftoColors(
    themeMode = DraftoThemeMode.AMOLED,
    isDark = true,
    background = BackgroundAmoled,
    onBackground = TextPrimaryDark,
    cardSurface = CardSurfaceAmoled,
    onCardSurface = TextPrimaryDark,
    primaryContainer = PrimaryContainerAmoled,
    onPrimaryContainer = OnPrimaryContainerAmoled,
    secondaryContainer = SecondaryContainerAmoled,
    onSecondaryContainer = OnSecondaryContainerAmoled,
    tertiaryContainer = TertiaryContainerAmoled,
    onTertiaryContainer = OnTertiaryContainerAmoled,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextPlaceholderDark,
    divider = Color(0x26FFFFFF),
    cardBorder = Color(0x24FFFFFF),
    cardBorderHighlight = Color(0x47FFFFFF),
    navBarSurface = NavBarSurfaceAmoled,
    navBarSurfaceTranslucent = NavBarSurfaceTranslucentAmoled,
    navBarContent = NavBarContentAmoled,
    navBarIndicator = AccentColor,
    onNavBarIndicator = PureBlack,
    navBarActionCard = Color(0x24FFFFFF),
    navBarCloseRed = DraftoNavBarCloseRed,
    navBarShadow = Color(0xCC000000),
    searchBarBackground = SearchBarBackgroundAmoled,
    searchBarPlaceholder = SearchBarPlaceholderAmoled,
    bottomSheetSurface = BottomSheetSurfaceAmoled,
    bottomSheetInnerCard = BottomSheetInnerCardAmoled,
    actionBadgeContainerAlpha = 0.20f,
    accent = AccentColor,
    freshSky = AccentColor,
    badgeGlyph = DraftoDarkGlyph,
    switchCheckedTrack = AccentColor.copy(alpha = 0.35f),
    switchCheckedThumb = AccentColor,
    switchCheckedIcon = PureBlack,
    switchUncheckedTrack = SecondaryContainerAmoled,
    switchUncheckedThumb = TextPlaceholderDark,
    switchUncheckedIcon = CardSurfaceAmoled,
    switchUncheckedBorder = Color(0x24FFFFFF)
)

val DraftoLightColors = DraftoColors(
    themeMode = DraftoThemeMode.LIGHT,
    isDark = false,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    cardSurface = CardSurfaceLight,
    onCardSurface = TextPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextPlaceholderLight,
    divider = Color(0x14000000),
    cardBorder = Color(0x0D000000),
    cardBorderHighlight = Color(0x1A000000),
    navBarSurface = NavBarSurfaceLight,
    navBarSurfaceTranslucent = NavBarSurfaceTranslucentLight,
    navBarContent = NavBarContentLight,
    navBarIndicator = AccentColor,
    onNavBarIndicator = PureWhite,
    navBarActionCard = Color(0x0F000000),
    navBarCloseRed = Color(0xFFE11D48),
    navBarShadow = Color(0x66000000),
    searchBarBackground = SearchBarBackgroundLight,
    searchBarPlaceholder = SearchBarPlaceholderLight,
    bottomSheetSurface = BottomSheetSurfaceLight,
    bottomSheetInnerCard = BottomSheetInnerCardLight,
    actionBadgeContainerAlpha = 0.15f,
    accent = AccentColor,
    freshSky = AccentColor,

    // Nodos cognitivos y acentos con contraste y saturación profesional para fondos claros (anti-lavado)
    electricMint = Color(0xFF059669),       // Emerald-600
    electricMintContainer = Color(0x24059669),
    aquaCyan = Color(0xFF0284C7),           // Sky-600
    aquaCyanContainer = Color(0x240284C7),
    cyberViolet = Color(0xFF7C3AED),        // Violet-600
    cyberVioletContainer = Color(0x247C3AED),
    warmMango = Color(0xFFD97706),          // Amber-600
    warmMangoContainer = Color(0x24D97706),
    emerald = Color(0xFF059669),            // Emerald-600
    emeraldContainer = Color(0x24059669),
    neonPink = Color(0xFFDB2777),           // Pink-600
    neonPinkContainer = Color(0x24DB2777),
    sunsetCoral = Color(0xFFE11D48),        // Rose-600
    sunsetCoralContainer = Color(0x24E11D48),
    indigo = Color(0xFF4F46E5),             // Indigo-600
    indigoContainer = Color(0x244F46E5),
    electricLime = Color(0xFF4D7C0F),       // Lime-700 (verde lima profundo para óptima legibilidad)
    electricLimeContainer = Color(0x244D7C0F),
    brightOrange = Color(0xFFEA580C),       // Orange-600
    brightOrangeContainer = Color(0x24EA580C),
    skyBlue = Color(0xFF0284C7),            // Sky-600
    skyBlueContainer = Color(0x240284C7),
    electricLavender = Color(0xFF9333EA),   // Purple-600
    electricLavenderContainer = Color(0x249333EA),
    badgeGlyph = DraftoLightGlyph,
    switchCheckedTrack = AccentColor,
    switchCheckedThumb = PureWhite,
    switchCheckedIcon = AccentColor,
    switchUncheckedTrack = Dust,
    switchUncheckedThumb = PureWhite,
    switchUncheckedIcon = Gunmetal,
    switchUncheckedBorder = Color(0x1F000000)
)

val DraftoKraftColors = DraftoColors(
    themeMode = DraftoThemeMode.KRAFT,
    isDark = false,
    background = BackgroundKraft,
    onBackground = TextPrimaryKraft,
    cardSurface = CardSurfaceKraft,
    onCardSurface = TextPrimaryKraft,
    primaryContainer = PrimaryContainerKraft,
    onPrimaryContainer = OnPrimaryContainerKraft,
    secondaryContainer = SecondaryContainerKraft,
    onSecondaryContainer = OnSecondaryContainerKraft,
    tertiaryContainer = TertiaryContainerKraft,
    onTertiaryContainer = OnTertiaryContainerKraft,
    textPrimary = TextPrimaryKraft,
    textSecondary = TextSecondaryKraft,
    textMuted = TextPlaceholderKraft,
    divider = Color(0x1F1F1C1A),
    cardBorder = Color(0x141F1C1A),
    cardBorderHighlight = Color(0x241F1C1A),
    navBarSurface = NavBarSurfaceKraft,
    navBarSurfaceTranslucent = NavBarSurfaceTranslucentKraft,
    navBarContent = NavBarContentKraft,
    navBarIndicator = WarmAmber,
    onNavBarIndicator = PureWhite,
    navBarActionCard = Color(0x0F1F1C1A),
    navBarCloseRed = Color(0xFFDC2626),
    navBarShadow = Color(0x40000000),
    searchBarBackground = SearchBarBackgroundKraft,
    searchBarPlaceholder = SearchBarPlaceholderKraft,
    bottomSheetSurface = BottomSheetSurfaceKraft,
    bottomSheetInnerCard = BottomSheetInnerCardKraft,
    actionBadgeContainerAlpha = 0.15f,
    accent = WarmAmber,
    freshSky = Color(0xFF0284C7),

    // Nodos cognitivos y acentos con contraste y saturación profesional para fondos cálidos
    electricMint = Color(0xFF059669),
    electricMintContainer = Color(0x24059669),
    aquaCyan = Color(0xFF0284C7),
    aquaCyanContainer = Color(0x240284C7),
    cyberViolet = Color(0xFF7C3AED),
    cyberVioletContainer = Color(0x247C3AED),
    warmMango = Color(0xFFD97706),
    warmMangoContainer = Color(0x24D97706),
    emerald = Color(0xFF059669),
    emeraldContainer = Color(0x24059669),
    neonPink = Color(0xFFDB2777),
    neonPinkContainer = Color(0x24DB2777),
    sunsetCoral = Color(0xFFDC2626),
    sunsetCoralContainer = Color(0x24DC2626),
    indigo = Color(0xFF4F46E5),
    indigoContainer = Color(0x244F46E5),
    electricLime = Color(0xFF4D7C0F),
    electricLimeContainer = Color(0x244D7C0F),
    brightOrange = Color(0xFFEA580C),
    brightOrangeContainer = Color(0x24EA580C),
    skyBlue = Color(0xFF0284C7),
    skyBlueContainer = Color(0x240284C7),
    electricLavender = Color(0xFF9333EA),
    electricLavenderContainer = Color(0x249333EA),
    badgeGlyph = DraftoLightGlyph,
    switchCheckedTrack = WarmAmber,
    switchCheckedThumb = WarmCanvas,
    switchCheckedIcon = WarmAmber,
    switchUncheckedTrack = PaleStone,
    switchUncheckedThumb = WarmCanvas,
    switchUncheckedIcon = AshStone,
    switchUncheckedBorder = LinenBorder
)

val LocalDraftoColors = staticCompositionLocalOf { DraftoDarkColors }

@Immutable
data class DraftoExtraColors(
    val save: Color = DraftoSave,
    val onSave: Color = DraftoOnSave,
    val saveContainer: Color = DraftoSaveContainer,
    val onSaveContainer: Color = DraftoOnSaveContainer,
    val backgroundVariant: Color = DraftoBackgroundVariant,
    val onBackgroundVariant: Color = DraftoOnBackgroundVariant,
    val statusBar: Color = DraftoStatusBar,
    val statusBarCamera: Color = DraftoStatusBarCamera,
    val handle: Color = DraftoHandle,
    val navBarSurface: Color = DraftoNavBarSurface,
    val onNavBarSurface: Color = DraftoOnNavBarSurface,
    val navBarContent: Color = DraftoNavBarContent,
    val navBarIndicator: Color = DraftoNavBarIndicator,
    val navBarActionCard: Color = DraftoNavBarActionCard,
    val navBarCloseRed: Color = DraftoNavBarCloseRed,
    val navBarShadow: Color = DraftoNavBarShadow,
    val navBarContainer: Color = DraftoNavBarContainer,
    val navBarSelected: Color = DraftoNavBarSelected,
    val navBarUnselected: Color = DraftoNavBarUnselected
)

val LocalDraftoExtraColors = staticCompositionLocalOf { DraftoExtraColors() }

private fun createExtraColors(colors: DraftoColors): DraftoExtraColors {
    return if (colors.isDark) {
        DraftoExtraColors(
            save = DraftoSave,
            onSave = DraftoOnSave,
            saveContainer = DraftoSaveContainer,
            onSaveContainer = DraftoOnSaveContainer,
            backgroundVariant = colors.cardSurface,
            onBackgroundVariant = colors.onBackground,
            statusBar = colors.onBackground,
            statusBarCamera = PureBlack,
            handle = colors.onBackground,
            navBarSurface = colors.navBarSurface,
            onNavBarSurface = colors.navBarContent,
            navBarContent = colors.navBarContent,
            navBarIndicator = colors.navBarIndicator,
            navBarActionCard = colors.navBarActionCard,
            navBarCloseRed = colors.navBarCloseRed,
            navBarShadow = colors.navBarShadow,
            navBarContainer = colors.background,
            navBarSelected = colors.navBarContent,
            navBarUnselected = colors.textSecondary
        )
    } else {
        DraftoExtraColors(
            save = Color(0xFF16A34A),
            onSave = PureWhite,
            saveContainer = Color(0x2416A34A),
            onSaveContainer = Color(0xFF15803D),
            backgroundVariant = colors.cardSurface,
            onBackgroundVariant = colors.onBackground,
            statusBar = colors.textPrimary,
            statusBarCamera = colors.textPrimary,
            handle = colors.textSecondary,
            navBarSurface = colors.navBarSurface,
            onNavBarSurface = colors.navBarContent,
            navBarContent = colors.navBarContent,
            navBarIndicator = colors.navBarIndicator,
            navBarActionCard = colors.navBarActionCard,
            navBarCloseRed = colors.navBarCloseRed,
            navBarShadow = colors.navBarShadow,
            navBarContainer = colors.background,
            navBarSelected = colors.navBarContent,
            navBarUnselected = colors.textSecondary
        )
    }
}

private fun createColorScheme(colors: DraftoColors): ColorScheme {
    return if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onNavBarIndicator,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.cyberViolet,
            onSecondary = colors.onBackground,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.onSecondaryContainer,
            tertiary = colors.electricMint,
            onTertiary = colors.onBackground,
            tertiaryContainer = colors.tertiaryContainer,
            onTertiaryContainer = colors.onTertiaryContainer,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.cardSurface,
            onSurface = colors.onCardSurface,
            surfaceVariant = colors.cardSurface,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.divider,
            outlineVariant = colors.cardBorder,
            error = DraftoError,
            onError = DraftoOnError,
            errorContainer = DraftoErrorContainer,
            onErrorContainer = DraftoOnErrorContainer
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onNavBarIndicator,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.cyberViolet,
            onSecondary = Color.White,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.onSecondaryContainer,
            tertiary = colors.electricMint,
            onTertiary = Color.White,
            tertiaryContainer = colors.tertiaryContainer,
            onTertiaryContainer = colors.onTertiaryContainer,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.cardSurface,
            onSurface = colors.onCardSurface,
            surfaceVariant = colors.background,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.divider,
            outlineVariant = colors.cardBorder,
            error = colors.sunsetCoral,
            onError = Color.White,
            errorContainer = colors.sunsetCoralContainer,
            onErrorContainer = colors.sunsetCoral
        )
    }
}

object DraftoTheme {
    val colors: DraftoColors
        @Composable
        get() = LocalDraftoColors.current

    val extraColors: DraftoExtraColors
        @Composable
        get() = LocalDraftoExtraColors.current
}

@Suppress("DEPRECATION")
@Composable
fun DraftoTheme(
    theme: String = "Dark",
    fontSizeStep: Int = 2,
    accentColor: String = DraftoAccentColor.DEFAULT.storageKey,
    isReducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeMode = DraftoThemeMode.fromString(theme)
    val baseColors = when (themeMode) {
        DraftoThemeMode.DARK -> DraftoDarkColors
        DraftoThemeMode.AMOLED -> DraftoAmoledColors
        DraftoThemeMode.LIGHT -> DraftoLightColors
        DraftoThemeMode.KRAFT -> DraftoKraftColors
    }

    val selectedAccent = DraftoAccentColor.fromString(accentColor)
    val resolvedAccent = selectedAccent.resolveColor(baseColors.isDark)
    val resolvedOnAccent = resolvedAccent.contrastContentColor()
    val draftoColors = baseColors.copy(
        accent = resolvedAccent,
        onAccent = resolvedOnAccent,
        navBarIndicator = resolvedAccent,
        onNavBarIndicator = resolvedOnAccent,
        switchCheckedTrack = if (baseColors.isDark) resolvedAccent.copy(alpha = 0.35f) else resolvedAccent,
        switchCheckedThumb = when (themeMode) {
            DraftoThemeMode.DARK, DraftoThemeMode.AMOLED -> resolvedAccent
            DraftoThemeMode.LIGHT -> PureWhite
            DraftoThemeMode.KRAFT -> WarmCanvas
        },
        switchCheckedIcon = if (baseColors.isDark) resolvedOnAccent else resolvedAccent
    )

    val colorScheme = createColorScheme(draftoColors)
    val extraColors = createExtraColors(draftoColors)
    val dynamicTypography = createScaledTypography(fontSizeStep)
    val windowSizeInfo = rememberWindowSizeInfo()

    val view = LocalView.current
    if (!view.isInEditMode) {
        val isDark = draftoColors.isDark
        DisposableEffect(isDark) {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.statusBarColor = android.graphics.Color.TRANSPARENT

                val statusBarStyle = if (isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                val navigationBarStyle = if (isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }

                (activity as? ComponentActivity)?.enableEdgeToEdge(
                    statusBarStyle = statusBarStyle,
                    navigationBarStyle = navigationBarStyle
                )

                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
            onDispose { }
        }
    }

    CompositionLocalProvider(
        LocalDraftoColors provides draftoColors,
        LocalDraftoExtraColors provides extraColors,
        LocalWindowSizeInfo provides windowSizeInfo,
        LocalDraftoReducedMotion provides isReducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = dynamicTypography,
            content = content
        )
    }
}

