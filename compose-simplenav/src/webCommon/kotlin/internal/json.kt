package net.lsafer.compose.simplenav.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@PublishedApi
internal fun <T> T.serializeToJsonString(
    serializer: KSerializer<T>,
    json: Json = Json,
): String {
    return json.encodeToString(serializer, this)
}

@PublishedApi
internal fun <T> String.deserializeJsonOrNull(
    serializer: KSerializer<T>,
    json: Json = Json,
): T? {
    return try {
        json.decodeFromString(serializer, this)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
