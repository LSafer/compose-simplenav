package net.lsafer.compose.simplenav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmName

/** Remember a tangent nav controller of [navCtrl] with the given initial parameters. */
@Composable
context(navCtrl: NavController<*>)
inline fun <reified T : Any> rememberSubNavCtrl(
    default: T,
    format: StringFormat = Json,
): NavController<T> = remember(navCtrl, default, format) {
    navCtrl.tangent(default, format)
}

/** Remember a tangent nav controller of [navCtrl] with the given initial parameters. */
@Composable
@JvmName("rememberSubNavCtrl_nullable")
context(navCtrl: NavController<*>)
inline fun <reified T> rememberSubNavCtrl(
    default: T? = null,
    format: StringFormat = Json,
): NavController<T?> = remember(navCtrl, default, format) {
    navCtrl.tangent(default, format)
}

//

/** Remember a tangent nav controller of [navCtrl] with the given initial parameters. */
@Composable
context(navCtrl: NavController<*>)
inline fun <reified T : Any> rememberSubNavCtrl(
    name: String,
    default: T,
    format: StringFormat = Json,
): NavController<T> = remember(navCtrl, name, default, format) {
    navCtrl.tangent(name, default, format)
}

/** Remember a tangent nav controller of [navCtrl] with the given initial parameters. */
@Composable
@JvmName("rememberSubNavCtrl_nullable")
context(navCtrl: NavController<*>)
inline fun <reified T> rememberSubNavCtrl(
    name: String,
    default: T? = null,
    format: StringFormat = Json,
): NavController<T?> = remember(navCtrl, name, default, format) {
    navCtrl.tangent(name, default, format)
}
