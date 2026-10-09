package com.ixeken.drafto.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * Receptor de eventos para procesar clics directos sobre las acciones "Completar" y "+3 h" de la notificación.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun manageLiveNoteUseCase(): ManageLiveNoteUseCase
        fun draftoNotificationManager(): DraftoNotificationManager
        fun scheduleNotificationUseCase(): ScheduleNotificationUseCase
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReceiverEntryPoint::class.java
                )
                val manageUseCase = entryPoint.manageLiveNoteUseCase()
                val notificationManager = entryPoint.draftoNotificationManager()
                val scheduleUseCase = entryPoint.scheduleNotificationUseCase()

                when (action) {
                    DraftoNotificationManager.ACTION_COMPLETE -> {
                        manageUseCase.completeLiveNote()
                        notificationManager.cancelNotification()
                        scheduleUseCase.cancelScheduledUpdate()
                    }
                    DraftoNotificationManager.ACTION_EXTEND -> {
                        manageUseCase.extendLiveNoteTime()
                        val updatedNote = manageUseCase.getActiveLiveNote().firstOrNull()
                        if (updatedNote != null) {
                            notificationManager.showOrUpdateNotification(updatedNote)
                            scheduleUseCase.scheduleHourlyUpdate()
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

