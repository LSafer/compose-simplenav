package net.lsafer.compose.simplenav

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import net.lsafer.compose.simplenav.internal.decodeFromStringOrNull
import net.lsafer.compose.simplenav.internal.decodeOrNull
import kotlin.io.encoding.Base64

@Serializable
data class NavState<out T>(
    val route: T,
    val tangents: Map<String, NavState<String>> = emptyMap(),
) {
    fun <U> getTangent(
        name: String,
        default: U,
        serializer: KSerializer<U>,
        format: StringFormat = Json,
    ): NavState<U> {
        val rawState = tangents[name] ?: return NavState(default)
        return NavState(
            format.decodeFromStringOrNull(serializer, rawState.route) ?: default,
            rawState.tangents,
        )
    }

    fun <U> withTangent(
        name: String,
        state: NavState<U>,
        serializer: KSerializer<U>,
        format: StringFormat = Json,
    ): NavState<T> {
        val rawState = NavState(
            route = format.encodeToString(serializer, state.route),
            tangents = state.tangents,
        )
        return copy(
            route = route,
            tangents = buildMap {
                putAll(tangents)
                put(name, rawState)
            }
        )
    }

    companion object {
        fun <T> fromUrlSafeString(
            source: String,
            serializer: KSerializer<T>,
            format: StringFormat = Json,
        ): NavState<T>? {
            val str = Base64.UrlSafe.decodeOrNull(source)?.decodeToString() ?: return null
            return format.decodeFromStringOrNull(serializer(serializer), str)
        }

        fun <T> NavState<T>.toUrlSafeString(
            serializer: KSerializer<T>,
            format: StringFormat = Json,
        ): String {
            val str = format.encodeToString(serializer(serializer), this)
            return Base64.UrlSafe.encode(str.encodeToByteArray())
        }
    }
}
