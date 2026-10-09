package com.ixeken.drafto.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials

/**
 * CompositionLocal que provee el [HazeState] global para compartir el efecto blur
 * en BottomSheets, Diálogos, TopBars y NavBar.
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * CompositionLocal que provee si el efecto de desenfoque esmerilado está habilitado globalmente.
 */
val LocalDraftoBlurEnabled = compositionLocalOf { true }

/**
 * CompositionLocal que provee si la descarga de previsualizaciones y favicons de internet está habilitada.
 * Se desacopla en un proveedor local para que las tarjetas de marcadores y componentes de visualización
 * supriman de inmediato las peticiones de red hacia imágenes y favicons externos sin requerir
 * recomposiciones pesadas ni consultar DataStore en cada elemento de lista.
 */
val LocalFetchWebMetadataEnabled = compositionLocalOf { true }

/**
 * CompositionLocal que provee el preset de material de vidrio activo para componentes esmerilados.
 */
val LocalDraftoGlassMaterial = compositionLocalOf { DraftoGlassMaterial.Default }

/**
 * Catálogo de materiales de vidrio esmerilado (Glassmorphism) para Drafto con Haze 2.0.
 * Permite alternar de manera inmediata entre diferentes niveles de transparencia y densidad.
 */
enum class DraftoGlassMaterial {
    UltraThin,   // Máxima transparencia y desenfoque sutil
    Thin,        // Equilibrio entre legibilidad, contraste y estética de vidrio
    Regular,     // Vidrio estándar con mayor densidad
    Thick,       // Vidrio grueso, ideal para paneles modales o fondos con mucho ruido visual
    UltraThick;  // Máxima opacidad con difuminado pronunciado

    companion object {
        /** Material predeterminado en todo el ecosistema Drafto */
        val Default: DraftoGlassMaterial = UltraThin
    }
}

/**
 * Convierte un [DraftoGlassMaterial] en su correspondiente [HazeBlurStyle] de Haze 2.0.
 */
@Composable
fun DraftoGlassMaterial.toHazeStyle(): HazeBlurStyle = when (this) {
    DraftoGlassMaterial.UltraThin -> HazeMaterials.ultraThin()
    DraftoGlassMaterial.Thin -> HazeMaterials.thin()
    DraftoGlassMaterial.Regular -> HazeMaterials.regular()
    DraftoGlassMaterial.Thick -> HazeMaterials.thick()
    DraftoGlassMaterial.UltraThick -> HazeMaterials.ultraThick()
}

/**
 * Modificador composable que aplica el efecto de superficie de cristal esmerilado Drafto
 * sobre cualquier elemento gráfico (botones, píldoras, tarjetas, barras de acción).
 *
 * Encapsula el recorte de forma, la evaluación condicional de blur mediante [LocalDraftoBlurEnabled]
 * y la degradación elegante a color de fondo plano ([fallbackColor]) cuando el blur está deshabilitado.
 */
@Composable
fun Modifier.draftoGlass(
    shape: Shape = CircleShape,
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    hazeState: HazeState? = LocalHazeState.current,
    material: DraftoGlassMaterial = LocalDraftoGlassMaterial.current,
    surfaceColor: Color = DraftoTheme.colors.navBarSurfaceTranslucent,
    fallbackColor: Color = DraftoTheme.colors.cardSurface
): Modifier {
    val shouldBlur = isBlurEnabled && hazeState != null
    val effectiveColor = if (shouldBlur) surfaceColor else fallbackColor
    return this
        .clip(shape)
        .then(
            if (shouldBlur) {
                Modifier.hazeBlur(
                    input = HazeInput.Sources(hazeState!!),
                    style = material.toHazeStyle()
                )
            } else Modifier
        )
        .background(effectiveColor)
}

/**
 * Crea un [HazeBlurStyle] ultra transparente y cristalino con máscara de gradiente vertical para TopBars (Scaffold Masked),
 * aplicando desenfoque óptico puro sin tintes oscuros ni efecto de viñeta sobre el fondo/wallpaper.
 */
@Composable
fun DraftoGlassMaterial.toMaskedTopHazeStyle(
    blurRadius: Dp = 20.dp
): HazeBlurStyle = remember(this, blurRadius) {
    HazeBlurStyle {
        blurRadius(blurRadius)
        noiseFactor(0f)
        backgroundColor(Color.Transparent)
        colorEffects(emptyList())
        mask(
            Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent)
            )
        )
    }
}

/**
 * Crea un [HazeBlurStyle] ultra transparente y cristalino con máscara de gradiente vertical para barras inferiores (Scaffold Masked),
 * aplicando desenfoque óptico puro sin tintes oscuros ni efecto de viñeta sobre el fondo/wallpaper.
 */
@Composable
fun DraftoGlassMaterial.toMaskedBottomHazeStyle(
    blurRadius: Dp = 20.dp
): HazeBlurStyle = remember(this, blurRadius) {
    HazeBlurStyle {
        blurRadius(blurRadius)
        noiseFactor(0f)
        backgroundColor(Color.Transparent)
        colorEffects(emptyList())
        mask(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black)
            )
        )
    }
}

/**
 * Crea un [HazeBlurStyle] con máscara progresiva vertical descendente (de 1f arriba a 0f abajo)
 * y máscara de gradiente de paradas para difuminar suavemente el borde inferior de las TopBars.
 */
@Composable
fun DraftoGlassMaterial.toProgressiveTopHazeStyle(
    startIntensity: Float = 1f,
    endIntensity: Float = 0f,
    fadeStartFraction: Float = 0.78f
): HazeBlurStyle {
    val baseStyle = toHazeStyle()
    return remember(this, baseStyle, startIntensity, endIntensity, fadeStartFraction) {
        baseStyle.then {
            progressive(
                HazeProgressive.verticalGradient(
                    startIntensity = startIntensity,
                    endIntensity = endIntensity
                )
            )
            mask(
                Brush.verticalGradient(
                    0.0f to Color.Black,
                    fadeStartFraction to Color.Black,
                    1.0f to Color.Transparent
                )
            )
        }
    }
}

/**
 * Crea un [HazeBlurStyle] con máscara progresiva vertical ascendente (de 0f arriba a 1f abajo)
 * y máscara de gradiente de paradas para barras inferiores o paneles de control.
 */
@Composable
fun DraftoGlassMaterial.toProgressiveBottomHazeStyle(
    startIntensity: Float = 0f,
    endIntensity: Float = 1f,
    fadeEndFraction: Float = 0.22f
): HazeBlurStyle {
    val baseStyle = toHazeStyle()
    return remember(this, baseStyle, startIntensity, endIntensity, fadeEndFraction) {
        baseStyle.then {
            progressive(
                HazeProgressive.verticalGradient(
                    startIntensity = startIntensity,
                    endIntensity = endIntensity
                )
            )
            mask(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    fadeEndFraction to Color.Black,
                    1.0f to Color.Black
                )
            )
        }
    }
}
