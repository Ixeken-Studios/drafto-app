package com.ixeken.drafto.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixeken.drafto.ui.theme.BookmarkCapsuleHeight
import com.ixeken.drafto.ui.theme.BookmarkCapsuleIconSizeDefault
import com.ixeken.drafto.ui.theme.BookmarkCapsuleIconSizeSmall
import com.ixeken.drafto.ui.theme.BookmarkCapsuleSpacing
import com.ixeken.drafto.ui.theme.BookmarkCapsuleTextPaddingEndDefault
import com.ixeken.drafto.ui.theme.BookmarkCapsuleTextPaddingEndSmall
import com.ixeken.drafto.ui.theme.BookmarkCapsuleThresholdCompactHeight
import com.ixeken.drafto.ui.theme.BookmarkDividerHeight
import com.ixeken.drafto.ui.theme.BookmarkFormCapsuleHeight
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoSprings
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.contrastContentColor

/**
 * Modelo de datos genérico y reutilizable para cualquier opción del selector de cápsula morfológica.
 *
 * @param T Tipo de dato o enum identificador de la opción.
 * @param key Llave única del elemento.
 * @param label Texto visible cuando la opción está activa (cápsula expandida).
 * @param icon Ícono vectorial opcional.
 * @param iconPainterRes Recurso drawable opcional para el ícono.
 * @param badgeColor Color del ícono y acento temático.
 * @param badgeContainerColor Color de fondo de la cápsula activa.
 * @param contentDescription Descripción de accesibilidad.
 */
data class DraftoCapsuleOption<T>(
    val key: T,
    val label: String,
    val icon: ImageVector? = null,
    val iconPainterRes: Int? = null,
    val badgeColor: Color = Color.Unspecified,
    val badgeContainerColor: Color = Color.Unspecified,
    val contentDescription: String? = label
)

/**
 * Componente modular genérico de selector segmentado con morfología elástica estilo cápsula activa
 * y botones circulares inactivos con físicas Snappy de respuesta inmediata y segura.
 *
 * Reutilizable en cualquier pantalla o flujo de la aplicación.
 *
 * Características:
 * - Físicas Snappy de Material 3 Expressive para expansión, contracción y color sin rebote residual ni desbordamiento de dimensiones.
 * - Soporta 2 o más opciones tipadas genéricamente (Enums, Strings, IDs numéricos, etc.).
 * - Sin colisiones de sub-layout ni dependencias de blur circulares.
 */
@Composable
fun <T> DraftoCapsuleSelector(
    options: List<DraftoCapsuleOption<T>>,
    selectedKey: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    spacing: Dp = 10.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            DraftoCapsuleItem(
                option = option,
                isActive = option.key == selectedKey,
                height = height,
                onClick = { onOptionSelected(option.key) }
            )
        }
    }
}

/**
 * Selector de cápsulas con distribución adaptativa multi-fila basado en [FlowRow].
 *
 * Se diseñó para que las opciones de colecciones o categorías puedan distribuirse
 * automáticamente en dos o más filas cuando el ancho de pantalla lo permita,
 * evitando obligar al usuario a desplazarse horizontalmente si hay espacio suficiente.
 *
 * @param options Lista de opciones disponibles.
 * @param selectedKey Llave actualmente seleccionada.
 * @param onOptionSelected Callback al seleccionar una opción.
 * @param modifier Modificador Compose opcional.
 * @param onOptionLongClick Callback opcional para pulsación prolongada.
 * @param dividersAfterKeys Conjunto de llaves tras las cuales se dibuja un divisor vertical.
 * @param height Altura de cada cápsula.
 * @param spacing Espaciado horizontal y vertical entre elementos.
 */
@Composable
fun <T> DraftoCapsuleFlowSelector(
    options: List<DraftoCapsuleOption<T>>,
    selectedKey: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    onOptionLongClick: ((T) -> Unit)? = null,
    dividersAfterKeys: Set<T> = emptySet(),
    height: Dp = BookmarkCapsuleHeight,
    spacing: Dp = BookmarkCapsuleSpacing
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        options.forEach { option ->
            DraftoCapsuleItem(
                option = option,
                isActive = option.key == selectedKey,
                height = height,
                onClick = { onOptionSelected(option.key) },
                onLongClick = onOptionLongClick?.let { callback -> { callback(option.key) } }
            )
            if (option.key in dividersAfterKeys) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(height)
                        .background(DraftoTheme.colors.divider)
                )
            }
        }
    }
}

