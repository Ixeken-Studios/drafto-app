package com.ixeken.drafto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ixeken.drafto.core.R

/**
 * Familia completa de fuentes Poppins con todos sus pesos:
 * Thin (100), ExtraLight (200), Light (300), Normal (400), Medium (500),
 * SemiBold (600), Bold (700), ExtraBold (800), Black (900).
 */
val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_thin, FontWeight.Thin),
    Font(R.font.poppins_extralight, FontWeight.ExtraLight),
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * Familia completa de fuentes Fraunces con todos sus pesos:
 * Thin (100), Light (300), Normal (400), SemiBold (600), Bold (700), Black (900).
 */
val FrauncesFontFamily = FontFamily(
    Font(R.font.fraunces_thin, FontWeight.Thin),
    Font(R.font.fraunces_light, FontWeight.Light),
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold),
    Font(R.font.fraunces_black, FontWeight.Black)
)

/**
 * Mapea el nivel de la barra de escala (0 a 4) a un multiplicador de tamaño relativo.
 */
fun resolveFontScale(step: Int): Float = when (step) {
    0 -> 0.80f
    1 -> 0.90f
    2 -> 1.00f
    3 -> 1.10f
    4 -> 1.20f
    else -> 1.00f
}

/**
 * Multiplica un tamaño TextUnit sp manteniendo TextUnit.Unspecified si corresponde.
 */
private fun TextUnit.scale(factor: Float): TextUnit = if (isSp) (value * factor).sp else this

/**
 * Genera el sistema de tipografía Material3 escalado dinámicamente:
 * - Títulos y Encabezados (Display, Headline, Title) usan Fraunces con peso SemiBold (600).
 * - Textos de cuerpo y etiquetas (Body, Label) usan Poppins con peso Medium (500) o SemiBold (600).
 */
fun createScaledTypography(fontSizeStep: Int = 2): Typography {
    val scale = resolveFontScale(fontSizeStep)
    val defaultTypography = Typography()

    fun transform(style: TextStyle, family: FontFamily, weight: FontWeight): TextStyle {
        return style.copy(
            fontFamily = family,
            fontWeight = weight,
            fontSize = style.fontSize.scale(scale),
            lineHeight = style.lineHeight.scale(scale)
        )
    }

    return Typography(
        // Encabezados editoriales / títulos grandes con Fraunces SemiBold
        displayLarge = transform(defaultTypography.displayLarge, FrauncesFontFamily, FontWeight.SemiBold),
        displayMedium = transform(defaultTypography.displayMedium, FrauncesFontFamily, FontWeight.SemiBold),
        displaySmall = transform(defaultTypography.displaySmall, FrauncesFontFamily, FontWeight.SemiBold),
        headlineLarge = transform(defaultTypography.headlineLarge, FrauncesFontFamily, FontWeight.SemiBold),
        headlineMedium = transform(defaultTypography.headlineMedium, FrauncesFontFamily, FontWeight.SemiBold),
        headlineSmall = transform(defaultTypography.headlineSmall, FrauncesFontFamily, FontWeight.SemiBold),
        titleLarge = transform(defaultTypography.titleLarge, FrauncesFontFamily, FontWeight.SemiBold),
        titleMedium = transform(defaultTypography.titleMedium, FrauncesFontFamily, FontWeight.SemiBold),
        titleSmall = transform(defaultTypography.titleSmall, FrauncesFontFamily, FontWeight.SemiBold),

        // Cuerpo y textos de lectura con Poppins Medium
        bodyLarge = transform(defaultTypography.bodyLarge, PoppinsFontFamily, FontWeight.Medium),
        bodyMedium = transform(defaultTypography.bodyMedium, PoppinsFontFamily, FontWeight.Medium),
        bodySmall = transform(defaultTypography.bodySmall, PoppinsFontFamily, FontWeight.Medium),

        // Botones, etiquetas y controles interactivos con Poppins
        labelLarge = transform(defaultTypography.labelLarge, PoppinsFontFamily, FontWeight.SemiBold),
        labelMedium = transform(defaultTypography.labelMedium, PoppinsFontFamily, FontWeight.Medium),
        labelSmall = transform(defaultTypography.labelSmall, PoppinsFontFamily, FontWeight.Medium)
    )
}

val Typography = createScaledTypography()

/**
 * Estilo y tamaño de fuente oficial unificado para los títulos de todas las pestañas principales.
 * Reducido un 50% (~24sp, headlineSmall con Fraunces Bold) con soporte de escalado dinámico.
 */
val TabTitleTextStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)

/**
 * Estilo y tamaño de fuente para los títulos de las subpáginas dentro de Ajustes (Appearance, About, etc.).
 * Utiliza titleLarge con Fraunces Bold y escala dinámicamente con el slider de tipografía de la app.
 */
val SubpageTitleTextStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold
    )

