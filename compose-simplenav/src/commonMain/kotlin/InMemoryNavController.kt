package net.lsafer.compose.simplenav

import androidx.compose.runtime.*
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.serialization.KSerializer
import kotlin.jvm.JvmName

fun <T : Any> InMemoryNavController(
    default: T,
    tangents: Map<String, String> = emptyMap(),
) = InMemoryNavController(NavState(default, tangents))

@JvmName("InMemoryNavController_nullable")
fun <T> InMemoryNavController(
    default: T? = null,
    tangents: Map<String, String> = emptyMap(),
) = InMemoryNavController(NavState(default, tangents))

fun <T> InMemoryNavController(
    initialState: NavState<T>
): InMemoryNavController<T> {
    return InMemoryNavControllerImpl(initialState)
}

sealed class InMemoryNavController<T> : NavController<T>() {
    internal abstract val lock: SynchronizedObject
    internal abstract fun internalSetState(newState: NavState<T>, replace: Boolean)

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
                else -> NavState(newRoute)
            }

            internalSetState(newState, replace)
        }
        return true
    }

    override fun <U> tangent(
        name: String,
        default: U,
        serializer: KSerializer<U>,
    ): InMemoryNavController<U> {
        return InMemoryNavControllerTangent(
            outer = this,
            name = name,
            defaultState = NavState(default),
            stateSerializer = NavState.serializer(serializer),
        )
    }
}

internal class InMemoryNavControllerImpl<T>(
    initialState: NavState<T>,
) : InMemoryNavController<T>() {
    override val lock = SynchronizedObject()

    private val stateList = mutableStateListOf<NavState<T>>(initialState)
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

    override fun internalSetState(newState: NavState<T>, replace: Boolean) {
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

internal class InMemoryNavControllerTangent<T, U>(
    private val outer: InMemoryNavController<T>,
    private val name: String,
    private val defaultState: NavState<U>,
    private val stateSerializer: KSerializer<NavState<U>>,
) : InMemoryNavController<U>() {
    override val lock = outer.lock
    override val state by derivedStateOf {
        outer.state.getTangent(name, stateSerializer)
            ?: defaultState
    }

    override fun back() = outer.back()
    override fun forward() = outer.forward()

    override fun internalSetState(newState: NavState<U>, replace: Boolean) {
        val newOuterState = outer.state.withTangent(
            name = name,
            value = newState,
            serializer = stateSerializer,
        )
        outer.internalSetState(newOuterState, replace)
    }
}
