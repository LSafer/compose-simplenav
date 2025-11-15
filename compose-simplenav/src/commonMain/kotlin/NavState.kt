package net.lsafer.compose.simplenav

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import net.lsafer.compose.simplenav.NavState.Companion.toUrlSafeString
import net.lsafer.compose.simplenav.internal.decodeFromStringOrNull
import net.lsafer.compose.simplenav.internal.decodeOrNull
import kotlin.io.encoding.Base64

/**
 * Represents a complete navigation entry consisting of:
 *
 * - [route]: the main value for this entry (usually a screen, page, tab, or data object)
 * - [tangents]: a map of auxiliary navigation states that live *inside* this entry
 *
 * Tangents behave like “attached sub-controllers.”
 * They track parallel pieces of navigation state (search filters, selected tab, scroll position…)
 * and follow the lifecycle of the parent entry — unless intentionally overridden with `inherit`.
 *
 * This structure is fully serializable (JSON) and supports nesting of tangents arbitrarily deep.
 */
@Serializable
data class NavState<out T>(
    val route: T,
    val tangents: Map<String, NavState<String>> = emptyMap(),
) {
    /**
     * Retrieves the tangent state with the given [name].
     *
     * If the tangent does not exist, returns a new [NavState] with [default] as its route.
     *
     * Tangent values are serialized inside the parent state with [format].
     */
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

    /**
     * Returns a copy of this NavState with the given tangent updated to [state].
     *
     * The tangent's value is serialized with [format] before being stored in the outer NavState.
     * Tangent nesting is preserved without modification.
     */
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
        /**
         * Decodes a URL-safe Base64 string back into a [NavState].
         *
         * If any step fails, this function returns `null`.
         *
         * @param source A Base64 URL-safe encoded string returned from [toUrlSafeString].
         * @param serializer Serializer for the route type `T`.
         * @param format Serialization format used for encoding/decoding. Defaults to [Json].
         * @return A decoded [NavState] instance, or `null` on failure.
         */
        fun <T> fromUrlSafeString(
            source: String,
            serializer: KSerializer<T>,
            format: StringFormat = Json,
        ): NavState<T>? {
            val str = Base64.UrlSafe.decodeOrNull(source)?.decodeToString() ?: return null
            return format.decodeFromStringOrNull(serializer(serializer), str)
        }

        /**
         * Serializes this [NavState] into a URL-safe Base64 string.
         *
         * The resulting string is suitable for embedding in:
         * - URL fragments (e.g., `#nav=...`)
         * - query parameters
         * - browser history serialization
         *
         * @param serializer Serializer for the route type `T`.
         * @param format Serialization format to use. Defaults to [Json].
         * @return A URL-safe Base64 encoded representation of this state.
         */
        fun <T> NavState<T>.toUrlSafeString(
            serializer: KSerializer<T>,
            format: StringFormat = Json,
        ): String {
            val str = format.encodeToString(serializer(serializer), this)
            return Base64.UrlSafe.encode(str.encodeToByteArray())
        }
    }
}
