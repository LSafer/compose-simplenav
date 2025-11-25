package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue

/**
 * A stack-like navigation controller inspired by browser navigation:
 *
 * - You can `push`, `replace`, `back`, `forward`, and `go(delta)`
 * - Each entry stores a full [NavState], including tangent states
 * - Implementations choose where state lives (memory, browser history, etc.)
 *
 * This controller supports *reactive* (Compose) API usage through snapshot state.
 */
abstract class NavController<T> {
    /** The full list of entries in this controller. */
    abstract val entries: List<NavState<T>>

    /** The index of the current entry. */
    abstract val currentIndex: Int

    /** The current full navigation state, including route and tangents. */
    abstract val state: NavState<T>

    /** The current route. */
    val current by derivedStateOf { state.route }
    /** The previous route. */
    val previous get() = entries.getOrNull(currentIndex - 1)
    /** The next route. */
    val next get() = entries.getOrNull(currentIndex + 1)

    /** The count of all entries in the navigation stack. */
    val length get() = entries.size
    /** The index of the last entry in the navigation stack. */
    val lastIndex get() = length - 1
    /** True, to indicate that navigating back is possible. */
    val canGoBack get() = currentIndex > 0
    /** True, to indicate that navigating forward is possible. */
    val canGoForward get() = currentIndex < lastIndex

    /** The entries before the current entry in the navigation stack sorted last-to-first */
    val backStack get() = entries.subList(0, currentIndex).asReversed()
    /** The entries after the current entry in the navigation stack sorted first-to-last */
    val forwardStack get() = entries.subList(currentIndex + 1, entries.size)

    /**
     * Attempt to navigate to the previous entry.
     *
     * @return false on failure.
     */
    abstract fun back(): Boolean
    /**
     * Attempt to navigate to the next entry.
     *
     * @return false on failure.
     */
    abstract fun forward(): Boolean
    /**
     * Attempt to navigate [delta] positions from the current entry.
     *
     * @return the number of positions navigated.
     */
    abstract fun go(delta: Int): Int

    /**
     * Attempt to navigate to the entry with [index].
     *
     * @return the number of positions navigated.
     */
    fun goTo(index: Int) = go(index - currentIndex)
    /**
     * Navigate to the first entry.
     *
     * @return the number of positions navigated.
     */
    fun goToFirst() = go(-currentIndex)
    /**
     * Navigate to the last entry.
     *
     * @return the number of positions navigated.
     */
    fun goToLast() = go(lastIndex - currentIndex)

    /**
     * Edits the current NavState.
     *
     * This is the core primitive of navigation; everything else (push/replace/navigate)
     * is implemented through this.
     *
     * When [replace] is set to false, the forward stack will be reset and the new entry
     * will be added to the navigation stack and navigated to.
     * Otherwise, if [replace] is set to true, the forward stack won't be changed and the
     * new entry will replace the current one instead.
     *
     * @param replace if true, modifies the current entry instead of adding a new one.
     * @param transform transformation block, returning null cancels the edit.
     * @return false, if [transform] returned null.
     */
    abstract fun edit(
        replace: Boolean = true,
        transform: (NavState<T>) -> NavState<T>?,
    ): Boolean

    /**
     * Navigates to a new route produced by [transform], optionally replacing the
     * current entry or inheriting its tangents.
     *
     * When [replace] is set to false, the forward stack will be reset and the new entry
     * will be added to the navigation stack and navigated to.
     * Otherwise, if [replace] is set to true, the forward stack won't be changed and the
     * new entry will replace the current one instead.
     *
     * @param replace if true, modifies the current entry instead of adding a new one. (false by default)
     * @param inherit if true, the new state inherits all tangents from the current state. (true by default)
     * @param force if false, navigation is skipped when the transformed route is equal to the current route. (false by default)
     * @return true, if navigation was performed.
     */
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

    /**
     * Navigates to a new route given as [route], optionally replacing the
     * current entry or inheriting its tangents.
     *
     * When [replace] is set to false, the forward stack will be reset and the new entry
     * will be added to the navigation stack and navigated to.
     * Otherwise, if [replace] is set to true, the forward stack won't be changed and the
     * new entry will replace the current one instead.
     *
     * @param replace if true, modifies the current entry instead of adding a new one. (false by default)
     * @param inherit if true, the new state inherits all tangents from the current state. (true by default)
     * @param force if false, navigation is skipped when the transformed route is equal to the current route. (false by default)
     * @return true, if navigation was performed.
     */
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
