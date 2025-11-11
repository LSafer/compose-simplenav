package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.jvm.JvmName

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
        inherit: Boolean = false,
        force: Boolean = false,
        transform: (T) -> T,
    ): Boolean

    fun navigate(
        route: T,
        replace: Boolean = false,
        inherit: Boolean = false,
        force: Boolean = false,
    ): Boolean = navigate(
        replace = replace,
        inherit = inherit,
        force = force,
        transform = { route },
    )

    fun push(route: T, force: Boolean = false) =
        navigate(route, replace = false, inherit = false, force = force)

    fun pushInherit(route: T, force: Boolean = false) =
        navigate(route, replace = false, inherit = true, force = force)

    fun replace(route: T, force: Boolean = false) =
        navigate(route, replace = true, inherit = false, force = force)

    fun replaceInherit(route: T, force: Boolean = false) =
        navigate(route, replace = true, inherit = true, force = force)

    fun push(force: Boolean = false, transform: (T) -> T) =
        navigate(replace = false, inherit = false, force = force, transform)

    fun pushInherit(force: Boolean = false, transform: (T) -> T) =
        navigate(replace = false, inherit = true, force = force, transform)

    fun replace(force: Boolean = false, transform: (T) -> T) =
        navigate(replace = true, inherit = false, force = force, transform)

    fun replaceInherit(force: Boolean = false, transform: (T) -> T) =
        navigate(replace = true, inherit = true, force = force, transform)

    abstract fun <U> tangent(
        name: String,
        default: U,
        serializer: KSerializer<U>,
    ): NavController<U>

    inline fun <reified U : Any> tangent(name: String, default: U) =
        tangent(name, default, serializer<U>())

    inline fun <reified U : Any> tangent(default: U) =
        tangent(U::class.simpleName.orEmpty(), default, serializer<U>())

    @JvmName("tangent_nullable")
    inline fun <reified U> tangent(name: String, default: U? = null) =
        tangent(name, default, serializer<U?>())

    @JvmName("tangent_nullable")
    inline fun <reified U> tangent(default: U? = null) =
        tangent(U::class.simpleName.orEmpty(), default, serializer<U?>())
}
