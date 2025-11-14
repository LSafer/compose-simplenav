package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
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
    tangents: Map<String, String> = emptyMap()
) = WindowNavController(NavState(default, tangents))

@JvmName("WindowNavController_nullable")
inline fun <reified T> WindowNavController(
    default: T? = null,
    tangents: Map<String, String> = emptyMap()
) = WindowNavController(NavState(default, tangents))

inline fun <reified T> WindowNavController(
    initialState: NavState<T>,
): WindowNavController<T> {
    return WindowNavController(initialState, serializer())
}

fun <T> WindowNavController(
    initialState: NavState<T>,
    serializer: KSerializer<T>,
): WindowNavController<T> {
    return WindowNavControllerImpl(
        initialState = initialState,
        stateSerializer = NavState.serializer(serializer),
    )
}

sealed class WindowNavController<T> : NavController<T>() {
    companion object {
        var globalIsInstalled by mutableStateOf(false)
    }

    abstract val isInstalled: Boolean

    abstract fun globalInstall()
    abstract fun globalUnInstall()

    fun tryGlobalInstall() = if (globalIsInstalled) false else run { globalInstall(); true }
    fun tryGlobalUninstall() = if (!isInstalled) false else run { globalUnInstall(); true }

    internal abstract fun internalSetState(newState: NavState<T>, replace: Boolean)

    override fun navigate(
        replace: Boolean,
        inherit: Boolean,
        force: Boolean,
        transform: (T) -> T,
    ): Boolean {
        require(isInstalled) { "NavController not installed" }
        val current = state
        val newRoute = transform(current.route)

        if (!force && newRoute == current.route)
            return false

        val newState = when {
            inherit -> current.copy(route = newRoute)
            else -> NavState(newRoute)
        }

        internalSetState(newState, replace)
        return true
    }

    override fun <U> tangent(
        name: String,
        default: U,
        serializer: KSerializer<U>,
    ): WindowNavController<U> {
        return WindowNavControllerTangent(
            outer = this,
            name = name,
            defaultState = NavState(default),
            stateSerializer = NavState.serializer(serializer),
        )
    }
}

internal class WindowNavControllerImpl<T>(
    initialState: NavState<T>,
    private val stateSerializer: KSerializer<NavState<T>>,
) : WindowNavController<T>() {
    override var isInstalled by mutableStateOf(false)
        private set
    override var state by mutableStateOf(initialState)
        private set

    override var length: Int by mutableStateOf(1)
        private set
    override var currentIndex: Int by mutableStateOf(0)
        private set

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

    override fun globalInstall() {
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

    override fun globalUnInstall() {
        check(isInstalled) { "NavController is not globally installed" }

        window.removeEventListener("hashchange", hashchangeListener)

        if (windowNavigationSupported()) {
            length = 1
            currentIndex = 0
        }

        isInstalled = false
        globalIsInstalled = false
    }

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

    override fun internalSetState(newState: NavState<T>, replace: Boolean) {
        if (replace) {
            state = newState
            window.location.replace("#${newState.encodeHash()}")
        } else {
            state = newState
            window.location.hash = newState.encodeHash()
        }
    }
}

internal class WindowNavControllerTangent<T, U>(
    private val outer: WindowNavController<T>,
    private val name: String,
    private val defaultState: NavState<U>,
    private val stateSerializer: KSerializer<NavState<U>>,
) : WindowNavController<U>() {
    override val isInstalled get() = outer.isInstalled
    override val state by derivedStateOf {
        outer.state.getTangent(name, stateSerializer)
            ?: defaultState
    }

    override val length get() = outer.length
    override val currentIndex get() = outer.currentIndex

    override fun globalInstall() = outer.globalInstall()
    override fun globalUnInstall() = outer.globalUnInstall()
    override fun back() = outer.back()
    override fun forward() = outer.forward()
    override fun go(delta: Int) = outer.go(delta)

    override fun internalSetState(newState: NavState<U>, replace: Boolean) {
        val newOuterState = outer.state.withTangent(
            name = name,
            value = newState,
            serializer = stateSerializer,
        )
        outer.internalSetState(newOuterState, replace)
    }
}
