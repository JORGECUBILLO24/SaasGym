package com.saasgym.app.ui.screen

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.saasgym.app.BuildConfig
import com.saasgym.app.ui.theme.FuenteTitulo

/** Dato que se muestra bajo el GIF: "4" / "series". */
internal data class DatoEjercicio(val valor: String, val etiqueta: String)

private const val PASOS_VISIBLES = 3

/**
 * Hoja con el GIF de demostración que sirve la API (/gifs/<codigo>.gif), los datos
 * que asignó el entrenador, la técnica paso a paso y el check para confirmar el ejercicio.
 * El check queda fijo abajo para no tener que desplazarse en pleno entrenamiento.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun DemostracionEjercicio(
    nombre: String,
    gifUrl: String?,
    datos: List<DatoEjercicio>,
    instrucciones: List<String>,
    completado: Boolean? = null,
    onToggleCompletado: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val cargadorGif = rememberCargadorGif()
    val urlCompleta = remember(gifUrl) { urlDeGif(gifUrl) }
    val pasos = remember(instrucciones) { pasosUtiles(instrucciones) }
    var gifAmpliado by remember { mutableStateOf(false) }
    var verTodosLosPasos by remember { mutableStateOf(false) }
    val acento = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF111114),
        contentColor = Color.White
    ) {
        Column(Modifier.fillMaxWidth()) {
            // fill = false: la parte desplazable ocupa solo lo que necesita y el botón queda
            // siempre visible debajo, sin que la hoja reserve alto de más.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    nombre,
                    color = Color.White,
                    fontSize = 28.sp,
                    lineHeight = 30.sp,
                    fontFamily = FuenteTitulo,
                    fontWeight = FontWeight.Bold
                )

                // Los GIF de la biblioteca son cuadrados y con fondo blanco.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .clickable(enabled = urlCompleta != null, onClickLabel = "Ampliar demostración") { gifAmpliado = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (urlCompleta == null) {
                        SinDemostracion()
                    } else {
                        GifEjercicio(urlCompleta, cargadorGif, "Demostración de $nombre", Modifier.fillMaxSize())
                        IconButton(
                            onClick = { gifAmpliado = true },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(Icons.Rounded.OpenInFull, contentDescription = "Ver en pantalla completa", tint = Color.White)
                        }
                    }
                }

                if (datos.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        datos.forEach { dato ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C21))
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(dato.valor, color = Color.White, fontSize = 26.sp, fontFamily = FuenteTitulo, fontWeight = FontWeight.Bold)
                                Text(dato.etiqueta, color = Color(0xFF9A9AA2), fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (pasos.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF17171B))
                            .padding(16.dp)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Técnica", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${pasos.size} pasos", color = Color(0xFF9A9AA2), fontSize = 13.sp)
                        }
                        val visibles = if (verTodosLosPasos) pasos else pasos.take(PASOS_VISIBLES)
                        visibles.forEachIndexed { i, paso ->
                            if (i > 0) HorizontalDivider(color = Color(0xFF26262C), thickness = 0.5.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape).background(acento.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${i + 1}", color = acento, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(paso, color = Color(0xFFE2E2E6), fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                            }
                        }
                        if (pasos.size > PASOS_VISIBLES) {
                            TextButton(
                                onClick = { verTodosLosPasos = !verTodosLosPasos },
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text(
                                    if (verTodosLosPasos) "Ver menos" else "Ver los ${pasos.size} pasos",
                                    color = acento,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    if (verTodosLosPasos) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = acento
                                )
                            }
                        }
                    }
                }
            }

            if (completado != null && onToggleCompletado != null) {
                HorizontalDivider(color = Color(0xFF26262C), thickness = 0.5.dp)
                BotonCompletado(
                    completado = completado,
                    onClick = onToggleCompletado,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }

    if (gifAmpliado && urlCompleta != null) {
        GifPantallaCompleta(nombre, urlCompleta, cargadorGif) { gifAmpliado = false }
    }
}

@Composable
private fun BotonCompletado(completado: Boolean, onClick: () -> Unit, modifier: Modifier) {
    if (completado) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, VerdeEstado)
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerdeEstado)
            Spacer(Modifier.width(8.dp))
            Text("Completado · tocar para desmarcar", color = VerdeEstado, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Marcar como hecho", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/** GIF a pantalla completa: pellizcar para acercar, arrastrar para mover, doble toque para acercar o restablecer. */
