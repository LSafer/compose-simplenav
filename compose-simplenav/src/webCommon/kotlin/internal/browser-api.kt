@file:Suppress("RedundantNullableReturnType", "FunctionName")
@file:OptIn(ExperimentalWasmJsInterop::class)

package net.lsafer.compose.simplenav.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import net.lsafer.compose.simplenav.NavState
import kotlin.Boolean
import kotlin.Int
import kotlin.OptIn
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.map
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsArray
import kotlin.js.JsString
import kotlin.js.js
import kotlin.js.toList
import kotlin.text.substringAfterLast

internal fun windowNavigationSupported(): Boolean =
    js("window != null && window.navigation != null")

internal fun windowNavigationCurrentEntryIndex(): Int? =
    js("window.navigation.currentEntry.index")

internal fun windowNavigationEntriesUrls(): JsArray<JsString> =
    js("window.navigation.entries().map(function(it) { return it.url })")

@OptIn(ExperimentalWasmJsInterop::class)
internal fun <T> buildWindowHistory(
    defaultState: NavState<T>,
    serializer: KSerializer<T>,
    format: StringFormat,
): List<NavState<T>> {
    return windowNavigationEntriesUrls()
        .toList().map { url ->
            NavState.fromUrlSafeString(
                source = url.toString()
                    .substringAfterLast("#"),
                serializer = serializer,
                format = format,
            ) ?: defaultState
        }
}
