package com.example.spartangymapp.ui.screen

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.spartangymapp.BuildConfig
import com.example.spartangymapp.ui.theme.FuenteTitulo

/** Dato que se muestra bajo el GIF: "4" / "series". */
internal data class DatoEjercicio(val valor: String, val etiqueta: String)

/**
 * Hoja con el GIF de demostración que sirve la API (/gifs/<codigo>.gif), los datos
 * que asignó el entrenador y el check para confirmar el ejercicio.
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
    val context = LocalContext.current
    // Android anima GIF de forma nativa desde la API 28; antes se usa el decodificador de Coil.
    val cargadorGif = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .build()
    }
    val urlCompleta = remember(gifUrl) { urlDeGif(gifUrl) }
    val acento = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF111114),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
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
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (urlCompleta == null) {
                    SinDemostracion()
                } else {
                    SubcomposeAsyncImage(
                        model = urlCompleta,
                        imageLoader = cargadorGif,
                        contentDescription = "Demostración de $nombre",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = acento, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                            }
                        },
                        error = { SinDemostracion("No se pudo cargar la demostración. Revisa tu conexión.") }
                    )
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

            if (instrucciones.isNotEmpty()) {
                Text("Cómo hacerlo", color = Color(0xFF9A9AA2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                instrucciones.forEachIndexed { i, paso ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${i + 1}", color = acento, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
                        Text(paso, color = Color(0xFFD8D8DC), fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }

            if (completado != null && onToggleCompletado != null) {
                Spacer(Modifier.height(4.dp))
                if (completado) {
                    OutlinedButton(
                        onClick = onToggleCompletado,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, VerdeEstado)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VerdeEstado)
                        Spacer(Modifier.width(8.dp))
                        Text("Completado · tocar para desmarcar", color = VerdeEstado, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onToggleCompletado,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = acento)
                    ) {
                        Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Marcar como hecho", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
