package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.lsafer.compose.simplenav.NavState.Companion.decoded
import net.lsafer.compose.simplenav.internal.inferTangentName
import kotlin.jvm.JvmName
import kotlin.math.absoluteValue
import kotlin.reflect.typeOf

fun <U> NavController<*>.tangent(name: String, default: U, serializer: KSerializer<U>, format: StringFormat = Json) =
    TangentNavController(this, name, NavState(default), serializer, format)

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
 * - adjacent duplicates are automatically merged
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
 * @param defaultState value to use when the tangent has not been created yet
 */
class TangentNavController<T>(
    val outer: NavController<*>,
    val name: String,
    private val defaultState: NavState<T>,
    private val serializer: KSerializer<T>,
    private val format: StringFormat = Json,
) : NavController<T>() {
    val root: NavController<*> = if (outer is TangentNavController<*>) outer.root else outer

    override val entries by derivedStateOf {
        buildList<NavState<T>> {
            var prevRawState: Any? = Any()

            for (entry in outer.entries) {
                val rawState = entry.tangents[name]
                if (prevRawState == rawState)
                    continue

                prevRawState = rawState
                add(rawState?.decoded(serializer, format) ?: defaultState)
            }
        }
    }

    override val state by derivedStateOf {
        outer.state.tangents[name]
            ?.decoded(serializer, format)
            ?: defaultState
    }

    override val currentIndex by derivedStateOf it@{
        var innerSteps = 0

        var prevRawState = outer.state.tangents[name]
        for (entry in outer.backStack) {
            val rawState = entry.tangents[name]
            if (prevRawState == rawState)
                continue

            innerSteps++
            prevRawState = rawState
        }

        innerSteps
    }

    override fun back(): Boolean {
        var outerSteps = 0
        val currentRawState = outer.state.tangents[name]

        for (entry in outer.backStack) {
            outerSteps++

            val rawState = entry.tangents[name]
            if (rawState == currentRawState)
                continue

            outer.go(-outerSteps)
            return true
        }

        return false
    }

    override fun forward(): Boolean {
        var outerSteps = 0
        val currentRawState = outer.state.tangents[name]

        for (entry in outer.forwardStack) {
            outerSteps++

            val rawState = entry.tangents[name]
            if (rawState == currentRawState)
                continue

            outer.go(outerSteps)
            return true
        }

        return false
    }

    override fun go(delta: Int): Int {
        if (delta == 0) return 0

        val targetInnerSteps = delta.absoluteValue
        var innerSteps = 0
        var outerSteps = 0
        var prevRawState = outer.state.tangents[name]
        var prevRawStateOuterSteps = 0

        if (delta > 0) {
            if (outer.forwardStack.isEmpty())
                return 0

            for (entry in outer.forwardStack) {
                outerSteps++

                val rawState = entry.tangents[name]
                if (rawState == prevRawState)
                    continue

                innerSteps++

                if (innerSteps == targetInnerSteps) {
                    outer.go(outerSteps)
                    return innerSteps
                }

                prevRawState = rawState
                prevRawStateOuterSteps = outerSteps
            }

            outer.go(prevRawStateOuterSteps)
            return innerSteps
        }
        if (delta < 0) {
            if (outer.backStack.isEmpty())
                return 0

            for (entry in outer.backStack) {
                outerSteps++

                val rawState = entry.tangents[name]
                if (rawState == prevRawState)
                    continue

                innerSteps++

                if (innerSteps == targetInnerSteps) {
                    outer.go(-outerSteps)
                    return -innerSteps
                }

                prevRawState = rawState
                prevRawStateOuterSteps = outerSteps
            }

            outer.go(-prevRawStateOuterSteps)
            return -innerSteps
        }

        return 0 // <-- this is unreachable
    }

    override fun edit(replace: Boolean, transform: (NavState<T>) -> NavState<T>?): Boolean {
        @Suppress("UNCHECKED_CAST")
        outer as NavController<Any?>
        return outer.edit(replace = replace) { outerState ->
            val newState = transform(outerState.getTangent(name, serializer, format) ?: defaultState)
            newState ?: return@edit null
            val newOuterState = outerState.withTangent(name, newState, serializer, format)
            return@edit newOuterState
        }
    }
}
