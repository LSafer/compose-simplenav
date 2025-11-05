package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

inline fun <reified T : Any> InMemorySimpleNavController(default: T) =
    InMemorySimpleNavController(InMemorySimpleNavController.State(listOf(default)))

inline fun <reified T : Any> InMemorySimpleNavController(
    entries: List<T> = emptyList(),
    position: Int = entries.size - 1,
) = InMemorySimpleNavController(InMemorySimpleNavController.State(entries, position))

inline fun <reified T : Any> InMemorySimpleNavController(
    initialState: InMemorySimpleNavController.State<T> =
        InMemorySimpleNavController.State(),
) = InMemorySimpleNavController(initialState, serializer())

class InMemorySimpleNavController<T : Any>(
    initialState: State<T> = State(),
    private val stateSerializer: KSerializer<State<T>>,
) : SimpleNavController<T> {
    @Serializable
    data class State<T : Any>(
        val entries: List<T> = emptyList(),
        val position: Int = entries.size - 1,
    ) {
        val route: T? get() = entries.getOrNull(position)
    }

    @OptIn(InternalCoroutinesApi::class)
    private val lock = SynchronizedObject()

    var state by mutableStateOf<State<T>>(initialState)
        private set

    override val current by derivedStateOf { state.route }

    override fun push(route: T): Boolean {
        synchronized(lock) {
            val it = state
            if (route == it.route) return false
            state = State(
                entries = it.entries.take(it.position + 1) + route,
                position = it.position + 1,
            )
        }
        return true
    }

    override fun replace(route: T): Boolean {
        synchronized(lock) {
            val it = state
            if (route == it.route) return false
            state = State(
                entries = buildList {
                    addAll(it.entries)
                    set(it.position, route)
                },
                position = it.position,
            )
        }
        return true
    }

    override fun back(): Boolean {
        synchronized(lock) {
            val it = state
            if (it.position <= -1) return false
            state = it.copy(position = it.position - 1)
        }
        return true
    }

    override fun forward(): Boolean {
        synchronized(lock) {
            val it = state
            if (it.position >= it.entries.lastIndex) return false
            state = it.copy(position = it.position + 1)
        }
        return true
    }
}
