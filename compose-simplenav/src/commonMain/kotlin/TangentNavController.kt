package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.lsafer.compose.simplenav.internal.inferTangentName
import kotlin.jvm.JvmName
import kotlin.reflect.typeOf

fun <U> NavController<*>.tangent(name: String, default: U, serializer: KSerializer<U>, format: StringFormat = Json) =
    TangentNavController(this, name, default, serializer, format)

inline fun <reified U : Any> NavController<*>.tangent(name: String, default: U, format: StringFormat = Json) =
    tangent(name, default, serializer<U>(), format)

inline fun <reified U : Any> NavController<*>.tangent(default: U, format: StringFormat = Json) =
    tangent(typeOf<U>().inferTangentName(), default, serializer<U>(), format)

@JvmName("tangent_nullable")
inline fun <reified U> NavController<*>.tangent(name: String, default: U? = null, format: StringFormat = Json) =
    tangent(name, default, serializer<U?>(), format)

@JvmName("tangent_nullable")
inline fun <reified U> NavController<*>.tangent(default: U? = null, format: StringFormat = Json) =
    tangent(typeOf<U>().inferTangentName(), default, serializer<U?>(), format)

/**
 * A proxy NavController that exposes one tangent inside a parent NavController.
 *
 * This acts like a small independent navigation controller but:
 * - it shares history with its parent
 * - its state is stored *inside* the parent's NavState
 * - backward/forward navigation is delegated
 *
 * Tangents are ideal for parallel UI state:
 * - selected tab
 * - filters
 * - currently expanded item
 * - map camera position
 *
 * @param name unique identifier for the tangent
 * @param default value to use when the tangent has not been created yet
 */
class TangentNavController<T>(
    outer: NavController<*>,
    private val name: String,
    private val default: T,
    private val serializer: KSerializer<T>,
    private val format: StringFormat = Json,
) : NavController<T>() {
    @Suppress("UNCHECKED_CAST")
    private val outer = outer as NavController<Any?>

    override val state by derivedStateOf {
        outer.state.getTangent(name, default, serializer, format)
    }

    override val length get() = outer.length
    override val currentIndex get() = outer.currentIndex

    override fun back() = outer.back()
    override fun forward() = outer.forward()
    override fun go(delta: Int) = outer.go(delta)

    override fun edit(replace: Boolean, transform: (NavState<T>) -> NavState<T>?): Boolean {
        return outer.edit(replace = replace) { outerState ->
            val newState = transform(outerState.getTangent(name, default, serializer, format))
            newState ?: return@edit null
            val newOuterState = outerState.withTangent(name, newState, serializer, format)
            return@edit newOuterState
        }
    }
}
