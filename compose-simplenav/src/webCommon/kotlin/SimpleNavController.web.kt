package net.lsafer.compose.simplenav

import kotlinx.browser.window
import org.w3c.dom.HashChangeEvent
import org.w3c.dom.events.Event

private var gIsInstalled = false
private var gInstalledListener: ((Event) -> Unit)? = null

fun <T : Any> WindowSimpleNavController<T>.tryGlobalInstall() =
    if (gIsInstalled) false else run { globalInstall(); true }

fun <T : Any> WindowSimpleNavController<T>.globalInstall() {
    check(!gIsInstalled) { "A NavController was already globally installed" }

    gIsInstalled = true
    isInstalled = true

    // initial [window.location.hash] => [navController]
    val initialState = window.location.hash.decodeHashOrNull()

    if (initialState != null)
        state.value = initialState

    // initial [navController] => [window.location.hash]
    window.location.replace("#${state.value.encodeHash()}")

    // collect [window.location.hash] => [navController]
    gInstalledListener = { event: Event ->
        @Suppress("USELESS_CAST")
        event as HashChangeEvent

        val newState = event.newURL
            .substringAfterLast("#")
            .decodeHashOrNull()

        if (newState != null)
            state.value = newState
    }

    window.addEventListener("hashchange", gInstalledListener)
}

fun <T : Any> WindowSimpleNavController<T>.tryGlobalUninstall() =
    if (!isInstalled) false else run { globalUninstall(); true }

fun <T : Any> WindowSimpleNavController<T>.globalUninstall() {
    check(isInstalled) { "NavController is not globally installed" }

    window.removeEventListener("hashchange", gInstalledListener)

    gInstalledListener = null
    isInstalled = false
    gIsInstalled = false
}
