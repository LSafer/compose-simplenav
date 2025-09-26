package net.lsafer.compose.simplenav.internal

import kotlin.coroutines.cancellation.CancellationException

private external fun atob(input: String): String
private external fun btoa(input: String): String

private fun atobUrlSafe(input: String): String {
    val base64 = base64UrlSafeToBase64(input)
    return atob(base64)
}

private fun btoaUrlSafe(input: String): String {
    val base64 = btoa(input)
    return base64ToBase64UrlSafe(base64)
}

private fun base64UrlSafeToBase64(base64UrlSafe: String): String {
    return base64UrlSafe.replace("-", "+").replace("_", "/")
}

private fun base64ToBase64UrlSafe(base64: String): String {
    return base64.replace("+", "-").replace("/", "_")
}

@PublishedApi
internal fun String.encodeBase64UrlSafe(): String {
    return btoaUrlSafe(this)
}

@PublishedApi
internal fun String.decodeBase64UrlSafeToStringOrNull(): String? {
    return try {
        atobUrlSafe(this)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        null
    }
}
