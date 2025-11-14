@file:Suppress("RedundantNullableReturnType", "FunctionName")
@file:OptIn(ExperimentalWasmJsInterop::class)

package net.lsafer.compose.simplenav.internal

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

internal fun windowNavigationSupported(): Boolean =
    js("window != null && window.navigation != null")

internal fun windowNavigationCurrentEntryIndex(): Int? =
    js("window.navigation.currentEntry.index")

internal fun windowNavigationEntriesLength(): Int? =
    js("window.navigation.entries().length")
