package com.example.spartangymapp.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spartangymapp.network.AppConfigResponse
import com.example.spartangymapp.network.CompraProductoAppRequest
import com.example.spartangymapp.network.ProductoCatalogoResponse
import com.example.spartangymapp.network.RetrofitClient
import com.example.spartangymapp.network.SocioResumenResponse
import com.example.spartangymapp.network.VentaEntrenadorRequest
import com.example.spartangymapp.network.mensajeDeError
import kotlinx.coroutines.launch

private val T_CARD = Color(0xFF111111)
private val T_CAMPO = Color(0xFF181818)
private val T_BORDE = Color(0xFF242424)
private val T_GRIS = Color(0xFF9A9A9A)
private val T_VERDE = Color(0xFF22C55E)

internal fun simboloDeMoneda(codigo: String?): String = when (codigo?.trim()?.uppercase()) {
    "NIO", "C$", "CORDOBA", "CÓRDOBA" -> "C$"
    "EUR" -> "€"
    else -> "$"
}

internal fun precioTexto(valor: Double?, moneda: String?) = "${simboloDeMoneda(moneda)}${"%.2f".format(valor ?: 0.0)}"

/** Productos que coinciden por nombre o algo relacionado (categoría, descripción). */
internal fun filtrarProductos(productos: List<ProductoCatalogoResponse>, consulta: String?): List<ProductoCatalogoResponse> {
    val terminos = terminosBusqueda(consulta)
    if (terminos.isEmpty()) return productos
    return productos.filter { p ->
        val texto = normalizarBusqueda(listOfNotNull(p.nombre, p.categoria, p.descripcion).joinToString(" "))
        terminos.all { texto.contains(it) }
    }
}

/** Persona que se puede elegir en un buscador (socio, cliente). */
internal data class PersonaBuscable(val id: String, val nombre: String, val detalle: String?)

internal fun SocioResumenResponse.comoPersona() = id?.let {
    PersonaBuscable(it, listOfNotNull(nombres, apellidos).joinToString(" ").ifBlank { email ?: "Socio" }, email ?: telefono)
}

/** Sin texto no devuelve a nadie: la lista solo aparece al buscar. */
internal fun filtrarPersonas(personas: List<PersonaBuscable>, consulta: String?): List<PersonaBuscable> {
    val terminos = terminosBusqueda(consulta)
    if (terminos.isEmpty()) return emptyList()
    return personas.filter { p ->
        val texto = normalizarBusqueda("${p.nombre} ${p.detalle.orEmpty()}")
        terminos.all { texto.contains(it) }
    }.sortedBy { normalizarBusqueda(it.nombre) }
}

