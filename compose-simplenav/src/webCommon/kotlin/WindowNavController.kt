package net.lsafer.compose.simplenav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.lsafer.compose.simplenav.NavState.Companion.toUrlSafeString
import net.lsafer.compose.simplenav.internal.windowNavigationCurrentEntryIndex
import net.lsafer.compose.simplenav.internal.windowNavigationEntriesLength
import net.lsafer.compose.simplenav.internal.windowNavigationSupported
import org.w3c.dom.HashChangeEvent
import org.w3c.dom.events.Event
import kotlin.jvm.JvmName

inline fun <reified T : Any> WindowNavController(
    default: T,
    tangents: Map<String, NavState<String>> = emptyMap(),
    format: StringFormat = Json,
) = WindowNavController(NavState(default, tangents), format)

@JvmName("WindowNavController_nullable")
inline fun <reified T> WindowNavController(
    default: T? = null,
    tangents: Map<String, NavState<String>> = emptyMap(),
    format: StringFormat = Json,
) = WindowNavController(NavState(default, tangents), format)

inline fun <reified T> WindowNavController(
    initialState: NavState<T>,
    format: StringFormat = Json,
): WindowNavController<T> {
    return WindowNavController(initialState, serializer<T>(), format)
}

/**
 * Navigation controller backed by the browser's hash and history APIs.
 *
 * Features:
 * - Synchronizes with window.location.hash
 * - Supports browser back/forward buttons
 * - Serializes NavState to Base64-encoded JSON inside the hash
 *
 * Requires calling [globalInstall] to bind to browser events.
 *
 * Limitations:
 * - Only one WindowNavController may be globally installed at a time
 */
class WindowNavController<T>(
    initialState: NavState<T>,
    private val serializer: KSerializer<T>,
    private val format: StringFormat = Json,
) : NavController<T>() {
    companion object {
        var globalIsInstalled by mutableStateOf(false)
    }

    var isInstalled by mutableStateOf(false)
        private set

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
            window.location.replace("#${newState.toUrlSafeString(serializer, format)}")
        } else {
            state = newState
            window.location.hash = newState.toUrlSafeString(serializer, format)
        }

        return true
    }

    // ========== BROWSER WINDOW API INTEGRATION ==========

    /**
     * Installs this controller as the globally active browser navigation handler.
     *
     * - Loads initial state from window.location.hash
     * - Writes initial hash based on internal state
     * - Listens to hashchange events
     */
    fun globalInstall() {
        check(!globalIsInstalled) { "A NavController was already globally installed" }

        globalIsInstalled = true
        isInstalled = true

        // initial [window.location.hash] => [navController]
        val initialState = NavState.fromUrlSafeString(
            window.location.hash.substringAfterLast("#"),
            serializer = serializer,
            format = format,
        )

        if (initialState != null)
            state = initialState

        if (windowNavigationSupported()) {
            length = windowNavigationEntriesLength() ?: 1
            currentIndex = windowNavigationCurrentEntryIndex() ?: 0
        }

        // initial [navController] => [window.location.hash]
        window.location.replace("#${state.toUrlSafeString(serializer, format)}")

        // collect [window.location.hash] => [navController]
        window.addEventListener("hashchange", hashchangeListener)
    }

    /**
     * Removes event listeners and resets internal indices.
     */
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

        val newState = NavState.fromUrlSafeString(
            event.newURL.substringAfterLast("#"),
            serializer = serializer,
            format = format,
        )

        if (newState != null)
            state = newState

        if (windowNavigationSupported()) {
            length = windowNavigationEntriesLength() ?: 1
            currentIndex = windowNavigationCurrentEntryIndex() ?: 0
        }
    }
}