/**
 * Selector de cápsulas con desplazamiento horizontal fluido para categorías, plataformas
 * y colecciones personalizadas de marcadores u otros módulos.
 *
 * Características arquitectónicas:
 * - Permite recorrer horizontalmente un conjunto arbitrario o dinámico de opciones sin recortes de layout.
 * - Soporta la inserción de un divisor visual vertical tenue tras una llave específica ([dividerAfterKey])
 *   para separar secciones lógicas (ej. filtros de plataforma vs colecciones de usuario).
 * - Admite una acción interactiva final ([trailingAction]), como el botón para añadir nuevas colecciones (+).
 * - Mantiene físicas Snappy de Material 3 Expressive en cada cápsula sin generar recomposiciones espurias a 120 FPS.
 *
 * @param options Lista completa de opciones tipadas genéricamente.
 * @param selectedKey Llave activa actual.
 * @param onOptionSelected Callback al pulsar una opción.
 * @param modifier Modificador Compose opcional.
 * @param dividerAfterKey Llave opcional tras la cual se dibuja un divisor vertical.
 * @param trailingAction Slot composable opcional renderizado al final del feed deslizable.
 * @param height Altura de cada cápsula (por defecto [BookmarkCapsuleHeight]).
 * @param spacing Espaciado horizontal entre elementos (por defecto [BookmarkCapsuleSpacing]).
 */
