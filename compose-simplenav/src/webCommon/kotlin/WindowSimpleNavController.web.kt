package net.lsafer.compose.simplenav

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.lsafer.compose.simplenav.internal.decodeBase64UrlSafeToStringOrNull
import net.lsafer.compose.simplenav.internal.deserializeJsonOrNull
import net.lsafer.compose.simplenav.internal.encodeBase64UrlSafe
import net.lsafer.compose.simplenav.internal.serializeToJsonString

inline fun <reified T : Any> WindowSimpleNavController(
    route: T? = null,
) = WindowSimpleNavController(WindowSimpleNavController.State(route))

inline fun <reified T : Any> WindowSimpleNavController(
    initialState: WindowSimpleNavController.State<T> =
        WindowSimpleNavController.State(),
) = WindowSimpleNavController(initialState, serializer())

class WindowSimpleNavController<T : Any>(
    initialState: State<T> = State(),
    override val stateSerializer: KSerializer<State<T>>,
) : SimpleNavController<T> {
    @Serializable
    data class State<T : Any>(
        override val route: T? = null,
    ) : SimpleNavController.State<T>

    var isInstalled: Boolean = false
        internal set

    override val state = MutableStateFlow(initialState)

    override fun push(route: T): Boolean {
        require(isInstalled) { "NavController not installed" }
        // no need to sync in js land
        val current = state.value
        if (route == current.route) return false
        val new = current.copy(route = route)
        state.value = new
        window.location.hash = new.encodeHash()
        return true
    }

    override fun replace(route: T): Boolean {
        require(isInstalled) { "NavController not installed" }
        // no need to sync in js land
        val current = state.value
        if (route == current.route) return false
        val new = current.copy(route = route)
        state.value = new
        window.location.replace("#${new.encodeHash()}")
        return true
    }

    override fun back(): Boolean {
        require(isInstalled) { "NavController not installed" }
        window.history.back()
        return true
    }

    override fun forward(): Boolean {
        require(isInstalled) { "NavController not installed" }
        window.history.forward()
        return true
    }

    internal fun State<T>.encodeHash(): String {
        return serializeToJsonString(stateSerializer)
            .encodeBase64UrlSafe()
    }

    internal fun String.decodeHashOrNull(): State<T>? {
        return decodeBase64UrlSafeToStringOrNull()
            ?.deserializeJsonOrNull(stateSerializer)
    }
}
