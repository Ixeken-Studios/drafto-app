package com.ixeken.drafto.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==========================================
// Tokens de Espaciado y Padding (Design Tokens)
// ==========================================

// --- Espaciados Generales Base ---
val PaddingMicro: Dp = 2.dp
val PaddingExtraSmall: Dp = 4.dp
val PaddingSmall: Dp = 8.dp
val PaddingMedium: Dp = 12.dp
val PaddingLarge: Dp = 16.dp
val PaddingExtraLarge: Dp = 24.dp
val PaddingHuge: Dp = 32.dp

// --- Espaciados y Márgenes de Pantalla ---
val PaddingScreenHorizontal: Dp = 16.dp
val PaddingScreenTopBarTop: Dp = 8.dp
val PaddingScreenTopBarBottom: Dp = 8.dp
val HeightTopBar: Dp = 56.dp
val PaddingScreenBottomSpacer: Dp = 32.dp

// --- Espaciados para Listas y Grillas con Scroll ---
val PaddingListBottomContent: Dp = 96.dp
val PaddingListContentValues = PaddingValues(
    start = PaddingScreenHorizontal,
    top = PaddingSmall,
    end = PaddingScreenHorizontal,
    bottom = PaddingListBottomContent
)

// --- Tokens de Espaciado para Barra de Navegación (DraftoNavBar) ---
val PaddingNavBarHorizontal: Dp = 14.dp
val PaddingNavBarVertical: Dp = 16.dp
val PaddingNavBarTop: Dp = 8.dp
val PaddingNavBarBottom: Dp = 16.dp
val PaddingNavBarInnerHorizontal: Dp = 4.dp
val PaddingNavBarInnerVertical: Dp = 4.dp
val PaddingNavBarItemsSpacing: Dp = 10.dp
val PaddingNavBarTabItemHorizontal: Dp = 4.dp
val PaddingNavBarTabItemVertical: Dp = 4.dp
val PaddingNavBarActionCardHorizontal: Dp = 4.dp
val PaddingNavBarActionCardVertical: Dp = 6.dp
val NavBarCollapsedHeight: Dp = 58.dp
val NavBarExpandedHeight: Dp = 198.dp
val NavBarMaxWidth: Dp = 440.dp
val NavBarIndicatorSize: Dp = 50.dp
val NavBarActionCircleSize: Dp = 48.dp
val NavBarActionIconSize: Dp = 22.dp
val NavBarElevation: Dp = 20.dp
val NavBarIndicatorElevation: Dp = 4.dp
val NavBarActionElevation: Dp = 2.dp
val NavBarRightButtonIconSize: Dp = 28.dp
val NavBarTabIconSize: Dp = 24.dp
val NavBarContextualButtonSize: Dp = 36.dp
val NavBarContextualIconSize: Dp = 20.dp

// ==========================================
// Tokens de Diseño para Marcadores (Bookmarks)
// ==========================================
val BookmarkCardCornerRadius: Dp = 18.dp
val BookmarkCardElevation: Dp = 3.dp
val BookmarkImageHeightGrid: Dp = 110.dp
val BookmarkImageHeightList: Dp = 72.dp
val BookmarkThumbnailWidthList: Dp = 96.dp
val BookmarkCapsuleHeight: Dp = 44.dp
val BookmarkCapsuleSpacing: Dp = 8.dp
val BookmarkCapsuleThresholdCompactHeight: Dp = 40.dp
val BookmarkCapsuleIconSizeSmall: Dp = 18.dp
val BookmarkCapsuleIconSizeDefault: Dp = 20.dp
val BookmarkCapsuleTextPaddingEndSmall: Dp = 12.dp
val BookmarkCapsuleTextPaddingEndDefault: Dp = 16.dp
val BookmarkFaviconSize: Dp = 22.dp
val BookmarkFaviconContainerSize: Dp = 28.dp
val BookmarkDetailImageHeight: Dp = 110.dp
val BookmarkColorDotSize: Dp = 44.dp
val BookmarkIconPickerSize: Dp = 44.dp
val BookmarkIconPickerIconSize: Dp = 22.dp
val BookmarkCollectionCircleSpacing: Dp = 8.dp
val BookmarkIconPickerGridHeight: Dp = 96.dp
val BookmarkDividerHeight: Dp = 20.dp
val BookmarkActionIconSize: Dp = 20.dp
val BookmarkActionTouchSize: Dp = 36.dp
val BookmarkSelectionIndicatorSize: Dp = 32.dp
val BookmarkPreviewThumbnailSize: Dp = 56.dp
val BookmarkFallbackTopPadding: Dp = 148.dp
val DraftoStickyScaffoldFallbackTopPadding: Dp = 140.dp
val BookmarkDragHandleWidth: Dp = 38.dp
val BookmarkDragHandleHeight: Dp = 4.dp
val BookmarkDragHandlePaddingTop: Dp = 10.dp
val BookmarkDragHandlePaddingBottom: Dp = 12.dp
val BookmarkBackupIconContainerSize: Dp = 48.dp
val BookmarkBackupIconSize: Dp = 24.dp
val BookmarkBackupBadgePaddingHorizontal: Dp = 6.dp
val BookmarkBackupBadgePaddingVertical: Dp = 2.dp

