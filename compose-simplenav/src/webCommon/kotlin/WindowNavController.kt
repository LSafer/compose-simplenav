package net.lsafer.compose.simplenav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import net.lsafer.compose.simplenav.internal.*
import org.w3c.dom.HashChangeEvent
import org.w3c.dom.events.Event
import kotlin.jvm.JvmName

inline fun <reified T : Any> WindowNavController(
    default: T,
    tangents: Map<String, NavState<String>> = emptyMap()
) = WindowNavController(NavState(default, tangents))

@JvmName("WindowNavController_nullable")
inline fun <reified T> WindowNavController(
    default: T? = null,
    tangents: Map<String, NavState<String>> = emptyMap()
) = WindowNavController(NavState(default, tangents))

inline fun <reified T> WindowNavController(
    initialState: NavState<T>,
): WindowNavController<T> {
    return WindowNavController(initialState, serializer<T>())
}

class WindowNavController<T>(
    initialState: NavState<T>,
    serializer: KSerializer<T>,
) : NavController<T>() {
    companion object {
        var globalIsInstalled by mutableStateOf(false)
    }

    var isInstalled by mutableStateOf(false)
        private set

    private val stateSerializer = NavState.serializer(serializer)

    override var state by mutableStateOf(initialState)
        private set
    override var length: Int by mutableStateOf(1)
        private set
    override var currentIndex: Int by mutableStateOf(0)
        private set

    override fun back(): Boolean {
        require(isInstalled) { "NavController not installed" }
        if (!canGoBack) return false
        window.history.back()
        return true
    }

    override fun forward(): Boolean {
        require(isInstalled) { "NavController not installed" }
        if (!canGoForward) return false
        window.history.forward()
        return true
    }

    override fun go(delta: Int) {
        require(isInstalled) { "NavController not installed" }
        if (delta == 0) return
        if (delta > 0) {
            if (currentIndex == lastIndex) return
            val d = minOf(delta, lastIndex - currentIndex)
            window.history.go(d)
        }
        if (delta < 0) {
            if (currentIndex == 0) return
            val d = maxOf(delta, -currentIndex)
            window.history.go(d)
        }
    }

    override fun edit(replace: Boolean, transform: (NavState<T>) -> NavState<T>?): Boolean {
        require(isInstalled) { "NavController not installed" }

        val newState = transform(state) ?: return false

        if (replace) {
            state = newState
            window.location.replace("#${newState.encodeHash()}")
        } else {
            state = newState
            window.location.hash = newState.encodeHash()
        }

        return true
    }

    // ========== BROWSER WINDOW API INTEGRATION ==========

    fun globalInstall() {
        check(!globalIsInstalled) { "A NavController was already globally installed" }

        globalIsInstalled = true
        isInstalled = true

        // initial [window.location.hash] => [navController]
        val initialState = window.location.hash
            .substringAfterLast("#")
            .decodeHashOrNull()

        if (initialState != null)
            state = initialState

        if (windowNavigationSupported()) {
            length = windowNavigationEntriesLength() ?: 1
            currentIndex = windowNavigationCurrentEntryIndex() ?: 0
        }

        // initial [navController] => [window.location.hash]
        window.location.replace("#${state.encodeHash()}")

        // collect [window.location.hash] => [navController]
        window.addEventListener("hashchange", hashchangeListener)
    }

    fun globalUnInstall() {
        check(isInstalled) { "NavController is not globally installed" }

        window.removeEventListener("hashchange", hashchangeListener)

        if (windowNavigationSupported()) {
            length = 1
            currentIndex = 0
        }

        isInstalled = false
        globalIsInstalled = false
    }

    fun tryGlobalInstall(): Boolean {
        if (globalIsInstalled) return false
        globalInstall()
        return true
    }

    fun tryGlobalUninstall(): Boolean {
        if (!isInstalled) return false
        globalUnInstall()
        return true
    }

    private val hashchangeListener = { event: Event ->
        @Suppress("USELESS_CAST")
        event as HashChangeEvent

        val newState = event.newURL
            .substringAfterLast("#")
            .decodeHashOrNull()

        if (newState != null)
            state = newState

        if (windowNavigationSupported()) {
            length = windowNavigationEntriesLength() ?: 1
            currentIndex = windowNavigationCurrentEntryIndex() ?: 0
        }
    }

    private fun NavState<T>.encodeHash(): String {
        return serializeToJsonString(stateSerializer)
            .encodeBase64UrlSafe()
    }

    private fun String.decodeHashOrNull(): NavState<T>? {
        return decodeBase64UrlSafeToStringOrNull()
            ?.deserializeJsonOrNull(stateSerializer)
    }
}
