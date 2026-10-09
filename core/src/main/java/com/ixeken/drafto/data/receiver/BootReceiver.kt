package com.ixeken.drafto.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ixeken.drafto.data.notification.DraftoNotificationManager
import com.ixeken.drafto.domain.usecase.ManageLiveNoteUseCase
import com.ixeken.drafto.domain.usecase.ScheduleNotificationUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Receptor del sistema que escucha el arranque del dispositivo (`ACTION_BOOT_COMPLETED`)
 * para restaurar la notificación activa y reprogramar las tareas periódicas de WorkManager.
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun manageLiveNoteUseCase(): ManageLiveNoteUseCase
        fun draftoNotificationManager(): DraftoNotificationManager
        fun scheduleNotificationUseCase(): ScheduleNotificationUseCase
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BootReceiverEntryPoint::class.java
                )
                val manageUseCase = entryPoint.manageLiveNoteUseCase()
                val notificationManager = entryPoint.draftoNotificationManager()
                val scheduleUseCase = entryPoint.scheduleNotificationUseCase()

                val activeNote = manageUseCase.getActiveLiveNote().firstOrNull()
                if (activeNote != null) {
                    notificationManager.showOrUpdateNotification(activeNote)
                    scheduleUseCase.scheduleHourlyUpdate()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

