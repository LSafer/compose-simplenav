package net.lsafer.compose.simplenav.internal

import kotlinx.browser.window

@PublishedApi
internal fun String.encodeBase64UrlSafe(): String {
    val str = this
    val b64 = window.btoa(str)
    val b64u = b64.replace("+", "-").replace("/", "_")
    return b64u
}

@PublishedApi
internal fun String.decodeBase64UrlSafeToStringOrNull(): String? {
    val b64u = this
    val b64 = b64u.replace("-", "+").replace("_", "/")
    val str = runCatching { window.atob(b64) }.getOrElse { return null }
    return str
}
