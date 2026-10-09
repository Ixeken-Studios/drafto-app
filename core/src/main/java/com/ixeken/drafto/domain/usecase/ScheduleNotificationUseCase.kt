package com.ixeken.drafto.domain.usecase

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ixeken.drafto.data.notification.NotificationUpdateWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caso de uso responsable de programar y cancelar la tarea periódica de WorkManager para notificaciones en vivo.
 * Evita desperdicio de batería utilizando la estrategia de intervalos periódicos de WorkManager.
 */
@Singleton
class ScheduleNotificationUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WORK_NAME = "live_note_update_work"
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * Encola el trabajo periódico de actualización cada hora.
     */
    fun scheduleHourlyUpdate() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationUpdateWorker>(
            1, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Cancela la tarea en cola de WorkManager.
     */
    fun cancelScheduledUpdate() {
        workManager.cancelUniqueWork(WORK_NAME)
    }
}

