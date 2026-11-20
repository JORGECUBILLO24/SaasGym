package ni.edu.uam.spartangymapp.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ni.edu.uam.spartangymapp.network.EjercicioCatalogoResponse
import ni.edu.uam.spartangymapp.network.GrupoMuscularResponse

private val SEL_FONDO = Color(0xFF0C0C0C)
private val SEL_CAMPO = Color(0xFF181818)
private val SEL_BORDE = Color(0xFF262626)
private val SEL_GRIS = Color(0xFF9A9A9A)

/**
 * Hoja para elegir un ejercicio de la biblioteca: buscador (sin tildes, en cualquier orden, por
 * sinónimo o nombre en inglés), filtro opcional por grupo y miniatura del GIF de cada uno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectorEjercicioSheet(
    ejercicios: List<EjercicioCatalogoResponse>,
    seleccionadoId: Long?,
    acento: Color,
    onElegir: (EjercicioCatalogoResponse) -> Unit,
    onCerrar: () -> Unit
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var consulta by rememberSaveable { mutableStateOf("") }
    var grupoId by rememberSaveable { mutableStateOf<Int?>(null) }

    val grupos = remember(ejercicios) {
        ejercicios.mapNotNull { it.grupoMuscular }
            .filter { it.id != null && !it.nombre.isNullOrBlank() }
            .distinctBy { it.id }
            .sortedBy { normalizarBusqueda(it.nombre) }
    }
    val resultados = remember(ejercicios, consulta, grupoId) { buscarEjercicios(ejercicios, consulta, grupoId) }
    val lista = rememberLazyListState()
    LaunchedEffect(consulta, grupoId) { lista.scrollToItem(0) }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = estado, containerColor = SEL_FONDO) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Elegir ejercicio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        "${resultados.size} de ${ejercicios.size} en la biblioteca",
                        color = SEL_GRIS, fontSize = 12.sp
                    )
                }
                IconButton(onClick = onCerrar) { Icon(Icons.Rounded.Close, "Cerrar", tint = SEL_GRIS) }
            }

            OutlinedTextField(
                value = consulta,
                onValueChange = { consulta = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = SEL_GRIS) },
                trailingIcon = {
                    if (consulta.isNotEmpty()) IconButton(onClick = { consulta = "" }) {
                        Icon(Icons.Rounded.Close, "Borrar búsqueda", tint = SEL_GRIS)
                    }
                },
                placeholder = { Text("Sentadilla, curl bíceps, bench press…", color = SEL_GRIS, fontSize = 14.sp) },
                textStyle = TextStyle(fontSize = 15.sp, color = Color.White),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = acento, unfocusedBorderColor = SEL_BORDE,
                    focusedContainerColor = SEL_CAMPO, unfocusedContainerColor = SEL_CAMPO,
                    cursorColor = acento
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { ChipGrupo("Todos", grupoId == null, acento) { grupoId = null } }
                items(grupos, key = { it.id ?: 0 }) { grupo: GrupoMuscularResponse ->
                    ChipGrupo(grupo.nombre.orEmpty(), grupoId == grupo.id, acento) {
                        grupoId = if (grupoId == grupo.id) null else grupo.id
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            if (resultados.isEmpty()) {
                Text(
                    if (grupoId != null) "Nada en este grupo. Toca «Todos» para buscar en toda la biblioteca."
                    else "Sin resultados. Prueba con otra palabra o con el nombre en inglés.",
                    color = SEL_GRIS, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                )
            }

            LazyColumn(state = lista, modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(resultados, key = { it.id ?: it.hashCode().toLong() }) { ejercicio ->
                    FilaEjercicio(ejercicio, ejercicio.id == seleccionadoId, acento) { onElegir(ejercicio) }
                }
            }
        }
    }
}

@Composable
private fun ChipGrupo(texto: String, activo: Boolean, acento: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (activo) acento.copy(alpha = .18f) else SEL_CAMPO,
        border = BorderStroke(1.dp, if (activo) acento.copy(alpha = .6f) else SEL_BORDE)
    ) {
        Text(
            texto, color = if (activo) Color.White else SEL_GRIS, fontSize = 13.sp,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun FilaEjercicio(ejercicio: EjercicioCatalogoResponse, elegido: Boolean, acento: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (elegido) acento.copy(alpha = .1f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Miniatura fija (primer cuadro): animar decenas de GIF a la vez gasta batería y memoria.
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            val url = urlDeGif(ejercicio.gifUrl)
            if (url != null) {
                AsyncImage(
                    model = url, contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Icon(Icons.Rounded.FitnessCenter, null, tint = Color(0xFF8A8A8A), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                ejercicio.nombre.orEmpty(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            val detalle = listOfNotNull(ejercicio.grupoMuscular?.nombre, ejercicio.equipo, ejercicio.dificultad)
                .joinToString(" · ")
            if (detalle.isNotEmpty()) Text(detalle, color = SEL_GRIS, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (elegido) Icon(Icons.Rounded.Check, "Elegido", tint = acento, modifier = Modifier.size(20.dp))
    }
}
