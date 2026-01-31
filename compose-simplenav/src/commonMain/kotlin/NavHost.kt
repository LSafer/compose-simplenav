package net.lsafer.compose.simplenav

import androidx.compose.runtime.Composable

internal data class NavHostEntry<T>(
    val condition: (T) -> Boolean,
    val content: @Composable (T) -> Unit,
)

@DslMarker
annotation class NavHostDsl

@NavHostDsl
class NavHostScope<T> {
    internal var default: @Composable ((T) -> Unit)? = null
    internal val mappings = mutableListOf<NavHostEntry<T>>()
}

@Composable
fun <T> NavHost(
    navCtrl: NavController<T>,
    block: context(NavHostScope<T>) () -> Unit
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
fun <T, U : T> NavHost(
    current: T,
    block: context(NavHostScope<U>) () -> Unit
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

@NavHostDsl
context(ctx: NavHostScope<T>)
fun <T> entry(condition: (T) -> Boolean, content: @Composable (T) -> Unit) {
    ctx.mappings += NavHostEntry(condition, content)
}

@NavHostDsl
context(ctx: NavHostScope<T>)
fun <T> entryScope(condition: (T) -> Boolean, content: @Composable context(T) () -> Unit) {
    ctx.mappings += NavHostEntry(condition, content)
}

@NavHostDsl
context(ctx: NavHostScope<in T>)
inline fun <reified T> entry(crossinline content: @Composable (T) -> Unit) {
    entry({ it is T }, { content(it as T) })
}

@NavHostDsl
context(ctx: NavHostScope<in T>)
inline fun <reified T> entryScope(crossinline content: @Composable T.() -> Unit) {
    entry({ it is T }, { content(it as T) })
}

@NavHostDsl
context(ctx: NavHostScope<T>)
inline fun <T> entry(value: T, crossinline content: @Composable (T) -> Unit) {
    entry({ it == value }, { content(it) })
}

@NavHostDsl
context(ctx: NavHostScope<T>)
inline fun <T> entryScope(value: T, crossinline content: @Composable T.() -> Unit) {
    entry({ it == value }, { content(it) })
}

@NavHostDsl
context(ctx: NavHostScope<T>)
fun <T> default(content: @Composable (T) -> Unit) {
    ctx.default = content
}

@NavHostDsl
context(ctx: NavHostScope<T>)
fun <T> defaultScope(content: @Composable T.() -> Unit) {
    ctx.default = content
}
