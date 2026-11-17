package com.example.spartangymapp.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spartangymapp.network.ConfirmarPedidoRequest
import com.example.spartangymapp.network.PedidoResponse
import com.example.spartangymapp.network.ProductoCatalogoResponse
import com.example.spartangymapp.network.RetrofitClient
import com.example.spartangymapp.network.mensajeDeError
import kotlinx.coroutines.launch

private val P_CARD = Color(0xFF111111)
private val P_CAMPO = Color(0xFF181818)
private val P_BORDE = Color(0xFF242424)
private val P_GRIS = Color(0xFF9A9A9A)
private val P_VERDE = Color(0xFF22C55E)
private val P_AMBAR = Color(0xFFF5A524)

internal fun colorEstadoPedido(estado: String?) = when (estado) {
    "Pendiente" -> P_AMBAR
    "Completada" -> P_VERDE
    else -> P_GRIS
}

@Composable
private fun ChipOpcion(texto: String, activo: Boolean, acento: Color, onClick: () -> Unit) {
    Surface(
        Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (activo) acento.copy(alpha = .18f) else P_CAMPO,
        border = BorderStroke(1.dp, if (activo) acento.copy(alpha = .6f) else P_BORDE)
    ) {
        Text(texto, color = if (activo) Color.White else P_GRIS, fontSize = 13.sp, fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}

@Composable
private fun campoColores(acento: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedBorderColor = acento, unfocusedBorderColor = P_BORDE,
    focusedContainerColor = P_CAMPO, unfocusedContainerColor = P_CAMPO, cursorColor = acento
)

/** Resumen del carrito del socio: productos, total, forma de pago y botón para generar el código. */
@Composable
internal fun CarritoSocio(
    carrito: List<Pair<ProductoCatalogoResponse, Int>>,
    moneda: String?,
    tasaImpuesto: Double,
    acento: Color,
    enviando: Boolean,
    onCambiar: (ProductoCatalogoResponse, Int) -> Unit,
    onConfirmar: (metodo: String, referencia: String?) -> Unit
) {
    var metodo by rememberSaveable { mutableStateOf("Efectivo") }
    var referencia by rememberSaveable { mutableStateOf("") }
    val subtotal = carrito.sumOf { (p, n) -> (p.precio ?: 0.0) * n }
    val impuesto = subtotal * tasaImpuesto

    Surface(shape = RoundedCornerShape(18.dp), color = P_CARD, border = BorderStroke(1.dp, acento.copy(alpha = .45f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ShoppingCart, null, tint = acento, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tu carrito", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${carrito.sumOf { it.second }} artículos", color = P_GRIS, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            carrito.forEach { (p, n) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.nombre.orEmpty(), color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${precioTexto(p.precio, moneda)} c/u · ${precioTexto((p.precio ?: 0.0) * n, moneda)}", color = P_GRIS, fontSize = 12.sp)
                    }
                    IconButton(onClick = { onCambiar(p, -1) }) { Icon(Icons.Rounded.Remove, "Quitar uno", tint = P_GRIS) }
                    Text("$n", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onCambiar(p, 1) }, enabled = n < (p.stock ?: 0)) { Icon(Icons.Rounded.Add, "Agregar uno", tint = P_GRIS) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row { Text("Subtotal", color = P_GRIS, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text(precioTexto(subtotal, moneda), color = P_GRIS, fontSize = 13.sp) }
            if (impuesto > 0) Row { Text("Impuesto", color = P_GRIS, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text(precioTexto(impuesto, moneda), color = P_GRIS, fontSize = 13.sp) }
            Row(Modifier.padding(top = 4.dp)) {
                Text("Total", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(precioTexto(subtotal + impuesto, moneda), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(14.dp))
            Text("¿Cómo vas a pagar en recepción?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipOpcion("Efectivo", metodo == "Efectivo", acento) { metodo = "Efectivo" }
                ChipOpcion("Transferencia", metodo == "Transferencia", acento) { metodo = "Transferencia" }
            }
            if (metodo == "Transferencia") {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = referencia, onValueChange = { referencia = it }, singleLine = true,
                    label = { Text("Referencia de la transferencia (opcional)", color = P_GRIS) },
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp), shape = RoundedCornerShape(14.dp),
                    colors = campoColores(acento), modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onConfirmar(metodo, referencia.trim().ifBlank { null }) },
                enabled = carrito.isNotEmpty() && !enviando,
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = acento, disabledContainerColor = P_BORDE)
            ) {
                if (enviando) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("Generar código de pago", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text("Los productos se apartan para ti. Muestra el código en recepción para pagar y retirarlos.",
                color = P_GRIS, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** El código grande que el socio muestra en recepción, con el detalle del pedido. */
@Composable
internal fun TarjetaPedido(pedido: PedidoResponse, moneda: String?, acento: Color, destacado: Boolean = false) {
    Surface(shape = RoundedCornerShape(18.dp), color = P_CARD, border = BorderStroke(1.dp, if (destacado) acento.copy(alpha = .6f) else P_BORDE)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.QrCode2, null, tint = acento, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (destacado) "Tu código de pago" else "Pedido", color = P_GRIS, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(pedido.estado.orEmpty(), color = colorEstadoPedido(pedido.estado), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                pedido.codigoPedido.orEmpty(), color = Color.White, fontSize = if (destacado) 38.sp else 24.sp,
                fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = if (destacado) 10.dp else 4.dp)
            )
            pedido.detalles.orEmpty().forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text("${d.cantidad ?: 0} × ${d.productoNombre.orEmpty()}", color = Color(0xFFE4E4E7), fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(precioTexto(d.totalLinea, moneda), color = P_GRIS, fontSize = 13.sp)
                }
            }
            Row(Modifier.padding(top = 6.dp)) {
                Text("Total · ${pedido.metodoPago.orEmpty()}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(precioTexto(pedido.total, moneda), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            if (destacado && pedido.estado == "Pendiente") {
                Text(
                    if (pedido.metodoPago == "Transferencia") "Haz la transferencia y muestra este código en recepción para retirar."
                    else "Paga en efectivo en recepción mostrando este código.",
                    color = P_GRIS, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Busca el pedido por el código que muestra el socio y lo completa (cobro recibido) o lo cancela.
 * Lo usa el entrenador desde su celular; recepción y administración lo hacen en la web.
 */
@Composable
internal fun ValidarPedidoApp(moneda: String?, acento: Color) {
    val scope = rememberCoroutineScope()
    var codigo by rememberSaveable { mutableStateOf("") }
    var pedido by remember { mutableStateOf<PedidoResponse?>(null) }
    var recibido by rememberSaveable { mutableStateOf("") }
    var referencia by rememberSaveable { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }

    fun ejecutar(accion: suspend () -> retrofit2.Response<PedidoResponse>, exito: (PedidoResponse) -> String?) {
        ocupado = true; mensaje = null
        scope.launch {
            try {
                val resp = accion()
                val cuerpo = resp.body()
                if (!resp.isSuccessful || cuerpo == null) {
                    esError = true
                    mensaje = mensajeDeError(resp.errorBody()?.string()) ?: "No se pudo completar (${resp.code()})."
                } else {
                    pedido = cuerpo; esError = false; mensaje = exito(cuerpo)
                    referencia = cuerpo.referenciaPago.orEmpty()
                }
            } catch (e: Exception) {
                esError = true; mensaje = e.message ?: "Sin conexión con el servidor."
            } finally { ocupado = false }
        }
    }

    Surface(shape = RoundedCornerShape(18.dp), color = P_CARD, border = BorderStroke(1.dp, P_BORDE)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("Validar pedido de un socio", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Escribe el código que te muestra en su app.", color = P_GRIS, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = codigo, onValueChange = { codigo = it.uppercase().take(12) }, singleLine = true,
                    placeholder = { Text("P-7K3M9Q", color = P_GRIS) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                    shape = RoundedCornerShape(14.dp), colors = campoColores(acento), modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { recibido = ""; ejecutar({ RetrofitClient.apiService.buscarPedido(codigo.trim()) }) { null } },
                    enabled = codigo.isNotBlank() && !ocupado, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = acento)
                ) { Icon(Icons.Rounded.Search, "Buscar pedido") }
            }

            mensaje?.let {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!esError) { Icon(Icons.Rounded.CheckCircle, null, tint = P_VERDE, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
                    Text(it, color = if (esError) Color(0xFFFF8A8A) else P_VERDE, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            pedido?.let { p ->
                Spacer(Modifier.height(10.dp))
                Text("${p.clienteNombre.orEmpty()} · ${p.sucursal.orEmpty()}", color = P_GRIS, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                TarjetaPedido(p, moneda, acento)
                if (p.estado == "Pendiente") {
                    val total = p.total ?: 0.0
                    val recibidoNum = recibido.replace(',', '.').toDoubleOrNull()
                    Spacer(Modifier.height(10.dp))
                    if (p.metodoPago == "Efectivo") {
                        OutlinedTextField(
                            value = recibido, onValueChange = { recibido = it }, singleLine = true,
                            label = { Text("Efectivo recibido (opcional)", color = P_GRIS) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp), shape = RoundedCornerShape(14.dp),
                            colors = campoColores(acento), modifier = Modifier.fillMaxWidth()
                        )
                        recibidoNum?.let {
                            Text(if (it < total) "Faltan ${precioTexto(total - it, moneda)}" else "Cambio: ${precioTexto(it - total, moneda)}",
                                color = if (it < total) Color(0xFFFF8A8A) else P_VERDE, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp))
                        }
                    } else {
                        OutlinedTextField(
                            value = referencia, onValueChange = { referencia = it }, singleLine = true,
                            label = { Text("Referencia de la transferencia", color = P_GRIS) },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp), shape = RoundedCornerShape(14.dp),
                            colors = campoColores(acento), modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        OutlinedButton(
                            onClick = { ejecutar({ RetrofitClient.apiService.cancelarPedido(p.codigoPedido.orEmpty()) }) { "Pedido cancelado; el stock volvió al inventario." } },
                            enabled = !ocupado, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, P_BORDE)
                        ) { Text("Cancelar", color = P_GRIS) }
                        Button(
                            onClick = {
                                ejecutar({
                                    RetrofitClient.apiService.confirmarPedido(
                                        p.codigoPedido.orEmpty(),
                                        ConfirmarPedidoRequest(
                                            montoRecibido = if (p.metodoPago == "Efectivo") recibidoNum else null,
                                            referenciaPago = if (p.metodoPago != "Efectivo") referencia.trim().ifBlank { null } else null
                                        )
                                    )
                                }) { "Pedido completado. Entrega los productos." }
                            },
                            enabled = !ocupado && (recibidoNum == null || recibidoNum >= total - 0.005),
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = acento)
                        ) {
                            Text(if (p.metodoPago == "Efectivo") "Recibí el efectivo" else "Transferencia recibida", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