// ==========================================
// Tokens de Diseño para Carpetas de Colección (Nothing OS Stepped Folders)
// ==========================================
val FolderCardHeight: Dp = 156.dp
val FolderCardCornerRadius: Dp = 22.dp
val FolderTopIconSize: Dp = 24.dp
val FolderTopPadding: Dp = 14.dp
val FolderContentPaddingHorizontal: Dp = 14.dp
val FolderContentPaddingBottom: Dp = 12.dp
val FolderActionDotsSize: Dp = 28.dp
val FolderGridMinSize: Dp = 152.dp

// Tokens de proporciones para la solapa escalonada
val FolderFlapTabHeightRatio: Float = 0.32f
val FolderFlapShelfHeightRatio: Float = 0.44f
val FolderFlapTabWidthRatio: Float = 0.36f
val FolderFlapCurveWidthRatio: Float = 0.16f

// ==========================================
// Tokens de Diseño para Conmutador de Marcadores (Bookmarks View Mode Switch)
// ==========================================
val BookmarkTabSwitchHeight: Dp = 36.dp
val BookmarkTabSwitchIconSize: Dp = 16.dp
val BookmarkTabSwitchSpacing: Dp = 6.dp
val BookmarkTabSwitchPaddingVertical: Dp = 6.dp
val BookmarkTabSwitchInnerPadding: Dp = 3.dp

// ==========================================
// Tokens de Diseño para Respaldo e Importación (Backup & Restore)
// ==========================================
val BackupOptionIconContainerSize: Dp = 48.dp
val BackupOptionIconSize: Dp = 24.dp

// ==========================================
// Tokens de Diseño para Tareas (To-dos)
// ==========================================
val TodoCheckboxSize: Dp = 24.dp
val TodoSubtaskCheckboxSize: Dp = 20.dp
val TodoSubtaskTouchSize: Dp = 32.dp
val TodoSubtaskBorderWidth: Dp = 1.5.dp
val TodoSubtaskCheckIconSize: Dp = 12.dp
val TodoProgressBarHeight: Dp = 4.dp
val TodoItemElevation: Dp = 2.dp
val TodoItemCornerRadius: Dp = 16.dp
val TodoFilterChipHeight: Dp = 36.dp
val NotebookFallbackTopPadding: Dp = 136.dp
val TodoSectionHeaderPaddingVertical: Dp = 6.dp
val TodoSectionBadgePaddingHorizontal: Dp = 8.dp
val TodoSectionBadgePaddingVertical: Dp = 2.dp
val TodoSectionChevronSize: Dp = 18.dp



// ==========================================
// Tokens de Diseño para Editor de Notas
// ==========================================
val EditorBulletDotSize: Dp = 6.dp
val EditorQuoteBarWidth: Dp = 4.dp
val EditorQuoteBarHeight: Dp = 28.dp
val EditorCodeBlockCornerRadius: Dp = 12.dp
val EditorMinContentHeight: Dp = 320.dp
val EditorBottomBarHeight: Dp = 48.dp
val EditorTopBarActionsSpacing: Dp = 4.dp
val EditorFormatBarElevation: Dp = 6.dp
val EditorFormatBarPaddingHorizontal: Dp = 8.dp
val EditorFormatBarPaddingVertical: Dp = 4.dp
val EditorFormatBarSpacing: Dp = 4.dp
val BorderWidthZero: Dp = 0.dp
val BorderWidthThin: Dp = 1.dp
val BorderWidthSelectedCard: Dp = 1.5.dp

