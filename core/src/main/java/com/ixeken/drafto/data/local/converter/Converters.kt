package com.ixeken.drafto.data.local.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import com.ixeken.drafto.domain.model.TodoSubtask

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) emptyList() else Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromSubtaskList(value: List<TodoSubtask>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toSubtaskList(value: String): List<TodoSubtask> {
        return if (value.isBlank()) emptyList() else Json.decodeFromString(value)
    }
}

