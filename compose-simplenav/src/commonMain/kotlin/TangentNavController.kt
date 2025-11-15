package net.lsafer.compose.simplenav

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.serialization.KSerializer

class TangentNavController<T>(
    outer: NavController<*>,
    private val name: String,
    private val default: T,
    private val serializer: KSerializer<T>,
) : NavController<T>() {
    @Suppress("UNCHECKED_CAST")
    private val outer = outer as NavController<Any?>

    override val state by derivedStateOf {
        outer.state.getTangent(name, default, serializer)
    }

    override val length get() = outer.length
    override val currentIndex get() = outer.currentIndex

    override fun back() = outer.back()
    override fun forward() = outer.forward()
    override fun go(delta: Int) = outer.go(delta)

    override fun edit(replace: Boolean, transform: (NavState<T>) -> NavState<T>?): Boolean {
        return outer.edit(replace = replace) { outerState ->
            val newState = transform(outerState.getTangent(name, default, serializer))
            newState ?: return@edit null
            val newOuterState = outerState.withTangent(name, newState, serializer)
            return@edit newOuterState
        }
    }
}
