package com.ixeken.drafto

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ixeken.drafto.data.notification.DraftoNotificationManager
import com.ixeken.drafto.data.share.SharedIntentProcessor
import com.ixeken.drafto.domain.model.SharedPayload
import com.ixeken.drafto.ui.components.DraftoConfirmationDialog
import com.ixeken.drafto.ui.components.DraftoLockScreen
import com.ixeken.drafto.ui.components.DraftoWalkthroughBottomSheet
import com.ixeken.drafto.ui.navigation.NavGraph
import com.ixeken.drafto.ui.security.BiometricAuthenticator
import com.ixeken.drafto.ui.settings.SettingsViewModel
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.utils.DraftoSystemInteractions
import com.ixeken.drafto.util.GitHubUpdateChecker
import com.ixeken.drafto.util.UpdateCheckResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val _editLiveNoteRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emite un evento cada vez que la app recibe ACTION_EDIT desde la notificación. */
    val editLiveNoteRequest: SharedFlow<Unit> = _editLiveNoteRequest.asSharedFlow()

    private val _sharedPayload = MutableStateFlow<SharedPayload?>(null)
    /** Emite el contenido recibido al compartir desde otras apps de Android. */
    val sharedPayload: StateFlow<SharedPayload?> = _sharedPayload.asStateFlow()

    fun clearSharedPayload() {
        _sharedPayload.value = null
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        val syncTheme = getSharedPreferences("drafto_sync_theme", Context.MODE_PRIVATE)
            .getString("theme", "Dark") ?: "Dark"
        val splashThemeId = when (syncTheme.lowercase()) {
            "amoled" -> R.style.Theme_Drafto_Amoled
            "light" -> R.style.Theme_Drafto_Light
            "kraft" -> R.style.Theme_Drafto_Kraft
            else -> R.style.Theme_Drafto_Dark
        }
        setTheme(splashThemeId)
        splashScreen.setSplashScreenTheme(splashThemeId)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        handleIncomingIntent(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            val isDeviceSecure = remember(this@MainActivity) { BiometricAuthenticator.isDeviceSecure(this@MainActivity) }
            val isLockAppEnabled = settingsState.isLockAppEnabled && isDeviceSecure
            var isAppUnlocked by rememberSaveable { mutableStateOf(!isLockAppEnabled) }
            var lockErrorMessage by remember { mutableStateOf<String?>(null) }
            var isAuthenticating by remember { mutableStateOf(false) }
            var isInitialCheckDone by rememberSaveable { mutableStateOf(false) }

            // Sincronizar estado inicial de bloqueo tras cargar las preferencias persistentes
            LaunchedEffect(settingsState.isPreferencesLoaded, isDeviceSecure) {
                if (settingsState.isPreferencesLoaded) {
                    if (!isDeviceSecure && settingsState.isLockAppEnabled) {
                        // Si el dispositivo no tiene PIN, patrón o biometría configurada, se desactiva
                        // de inmediato la protección para evitar que el usuario quede atrapado
                        settingsViewModel.setLockAppEnabled(false)
                        isAppUnlocked = true
                    } else if (!isInitialCheckDone) {
                        isInitialCheckDone = true
                        if (isLockAppEnabled) {
                            isAppUnlocked = false
                        }
                    }
                }
            }

            var isStartupUpdateChecked by rememberSaveable { mutableStateOf(false) }
            var startupUpdateAvailable by remember { mutableStateOf<UpdateCheckResult.UpdateAvailable?>(null) }
            val currentVersion = stringResource(R.string.about_version)

            // Comprobación silenciosa de actualizaciones al inicio respetando la privacidad del usuario
            LaunchedEffect(settingsState.isPreferencesLoaded) {
                if (settingsState.isPreferencesLoaded && !isStartupUpdateChecked) {
                    isStartupUpdateChecked = true
                    if (settingsState.isCheckUpdateOnStartEnabled) {
                        val result = GitHubUpdateChecker.checkUpdate(currentVersion)
                        if (result is UpdateCheckResult.UpdateAvailable) {
                            startupUpdateAvailable = result
                        }
                    }
                }
            }

            // Protección de ventana contra vista previa en selector de aplicaciones (Recent Apps)
            DisposableEffect(isLockAppEnabled) {
                if (isLockAppEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    isAppUnlocked = true
                }
                onDispose { }
            }

            // Restablecer bloqueo cuando el usuario abandona la aplicación (ON_STOP)
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, isLockAppEnabled) {
                if (!isLockAppEnabled) {
                    return@DisposableEffect onDispose { }
                }
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        isAppUnlocked = false
                        lockErrorMessage = null
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val triggerAuthentication: () -> Unit = {
                if (!isAuthenticating && isLockAppEnabled) {
                    isAuthenticating = true
                    lockErrorMessage = null
                    BiometricAuthenticator.authenticate(
                        activity = this@MainActivity,
                        onSuccess = {
                            isAppUnlocked = true
                            lockErrorMessage = null
                            isAuthenticating = false
                        },
                        onError = { errorMsg ->
                            lockErrorMessage = errorMsg
                            isAuthenticating = false
                        }
                    )
                }
            }

            // Disparar autenticación al iniciar o tras desbloqueo de pantalla si la app requiere bloqueo
            LaunchedEffect(settingsState.isPreferencesLoaded, isLockAppEnabled, isAppUnlocked) {
                if (settingsState.isPreferencesLoaded && isLockAppEnabled && !isAppUnlocked) {
                    triggerAuthentication()
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ ->
                // Permiso otorgado o denegado
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val isSystemReducedMotion = remember(this@MainActivity) {
                try {
                    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
                } catch (e: Exception) {
                    false
                }
            }
            val isReducedMotion = settingsState.isReduceMotionEnabled || isSystemReducedMotion

            DraftoTheme(
                theme = settingsState.theme,
                fontSizeStep = settingsState.fontSizeStep,
                accentColor = settingsState.accentColor,
                isReducedMotion = isReducedMotion
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NavGraph(settingsViewModel = settingsViewModel)

                    if (settingsState.isPreferencesLoaded && isLockAppEnabled && !isAppUnlocked) {
                        DraftoLockScreen(
                            onUnlockClick = triggerAuthentication,
                            errorMessage = lockErrorMessage
                        )
                    }

                    if (isAppUnlocked) {
                        startupUpdateAvailable?.let { update ->
                            DraftoConfirmationDialog(
                                title = stringResource(R.string.update_available_title),
                                message = stringResource(R.string.update_available_message, update.version),
                                icon = Icons.Rounded.SystemUpdate,
                                confirmText = stringResource(R.string.update_available_view),
                                cancelText = stringResource(R.string.update_available_later),
                                isDestructive = false,
                                onConfirm = {
                                    DraftoSystemInteractions.openUrl(this@MainActivity, update.releaseUrl)
                                    startupUpdateAvailable = null
                                },
                                onDismiss = {
                                    startupUpdateAvailable = null
                                }
                            )
                        }

                        if (settingsState.isPreferencesLoaded && !settingsState.hasSeenWalkthrough && startupUpdateAvailable == null) {
                            DraftoWalkthroughBottomSheet(
                                onDismiss = {
                                    settingsViewModel.setHasSeenWalkthrough(true)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == DraftoNotificationManager.ACTION_EDIT) {
            _editLiveNoteRequest.tryEmit(Unit)
        } else {
            val payload = SharedIntentProcessor.processIntent(intent)
            if (payload != null) {
                _sharedPayload.value = payload
            }
        }
    }
}