package com.ixeken.drafto.ui.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Utilidad estática y thread-safe para formateo de fechas y horas en Drafto.
 *
 * Cumple con los estándares de /jetpack-compose-performance para evitar recolección de basura
 * (Zero GC Churn a 120 FPS) al no instanciar formateadores pesados dentro del ciclo de composición.
 */
object DraftoDateFormatter {

    private val formatters = ConcurrentHashMap<String, ThreadLocal<SimpleDateFormat>>()

    private fun getFormatter(pattern: String, locale: Locale = Locale.getDefault()): SimpleDateFormat {
        val key = "$pattern-${locale.toLanguageTag()}"
        val threadLocal = formatters.computeIfAbsent(key) {
            ThreadLocal.withInitial { SimpleDateFormat(pattern, locale) }
        }
        return threadLocal.get() ?: SimpleDateFormat(pattern, locale)
    }

    /**
     * Formatea una marca de tiempo en formato de fecha estándar corto (ej. "22/03/26").
     */
    fun formatShortDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        if (timestamp <= 0L) return ""
        return getFormatter("dd/MM/yy", locale).format(Date(timestamp))
    }

    /**
     * Formatea una marca de tiempo en formato editorial medio (ej. "22 Mar 2026").
     */
    fun formatMediumDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        if (timestamp <= 0L) return ""
        return getFormatter("dd MMM yyyy", locale).format(Date(timestamp))
    }

    /**
     * Formatea una marca de tiempo en formato de fecha y hora (ej. "22 Mar 2026, 14:30").
     */
    fun formatDateTime(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        if (timestamp <= 0L) return ""
        return getFormatter("dd MMM yyyy, HH:mm", locale).format(Date(timestamp))
    }

    /**
     * Formatea una fecha de vencimiento para tarjetas de tareas (To-Dos) con resolución relativa inteligente.
     * Si corresponde al día de hoy o mañana, devuelve un texto contextual breve.
     */
    fun formatTodoDueDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        if (timestamp <= 0L) return ""
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        val isSameDay = isSameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        val isTomorrow = isSameYear && target.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR) == 1

        return when {
            isSameDay -> "Hoy"
            isTomorrow -> "Mañana"
            isSameYear -> getFormatter("d MMM", locale).format(Date(timestamp))
            else -> getFormatter("d MMM yyyy", locale).format(Date(timestamp))
        }
    }
}
