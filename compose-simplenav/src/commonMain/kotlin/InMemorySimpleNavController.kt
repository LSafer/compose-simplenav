package net.lsafer.compose.simplenav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    override val stateSerializer: KSerializer<State<T>>,
) : SimpleNavController<T> {
    @Serializable
    data class State<T : Any>(
        val entries: List<T> = emptyList(),
        val position: Int = entries.size - 1,
    ) : SimpleNavController.State<T> {
        override val route: T? get() = entries.getOrNull(position)
    }

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<State<T>> = _state

    override fun push(route: T): Boolean {
        this._state.update {
            if (route == it.route) return false
            State(
                entries = it.entries.take(it.position + 1) + route,
                position = it.position + 1,
            )
        }
        return true
    }

    override fun replace(route: T): Boolean {
        this._state.update {
            if (route == it.route) return false
            State(
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
        this._state.update {
            if (it.position <= -1) return false
            it.copy(position = it.position - 1)
        }
        return true
    }

    override fun forward(): Boolean {
        this._state.update {
            if (it.position >= it.entries.lastIndex) return false
            it.copy(position = it.position + 1)
        }
        return true
    }
}
