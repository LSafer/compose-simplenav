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

    abstract fun push(route: T): Boolean
    abstract fun replace(route: T): Boolean
    abstract fun back(): Boolean
    abstract fun forward(): Boolean

    abstract fun <U : Any> tangent(
        name: String,
        default: U,
        serializer: KSerializer<U>,
    ): SimpleNavController<U>

    inline fun <reified U : Any> tangent(name: String, default: U) =
        tangent(name, default, serializer<U>())

    inline fun <reified U : Any> tangent(default: U) =
        tangent(U::class.simpleName.orEmpty(), default, serializer<U>())
}
