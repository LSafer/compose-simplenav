package net.lsafer.compose.simplenav

import androidx.compose.runtime.*
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.jvm.JvmName

fun <T : Any> InMemoryNavController(
    default: T,
    tangents: NavTangentMap = emptyMap(),
) = InMemoryNavController(NavState(default, tangents))

@JvmName("InMemoryNavController_nullable")
fun <T> InMemoryNavController(
    default: T? = null,
    tangents: NavTangentMap = emptyMap(),
) = InMemoryNavController(NavState(default, tangents))

/**
 * Simple in-memory navigation controller.
 *
 * This behaves like a browser history but without persistence.
 * It is suitable for Compose Desktop, tests, previews, and headless logic.
 *
 * All operations are thread-safe via an atomicfu SynchronizedObject.
 */
class InMemoryNavController<T>(
    initialState: NavState<T>,
) : NavController<T>() {
    private val lock = SynchronizedObject()
    private val stateList = mutableStateListOf<NavState<T>>(initialState)
    private var position by mutableStateOf(0)

    override val state by derivedStateOf { stateList[position] }

    override val entries get() = stateList.toList()
    override val currentIndex get() = position

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

    override fun go(delta: Int) {
        synchronized(lock) {
            if (delta == 0) return
            if (delta > 0) {
                if (currentIndex == lastIndex) return
                val d = minOf(delta, lastIndex - currentIndex)
                position += d
            }
            if (delta < 0) {
                if (currentIndex == 0) return
                val d = maxOf(delta, -currentIndex)
                position += d
            }
        }
    }

    override fun edit(replace: Boolean, transform: (NavState<T>) -> NavState<T>?): Boolean {
        synchronized(lock) {
            val newState = transform(state) ?: return false

            if (replace) {
                stateList.removeRange(position + 1, stateList.size)
                stateList[position] = newState
            } else {
                stateList.removeRange(position + 1, stateList.size)
                stateList += newState
                position++
            }

            return true
        }
    }
}
