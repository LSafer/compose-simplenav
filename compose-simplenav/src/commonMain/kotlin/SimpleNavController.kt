package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

abstract class SimpleNavController<T> {
    /**
     * The current navigation state. Snapshot.
     */
    abstract val state: SimpleNavState<T>

    /**
     * The current route. Snapshot.
     */
    val current by derivedStateOf { state.route }

    abstract fun back(): Boolean
    abstract fun forward(): Boolean

    abstract fun navigate(
        replace: Boolean = false,
        inherit: Boolean = false,
        transform: (T) -> T,
    ): Boolean

    fun navigate(
        route: T,
        replace: Boolean = false,
        inherit: Boolean = false,
    ): Boolean = navigate(
        replace = replace,
        inherit = inherit,
        transform = { route },
    )

    fun push(route: T) = navigate(route, replace = false, inherit = false)
    fun pushInherit(route: T) = navigate(route, replace = false, inherit = true)
    fun replace(route: T) = navigate(route, replace = true, inherit = false)
    fun replaceInherit(route: T) = navigate(route, replace = true, inherit = true)

    fun push(transform: (T) -> T) = navigate(replace = false, inherit = false, transform)
    fun pushInherit(transform: (T) -> T) = navigate(replace = false, inherit = true, transform)
    fun replace(transform: (T) -> T) = navigate(replace = true, inherit = false, transform)
    fun replaceInherit(transform: (T) -> T) = navigate(replace = true, inherit = true, transform)

    abstract fun <U> tangent(
        name: String,
        default: U,
        serializer: KSerializer<U>,
    ): SimpleNavController<U>

    inline fun <reified U> tangent(name: String, default: U) =
        tangent(name, default, serializer<U>())

    inline fun <reified U> tangent(default: U) =
        tangent(U::class.simpleName.orEmpty(), default, serializer<U>())
}
