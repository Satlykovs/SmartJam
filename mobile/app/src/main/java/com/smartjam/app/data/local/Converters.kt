package com.smartjam.app.data.local

import android.util.Log
import androidx.room.TypeConverter
import com.smartjam.app.model.FeedbackEvent
import java.net.URI
import java.time.Instant
import java.util.UUID
import org.openapitools.client.infrastructure.Serializer.gson

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter fun dateToTimestamp(date: Instant?): Long? = date?.toEpochMilli()

    @TypeConverter
    fun fromUUID(value: String?): UUID? =
        try {
            value?.let { UUID.fromString(it) }
        } catch (e: IllegalArgumentException) {
            Log.e("Converters", "Failed to parse UUID from string: $value", e)
            null
        }

    @TypeConverter fun uuidToString(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun fromURI(value: String?): URI? =
        try {
            value?.let { URI.create(it) }
        } catch (e: Exception) {
            Log.e("Converters", "Failed to parse URI from string: $value", e)
            null
        }

    @TypeConverter fun uriToString(uri: URI?): String? = uri?.toString()

    @TypeConverter fun fromFloatList(value: List<Float>?): String? = value?.joinToString(",")

    @TypeConverter
    fun toFloatList(value: String?): List<Float>? {
        if (value.isNullOrBlank()) return null
        return value.split(",").mapNotNull { it.toFloatOrNull() }
    }

    @TypeConverter fun fromFeedbackList(value: List<FeedbackEvent>?): String? = gson.toJson(value)

    @TypeConverter
    fun toFeedbackList(value: String?): List<FeedbackEvent>? {
        if (value.isNullOrBlank()) return null
        val type = object : com.google.gson.reflect.TypeToken<List<FeedbackEvent>>() {}.type
        return gson.fromJson(value, type)
    }
}
