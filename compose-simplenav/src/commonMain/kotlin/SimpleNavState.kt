package net.lsafer.compose.simplenav

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.lsafer.compose.simplenav.internal.deserializeJsonOrNull
import net.lsafer.compose.simplenav.internal.serializeToJsonString

@Serializable
data class SimpleNavState<T>(
    val route: T,
    val tangents: Map<String, String> = emptyMap(),
) {
    fun <U> getTangent(name: String, serializer: KSerializer<U>): U? {
        return tangents[name]?.deserializeJsonOrNull(serializer)
    }

    fun <U> withTangent(name: String, value: U, serializer: KSerializer<U>): SimpleNavState<T> {
        return copy(
            route = route,
            tangents = buildMap {
                putAll(tangents)
                put(name, value.serializeToJsonString(serializer))
            }
        )
    }
}