@Composable
fun <T> DraftoScrollableCapsuleSelector(
    options: List<DraftoCapsuleOption<T>>,
    selectedKey: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    onOptionLongClick: ((T) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    dividerAfterKey: T? = null,
    dividersAfterKeys: Set<T> = emptySet(),
    trailingAction: (@Composable () -> Unit)? = null,
    height: Dp = BookmarkCapsuleHeight,
    spacing: Dp = BookmarkCapsuleSpacing
) {
    val scrollState = rememberScrollState()
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val itemBounds = remember { mutableStateMapOf<T, Pair<Int, Int>>() }
    val coroutineScope = rememberCoroutineScope()

    val scrollToOption: (T) -> Unit = { key ->
        coroutineScope.launch {
            delay(120)
            itemBounds[key]?.let { (itemLeft, itemWidth) ->
                if (viewportWidthPx > 0) {
                    val itemCenterX = itemLeft + itemWidth / 2
                    val targetScroll = (itemCenterX - viewportWidthPx / 2).coerceIn(0, scrollState.maxValue)
                    scrollState.animateScrollTo(
                        targetScroll,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(selectedKey, viewportWidthPx) {
        if (viewportWidthPx <= 0) return@LaunchedEffect
        delay(120)
        itemBounds[selectedKey]?.let { (itemLeft, itemWidth) ->
            val itemCenterX = itemLeft + itemWidth / 2
            val targetScroll = (itemCenterX - viewportWidthPx / 2).coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(
                targetScroll,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                viewportWidthPx = coordinates.size.width
            }
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option.key == selectedKey
                DraftoCapsuleItem(
                    option = option,
                    isActive = isSelected,
                    height = height,
                    onClick = {
                        onOptionSelected(option.key)
                        scrollToOption(option.key)
                    },
                    onLongClick = onOptionLongClick?.let { callback -> { callback(option.key) } },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        val positionInParent = coordinates.positionInParent()
                        itemBounds[option.key] = Pair(positionInParent.x.roundToInt(), coordinates.size.width)
                    }
                )
                if ((dividerAfterKey != null && option.key == dividerAfterKey) || option.key in dividersAfterKeys) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(BookmarkDividerHeight)
                            .background(DraftoTheme.colors.divider)
                    )
                }
            }
            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}

/**
 * Elemento individual del selector de cápsula que realiza la transición elástica entre
 * botón circular inactivo y cápsula expandida activa, siguiendo Nothing OS 5:
 * - Sin contenedores squircle o badges anidados alrededor del ícono; el glifo descansa limpio sobre la superficie.
 * - En reposo (inactivo) es un círculo geométrico puro ([height] x [height]) en superficie sólida sin transparencias ni glass.
 * - Al activarse, la cápsula se expande orgánicamente hacia la derecha revelando la etiqueta con
 *   físicas Snappy coordinadas sin saltos bruscos ni empujes agresivos entre elementos adyacentes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> DraftoCapsuleItem(
    option: DraftoCapsuleOption<T>,
    isActive: Boolean,
    height: Dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Colores sólidos según Nothing OS 5: superficie sólida para inactivo, acento temático para activo
    val inactiveBg = DraftoTheme.colors.primaryContainer
    val activeBg = when {
        option.badgeContainerColor != Color.Unspecified -> option.badgeContainerColor
        option.badgeColor != Color.Unspecified && option.badgeColor != DraftoTheme.colors.badgeGlyph -> option.badgeColor
        else -> DraftoTheme.colors.accent
    }

    val inactiveContentColor = DraftoTheme.colors.textSecondary
    val activeContentColor = when {
        option.badgeColor != Color.Unspecified && option.badgeColor != DraftoTheme.colors.badgeGlyph -> option.badgeColor
        option.badgeContainerColor != Color.Unspecified -> option.badgeContainerColor.contrastContentColor()
        else -> DraftoTheme.colors.onAccent
    }

    val animatedBgColor by animateColorAsState(
        targetValue = if (isActive) activeBg else inactiveBg,
        animationSpec = DraftoSprings.SnappyColor,
        label = "capsuleBgColor"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (isActive) activeContentColor else inactiveContentColor,
        animationSpec = DraftoSprings.SnappyColor,
        label = "capsuleContentColor"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val rippleColor = if (isActive) DraftoTheme.colors.onAccent else DraftoTheme.colors.textSecondary

    val clickableModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = rippleColor),
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = rippleColor),
            onClick = onClick
        )
    }

    val iconSize = if (height < BookmarkCapsuleThresholdCompactHeight) BookmarkCapsuleIconSizeSmall else BookmarkCapsuleIconSizeDefault
    val textPaddingEnd = if (height < BookmarkCapsuleThresholdCompactHeight) BookmarkCapsuleTextPaddingEndSmall else BookmarkCapsuleTextPaddingEndDefault
    val hasIcon = option.icon != null || option.iconPainterRes != null

    Row(
        modifier = modifier
            .height(height)
            .clip(DraftoShapePill)
            .background(animatedBgColor)
            .then(clickableModifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Círculo base para el ícono con tamaño [height] x [height]
        // Cuando está inactivo, este contenedor define exactamente un círculo 1:1 donde el ícono
        // permanece anclado y perfectamente centrado sin jitter o desplazamientos espurios.
        if (hasIcon) {
            Box(
                modifier = Modifier.size(height),
                contentAlignment = Alignment.Center
            ) {
                if (option.icon != null) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.contentDescription,
                        tint = animatedContentColor,
                        modifier = Modifier.size(iconSize)
                    )
                } else if (option.iconPainterRes != null) {
                    Icon(
                        painter = painterResource(option.iconPainterRes),
                        contentDescription = option.contentDescription,
                        tint = animatedContentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        // Expansión orgánica y fluida hacia la derecha para revelar la etiqueta
        AnimatedVisibility(
            visible = isActive,
            enter = expandHorizontally(
                animationSpec = DraftoSprings.SnappySize,
                expandFrom = Alignment.Start
            ) + fadeIn(animationSpec = DraftoSprings.SnappyFloat),
            exit = shrinkHorizontally(
                animationSpec = DraftoSprings.SnappySize,
                shrinkTowards = Alignment.Start
            ) + fadeOut(animationSpec = DraftoSprings.SnappyFloat)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = if (hasIcon) 0.dp else textPaddingEnd,
                    end = textPaddingEnd
                )
            ) {
                Text(
                    text = option.label,
                    style = if (height < 40.dp) {
                        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    },
                    color = animatedContentColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Cápsula estática informativa (de solo lectura) con morfología unificada Nothing OS 5:
 * - Sin contenedores o badges intermedios; el glifo descansa limpio directamente sobre la superficie continua.
 * - Fondo sólido continuo ([backgroundColor]) con glifo y texto en contraste limpio ([contentColor]).
 * - Altura estandarizada ([height]) con el ícono perfectamente anclado en su cuadrante circular ([height] x [height]).
 *
 * Se utiliza en pantallas y modales de detalle (como el modal de info de marcadores) para representar
 * la cápsula de una colección o categoría con exactamente el mismo estilo visual que una cápsula activa.
 *
 * @param label Texto visible de la cápsula.
 * @param icon Ícono vectorial opcional.
 * @param backgroundColor Color sólido de fondo de la cápsula.
 * @param contentColor Color del texto e ícono (por defecto [DraftoTheme.colors.onAccent]).
 * @param height Altura de la cápsula (por defecto [BookmarkFormCapsuleHeight] = 38.dp).
 * @param modifier Modificador Compose opcional.
 */
@Composable
fun DraftoStaticCapsule(
    label: String,
    icon: ImageVector? = null,
    backgroundColor: Color = DraftoTheme.colors.accent,
    contentColor: Color = DraftoTheme.colors.onAccent,
    height: Dp = BookmarkFormCapsuleHeight,
    modifier: Modifier = Modifier
) {
    val iconSize = if (height < BookmarkCapsuleThresholdCompactHeight) BookmarkCapsuleIconSizeSmall else BookmarkCapsuleIconSizeDefault
    val textPaddingEnd = if (height < BookmarkCapsuleThresholdCompactHeight) BookmarkCapsuleTextPaddingEndSmall else BookmarkCapsuleTextPaddingEndDefault
    val textStyle = if (height < BookmarkCapsuleThresholdCompactHeight) {
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    }

    Row(
        modifier = modifier
            .height(height)
            .clip(DraftoShapePill)
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(height),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        Text(
            text = label,
            style = textStyle,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(
                start = if (icon != null) 0.dp else textPaddingEnd,
                end = textPaddingEnd
            )
        )
    }
}

