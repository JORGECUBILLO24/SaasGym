package ni.edu.uam.spartangymapp.ui.screen

import ni.edu.uam.spartangymapp.network.EjercicioCatalogoResponse
import java.text.Normalizer
import java.util.Locale

// Búsqueda de la biblioteca de ejercicios. Misma lógica que la API (BusquedaTexto y
// CatalogoService) y la web: ignora tildes, mayúsculas, plurales simples y el orden de las
// palabras, y también encuentra por sinónimo, nombre en inglés, músculo, equipo o grupo.

private val MARCAS = "\\p{M}+".toRegex()
private val NO_ALFANUMERICO = "[^a-z0-9]+".toRegex()

internal fun normalizarBusqueda(texto: String?): String {
    if (texto.isNullOrBlank()) return ""
    val sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replace(MARCAS, "")
    return sinTildes.lowercase(Locale.ROOT).replace(NO_ALFANUMERICO, " ").trim()
}

private fun singular(palabra: String): String = when {
    palabra.length > 4 && palabra.endsWith("es") && !palabra.endsWith("ses") -> palabra.dropLast(2)
    palabra.length > 3 && palabra.endsWith("s") -> palabra.dropLast(1)
    else -> palabra
}

internal fun terminosBusqueda(consulta: String?): List<String> {
    val normalizada = normalizarBusqueda(consulta)
    if (normalizada.isEmpty()) return emptyList()
    return normalizada.split(" ").map(::singular).filter { it.isNotEmpty() }
}

private fun contieneTodos(texto: String, terminos: List<String>) =
    terminos.isNotEmpty() && terminos.all { texto.contains(it) }

private fun puntuar(e: EjercicioCatalogoResponse, terminos: List<String>): Int {
    val nombre = normalizarBusqueda(e.nombre)
    val consulta = terminos.joinToString(" ")
    if (nombre == consulta) return 100
    if (nombre.startsWith(consulta)) return 80
    if (contieneTodos(nombre, terminos)) return 60
    val alias = normalizarBusqueda(e.alias.orEmpty().joinToString(" "))
    if (contieneTodos(alias, terminos)) return 40
    val resto = normalizarBusqueda(
        (listOf(nombre, alias, e.nombreOriginal, e.musculoPrincipal, e.equipo, e.zona, e.grupoMuscular?.nombre) +
            e.musculosSecundarios.orEmpty()).filterNotNull().joinToString(" ")
    )
    return if (contieneTodos(resto, terminos)) 20 else 0
}

/**
 * Ejercicios que coinciden, mejor coincidencia primero. Sin texto devuelve todos (del grupo,
 * si se indica) en orden alfabético.
 */
internal fun buscarEjercicios(
    ejercicios: List<EjercicioCatalogoResponse>,
    consulta: String?,
    grupoMuscularId: Int? = null
): List<EjercicioCatalogoResponse> {
    val terminos = terminosBusqueda(consulta)
    val delGrupo = ejercicios.filter { grupoMuscularId == null || it.grupoMuscular?.id == grupoMuscularId }
    if (terminos.isEmpty()) return delGrupo.sortedBy { normalizarBusqueda(it.nombre) }

    return delGrupo
        .map { it to puntuar(it, terminos) }
        .filter { it.second > 0 }
        // En empate va primero el que tiene GIF y después el nombre más corto (la variante base).
        .sortedWith(
            compareByDescending<Pair<EjercicioCatalogoResponse, Int>> { it.second }
                .thenBy { if (it.first.gifUrl.isNullOrBlank()) 1 else 0 }
                .thenBy { it.first.nombre.orEmpty().length }
                .thenBy { normalizarBusqueda(it.first.nombre) }
        )
        .map { it.first }
}
