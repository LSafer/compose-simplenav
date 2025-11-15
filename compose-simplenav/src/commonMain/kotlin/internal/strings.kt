package net.lsafer.compose.simplenav.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.StringFormat
import kotlin.io.encoding.Base64

internal fun <T> StringFormat.decodeFromStringOrNull(
    serializer: KSerializer<T>,
    string: String,
): T? {
    return try {
        decodeFromString(serializer, string)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal fun Base64.decodeOrNull(string: String): ByteArray? {
    return try {
        decode(string)
    } catch (_: IllegalArgumentException) {
        null
    }
}
