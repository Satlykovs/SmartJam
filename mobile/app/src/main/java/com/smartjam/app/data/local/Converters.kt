package com.smartjam.app.data.local

import android.util.Log
import androidx.room.TypeConverter
import java.net.URI
import java.time.Instant
import java.util.UUID

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
}
