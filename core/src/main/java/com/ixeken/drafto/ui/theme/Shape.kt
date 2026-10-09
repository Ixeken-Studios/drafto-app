package com.ixeken.drafto.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==========================================
// Dimensiones y Radios de Curvatura (Design Tokens)
// ==========================================

// --- Tokens de Formas M3 Expressive ---
val DraftoShapeCircle = RoundedCornerShape(50)
val DraftoShapePill = RoundedCornerShape(50)
val DraftoShapeSquircle = RoundedCornerShape(22.dp)
val DraftoShapeCard = RoundedCornerShape(20.dp)
val DraftoShapeCardSmall = RoundedCornerShape(12.dp)

/**
 * Radio de curvatura unificado para la barra de navegación Nothing OS.
 * Píldora completa en reposo (50%) y tarjeta curvada (28.dp) al expandirse.
 */
val RadioNavBar: Dp = 29.dp
val RadioNavBarExpanded: Dp = 28.dp
val RadioNavBarCloseButton: Dp = 18.dp

// Shapes derivados del radio de la navbar
val DraftoNavBarShape = DraftoShapePill
val DraftoNavBarExpandedShape = RoundedCornerShape(RadioNavBarExpanded)
val DraftoNavBarItemShape = DraftoShapeCircle
val DraftoNavBarActionCardShape = DraftoShapeCircle
val DraftoNavBarRightButtonShape = DraftoShapeCircle

/**
 * Radio de curvatura unificado para las barras de búsqueda (Search Bars).
 * Configurado como píldora completa (DraftoShapePill / 50) unificada en toda la app.
 */
val RadioSearchBar: Dp = 26.dp
val DraftoSearchBarShape = DraftoShapePill
/**
 * Radio de curvatura unificado para todos los contenedores e insignias de iconos (Action Badges).
 * Configurado como circular (50) para diseño de insignias redondas.
 */
val RadioActionBadgePercent: Int = 50
val DraftoShapeActionBadge = RoundedCornerShape(RadioActionBadgePercent)

/**
 * Forma canónica para la píldora de notificación flotante (DraftoFloatingToast).
 */
val DraftoToastShape = DraftoShapePill

val DraftoShapeBookmarkCard = RoundedCornerShape(BookmarkCardCornerRadius)
val DraftoShapeBookmarkDetailImage = RoundedCornerShape(16.dp)

/**
 * Radio de curvatura unificado para el recorte inferior de pantallas principales sobre la barra de navegación.
 */
val DraftoShapeScreenBottom = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
val DraftoShapeBookmarkFormInput = RoundedCornerShape(BookmarkFormInputCornerRadius)

/**
 * Formas y curvaturas canónicas para Bottom Sheets, Cuadros de Diálogo y Menús Nothing OS.
 */
val RadioBottomSheetTop: Dp = SheetCornerRadius
val DraftoShapeBottomSheet = RoundedCornerShape(topStart = RadioBottomSheetTop, topEnd = RadioBottomSheetTop)
val DraftoShapeSheetCard = RoundedCornerShape(SheetCardCornerRadius)
val DraftoShapeSheetInput = RoundedCornerShape(SheetInputCornerRadius)
val DraftoShapeDialog = RoundedCornerShape(24.dp)
val DraftoShapeVerticalMenu = RoundedCornerShape(VerticalMenuCornerRadius)
val DraftoShapeMenu = DraftoShapeVerticalMenu
val DraftoShapePillButton = DraftoShapePill

/**
 * Formas y curvaturas canónicas para Dynamic Island Nothing OS 5 (Hardware Camouflage).
 */
val DraftoShapeDynamicIsland = RoundedCornerShape(DynamicIslandCornerRadius)
val DraftoShapeDynamicIslandInput = RoundedCornerShape(DynamicIslandInputCornerRadius)