@Composable
private fun GifPantallaCompleta(nombre: String, url: String, cargador: ImageLoader, onCerrar: () -> Unit) {
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTransformGestures { _, paneo, zoom, _ ->
                        escala = (escala * zoom).coerceIn(1f, 4f)
                        desplazamiento = if (escala == 1f) Offset.Zero else desplazamiento + paneo
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (escala > 1f) {
                            escala = 1f
                            desplazamiento = Offset.Zero
                        } else {
                            escala = 2.5f
                        }
                    })
                }
        ) {
            GifEjercicio(
                url,
                cargador,
                "Demostración ampliada de $nombre",
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                        translationX = desplazamiento.x
                        translationY = desplazamiento.y
                    }
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    nombre,
                    color = Color(0xFF111114),
                    fontSize = 22.sp,
                    fontFamily = FuenteTitulo,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(
                    onClick = onCerrar,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF111114))
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
            Text(
                "Pellizca para acercar · doble toque para restablecer",
                color = Color(0xFFA1A1AA),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
            )
        }
    }
}

@Composable
private fun GifEjercicio(url: String, cargador: ImageLoader, descripcion: String, modifier: Modifier) {
    SubcomposeAsyncImage(
        model = url,
        imageLoader = cargador,
        contentDescription = descripcion,
        contentScale = ContentScale.Fit,
        modifier = modifier,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            }
        },
        error = { SinDemostracion("No se pudo cargar la demostración. Revisa tu conexión.") }
    )
}

/** Android anima GIF de forma nativa desde la API 28; antes se usa el decodificador de Coil. */
@Composable
private fun rememberCargadorGif(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .build()
    }
}

@Composable
private fun SinDemostracion(texto: String = "Este ejercicio no tiene demostración. Pide a tu entrenador que lo cambie por uno de la biblioteca.") {
    Box(
        Modifier.fillMaxSize().background(Color(0xFF1C1C21)).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, color = Color(0xFFB8B8BE), fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

private val INDICIOS_UTILES = listOf("otro", "otra", "cambia", "altern", "lado", "brazo", "pierna", "inmediato")

/**
 * Quita el último paso cuando solo dice que se repita el movimiento ("Repite hasta completar
 * las repeticiones"): ese dato ya se muestra arriba. Se conserva si aporta algo, como
 * cambiar de brazo o de lado.
 */
internal fun pasosUtiles(instrucciones: List<String>): List<String> {
    val limpios = instrucciones.map { it.trim() }.filter { it.isNotEmpty() }
    val ultimo = limpios.lastOrNull()?.lowercase() ?: return limpios
    val esRelleno = limpios.size > 1 &&
        (ultimo.startsWith("repite") || ultimo.startsWith("completa")) &&
        INDICIOS_UTILES.none { ultimo.contains(it) }
    return if (esRelleno) limpios.dropLast(1) else limpios
}

/** La API devuelve rutas relativas ("/gifs/0025.gif"); se completan con la URL base del build. */
internal fun urlDeGif(gifUrl: String?): String? {
    val ruta = gifUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (ruta.startsWith("http://") || ruta.startsWith("https://")) return ruta
    return BuildConfig.API_BASE_URL.trimEnd('/') + "/" + ruta.trimStart('/')
}

/** Series, repeticiones, peso y tiempo asignados por el entrenador, solo los que existen. */
internal fun datosEjercicio(
    tipo: String?,
    series: Int?,
    repeticiones: Int?,
    pesoKg: Double?,
    duracionSegundos: Int?,
    descansoSegundos: Int?
): List<DatoEjercicio> = buildList {
    if (tipo != "Cardio") {
        series?.let { add(DatoEjercicio("$it", "series")) }
        repeticiones?.let { add(DatoEjercicio("$it", "repeticiones")) }
        pesoKg?.let { add(DatoEjercicio(formatoNumero(it), "kg")) }
    }
    duracionSegundos?.let { add(DatoEjercicio(formatoTiempo(it), "tiempo")) }
    descansoSegundos?.let { add(DatoEjercicio("${it}s", "descanso")) }
}.take(4)

private fun formatoNumero(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toInt().toString() else valor.toString()

internal fun formatoTiempo(segundos: Int): String =
    if (segundos >= 60) "${segundos / 60}:${(segundos % 60).toString().padStart(2, '0')}" else "${segundos}s"