// ==========================================
// Tokens de Diseño para Pinboard
// ==========================================
val PinboardFilterCapsuleHeight: Dp = 36.dp
val PinboardGridCardHeight: Dp = 180.dp
val PinboardPinBadgeSize: Dp = 26.dp
val PinboardPinBadgeSizeSmall: Dp = 20.dp
val PinboardPinIconSize: Dp = 16.dp
val PinboardPinIconSizeSmall: Dp = 12.dp
val PinboardAvatarSizeGrid: Dp = 80.dp
val PinboardAvatarSizeList: Dp = 48.dp

// ==========================================
// Tokens de Diseño para Barra de Búsqueda (Search Bar)
// ==========================================
val SearchBarHeight: Dp = 52.dp
val SearchBarIconSize: Dp = 22.dp
val SearchBarActionIconSize: Dp = 20.dp
val SearchBarActionTouchSize: Dp = 36.dp
val SearchBarInnerHorizontalPadding: Dp = 14.dp

// ==========================================
// Tokens de Diseño para Pantalla de Ajustes (Settings)
// ==========================================
val SettingsItemIconSize: Dp = 24.dp
val SettingsGroupSpacing: Dp = 16.dp
val SettingsItemPadding: Dp = 16.dp
val AboutTopBarLogoSize: Dp = 72.dp

// ==========================================
// Tokens de Diseño para Formulario de Marcadores (New/Edit Bookmark Bottom Sheet)
// ==========================================
val BookmarkFormButtonHeight: Dp = 50.dp
val BookmarkFormInputCornerRadius: Dp = 16.dp
val BookmarkFormCapsuleHeight: Dp = 38.dp
val BookmarkFormDividerHeight: Dp = 24.dp
val BookmarkFormSectionSpacing: Dp = 14.dp

// ==========================================
// Tokens de Diseño para Detalle de Marcadores (Bookmark Detail Bottom Sheet)
// ==========================================
val BookmarkDetailActionButtonSize: Dp = 56.dp
val BookmarkDetailActionIconSize: Dp = 24.dp
val BookmarkDetailImageHeightLarge: Dp = 190.dp
val BookmarkDetailToggleHeight: Dp = 40.dp
val BookmarkDetailToggleIconSize: Dp = 18.dp
val BookmarkDetailToggleEyeSize: Dp = 38.dp
val BookmarkDetailToggleEyeIconSize: Dp = 20.dp
val BookmarkDetailPillToggleHeight: Dp = 32.dp
val BookmarkDetailPillTogglePaddingHorizontal: Dp = 12.dp
val BookmarkDetailPillToggleIconSize: Dp = 16.dp

// ==========================================
// Tokens de Diseño para Selector de Color de Acento (Appearance)
// ==========================================
val AccentColorDotSize: Dp = 34.dp
val AccentColorIconCheckSize: Dp = 18.dp

// ==========================================
// Tokens de Diseño para Selector de Tema Visual (Theme Selection)
// ==========================================
val ThemeOptionCircleOuterSize: Dp = 56.dp
val ThemeOptionCircleInnerSize: Dp = 38.dp
val ThemeOptionSelectedBorderWidth: Dp = 2.5.dp
val ThemeOptionTextSpacing: Dp = 10.dp
val ThemeOptionGroupPadding: Dp = 16.dp

// ==========================================
// Tokens de Diseño para Colecciones Globales (Global Collections)
// ==========================================
val CollectionCapsulePaddingHorizontal: Dp = 12.dp
val CollectionCapsulePaddingVertical: Dp = 6.dp
val CollectionCapsuleDotSize: Dp = 8.dp
val CollectionCapsuleSpacing: Dp = 6.dp

// ==========================================
// Tokens de Diseño para Datos y Almacenamiento (Data & Storage)
// ==========================================
val StorageProgressBarHeight: Dp = 8.dp
val StorageProgressBarSegmentSpacing: Dp = 4.dp

// ==========================================
// Tokens Canónicos de Diseño para Bottom Sheets (Editorial Nothing OS)
// ==========================================
val SheetDragHandleWidth: Dp = 40.dp
val SheetDragHandleHeight: Dp = 4.dp
val SheetDragHandleVerticalPadding: Dp = 12.dp
val SheetHeaderPaddingTop: Dp = 8.dp
val SheetHeaderPaddingBottom: Dp = 16.dp
val SheetCardGroupSpacing: Dp = 12.dp
val SheetActionPillHeight: Dp = 48.dp
val SheetCancelButtonHeight: Dp = 48.dp
val SheetOptionItemPaddingVertical: Dp = 14.dp
val SheetOptionItemPaddingHorizontal: Dp = 16.dp
val SheetOptionIconSize: Dp = 20.dp
val SheetCornerRadius: Dp = 28.dp
val SheetCardCornerRadius: Dp = 16.dp
val SheetInputCornerRadius: Dp = 12.dp

