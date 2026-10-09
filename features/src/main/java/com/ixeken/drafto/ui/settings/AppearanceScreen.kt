package com.ixeken.drafto.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import com.ixeken.drafto.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R

import com.ixeken.drafto.ui.components.DraftoSwitch
import com.ixeken.drafto.ui.components.DraftoStickyHeader
import com.ixeken.drafto.ui.components.DraftoTabTopBar
import com.ixeken.drafto.ui.theme.DraftoTheme
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ixeken.drafto.ui.theme.LocalHazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Pantalla de configuración visual y tipográfica (Appearance).
 */
@Composable
fun AppearanceScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onOpenThemeSelection: () -> Unit,
    onToggleNavbarBlur: (Boolean) -> Unit = {},
    onToggleGlobalBlur: (Boolean) -> Unit = onToggleNavbarBlur,
    onToggleReduceMotion: (Boolean) -> Unit = {},
    onSelectAccentColor: (String) -> Unit = {},
    onChangeFontSizeStep: (Int) -> Unit
) {
    BackHandler(onBack = onBack)

    var headerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    val safeTopPadding = if (headerHeightDp > 0.dp) headerHeightDp + PaddingSmall else 90.dp

    val hazeState = LocalHazeState.current
    val scrollState = rememberScrollState()
    val isScrolled = scrollState.value > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(safeTopPadding))

            // Tarjeta 1: Opciones de Tema y Efectos Visuales
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .clip(DraftoShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Selección de tema
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenThemeSelection() }
                        .padding(SettingsItemPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ColorLens,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(SettingsItemIconSize)
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Column {
                            Text(
                                text = stringResource(R.string.appearance_item_theme_selection_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(PaddingMicro))
                            Text(
                                text = stringResource(R.string.appearance_item_theme_selection_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BorderWidthThin)
                        .background(MaterialTheme.colorScheme.outline)
                )

                // Desenfoque global / navbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleGlobalBlur(!uiState.isGlobalBlurEnabled) }
                        .padding(SettingsItemPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BlurOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(SettingsItemIconSize)
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Column {
                            Text(
                                text = stringResource(R.string.appearance_item_navbar_blur_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(PaddingMicro))
                            Text(
                                text = stringResource(R.string.appearance_item_navbar_blur_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DraftoSwitch(
                        checked = uiState.isGlobalBlurEnabled,
                        onCheckedChange = onToggleGlobalBlur
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BorderWidthThin)
                        .background(MaterialTheme.colorScheme.outline)
                )

                // Reducir animaciones / Modo rendimiento
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleReduceMotion(!uiState.isReduceMotionEnabled) }
                        .padding(SettingsItemPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MotionPhotosOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(SettingsItemIconSize)
                        )
                        Spacer(modifier = Modifier.width(PaddingLarge))
                        Column {
                            Text(
                                text = stringResource(R.string.appearance_item_reduce_motion_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(PaddingMicro))
                            Text(
                                text = stringResource(R.string.appearance_item_reduce_motion_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DraftoSwitch(
                        checked = uiState.isReduceMotionEnabled,
                        onCheckedChange = onToggleReduceMotion
                    )
                }
            }

            Spacer(modifier = Modifier.height(SettingsGroupSpacing))

            // Tarjeta 2: Selector de Color de Acento (Accent Color)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .clip(DraftoShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(SettingsItemPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(SettingsItemIconSize)
                    )
                    Spacer(modifier = Modifier.width(PaddingLarge))
                    Column {
                        Text(
                            text = stringResource(R.string.appearance_item_accent_color_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(PaddingMicro))
                        Text(
                            text = stringResource(R.string.appearance_item_accent_color_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PaddingLarge))

                val isDarkTheme = DraftoTheme.colors.isDark
                val dividerColor = DraftoTheme.colors.divider

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DraftoAccentColor.entries.forEach { option ->
                        val isSelected = option.storageKey.equals(uiState.accentColor, ignoreCase = true) ||
                            (option == DraftoAccentColor.MONOCHROME && (uiState.accentColor.equals("pure_white", true) || uiState.accentColor.equals("pure_black", true)))
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                            label = "accent_scale_${option.storageKey}"
                        )
                        val optionColor = option.resolveColor(isDarkTheme)
                        val optionOnColor = option.resolveOnColor(isDarkTheme)
                        val dotBorderColor = when {
                            isSelected -> optionColor
                            option == DraftoAccentColor.MONOCHROME -> dividerColor
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .size(AccentColorDotSize)
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                                .clip(CircleShape)
                                .background(optionColor)
                                .then(
                                    if (dotBorderColor != Color.Transparent) {
                                        Modifier.border(
                                            width = BorderWidthThin,
                                            color = dotBorderColor,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelectAccentColor(option.storageKey) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = stringResource(option.titleRes),
                                    tint = optionOnColor,
                                    modifier = Modifier.size(AccentColorIconCheckSize)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(PaddingMedium))

                val currentAccent = remember(uiState.accentColor) {
                    DraftoAccentColor.fromString(uiState.accentColor)
                }
                val accentTitleColor = if (currentAccent == DraftoAccentColor.MONOCHROME) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    currentAccent.resolveColor(isDarkTheme)
                }

                Text(
                    text = stringResource(currentAccent.titleRes),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = accentTitleColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(SettingsGroupSpacing))

            // Tarjeta 3: Escala Tipográfica (Font Size)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingScreenHorizontal)
                    .clip(DraftoShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(SettingsItemPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(SettingsItemIconSize)
                    )
                    Spacer(modifier = Modifier.width(PaddingLarge))
                    Column {
                        Text(
                            text = stringResource(R.string.appearance_item_font_size_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(PaddingMicro))
                        Text(
                            text = stringResource(R.string.appearance_item_font_size_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PaddingLarge))

                Slider(
                    value = uiState.fontSizeStep.toFloat(),
                    onValueChange = { onChangeFontSizeStep(it.toInt()) },
                    valueRange = 0f..4f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = DraftoTheme.colors.accent,
                        activeTrackColor = DraftoTheme.colors.accent,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                val fontSizeLabel = when (uiState.fontSizeStep) {
                    0 -> stringResource(R.string.appearance_font_size_very_small)
                    1 -> stringResource(R.string.appearance_font_size_small)
                    2 -> stringResource(R.string.appearance_font_size_default)
                    3 -> stringResource(R.string.appearance_font_size_big)
                    4 -> stringResource(R.string.appearance_font_size_very_big)
                    else -> stringResource(R.string.appearance_font_size_default)
                }

                Text(
                    text = fontSizeLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DraftoTheme.colors.accent,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(PaddingListBottomContent))
        }

        // Header adhesivo superior con efecto Spatial Pure Blur Ultra Thin
        DraftoStickyHeader(
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { headerHeightPx = it.size.height }
        ) {
            DraftoTabTopBar(
                title = stringResource(R.string.appearance_title),
                titleTextStyle = SubpageTitleTextStyle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    }
}
