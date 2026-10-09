package com.ixeken.drafto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.BookmarkCollection
import com.ixeken.drafto.domain.model.DraftoCollectionPresetColors
import com.ixeken.drafto.ui.adaptive.DeviceFormFactor
import com.ixeken.drafto.ui.adaptive.LocalWindowSizeInfo
import com.ixeken.drafto.ui.theme.BookmarkColorDotSize
import com.ixeken.drafto.ui.theme.BookmarkCollectionCircleSpacing
import com.ixeken.drafto.ui.theme.BookmarkDragHandleHeight
import com.ixeken.drafto.ui.theme.BookmarkDragHandlePaddingBottom
import com.ixeken.drafto.ui.theme.BookmarkDragHandlePaddingTop
import com.ixeken.drafto.ui.theme.BookmarkDragHandleWidth
import com.ixeken.drafto.ui.theme.BookmarkFormButtonHeight
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.BookmarkIconPickerGridHeight
import com.ixeken.drafto.ui.theme.BookmarkIconPickerIconSize
import com.ixeken.drafto.ui.theme.BookmarkIconPickerSize
import com.ixeken.drafto.ui.theme.BorderWidthThin
import com.ixeken.drafto.ui.theme.DraftoShapeBookmarkFormInput
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.DraftoWarmMango
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PureBlack
import com.ixeken.drafto.ui.theme.PureWhite
import com.ixeken.drafto.ui.theme.contrastContentColor

/**
 * Nombres clave de iconos Material disponibles para asignar a colecciones.
 */
val DraftoCollectionPresetIcons: List<String> = listOf(
    "Folder",
    "Bookmarks",
    "Bookmark",
    "Star",
    "Favorite",
    "Code",
    "Terminal",
    "Calendar",
    "TaskAlt",
    "Description",
    "Work",
    "School",
    "Language",
    "Palette",
    "Brush",
    "Movie",
    "MusicNote",
    "Headphones",
    "PhotoCamera",
    "Gamepad",
    "Home",
    "ShoppingCart",
    "Restaurant",
    "LocalCafe",
    "Savings",
    "CreditCard",
    "FitnessCenter",
    "DirectionsRun",
    "SelfImprovement",
    "Flight",
    "Map",
    "DirectionsCar",
    "Lightbulb",
    "Pets",
    "Build",
    "Lock",
    "Warning",
    "Hub",
    "Person"
)

/**
 * Normaliza claves semánticas y códigos hexadecimales legados a su token semántico correspondiente.
 * Asegura retrocompatibilidad con colecciones creadas con versiones anteriores de Drafto.
 */
fun normalizeCollectionColorToken(color: String): String = when (color.lowercase().trim()) {
    "monochrome" -> "monochrome"
    "crimson", "#e05d58" -> "crimson"
    "orange" -> "orange"
    "amber", "#e5a038" -> "amber"
    "lime", "#2ea57d" -> "lime"
    "emerald", "#259e7a" -> "emerald"
    "cyan", "#2e97a6" -> "cyan"
    "cobalt", "#4a97c9" -> "cobalt"
    "indigo", "#5e65c7" -> "indigo"
    "purple", "#8b68c8" -> "purple"
    "pink", "#d95d7f" -> "pink"
    "clay", "#d97736" -> "clay"
    else -> color
}

/**
 * Resuelve el color de una colección considerando si el tema actual es oscuro.
 * Soporta tokens semánticos adaptables (incluyendo "monochrome") y códigos hexadecimales directos,
 * garantizando contraste óptimo y retrocompatibilidad con registros preexistentes.
 */
fun parseCollectionColor(
    colorHex: String,
    isDark: Boolean,
    fallback: Color = DraftoWarmMango
): Color {
    return when (normalizeCollectionColorToken(colorHex)) {
        "monochrome" -> if (isDark) PureWhite else PureBlack
        "crimson" -> if (isDark) Color(0xFFE11D48) else Color(0xFFDC2626)
        "orange" -> if (isDark) Color(0xFFF97316) else Color(0xFFEA580C)
        "amber" -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        "lime" -> if (isDark) Color(0xFF84CC16) else Color(0xFF4D7C0F)
        "emerald" -> if (isDark) Color(0xFF10B981) else Color(0xFF047857)
        "cyan" -> if (isDark) Color(0xFF06B6D4) else Color(0xFF0284C7)
        "cobalt" -> if (isDark) Color(0xFF3B82F6) else Color(0xFF1D4ED8)
        "indigo" -> if (isDark) Color(0xFF6366F1) else Color(0xFF4338CA)
        "purple" -> if (isDark) Color(0xFFA855F7) else Color(0xFF7E22CE)
        "pink" -> if (isDark) Color(0xFFEC4899) else Color(0xFFBE185D)
        "clay" -> if (isDark) Color(0xFFD97736) else Color(0xFF92400E)
        else -> try {
            val sanitizedHex = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
            Color(android.graphics.Color.parseColor(sanitizedHex))
        } catch (_: Exception) {
            fallback
        }
    }
}

