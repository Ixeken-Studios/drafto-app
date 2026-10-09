package com.ixeken.drafto.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Pruebas unitarias para la resolución de [DraftoThemeMode] y las propiedades de [DraftoKraftColors].
 */
class ThemeTest {

    @Test
    fun `DraftoThemeMode fromString resolves all modes accurately`() {
        assertEquals(DraftoThemeMode.DARK, DraftoThemeMode.fromString("Dark"))
        assertEquals(DraftoThemeMode.DARK, DraftoThemeMode.fromString("dark"))
        assertEquals(DraftoThemeMode.LIGHT, DraftoThemeMode.fromString("Light"))
        assertEquals(DraftoThemeMode.LIGHT, DraftoThemeMode.fromString("light"))
        assertEquals(DraftoThemeMode.AMOLED, DraftoThemeMode.fromString("Amoled"))
        assertEquals(DraftoThemeMode.AMOLED, DraftoThemeMode.fromString("AMOLED"))
        assertEquals(DraftoThemeMode.KRAFT, DraftoThemeMode.fromString("Kraft"))
        assertEquals(DraftoThemeMode.KRAFT, DraftoThemeMode.fromString("kraft"))
        assertEquals(DraftoThemeMode.DARK, DraftoThemeMode.fromString("unknown_value"))
    }

    @Test
    fun `DraftoKraftColors has correct light theme attributes and paper palette`() {
        assertFalse(DraftoKraftColors.isDark)
        assertEquals(BackgroundKraft, DraftoKraftColors.background)
        assertEquals(CardSurfaceKraft, DraftoKraftColors.cardSurface)
        assertEquals(PrimaryContainerKraft, DraftoKraftColors.primaryContainer)
        assertEquals(TextPrimaryKraft, DraftoKraftColors.textPrimary)
        assertNotNull(DraftoKraftColors.accent)
    }

    @Test
    fun `DraftoColors has correct themeMode for each palette`() {
        assertEquals(DraftoThemeMode.DARK, DraftoDarkColors.themeMode)
        assertEquals(DraftoThemeMode.AMOLED, DraftoAmoledColors.themeMode)
        assertEquals(DraftoThemeMode.LIGHT, DraftoLightColors.themeMode)
        assertEquals(DraftoThemeMode.KRAFT, DraftoKraftColors.themeMode)
    }

    @Test
    fun `DraftoLightColors and DraftoKraftColors have dedicated switch tokens`() {
        // Light mode switch uses high contrast PureWhite thumb and Dust track
        assertEquals(PureWhite, DraftoLightColors.switchUncheckedThumb)
        assertEquals(Dust, DraftoLightColors.switchUncheckedTrack)
        assertEquals(Gunmetal, DraftoLightColors.switchUncheckedIcon)

        // Kraft mode switch uses warm paper WarmCanvas thumb and PaleStone track
        assertEquals(WarmCanvas, DraftoKraftColors.switchUncheckedThumb)
        assertEquals(PaleStone, DraftoKraftColors.switchUncheckedTrack)
        assertEquals(AshStone, DraftoKraftColors.switchUncheckedIcon)
        assertEquals(LinenBorder, DraftoKraftColors.switchUncheckedBorder)
    }
}
