package com.ixeken.drafto.ui.adaptive

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==========================================================
// Sistema Adaptativo de Dispositivos (Phones, Folds, Tablets)
// ==========================================================

/**
 * Clasificación de factor de forma del dispositivo para adaptación de layouts.
 */
enum class DeviceFormFactor {
    /** Teléfono móvil convencional (ancho < 600dp). */
    PHONE,

    /** Dispositivo plegable (Foldable) desplegado o formato casi cuadrado (600dp <= ancho < 840dp). */
    FOLDABLE,

    /** Tablet o pantalla ancha / modo apaisado amplio (ancho >= 840dp). */
    TABLET
}

/**
 * Información de dimensiones y factor de forma de la ventana de la aplicación.
 */
data class WindowSizeInfo(
    val widthDp: Dp,
    val heightDp: Dp,
    val orientation: Int,
    val formFactor: DeviceFormFactor
) {
    val isPhone: Boolean get() = formFactor == DeviceFormFactor.PHONE
    val isFoldable: Boolean get() = formFactor == DeviceFormFactor.FOLDABLE
    val isTablet: Boolean get() = formFactor == DeviceFormFactor.TABLET
    val isLandscape: Boolean get() = orientation == Configuration.ORIENTATION_LANDSCAPE

    /**
     * Determina si la barra de navegación debe mostrar etiquetas de texto debajo/junto a los iconos.
     * - En teléfonos normales (PHONE): false (solo iconos para maximizar espacio horizontal y ergonomía).
     * - En dispositivos plegables (FOLDABLE) y Tablets (TABLET): true (suficiente espacio para texto).
     */
    val shouldShowNavBarLabels: Boolean
        get() = formFactor != DeviceFormFactor.PHONE || widthDp >= 600.dp
}

/**
 * CompositionLocal que provee la información adaptativa del dispositivo a todo el árbol Compose.
 */
val LocalWindowSizeInfo = compositionLocalOf {
    WindowSizeInfo(
        widthDp = 360.dp,
        heightDp = 800.dp,
        orientation = Configuration.ORIENTATION_PORTRAIT,
        formFactor = DeviceFormFactor.PHONE
    )
}

/**
 * Calcula y recuerda la información adaptativa de la pantalla actual ante rotaciones y cambios de tamaño.
 */
@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val orientation = configuration.orientation

    return remember(widthDp, heightDp, orientation) {
        val formFactor = when {
            widthDp >= 840.dp -> DeviceFormFactor.TABLET
            widthDp >= 600.dp -> {
                // Rango 600-839dp: Folds desplegados típicamente tienen un aspect ratio cercano a 1 (cuadrado)
                val aspectRatio = if (heightDp.value > 0f) (widthDp.value / heightDp.value) else 1f
                if (aspectRatio in 0.7f..1.45f) {
                    DeviceFormFactor.FOLDABLE
                } else {
                    DeviceFormFactor.TABLET
                }
            }
            else -> DeviceFormFactor.PHONE
        }

        WindowSizeInfo(
            widthDp = widthDp,
            heightDp = heightDp,
            orientation = orientation,
            formFactor = formFactor
        )
    }
}
