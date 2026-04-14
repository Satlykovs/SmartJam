package com.smartjam.app.data.local

import androidx.room.TypeConverter
import java.net.URI
import java.time.Instant
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    @TypeConverter
    fun fromUUID(value: String?): UUID? {
        return value?.let { UUID.fromString(it) }
    }

    @TypeConverter
    fun uuidToString(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun fromURI(value: String?): URI? {
        return value?.let { URI.create(it) }
    }

    @TypeConverter
    fun uriToString(uri: URI?): String? {
        return uri?.toString()
    }
}

