package net.lsafer.compose.simplenav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer

interface SimpleNavController<T : Any> {
    interface State<T : Any> {
        val route: T?
    }

    val stateSerializer: KSerializer<out State<T>>
    val state: StateFlow<State<T>>

    fun push(route: T): Boolean
    fun replace(route: T): Boolean
    fun back(): Boolean
    fun forward(): Boolean
}

val <T : Any> SimpleNavController<T>.current: T?
    @Composable get() = state.collectAsState().value.route