@Composable
internal fun CampoBusqueda(
    valor: String,
    onValor: (String) -> Unit,
    placeholder: String,
    acento: Color,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = T_GRIS) },
        trailingIcon = {
            if (valor.isNotEmpty()) IconButton(onClick = { onValor("") }) { Icon(Icons.Rounded.Close, "Borrar búsqueda", tint = T_GRIS) }
        },
        placeholder = { Text(placeholder, color = T_GRIS, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        textStyle = TextStyle(fontSize = 15.sp, color = Color.White),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = acento, unfocusedBorderColor = T_BORDE,
            focusedContainerColor = T_CAMPO, unfocusedContainerColor = T_CAMPO, cursorColor = acento
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Buscador de personas: muestra la elegida o un campo; los resultados aparecen solo al
 * escribir y se limitan a unos pocos para no llenar la pantalla.
 */
@Composable
internal fun BuscadorPersona(
    personas: List<PersonaBuscable>,
    seleccionadoId: String?,
    acento: Color,
    placeholder: String = "Busca por nombre, correo o teléfono",
    onElegir: (PersonaBuscable?) -> Unit
) {
    var consulta by rememberSaveable { mutableStateOf("") }
    val elegido = personas.firstOrNull { it.id == seleccionadoId }
    if (elegido != null) {
        Surface(shape = RoundedCornerShape(14.dp), color = T_CAMPO, border = BorderStroke(1.dp, acento.copy(alpha = .5f))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Person, null, tint = acento, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(elegido.nombre, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    elegido.detalle?.let { Text(it, color = T_GRIS, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                IconButton(onClick = { onElegir(null) }) { Icon(Icons.Rounded.Close, "Cambiar", tint = T_GRIS) }
            }
        }
        return
    }

    CampoBusqueda(consulta, { consulta = it }, placeholder, acento)
    val resultados = filtrarPersonas(personas, consulta)
    if (consulta.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = T_CARD, border = BorderStroke(1.dp, T_BORDE)) {
            Column(Modifier.fillMaxWidth()) {
                resultados.take(6).forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { consulta = ""; onElegir(p) }.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Person, null, tint = T_GRIS, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.nombre, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            p.detalle?.let { Text(it, color = T_GRIS, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
                Text(
                    when {
                        resultados.isEmpty() -> "Nadie coincide con «$consulta»."
                        resultados.size > 6 -> "Mostrando 6 de ${resultados.size}. Escribe más para afinar."
                        else -> "${resultados.size} ${if (resultados.size == 1) "resultado" else "resultados"}"
                    },
                    color = T_GRIS, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

/** Productos en bloques de dos columnas. */
@Composable
internal fun CuadriculaProductos(
    productos: List<ProductoCatalogoResponse>,
    moneda: String?,
    acento: Color,
    textoAccion: (ProductoCatalogoResponse) -> String,
    ocupadoId: String? = null,
    cantidadEn: (ProductoCatalogoResponse) -> Int = { 0 },
    onAccion: (ProductoCatalogoResponse) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        productos.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEach { p ->
                    TarjetaProducto(p, moneda, acento, textoAccion(p), ocupadoId == p.id, cantidadEn(p), Modifier.weight(1f)) { onAccion(p) }
                }
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TarjetaProducto(
    p: ProductoCatalogoResponse,
    moneda: String?,
    acento: Color,
    textoAccion: String,
    ocupado: Boolean,
    enCarrito: Int,
    modifier: Modifier,
    onAccion: () -> Unit
) {
    val agotado = (p.stock ?: 0) <= 0
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = T_CARD, border = BorderStroke(1.dp, if (enCarrito > 0) acento.copy(alpha = .6f) else T_BORDE)) {
        Column(Modifier.padding(10.dp)) {
            Box {
                ProductoImagen(
                    imagenUrl = p.imagenUrl,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A))
                )
                if (enCarrito > 0) {
                    Surface(shape = RoundedCornerShape(50), color = acento, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                        Text("$enCarrito", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(p.nombre.orEmpty(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp, modifier = Modifier.heightIn(min = 34.dp))
            Text(p.categoria.orEmpty(), color = T_GRIS, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(precioTexto(p.precio, moneda), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text(if (agotado) "Agotado" else "${p.stock} disp.", color = if (agotado) T_GRIS else T_VERDE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onAccion,
                enabled = !agotado && !ocupado,
                modifier = Modifier.fillMaxWidth().height(38.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = acento, disabledContainerColor = T_BORDE)
            ) {
                if (ocupado) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                else Text(if (agotado) "Agotado" else textoAccion, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Chip(texto: String, activo: Boolean, acento: Color, onClick: () -> Unit) {
    Surface(
        Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (activo) acento.copy(alpha = .18f) else T_CAMPO,
        border = BorderStroke(1.dp, if (activo) acento.copy(alpha = .6f) else T_BORDE)
    ) {
        Text(texto, color = if (activo) Color.White else T_GRIS, fontSize = 13.sp, fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

/**
 * Venta desde la app del entrenador. Los productos aparecen al buscar o al tocar una categoría;
 * el cliente es un socio (buscador) o un cliente eventual. La API factura en su sucursal.
 */
@Composable
internal fun TabVenderEntrenador(appConfig: AppConfigResponse, acento: Color) {
    val scope = rememberCoroutineScope()
    var productos by remember { mutableStateOf<List<ProductoCatalogoResponse>>(emptyList()) }
    var socios by remember { mutableStateOf<List<PersonaBuscable>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var consulta by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf<String?>(null) }
    val carrito = remember { mutableStateListOf<Pair<ProductoCatalogoResponse, Int>>() }
    var eventual by rememberSaveable { mutableStateOf(false) }
    var socioId by rememberSaveable { mutableStateOf<String?>(null) }
    var nombreEventual by rememberSaveable { mutableStateOf("") }
    var metodo by rememberSaveable { mutableStateOf("Efectivo") }
    var recibido by rememberSaveable { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }

    suspend fun cargar() {
        try {
            productos = RetrofitClient.apiService.listarProductosCatalogo().body().orEmpty()
            socios = RetrofitClient.apiService.listarSocios().body().orEmpty().mapNotNull { it.comoPersona() }
        } catch (e: Exception) {
            esError = true; mensaje = e.message ?: "No se pudieron cargar los productos."
        } finally { cargando = false }
    }
    LaunchedEffect(Unit) { cargar() }

    val impuesto = (appConfig.taxRate?.toDoubleOrNull() ?: 0.0) / 100.0
    val subtotal = carrito.sumOf { (p, n) -> (p.precio ?: 0.0) * n }
    val total = subtotal * (1 + impuesto)
    val categorias = productos.mapNotNull { it.categoria }.distinct().sorted()
    val buscando = consulta.isNotBlank() || categoria != null
    val visibles = if (!buscando) emptyList() else filtrarProductos(productos, consulta)
        .filter { categoria == null || categoria == "Todo" || it.categoria == categoria }

    fun cantidadEn(p: ProductoCatalogoResponse) = carrito.firstOrNull { it.first.id == p.id }?.second ?: 0
    fun cambiar(p: ProductoCatalogoResponse, delta: Int) {
        val i = carrito.indexOfFirst { it.first.id == p.id }
        val nueva = ((if (i >= 0) carrito[i].second else 0) + delta).coerceIn(0, p.stock ?: 0)
        when {
            i < 0 && nueva > 0 -> carrito.add(p to nueva)
            i >= 0 && nueva == 0 -> carrito.removeAt(i)
            i >= 0 -> carrito[i] = p to nueva
        }
        mensaje = null
    }

    val recibidoNum = recibido.replace(',', '.').toDoubleOrNull()
    val listo = carrito.isNotEmpty() && !guardando &&
        (if (eventual) nombreEventual.isNotBlank() else socioId != null) &&
        (metodo != "Efectivo" || (recibidoNum != null && recibidoNum >= total - 0.005))

    fun vender() {
        guardando = true; mensaje = null
        scope.launch {
            try {
                val resp = RetrofitClient.apiService.venderComoEntrenador(
                    VentaEntrenadorRequest(
                        clienteEventual = eventual,
                        socioId = if (eventual) null else socioId,
                        clienteNombre = if (eventual) nombreEventual.trim() else null,
                        metodoPago = metodo,
                        montoRecibido = if (metodo == "Efectivo") recibidoNum else null,
                        detalles = carrito.mapNotNull { (p, n) -> p.id?.let { CompraProductoAppRequest(it, n) } }
                    )
                )
                val cuerpo = resp.body()
                if (!resp.isSuccessful || cuerpo == null) {
                    esError = true
                    mensaje = mensajeDeError(resp.errorBody()?.string()) ?: "No se pudo registrar la venta (${resp.code()})."
                } else {
                    esError = false
                    val cambio = cuerpo.cambio?.takeIf { it > 0 }?.let { " · Cambio ${precioTexto(it, appConfig.currency)}" }.orEmpty()
                    mensaje = "Venta registrada. Factura ${cuerpo.numeroFactura.orEmpty()} por ${precioTexto(cuerpo.total, appConfig.currency)}$cambio"
                    carrito.clear(); recibido = ""; nombreEventual = ""; socioId = null
                    cargar()
                }
            } catch (e: Exception) {
                esError = true; mensaje = e.message ?: "No se pudo registrar la venta."
            } finally { guardando = false }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text("Vender productos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Se factura en tu sucursal y descuenta el inventario.", color = T_GRIS, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))

        ValidarPedidoApp(appConfig.currency, acento)
        Spacer(Modifier.height(18.dp))
        Text("Venta directa", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        mensaje?.let {
            Surface(shape = RoundedCornerShape(14.dp), color = if (esError) Color(0xFF1A0608) else Color(0xFF0D2B18),
                border = BorderStroke(1.dp, (if (esError) acento else T_VERDE).copy(alpha = .4f)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!esError) { Icon(Icons.Rounded.CheckCircle, null, tint = T_VERDE, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
                    Text(it, color = if (esError) Color(0xFFFF8A8A) else T_VERDE, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        CampoBusqueda(consulta, { consulta = it }, "Busca: proteína, agua, shaker…", acento)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("Todo") + categorias).forEach { c ->
                Chip(if (c == "Todo") "Ver todo" else c, categoria == c, acento) { categoria = if (categoria == c) null else c }
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            cargando -> CircularProgressIndicator(color = acento, strokeWidth = 2.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
            productos.isEmpty() -> Text("Tu sucursal aún no tiene productos.", color = T_GRIS, fontSize = 13.sp)
            !buscando -> Text("Busca un producto por nombre o elige una categoría. Hay ${productos.size} disponibles.", color = T_GRIS, fontSize = 13.sp)
            visibles.isEmpty() -> Text("Ningún producto coincide. Prueba con otra palabra.", color = T_GRIS, fontSize = 13.sp)
            else -> CuadriculaProductos(visibles, appConfig.currency, acento, { if (cantidadEn(it) > 0) "Agregar otro" else "Agregar" },
                cantidadEn = ::cantidadEn) { cambiar(it, 1) }
        }

        if (carrito.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = T_CARD, border = BorderStroke(1.dp, T_BORDE)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ShoppingCart, null, tint = acento, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cobro", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    carrito.toList().forEach { (p, n) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.nombre.orEmpty(), color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(precioTexto((p.precio ?: 0.0) * n, appConfig.currency), color = T_GRIS, fontSize = 12.sp)
                            }
                            IconButton(onClick = { cambiar(p, -1) }) { Icon(Icons.Rounded.Remove, "Quitar uno", tint = T_GRIS) }
                            Text("$n", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { cambiar(p, 1) }, enabled = n < (p.stock ?: 0)) { Icon(Icons.Rounded.Add, "Agregar uno", tint = T_GRIS) }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Socio", !eventual, acento) { eventual = false }
                        Chip("Cliente eventual", eventual, acento) { eventual = true; socioId = null }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (eventual) {
                        OutlinedTextField(
                            value = nombreEventual, onValueChange = { nombreEventual = it }, singleLine = true,
                            placeholder = { Text("Nombre del cliente", color = T_GRIS) },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp), shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = acento, unfocusedBorderColor = T_BORDE,
                                focusedContainerColor = T_CAMPO, unfocusedContainerColor = T_CAMPO, cursorColor = acento),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        BuscadorPersona(socios, socioId, acento) { socioId = it?.id }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("Efectivo", "Tarjeta", "Transferencia").forEach { m -> Chip(m, metodo == m, acento) { metodo = m } }
                    }
                    if (metodo == "Efectivo") {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = recibido, onValueChange = { recibido = it }, singleLine = true,
                            label = { Text("Recibido (${simboloDeMoneda(appConfig.currency)})", color = T_GRIS) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp), shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = acento, unfocusedBorderColor = T_BORDE,
                                focusedContainerColor = T_CAMPO, unfocusedContainerColor = T_CAMPO, cursorColor = acento),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (recibidoNum != null && recibidoNum >= total) {
                            Text("Cambio: ${precioTexto(recibidoNum - total, appConfig.currency)}", color = T_VERDE, fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row { Text("Subtotal", color = T_GRIS, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text(precioTexto(subtotal, appConfig.currency), color = T_GRIS, fontSize = 13.sp) }
                    Row { Text("Total con impuesto", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(precioTexto(total, appConfig.currency), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = ::vender, enabled = listo, modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = acento, disabledContainerColor = T_BORDE)
                    ) {
                        if (guardando) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Text("Registrar venta", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
