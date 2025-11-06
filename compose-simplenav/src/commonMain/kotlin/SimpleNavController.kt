package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

abstract class SimpleNavController<T : Any> {
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
        route: T,
        replace: Boolean = false,
        inherit: Boolean = false,
    ): Boolean

    abstract fun <U : Any> tangent(
        name: String,
        default: U,
        serializer: KSerializer<U>,
    ): SimpleNavController<U>

    fun push(route: T) = navigate(route, replace = false, inherit = false)
    fun pushInherit(route: T) = navigate(route, replace = false, inherit = true)
    fun replace(route: T) = navigate(route, replace = true, inherit = false)
    fun replaceInherit(route: T) = navigate(route, replace = true, inherit = true)

    inline fun <reified U : Any> tangent(name: String, default: U) =
        tangent(name, default, serializer<U>())

    inline fun <reified U : Any> tangent(default: U) =
        tangent(U::class.simpleName.orEmpty(), default, serializer<U>())
}
