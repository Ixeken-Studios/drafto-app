package com.ixeken.drafto.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ==========================================
// Drafto Core Named Palette (Preliminary)
// ==========================================
val PureBlack = Color(0xFF000000)
val PureWhite = Color(0xFFFFFFFF)
val CarbonBlack = Color(0xFF1F1F1F)
val Graphite = Color(0xFF2D2D2D)
val WhiteSmoke = Color(0xFFEDEDED)
val Onyx = Color(0xFF0A0A0A)
val Charred = Color(0xFF171717)
val Gunmetal = Color(0xFF737373)
val Silver = Color(0xFFA3A3A3)
val Snow = Color(0xFFFAFAFA)
val Pearl = Color(0xFFF5F5F5)
val Dust = Color(0xFFD4D4D4)
val Charcoal = Color(0xFF525252)
val PureSky = Color(0xFF43B6ED)

// --- Paleta Kraft Lino (Cuaderno / Papel Cálido) ---
val LinenBackground = Color(0xFFEDE7DF)
val OatPaper = Color(0xFFF5F0E8)
val WarmCanvas = Color(0xFFFBF8F3)
val WarmSand = Color(0xFFE4DCD0)
val PaleStone = Color(0xFFDDD3C5)
val GraphiteInk = Color(0xFF1F1C1A)
val AshStone = Color(0xFF635D55)
val WarmMuted = Color(0xFF857D74)
val LinenBorder = Color(0xFFD6CCC0)
val WarmAmber = Color(0xFFD97706)

// ==========================================
// 2. Tokens Semánticos de Elementos (Las "Cosas" -> Nombre del Color)
// Para cambiar el color de un elemento, cambia una sola línea aquí
// y se propagará automáticamente a todos los temas y componentes.
// ==========================================

// Acento Principal
val AccentColor = PureSky

// Fondos de Pantalla (Background)
val BackgroundDark = CarbonBlack
val BackgroundAmoled = PureBlack
val BackgroundLight = WhiteSmoke
val BackgroundKraft = LinenBackground

// Superficies de Tarjetas y Contenedores (CardSurface)
val CardSurfaceDark = Graphite
val CardSurfaceAmoled = Graphite
val CardSurfaceLight = PureWhite
val CardSurfaceKraft = OatPaper

// Textos - Modo Oscuro y AMOLED
val TextTitleDark = PureWhite
val TextPrimaryDark = PureWhite
val TextSecondaryDark = Dust
val TextPlaceholderDark = Charcoal

// Textos - Modo Claro
val TextTitleLight = PureBlack
val TextPrimaryLight = PureBlack
val TextSecondaryLight = Gunmetal
val TextPlaceholderLight = Silver

// Textos - Modo Kraft (Cuaderno)
val TextTitleKraft = GraphiteInk
val TextPrimaryKraft = GraphiteInk
val TextSecondaryKraft = AshStone
val TextPlaceholderKraft = WarmMuted

// Barra de Navegación Flotante (NavBar) - Superficies basadas en PrimaryContainer
val NavBarPrimaryContainerDark = Graphite
val NavBarPrimaryContainerAmoled = Graphite
val NavBarPrimaryContainerLight = PureWhite
val NavBarPrimaryContainerKraft = OatPaper

val NavBarSurfaceDark = NavBarPrimaryContainerDark
val NavBarSurfaceAmoled = NavBarPrimaryContainerAmoled
val NavBarSurfaceLight = NavBarPrimaryContainerLight
val NavBarSurfaceKraft = NavBarPrimaryContainerKraft

val NavBarSurfaceTranslucentDark = NavBarPrimaryContainerDark.copy(alpha = 0.20f)
val NavBarSurfaceTranslucentAmoled = NavBarPrimaryContainerAmoled.copy(alpha = 0.20f)
val NavBarSurfaceTranslucentLight = NavBarPrimaryContainerLight.copy(alpha = 0.20f)
val NavBarSurfaceTranslucentKraft = NavBarPrimaryContainerKraft.copy(alpha = 0.20f)

val NavBarContentDark = PureWhite
val NavBarContentAmoled = PureWhite
val NavBarContentLight = PureBlack
val NavBarContentKraft = GraphiteInk

// Barra de Búsqueda (SearchBar)
val SearchBarBackgroundDark = Graphite
val SearchBarBackgroundAmoled = Graphite
val SearchBarBackgroundLight = PureWhite
val SearchBarBackgroundKraft = WarmCanvas

val SearchBarPlaceholderDark = TextPlaceholderDark
val SearchBarPlaceholderAmoled = TextPlaceholderDark
val SearchBarPlaceholderLight = TextPlaceholderLight
val SearchBarPlaceholderKraft = TextPlaceholderKraft