/**
 * Parsea con seguridad un código o token de color, delegando en la resolución adaptativa en tema oscuro por defecto.
 */
fun parseCollectionColor(colorHex: String, fallback: Color = DraftoWarmMango): Color {
    return parseCollectionColor(colorHex = colorHex, isDark = true, fallback = fallback)
}

/**
 * Mapea el nombre clave de un icono a su correspondiente [ImageVector] redondeado/sólido.
 */
fun getCollectionIcon(iconName: String): ImageVector = when (iconName.lowercase()) {
    "code" -> Icons.Rounded.Code
    "terminal", "console" -> Icons.Rounded.Terminal
    "bookmarks", "collections" -> Icons.Rounded.Bookmarks
    "bookmark" -> Icons.Rounded.Bookmark
    "calendar", "event", "date" -> Icons.Rounded.CalendarToday
    "task", "todo", "check", "taskalt" -> Icons.Rounded.TaskAlt
    "note", "description", "document" -> Icons.Rounded.Description
    "star" -> Icons.Rounded.Star
    "favorite", "heart" -> Icons.Rounded.Favorite
    "work", "business" -> Icons.Rounded.Work
    "school", "book", "education" -> Icons.Rounded.School
    "language", "web", "globe" -> Icons.Rounded.Language
    "palette", "art" -> Icons.Rounded.Palette
    "brush", "paint", "design" -> Icons.Rounded.Brush
    "movie", "video", "film" -> Icons.Rounded.Movie
    "music", "musicnote", "audio" -> Icons.Rounded.MusicNote
    "headphones", "podcast" -> Icons.Rounded.Headphones
    "camera", "photocamera", "photo" -> Icons.Rounded.PhotoCamera
    "game", "gamepad", "sportsesports" -> Icons.Rounded.SportsEsports
    "home", "house" -> Icons.Rounded.Home
    "cart", "shopping", "shoppingcart" -> Icons.Rounded.ShoppingCart
    "restaurant", "food", "dining" -> Icons.Rounded.Restaurant
    "cafe", "coffee", "localcafe" -> Icons.Rounded.LocalCafe
    "savings", "money", "piggy" -> Icons.Rounded.Savings
    "card", "creditcard", "finance" -> Icons.Rounded.CreditCard
    "fitness", "gym", "fitnesscenter" -> Icons.Rounded.FitnessCenter
    "run", "running", "directionsrun", "sport" -> Icons.AutoMirrored.Rounded.DirectionsRun
    "meditation", "yoga", "selfimprovement", "spa" -> Icons.Rounded.SelfImprovement
    "travel", "flight", "plane" -> Icons.Rounded.Flight
    "map", "navigation", "location" -> Icons.Rounded.Map
    "car", "auto", "directionscar" -> Icons.Rounded.DirectionsCar
    "idea", "lightbulb", "bulb" -> Icons.Rounded.Lightbulb
    "pet", "pets", "dog", "cat", "animal" -> Icons.Rounded.Pets
    "build", "tool", "tools", "wrench" -> Icons.Rounded.Build
    "lock", "security", "private" -> Icons.Rounded.Lock
    "warning", "alert" -> Icons.Rounded.Warning
    "hub", "nodes", "tech" -> Icons.Rounded.Hub
    "person", "user", "accessibility" -> Icons.Rounded.AccessibilityNew
    else -> Icons.Rounded.Folder
}

