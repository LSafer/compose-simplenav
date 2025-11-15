package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue

abstract class NavController<T> {
    /**
     * The current navigation state. Snapshot.
     */
    abstract val state: NavState<T>

    /**
     * The current route. Snapshot.
     */
    val current by derivedStateOf { state.route }

    abstract val length: Int
    abstract val currentIndex: Int

    val lastIndex get() = length - 1
    val canGoBack get() = currentIndex > 0
    val canGoForward get() = currentIndex < lastIndex

    abstract fun back(): Boolean
    abstract fun forward(): Boolean
    abstract fun go(delta: Int)

    fun goTo(index: Int) = go(index - currentIndex)
    fun goToFirst() = go(-currentIndex)
    fun goToLast() = go(lastIndex - currentIndex)

    abstract fun edit(
        replace: Boolean = true,
        transform: (NavState<T>) -> NavState<T>?,
    ): Boolean

    fun navigate(
        replace: Boolean = false,
        inherit: Boolean = true,
        force: Boolean = false,
        transform: (T) -> T
    ): Boolean {
        return edit(replace = replace) { current ->
            val newRoute = transform(current.route)

            if (!force && newRoute == current.route)
                return@edit null

            val newState = when {
                inherit -> current.copy(route = newRoute)
                else -> NavState(newRoute)
            }

            return@edit newState
        }
    }

    fun navigate(
        route: T,
        replace: Boolean = false,
        inherit: Boolean = true,
        force: Boolean = false,
    ): Boolean {
        return edit(replace = replace) { current ->
            if (!force && route == current.route)
                return@edit null

            val newState = when {
                inherit -> current.copy(route = route)
                else -> NavState(route)
            }

            return@edit newState
        }
    }

    fun push(route: T, inherit: Boolean = true, force: Boolean = false) =
        navigate(route, replace = false, inherit = inherit, force = force)

    fun replace(route: T, inherit: Boolean = true, force: Boolean = false) =
        navigate(route, replace = true, inherit = inherit, force = force)

    fun push(inherit: Boolean = true, force: Boolean = false, transform: (T) -> T) =
        navigate(replace = false, inherit = inherit, force = force, transform)

    fun replace(inherit: Boolean = true, force: Boolean = false, transform: (T) -> T) =
        navigate(replace = true, inherit = inherit, force = force, transform)
}
