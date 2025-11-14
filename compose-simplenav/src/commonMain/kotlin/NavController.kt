package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import net.lsafer.compose.simplenav.internal.inferTangentName
import kotlin.jvm.JvmName
import kotlin.reflect.typeOf

abstract class NavController<T> {
    /**
     * The current navigation state. Snapshot.
     */
    abstract val state: NavState<T>

    /**
     * The current route. Snapshot.
     */
    val current by derivedStateOf { state.route }

    abstract fun back(): Boolean
    abstract fun forward(): Boolean

    abstract fun navigate(
        replace: Boolean = false,
        inherit: Boolean = true,
        force: Boolean = false,
        transform: (T) -> T,
    ): Boolean

    fun navigate(
        route: T,
        replace: Boolean = false,
        inherit: Boolean = true,
        force: Boolean = false,
    ): Boolean = navigate(
        replace = replace,
        inherit = inherit,
        force = force,
        transform = { route },
    )

    fun push(route: T, inherit: Boolean = true, force: Boolean = false) =
        navigate(route, replace = false, inherit = inherit, force = force)

    fun replace(route: T, inherit: Boolean = true, force: Boolean = false) =
        navigate(route, replace = true, inherit = inherit, force = force)

    fun push(inherit: Boolean = true, force: Boolean = false, transform: (T) -> T) =
        navigate(replace = false, inherit = inherit, force = force, transform)

    fun replace(inherit: Boolean = true, force: Boolean = false, transform: (T) -> T) =
        navigate(replace = true, inherit = inherit, force = force, transform)

    abstract fun <U> tangent(
        name: String,
        default: U,
        serializer: KSerializer<U>,
    ): NavController<U>

    inline fun <reified U : Any> tangent(name: String, default: U) =
        tangent(name, default, serializer<U>())

    inline fun <reified U : Any> tangent(default: U) =
        tangent(typeOf<U>().inferTangentName(), default, serializer<U>())

    @JvmName("tangent_nullable")
    inline fun <reified U> tangent(name: String, default: U? = null) =
        tangent(name, default, serializer<U?>())

    @JvmName("tangent_nullable")
    inline fun <reified U> tangent(default: U? = null) =
        tangent(typeOf<U>().inferTangentName(), default, serializer<U?>())
}
