package com.ixeken.drafto.data.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ixeken.drafto.domain.usecase.ManageLiveNoteUseCase
import com.ixeken.drafto.domain.usecase.ScheduleNotificationUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull

/**
 * Worker de WorkManager que se ejecuta periódicamente cada hora para actualizar el tiempo impreciso de la notificación
 * o conmutar el título a "Nota vencida" cuando transcurre la duración.
 */
class NotificationUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationWorkerEntryPoint {
        fun manageLiveNoteUseCase(): ManageLiveNoteUseCase
        fun draftoNotificationManager(): DraftoNotificationManager
        fun scheduleNotificationUseCase(): ScheduleNotificationUseCase
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationWorkerEntryPoint::class.java
        )

        val manageUseCase = entryPoint.manageLiveNoteUseCase()
        val notificationManager = entryPoint.draftoNotificationManager()
        val activeNote = manageUseCase.getActiveLiveNote().firstOrNull()

        if (activeNote == null) {
            notificationManager.cancelNotification()
            entryPoint.scheduleNotificationUseCase().cancelScheduledUpdate()
            return Result.success()
        }

        val now = System.currentTimeMillis()
        if (activeNote.expirationTimestampMillis <= now && !activeNote.isExpired) {
            manageUseCase.markExpired(true)
        }

        val updatedNote = manageUseCase.getActiveLiveNote().firstOrNull() ?: activeNote
        notificationManager.showOrUpdateNotification(updatedNote)

        return Result.success()
    }
}