// Hojas Desplegables Inferiores (BottomSheet)
val BottomSheetSurfaceDark = CarbonBlack
val BottomSheetSurfaceAmoled = PureBlack
val BottomSheetSurfaceLight = WhiteSmoke
val BottomSheetSurfaceKraft = LinenBackground

val BottomSheetInnerCardDark = Graphite
val BottomSheetInnerCardAmoled = Graphite
val BottomSheetInnerCardLight = PureWhite
val BottomSheetInnerCardKraft = OatPaper

// Contenedores Jerárquicos - Modo Claro (3 Capas: PureWhite -> WhiteSmoke -> Dust)
val PrimaryContainerLight = PureWhite
val OnPrimaryContainerLight = PureBlack

val SecondaryContainerLight = WhiteSmoke
val OnSecondaryContainerLight = PureBlack

val TertiaryContainerLight = Dust
val OnTertiaryContainerLight = PureBlack

// Contenedores Jerárquicos - Modo Oscuro
val PrimaryContainerDark = Graphite
val OnPrimaryContainerDark = PureWhite

val SecondaryContainerDark = Charred
val OnSecondaryContainerDark = PureWhite

val TertiaryContainerDark = Charcoal
val OnTertiaryContainerDark = PureWhite

// Contenedores Jerárquicos - Modo AMOLED
val PrimaryContainerAmoled = Graphite
val OnPrimaryContainerAmoled = PureWhite

val SecondaryContainerAmoled = Onyx
val OnSecondaryContainerAmoled = PureWhite

val TertiaryContainerAmoled = Charred
val OnTertiaryContainerAmoled = PureWhite

// Contenedores Jerárquicos - Modo Kraft (3 Capas: WarmCanvas -> WarmSand -> PaleStone)
val PrimaryContainerKraft = WarmCanvas
val OnPrimaryContainerKraft = GraphiteInk

val SecondaryContainerKraft = WarmSand
val OnSecondaryContainerKraft = GraphiteInk

val TertiaryContainerKraft = PaleStone
val OnTertiaryContainerKraft = GraphiteInk

// ==========================================
// 3. Tokens de Compatibilidad con Componentes Existentes
// ==========================================
val DraftoBackground = BackgroundDark
val DraftoOnBackground = TextPrimaryDark
val DraftoPrimaryContainer = PrimaryContainerDark
val DraftoOnPrimaryContainer = OnPrimaryContainerDark
val DraftoSecondaryContainer = SecondaryContainerDark
val DraftoOnSecondaryContainer = OnSecondaryContainerDark
val DraftoTertiaryContainer = TertiaryContainerDark
val DraftoOnTertiaryContainer = OnTertiaryContainerDark
val DraftoSurface = CardSurfaceDark
val DraftoOnSurface = TextPrimaryDark
val DraftoError = Color(0xFFD90429)
val DraftoOnError = PureWhite
val DraftoErrorContainer = Color(0xFFD90429)
val DraftoOnErrorContainer = PureWhite
val DraftoSurfaceVariant = PureWhite
val DraftoOnSurfaceVariant = PureBlack
val DraftoSave = Color(0xFF6A994E)
val DraftoOnSave = PureWhite
val DraftoSaveContainer = Color(0xFF6A994E)
val DraftoOnSaveContainer = PureWhite
val DraftoBackgroundVariant = CardSurfaceDark
val DraftoOnBackgroundVariant = TextPrimaryDark
val DraftoStatusBar = PureWhite
val DraftoStatusBarCamera = PureBlack
val DraftoHandle = PureWhite

// Dynamic NavBar Tokens (Dark Mode & Glassmorphic)
val DraftoNavBarSurface = NavBarSurfaceDark
val DraftoNavBarSurfaceTranslucent = NavBarSurfaceTranslucentDark
val DraftoNavBarContent = NavBarContentDark
val DraftoNavBarIndicator = AccentColor
val DraftoOnNavBarIndicator = PureBlack
val DraftoNavBarActionCard = Color(0x1AFFFFFF)
val DraftoNavBarCloseRed = Color(0xFFE05D58)
val DraftoNavBarShadow = Color(0xCC000000)

// Legacy / Component Compatibility Tokens
val DraftoOnNavBarSurface = NavBarContentDark
val DraftoNavBarContainer = BackgroundDark
val DraftoNavBarSelected = NavBarContentDark
val DraftoNavBarUnselected = TextSecondaryDark

// --- Paleta Organica y Matizada para Nodos Cognitivos (Muted / Low Saturation Botanical Palette) ---
val DraftoElectricMint = Color(0xFF2EA57D)
val DraftoElectricMintContainer = Color(0x262EA57D) // 15% opacidad para badges

