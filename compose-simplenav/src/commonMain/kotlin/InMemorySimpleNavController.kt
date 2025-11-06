package net.lsafer.compose.simplenav

import androidx.compose.runtime.*
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.KSerializer
import kotlin.jvm.JvmName

fun <T : Any> InMemorySimpleNavController(
    default: T,
    tangents: Map<String, String> = emptyMap(),
) = InMemorySimpleNavController(SimpleNavState(default, tangents))

@JvmName("InMemorySimpleNavController_nullable")
fun <T> InMemorySimpleNavController(
    default: T? = null,
    tangents: Map<String, String> = emptyMap(),
) = InMemorySimpleNavController(SimpleNavState(default, tangents))

fun <T> InMemorySimpleNavController(
    initialState: SimpleNavState<T>
): InMemorySimpleNavController<T> {
    return InMemorySimpleNavControllerImpl(initialState)
}

sealed class InMemorySimpleNavController<T> : SimpleNavController<T>() {
    internal abstract val lock: SynchronizedObject
    internal abstract fun internalSetState(newState: SimpleNavState<T>, replace: Boolean)

    override fun navigate(
        replace: Boolean,
        inherit: Boolean,
        force: Boolean,
        transform: (T) -> T,
    ): Boolean {
        synchronized(lock) {
            val current = state
            val newRoute = transform(current.route)

            if (!force && newRoute == current.route)
                return false

            val newState = when {
                inherit -> current.copy(route = newRoute)
                else -> SimpleNavState(newRoute)
            }

            internalSetState(newState, replace)
        }
        return true
    }

    override fun <U> tangent(
        name: String,
        default: U,
        serializer: KSerializer<U>,
    ): InMemorySimpleNavController<U> {
        return InMemorySimpleNavControllerTangent(
            outer = this,
            name = name,
            defaultState = SimpleNavState(default),
            stateSerializer = SimpleNavState.serializer(serializer),
        )
    }
}

internal class InMemorySimpleNavControllerImpl<T>(
    initialState: SimpleNavState<T>,
) : InMemorySimpleNavController<T>() {
    override val lock = SynchronizedObject()

    private val stateList = mutableStateListOf<SimpleNavState<T>>(initialState)
    private var position by mutableStateOf(0)

    override val state by derivedStateOf { stateList[position] }

    override fun back(): Boolean {
        synchronized(lock) {
            if (position <= 0)
                return false

            position--
        }
        return true
    }

    override fun forward(): Boolean {
        synchronized(lock) {
            if (position >= stateList.lastIndex)
                return false

            position++
        }
        return true
    }

    override fun internalSetState(newState: SimpleNavState<T>, replace: Boolean) {
        if (replace) {
            stateList.removeRange(position + 1, stateList.size)
            stateList[position] = newState
        } else {
            stateList.removeRange(position + 1, stateList.size)
            stateList += newState
            position++
        }
    }
}

internal class InMemorySimpleNavControllerTangent<T, U>(
    private val outer: InMemorySimpleNavController<T>,
    private val name: String,
    private val defaultState: SimpleNavState<U>,
    private val stateSerializer: KSerializer<SimpleNavState<U>>,
) : InMemorySimpleNavController<U>() {
    override val lock = outer.lock
    override val state by derivedStateOf {
        outer.state.getTangent(name, stateSerializer)
            ?: defaultState
    }

    override fun back() = outer.back()
    override fun forward() = outer.forward()

    override fun internalSetState(newState: SimpleNavState<U>, replace: Boolean) {
        val newOuterState = outer.state.withTangent(
            name = name,
            value = newState,
            serializer = stateSerializer,
        )
        outer.internalSetState(newOuterState, replace)
    }
}
