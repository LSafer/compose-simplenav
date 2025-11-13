package net.lsafer.compose.simplenav.internal

import kotlin.reflect.KClass
import kotlin.reflect.KType

@PublishedApi
internal fun KType.inferTangentName(): String {
    val classOrNull = classifier as? KClass<*>
    return buildString {
        append(classOrNull?.simpleName ?: hashCode())
        arguments.forEach {
            append("_")
            append(it.type?.inferTangentName())
        }
    }
}
