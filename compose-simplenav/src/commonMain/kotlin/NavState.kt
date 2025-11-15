package net.lsafer.compose.simplenav

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.lsafer.compose.simplenav.internal.deserializeJsonOrNull
import net.lsafer.compose.simplenav.internal.serializeToJsonString

@Serializable
data class NavState<out T>(
    val route: T,
    val tangents: Map<String, NavState<String>> = emptyMap(),
) {
    fun <U> getTangent(name: String, default: U, serializer: KSerializer<U>): NavState<U> {
        val rawState = tangents[name] ?: return NavState(default)
        return NavState(
            rawState.route.deserializeJsonOrNull(serializer) ?: default,
            rawState.tangents,
        )
    }

    fun <U> withTangent(name: String, state: NavState<U>, serializer: KSerializer<U>): NavState<T> {
        val rawState = NavState(
            route = state.route.serializeToJsonString(serializer),
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
}
