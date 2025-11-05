package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.lsafer.compose.simplenav.internal.decodeBase64UrlSafeToStringOrNull
import net.lsafer.compose.simplenav.internal.deserializeJsonOrNull
import net.lsafer.compose.simplenav.internal.encodeBase64UrlSafe
import net.lsafer.compose.simplenav.internal.serializeToJsonString
import org.w3c.dom.HashChangeEvent
import org.w3c.dom.events.Event

inline fun <reified T : Any> WindowSimpleNavController(
    route: T? = null,
) = WindowSimpleNavController(WindowSimpleNavController.State(route))

inline fun <reified T : Any> WindowSimpleNavController(
    initialState: WindowSimpleNavController.State<T> =
        WindowSimpleNavController.State(),
) = WindowSimpleNavController(initialState, serializer())

class WindowSimpleNavController<T : Any>(
    initialState: State<T> = State(),
    private val stateSerializer: KSerializer<State<T>>,
) : SimpleNavController<T> {
    @Serializable
    data class State<T : Any>(
        val route: T? = null,
    )

    var isInstalled: Boolean = false
        private set

    var state by mutableStateOf<State<T>>(initialState)
        private set

    override val current by derivedStateOf { state.route }

    override fun push(route: T): Boolean {
        require(isInstalled) { "NavController not installed" }
        // no need to sync in js land
        val current = state
        if (route == current.route) return false
        val new = current.copy(route = route)
        state = new
        window.location.hash = new.encodeHash()
        return true
    }

    override fun replace(route: T): Boolean {
        require(isInstalled) { "NavController not installed" }
        // no need to sync in js land
        val current = state
        if (route == current.route) return false
        val new = current.copy(route = route)
        state = new
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

    // ========== GLOBAL INSTALL ==========

    companion object {
        var globalIsInstalled = false
            private set
    }

    private val _hashchangeListener = { event: Event ->
        @Suppress("USELESS_CAST")
        event as HashChangeEvent

        val newState = event.newURL
            .substringAfterLast("#")
            .decodeHashOrNull()

        if (newState != null)
            state = newState
    }

    fun tryGlobalInstall() = if (globalIsInstalled) false else run { globalInstall(); true }
    fun tryGlobalUninstall() = if (!isInstalled) false else run { globalUninstall(); true }

    fun globalInstall() {
        check(!globalIsInstalled) { "A NavController was already globally installed" }

        globalIsInstalled = true
        isInstalled = true

        // initial [window.location.hash] => [navController]
        val initialState = window.location.hash.decodeHashOrNull()

        if (initialState != null)
            state = initialState

        // initial [navController] => [window.location.hash]
        window.location.replace("#${state.encodeHash()}")

        // collect [window.location.hash] => [navController]
        window.addEventListener("hashchange", _hashchangeListener)
    }

    fun globalUninstall() {
        check(isInstalled) { "NavController is not globally installed" }

        window.removeEventListener("hashchange", _hashchangeListener)

        isInstalled = false
        globalIsInstalled = false
    }

    // ========== INTERNALS ==========

    private fun State<T>.encodeHash(): String {
        return serializeToJsonString(stateSerializer)
            .encodeBase64UrlSafe()
    }

    private fun String.decodeHashOrNull(): State<T>? {
        return decodeBase64UrlSafeToStringOrNull()
            ?.deserializeJsonOrNull(stateSerializer)
    }
}
