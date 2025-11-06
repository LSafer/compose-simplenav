package net.lsafer.compose.simplenav

import androidx.compose.runtime.*
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.KSerializer

fun <T : Any> InMemorySimpleNavController(
    default: T,
    tangents: Map<String, String> = emptyMap(),
) = InMemorySimpleNavController(SimpleNavState(default, tangents))

fun <T : Any> InMemorySimpleNavController(
    initialState: SimpleNavState<T>
): InMemorySimpleNavController<T> {
    return InMemorySimpleNavControllerImpl(initialState)
}

sealed class InMemorySimpleNavController<T : Any> : SimpleNavController<T>() {
    internal abstract val lock: SynchronizedObject
    internal abstract fun internalSetState(newState: SimpleNavState<T>, replace: Boolean)

    override fun navigate(
        route: T,
        replace: Boolean,
        inherit: Boolean,
    ): Boolean {
        synchronized(lock) {
            val current = state

            if (route == current.route)
                return false

            val newState = when {
                inherit -> current.copy(route = route)
                else -> SimpleNavState(route)
            }

            internalSetState(newState, replace)
        }
        return true
    }

    override fun <U : Any> tangent(
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

internal class InMemorySimpleNavControllerImpl<T : Any>(
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

internal class InMemorySimpleNavControllerTangent<T : Any, U : Any>(
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
