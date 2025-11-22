package net.lsafer.compose.simplenav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmName

/** Remember an in-memory nav controller with the given initial parameters. */
@Composable
fun <T : Any> rememberNavCtrl(
    default: T,
    tangents: NavTangentMap = emptyMap(),
): NavController<T> = remember(default, tangents) {
    InMemoryNavController(default, tangents)
}

/** Remember an in-memory nav controller with the given initial parameters. */
@Composable
@JvmName("rememberNavCtrl_nullable")
fun <T> rememberNavCtrl(
    default: T? = null,
    tangents: NavTangentMap = emptyMap(),
): NavController<T?> = remember(default, tangents) {
    InMemoryNavController(default, tangents)
}

//

/** Remember a tangent nav controller of [navCtrl] with the given initial parameters. */
@Composable
inline fun <reified T : Any> rememberNavCtrl(
    navCtrl: NavController<*>,
    default: T,
    format: StringFormat = Json,
): NavController<T> = remember(navCtrl, default, format) {
    navCtrl.tangent(default, format)
}

/** Remember a tangent nav controller of [navCtrl] with the given initial parameters. */
@Composable
@JvmName("rememberNavCtrl_nullable")
inline fun <reified T> rememberNavCtrl(
    navCtrl: NavController<*>,
    default: T? = null,
    format: StringFormat = Json,
): NavController<T?> = remember(navCtrl, default, format) {
    navCtrl.tangent(default, format)
}

//

/** Remember a tangent nav controller of [navCtrl] with the given initial parameters. */
@Composable
inline fun <reified T : Any> rememberNavCtrl(
    navCtrl: NavController<*>,
    name: String,
    default: T,
    format: StringFormat = Json,
): NavController<T> = remember(navCtrl, name, default, format) {
    navCtrl.tangent(name, default, format)
}

/** Remember a tangent nav controller of [navCtrl] with the given initial parameters. */
@Composable
@JvmName("rememberNavCtrl_nullable")
inline fun <reified T> rememberNavCtrl(
    navCtrl: NavController<*>,
    name: String,
    default: T? = null,
    format: StringFormat = Json,
): NavController<T?> = remember(navCtrl, name, default, format) {
    navCtrl.tangent(name, default, format)
}