val DraftoAquaCyan = Color(0xFF2E97A6)
val DraftoAquaCyanContainer = Color(0x262E97A6)

val DraftoCyberViolet = Color(0xFF8B68C8)
val DraftoCyberVioletContainer = Color(0x268B68C8)

val DraftoWarmMango = Color(0xFFE5A038)
val DraftoWarmMangoContainer = Color(0x26E5A038)

val DraftoEmerald = Color(0xFF259E7A) // Tono verde salvia/teal exacto al diseño de referencia
val DraftoEmeraldContainer = Color(0x26259E7A)

val DraftoNeonPink = Color(0xFFD95D7F)
val DraftoNeonPinkContainer = Color(0x26D95D7F)

val DraftoSunsetCoral = Color(0xFFE05D58)
val DraftoSunsetCoralContainer = Color(0x26E05D58)

val DraftoIndigo = Color(0xFF5E65C7)
val DraftoIndigoContainer = Color(0x265E65C7)

val DraftoElectricLime = Color(0xFF8EA83B)
val DraftoElectricLimeContainer = Color(0x268EA83B)

val DraftoBrightOrange = Color(0xFFD97736)
val DraftoBrightOrangeContainer = Color(0x26D97736)

val DraftoSkyBlue = Color(0xFF4A97C9)
val DraftoSkyBlueContainer = Color(0x264A97C9)

val DraftoElectricLavender = Color(0xFFA67EC8)
val DraftoElectricLavenderContainer = Color(0x26A67EC8)

// --- Colores de Acento Seleccionables (Accent Palette: Duplas Oscuro / Claro) ---
val DraftoFreshSkyDark = PureSky // #43B6ED
val DraftoFreshSkyLight = Color(0xFF0284C7)

val DraftoSchoolBusDark = Color(0xFFFFC700)
val DraftoSchoolBusLight = Color(0xFFD97706)

val DraftoMutedOliveDark = Color(0xFF85B979)
val DraftoMutedOliveLight = Color(0xFF4D7C0F)

val DraftoAmethystDark = Color(0xFFA383C5)
val DraftoAmethystLight = Color(0xFF7E22CE)

val DraftoDarkOrangeDark = Color(0xFFFF8C00)
val DraftoDarkOrangeLight = Color(0xFFEA580C)

val DraftoFlagRedDark = Color(0xFFD71A21)
val DraftoFlagRedLight = Color(0xFFDC2626)

// Retrocompatibilidad con referencias existentes
val DraftoFreshSky = DraftoFreshSkyDark
val DraftoSchoolBus = DraftoSchoolBusDark
val DraftoMutedOlive = DraftoMutedOliveDark
val DraftoAmethyst = DraftoAmethystDark
val DraftoDarkOrange = DraftoDarkOrangeDark
val DraftoFlagRed = DraftoFlagRedDark
val DraftoAccentWhite = PureWhite
val DraftoAccentBlack = PureBlack

/**
 * Determina el color de texto o glifo de mayor contraste según la luminancia relativa WCAG.
 *
 * Si la luminancia de la superficie supera el umbral de 0.40f, devuelve [PureBlack].
 * De lo contrario, devuelve [PureWhite].
 */
fun Color.contrastContentColor(): Color =
    if (luminance() > 0.40f) PureBlack else PureWhite


// --- Bordes y Sombras Especulares para Vidrio y Tarjetas ---
val DraftoCardSurface = Graphite
val DraftoCardBorder = Color(0x1FFFFFFF)
val DraftoCardBorderHighlight = Color(0x3DFFFFFF)

// Settings Tokens (Cohesive with Drafto Brain OS)
val DraftoSettingsBackground = DraftoBackground // CarbonBlack
val DraftoSettingsCardBackground = DraftoCardSurface // Graphite
val DraftoSettingsSheetBackground = DraftoBackground // CarbonBlack
val DraftoSettingsSheetCardBackground = DraftoCardSurface // Graphite
val DraftoSettingsRedClose = DraftoSunsetCoral // #FF5757
val DraftoSettingsActiveGreen = DraftoEmerald // #10B981

// Token semántico para glifos e íconos sobre badges y contenedores sólidos vibrantes
val DraftoDarkGlyph = Charred
val DraftoLightGlyph = PureWhite

// Tokens semánticos para botones de cancelación en Bottom Sheets (Editorial Nothing OS)
val DraftoSheetCancelRed = DraftoFlagRed // #D71A21 rojo intenso canónico de las capturas
val DraftoSheetCancelRedSecondary = DraftoSunsetCoral // #FF5757
