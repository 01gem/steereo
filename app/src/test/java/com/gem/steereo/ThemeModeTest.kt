package com.gem.steereo

import com.gem.steereo.data.prefs.ThemeMode
import com.gem.steereo.util.PureRules.resolveDarkTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun system_systemDark_isDark() {
        assertTrue(resolveDarkTheme(ThemeMode.SYSTEM, systemDark = true))
    }

    @Test
    fun system_systemLight_isLight() {
        assertFalse(resolveDarkTheme(ThemeMode.SYSTEM, systemDark = false))
    }

    @Test
    fun light_systemDark_isLight() {
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemDark = true))
    }

    @Test
    fun light_systemLight_isLight() {
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemDark = false))
    }

    @Test
    fun dark_systemDark_isDark() {
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemDark = true))
    }

    @Test
    fun dark_systemLight_isDark() {
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemDark = false))
    }
}
