package com.ixeken.drafto.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.LiveNote
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Administrador responsable de construir y actualizar la notificación persistente de la nota en vivo.
 * Se encarga de formatear el tiempo restante de forma imprecisa (horas) sin despertar ciclos innecesarios de CPU.
 */
@Singleton
class DraftoNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "drafto_live_note_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_EDIT = "com.ixeken.drafto.ACTION_EDIT_LIVE_NOTE"
        const val ACTION_COMPLETE = "com.ixeken.drafto.ACTION_COMPLETE_LIVE_NOTE"
        const val ACTION_EXTEND = "com.ixeken.drafto.ACTION_EXTEND_LIVE_NOTE"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Registra el canal de notificaciones persistentes en Android O+.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra o refresca la notificación persistente calculando el tiempo restante.
     */
    fun showOrUpdateNotification(note: LiveNote) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val now = System.currentTimeMillis()
        val remainingMillis = note.expirationTimestampMillis - now
        val isExpired = remainingMillis <= 0 || note.isExpired

        val titleText = if (isExpired) {
            context.getString(R.string.notification_expired)
        } else {
            val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
            if (remainingHours >= 1) {
                context.getString(R.string.notification_hours_remaining, remainingHours)
            } else {
                context.getString(R.string.notification_less_than_hour)
            }
        }

        // PendingIntent para abrir MainActivity en modo edición de nota
        val editIntent = (context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setClassName(context.packageName, "com.ixeken.drafto.MainActivity")).apply {
            action = ACTION_EDIT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val editPendingIntent = PendingIntent.getActivity(
            context,
            1,
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent para completar (borrar) la nota desde la notificación
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_COMPLETE
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent para extender +3h la nota desde la notificación
        val extendIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_EXTEND
        }
        val extendPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            extendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleText)
            .setContentText(note.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(!isExpired)
            .setAutoCancel(false)
            .setContentIntent(editPendingIntent)
            .addAction(0, context.getString(R.string.notification_action_complete), completePendingIntent)
            .addAction(0, context.getString(R.string.notification_action_extend), extendPendingIntent)
            .addAction(0, context.getString(R.string.notification_action_edit), editPendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * Cancela y remueve la notificación persistente.
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

