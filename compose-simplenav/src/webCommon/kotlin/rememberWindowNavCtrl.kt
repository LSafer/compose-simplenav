package net.lsafer.compose.simplenav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json

/** Remember and install a web nav controller with the given initial parameters. */
@Composable
inline fun <reified T : Any> rememberWindowNavCtrl(
    default: T,
    tangents: NavTangentMap = emptyMap(),
    format: StringFormat = Json,
): NavController<T> {
    val navCtrl = remember(default, tangents, format) {
        WindowNavController(default, tangents, format)
    }

    DisposableEffect(navCtrl) {
        navCtrl.globalInstall()
        onDispose { navCtrl.globalUnInstall() }
    }

    return navCtrl
}

/** Remember and install a web nav controller with the given initial parameters. */
@Composable
inline fun <reified T> rememberWindowNavCtrl(
    default: T? = null,
    tangents: NavTangentMap = emptyMap(),
    format: StringFormat = Json,
): NavController<T?> {
    val navCtrl = remember(default, tangents, format) {
        WindowNavController(default, tangents, format)
    }

    DisposableEffect(navCtrl) {
        navCtrl.globalInstall()
        onDispose { navCtrl.globalUnInstall() }
    }

    return navCtrl
}
