package com.smartjam.app.data.api

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.time.Instant

class InstantAdapter : TypeAdapter<Instant>() {
    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    override fun read(`in`: JsonReader): Instant? {
        if (`in`.peek() == JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        val s = `in`.nextString()
        return try {
            Instant.parse(s)
        } catch (e: Exception) {
            null
        }
    }
}

