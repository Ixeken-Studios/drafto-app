package com.ixeken.drafto

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeFeatureFlags

@HiltAndroidApp
class DraftoApplication : Application(), ImageLoaderFactory {

    @OptIn(ExperimentalHazeApi::class)
    override fun onCreate() {
        super.onCreate()
        // Habilita el renderizado nativo experimental de backdrop en Android 17+ (con fallback automático a sources)
        HazeFeatureFlags.isPlatformBackdropEnabled = true

        // Sincroniza el modo nocturno del sistema con el tema guardado para que el Splash Screen use el tema correcto
        val prefs = getSharedPreferences("drafto_sync_theme", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("theme", "Dark") ?: "Dark"
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        uiModeManager?.let {
            val mode = when (savedTheme.lowercase()) {
                "dark", "amoled" -> UiModeManager.MODE_NIGHT_YES
                "light", "kraft" -> UiModeManager.MODE_NIGHT_NO
                else -> UiModeManager.MODE_NIGHT_YES
            }
            if (it.nightMode != mode) {
                it.setApplicationNightMode(mode)
            }
        }
    }
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