/**
 * Modal Bottom Sheet unificado para la creación y edición de una colección temática de marcadores.
 *
 * Características de diseño (Nothing OS / Cohesión Editorial):
 * - Manija de arrastre superior estándar sin botón de cierre (X).
 * - Título centrado en [FrauncesFontFamily] ("New collection" o "Edit collection").
 * - Sección "Collection name" en [FrauncesFontFamily] con contenedor en primaryContainer y texto en negrita.
 * - Selector horizontal de colores circulares de 46dp con checkmark oscuro en el seleccionado.
 * - Selector horizontal de iconos circulares de 46dp que adopta el color seleccionado como fondo.
 * - Botones verticales (un botón por row):
 *   - En creación: Save arriba (100% ancho) y Cancel abajo (100% ancho).
 *   - En edición: Save arriba (100% ancho) y fila inferior 50/50 con Delete (OutlinedButton con borde fino en sunsetCoral) y Cancel.
 *   - Diálogo modal de confirmación [DraftoConfirmationDialog] al eliminar una colección.
 *
 * @param onDismiss Callback ejecutado al cerrar el modal.
 * @param onSaveCollection Callback ejecutado con los datos de la colección creada o actualizada.
 * @param initialCollection Datos de la colección a editar, o null si se está creando una nueva.
 * @param onDeleteCollection Callback opcional para eliminar la colección por ID.
 * @param modifier Modificador Compose opcional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoCollectionFormBottomSheet(
    onDismiss: () -> Unit,
    onSaveCollection: (name: String, colorHex: String, iconName: String) -> Unit,
    initialCollection: BookmarkCollection? = null,
    onDeleteCollection: ((id: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isEditMode = initialCollection != null
    val isDarkTheme = DraftoTheme.colors.isDark
    var nameState by remember(initialCollection) {
        mutableStateOf(initialCollection?.name ?: "")
    }
    var selectedColorHex by remember(initialCollection) {
        mutableStateOf(initialCollection?.colorHex ?: DraftoCollectionPresetColors[0])
    }
    var selectedIconName by remember(initialCollection) {
        mutableStateOf(initialCollection?.iconName ?: DraftoCollectionPresetIcons[0])
    }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    DraftoModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaddingScreenHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            DraftoSheetHeader(
                title = stringResource(
                    if (isEditMode) R.string.bookmark_menu_edit_collection
                    else R.string.bookmark_menu_create_collection
                )
            )

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 1. SECCIÓN: Collection name
            Text(
                text = stringResource(R.string.bookmark_collection_name),
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DraftoShapeBookmarkFormInput)
                    .background(DraftoTheme.colors.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (nameState.isEmpty()) {
                    Text(
                        text = stringResource(R.string.bookmark_placeholder_collection_name),
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = DraftoTheme.colors.textMuted
                    )
                }
                BasicTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(DraftoTheme.colors.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 2. SECCIÓN: Color (selector adaptable FlowRow en 2 o más filas)
            Text(
                text = stringResource(R.string.bookmark_collection_color),
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            val colorRows = remember { DraftoCollectionPresetColors.chunked(6) }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BookmarkCollectionCircleSpacing)
            ) {
                colorRows.forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowColors.forEach { colorHex ->
                            val color = parseCollectionColor(colorHex, isDark = isDarkTheme)
                            val isSelected = normalizeCollectionColorToken(colorHex).equals(
                                normalizeCollectionColorToken(selectedColorHex),
                                ignoreCase = true
                            )
                            val isMonochrome = colorHex.equals("monochrome", ignoreCase = true)
                            val checkTint = color.contrastContentColor()

                            Box(
                                modifier = Modifier
                                    .size(BookmarkColorDotSize)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isMonochrome) {
                                            Modifier.border(BorderWidthThin, DraftoTheme.colors.cardBorder, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { selectedColorHex = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = checkTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 3. SECCIÓN: Icon (selector adaptable de iconos en 6 columnas justificadas)
            Text(
                text = stringResource(R.string.bookmark_collection_icon),
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(PaddingSmall))

            val activeColor = parseCollectionColor(selectedColorHex, isDark = isDarkTheme)
            val iconTintActive = activeColor.contrastContentColor()

            val windowSizeInfo = LocalWindowSizeInfo.current
            val isCompactScreen = windowSizeInfo.formFactor != DeviceFormFactor.TABLET

            if (isCompactScreen) {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BookmarkIconPickerGridHeight),
                    horizontalArrangement = Arrangement.spacedBy(BookmarkCollectionCircleSpacing),
                    verticalArrangement = Arrangement.spacedBy(BookmarkCollectionCircleSpacing)
                ) {
                    items(
                        items = DraftoCollectionPresetIcons,
                        key = { it },
                        contentType = { "collection_icon_item" }
                    ) { iconKey ->
                        CollectionIconPickerItem(
                            iconKey = iconKey,
                            isSelected = iconKey.equals(selectedIconName, ignoreCase = true),
                            activeColor = activeColor,
                            iconTintActive = iconTintActive,
                            onClick = { selectedIconName = iconKey }
                        )
                    }
                }
            } else {
                val iconRows = remember { DraftoCollectionPresetIcons.chunked(6) }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(BookmarkCollectionCircleSpacing)
                ) {
                    iconRows.forEach { rowIcons ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowIcons.forEach { iconKey ->
                                CollectionIconPickerItem(
                                    iconKey = iconKey,
                                    isSelected = iconKey.equals(selectedIconName, ignoreCase = true),
                                    activeColor = activeColor,
                                    iconTintActive = iconTintActive,
                                    onClick = { selectedIconName = iconKey }
                                )
                            }
                            repeat(6 - rowIcons.size) {
                                Spacer(modifier = Modifier.size(BookmarkIconPickerSize))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaddingLarge))

            // 4. ACCIONES INFERIORES: Un botón por row (Save arriba, Cancel/Delete abajo)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(PaddingSmall)
            ) {
                // Save (Arriba, 100% ancho)
                val isSaveEnabled = nameState.trim().isNotBlank()
                DraftoSheetPrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        val trimmedName = nameState.trim()
                        if (trimmedName.isNotBlank()) {
                            onSaveCollection(trimmedName, selectedColorHex, selectedIconName)
                            onDismiss()
                        }
                    },
                    enabled = isSaveEnabled,
                    containerColor = DraftoTheme.colors.emerald,
                    contentColor = Color.White
                )

                if (isEditMode && onDeleteCollection != null) {
                    // Fila inferior dividida 50/50: Delete (OutlinedButton) y Cancel (DraftoSheetCancelButton)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                    ) {
                        // Delete (OutlinedButton con borde sutil en sunsetCoral)
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(BookmarkFormButtonHeight),
                            shape = DraftoShapePill,
                            border = BorderStroke(BorderWidthThin, DraftoTheme.colors.sunsetCoral),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = DraftoTheme.colors.sunsetCoral
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.action_delete),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Cancel
                        DraftoSheetCancelButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Cancel (Abajo, 100% ancho)
                    DraftoSheetCancelButton(
                        onClick = onDismiss
                    )
                }
            }
        }
    }

    // Diálogo modal de confirmación de eliminación segura
    if (showDeleteConfirmDialog && initialCollection != null && onDeleteCollection != null) {
        DraftoConfirmationDialog(
            title = stringResource(R.string.dialog_delete_collection_title),
            message = stringResource(R.string.dialog_delete_collection_message),
            confirmText = stringResource(R.string.action_delete),
            cancelText = stringResource(R.string.action_cancel),
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmDialog = false
                onDeleteCollection(initialCollection.id)
                onDismiss()
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

/**
 * Adaptador de compatibilidad para creación de colecciones.
 *
 * Mantiene compatibilidad directa con llamadas existentes a [DraftoCreateCollectionBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoCreateCollectionBottomSheet(
    onDismiss: () -> Unit,
    onCreateCollection: (name: String, colorHex: String, iconName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    DraftoCollectionFormBottomSheet(
        onDismiss = onDismiss,
        onSaveCollection = onCreateCollection,
        initialCollection = null,
        onDeleteCollection = null,
        modifier = modifier
    )
}

/**
 * Botón circular atómico para cada icono del selector de colecciones.
 *
 * Se extrae como función componible para reutilizar la misma presentación e interacción
 * tanto en la cuadrícula horizontal compacta para teléfonos como en la vista de escritorio o tableta.
 */
@Composable
private fun CollectionIconPickerItem(
    iconKey: String,
    isSelected: Boolean,
    activeColor: Color,
    iconTintActive: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconVector = getCollectionIcon(iconKey)
    Box(
        modifier = modifier
            .size(BookmarkIconPickerSize)
            .clip(CircleShape)
            .background(
                if (isSelected) activeColor
                else DraftoTheme.colors.primaryContainer
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = iconKey,
            tint = if (isSelected) iconTintActive else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(BookmarkIconPickerIconSize)
        )
    }
}
