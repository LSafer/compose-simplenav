package net.lsafer.compose.simplenav

interface SimpleNavController<T : Any> {
    /**
     * The current route. Snapshot.
     */
    val current: T?

    fun push(route: T): Boolean
    fun replace(route: T): Boolean
    fun back(): Boolean
    fun forward(): Boolean
}