// ==========================================
// Tokens de Diseño para Recorrido de Bienvenida (Welcome Walkthrough)
// ==========================================
val WalkthroughDotActiveWidth: Dp = 22.dp
val WalkthroughDotInactiveWidth: Dp = 6.dp
val WalkthroughDotHeight: Dp = 6.dp
val WalkthroughDotSpacing: Dp = 6.dp
val WalkthroughCardMinHeight: Dp = 210.dp
val WalkthroughIconContainerSize: Dp = 40.dp
val WalkthroughIconSize: Dp = 22.dp
val WalkthroughNavButtonHeight: Dp = 44.dp
val WalkthroughTodoCheckboxSize: Dp = 18.dp
val WalkthroughTodoCheckIconSize: Dp = 12.dp
val WalkthroughFaviconContainerSize: Dp = 24.dp
val WalkthroughQuoteBarWidth: Dp = 2.dp
val WalkthroughQuoteBarHeight: Dp = 18.dp

// ==========================================
// Tokens de Diseño para Cápsula Flotante de Selección (Selection Floating Dock)
// ==========================================
val SelectionDockHeight: Dp = 56.dp
val SelectionDockButtonSize: Dp = 44.dp
val SelectionDockIconSize: Dp = 22.dp

// ==========================================
// Tokens Canónicos para Menús Contextuales (Nothing OS Vertical Menu)
// ==========================================
val VerticalMenuElevation: Dp = 10.dp
val VerticalMenuCornerRadius: Dp = 24.dp
val VerticalMenuMinWidth: Dp = 230.dp
val VerticalMenuQuickActionButtonSize: Dp = 44.dp
val VerticalMenuQuickActionIconSize: Dp = 22.dp
val VerticalMenuQuickActionSpacing: Dp = 4.dp
val VerticalMenuItemPaddingHorizontal: Dp = 14.dp
val VerticalMenuItemPaddingVertical: Dp = 10.dp
val VerticalMenuItemIconSize: Dp = 20.dp

// ==========================================
// Tokens de Diseño para Botón Flotante de Acción (Floating Action Capsule)
// ==========================================
val FloatingActionCapsuleHeight: Dp = 52.dp
val FloatingActionCapsuleIconSize: Dp = 20.dp
val FloatingActionCapsulePaddingHorizontal: Dp = 20.dp
val FloatingActionCapsuleSpacing: Dp = 8.dp

// ==========================================
// Tokens de Diseño para Dynamic Island (Nothing OS 5 Live Note)
// ==========================================
val DynamicIslandWidth: Dp = 320.dp
val DynamicIslandCornerRadius: Dp = 32.dp
val DynamicIslandElevation: Dp = 20.dp
val DynamicIslandPaddingHorizontal: Dp = 16.dp
val DynamicIslandPaddingBottom: Dp = 16.dp
val DynamicIslandInputHeight: Dp = 120.dp
val DynamicIslandInputCornerRadius: Dp = 18.dp
val DynamicIslandButtonHeight: Dp = 42.dp
val DynamicIslandButtonSpacing: Dp = 8.dp
val DynamicIslandDotSize: Dp = 8.dp
val DynamicIslandDotGlowSize: Dp = 16.dp
val DynamicIslandCounterPaddingHorizontal: Dp = 8.dp
val DynamicIslandCounterPaddingVertical: Dp = 3.dp

// ==========================================
// Tokens de Diseño para Pantalla de Bloqueo (DraftoLockScreen)
// ==========================================
val LockScreenIconContainerSize: Dp = 80.dp
val LockScreenIconSize: Dp = 36.dp
val LockScreenButtonHeight: Dp = 52.dp
val LockScreenButtonIconSize: Dp = 24.dp

// ==========================================
// Tokens de Diseño para Notificación Flotante (DraftoFloatingToast)
// ==========================================
val ToastPaddingHorizontal: Dp = 16.dp
val ToastPaddingVertical: Dp = 10.dp
val ToastIconSize: Dp = 18.dp
val ToastSpacing: Dp = 10.dp
val ToastElevation: Dp = 12.dp
val ToastMaxWidth: Dp = 380.dp
val ToastBorderWidth: Dp = 1.dp
val ToastBottomOffsetWithNavBar: Dp = 90.dp
val ToastBottomOffsetWithoutNavBar: Dp = 24.dp



