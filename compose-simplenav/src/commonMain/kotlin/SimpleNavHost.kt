package net.lsafer.compose.simplenav

import androidx.compose.runtime.Composable

internal data class NavHostEntry<T : Any>(
    val condition: (T?) -> Boolean,
    val content: @Composable (T?) -> Unit,
)

class NavHostScope<T : Any> {
    internal var default: @Composable ((T?) -> Unit)? = null
    internal val mappings = mutableListOf<NavHostEntry<T>>()

    fun entry(condition: (T?) -> Boolean, content: @Composable (T?) -> Unit) {
        mappings += NavHostEntry(condition, content)
    }

    inline fun <reified U : T> entry(crossinline content: @Composable (U) -> Unit) {
        entry({ it is U }, { content(it as U) })
    }

    fun entry(value: T, content: @Composable (T) -> Unit) {
        entry({ it == value }, { content(it as T) })
    }

    fun default(content: @Composable (T?) -> Unit) {
        default = content
    }
}

@Composable
fun <T : Any> SimpleNavHost(
    navCtrl: SimpleNavController<T>,
    block: NavHostScope<T>.() -> Unit
) {
    val scope = NavHostScope<T>().apply(block)
    val current = navCtrl.current

    for (mapping in scope.mappings) {
        if (mapping.condition(current)) {
            mapping.content(current)
            return
        }
    }

    scope.default?.invoke(current)
}

// using the U : T shenanigan here due to this:
// https://youtrack.jetbrains.com/issue/KT-81365/Overload-resolution-ambiguity-between-candidates
@Composable
fun <T : Any, U : T> SimpleNavHost(
    current: T?,
    block: NavHostScope<U>.() -> Unit
) {
    @Suppress("UNCHECKED_CAST")
    block as NavHostScope<T>.() -> Unit
    val scope = NavHostScope<T>().apply(block)

    for (mapping in scope.mappings) {
        if (mapping.condition(current)) {
            mapping.content(current)
            return
        }
    }

    scope.default?.invoke(current)
}
