package ni.edu.uam.spartangymapp.ui.screen

import ni.edu.uam.spartangymapp.network.EjercicioCatalogoResponse
import ni.edu.uam.spartangymapp.network.GrupoMuscularResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusquedaEjerciciosTest {

    private val pecho = GrupoMuscularResponse(1, "Pecho")
    private val biceps = GrupoMuscularResponse(6, "Bíceps")

    private val biblioteca = listOf(
        EjercicioCatalogoResponse(1, "Aperturas con mancuernas", pecho, equipo = "Mancuerna",
            musculoPrincipal = "Pectorales", alias = listOf("pec fly", "dumbbell fly"), gifUrl = "/gifs/1.gif"),
        EjercicioCatalogoResponse(2, "Press de banca con barra", pecho, equipo = "Barra",
            musculoPrincipal = "Pectorales", alias = listOf("press plano", "barbell bench press"), gifUrl = "/gifs/2.gif"),
        EjercicioCatalogoResponse(3, "Curl de bíceps con mancuerna", biceps, equipo = "Mancuerna",
            musculoPrincipal = "Bíceps", alias = listOf("curl alterno", "dumbbell biceps curl"), gifUrl = "/gifs/3.gif"),
        EjercicioCatalogoResponse(4, "Press de banca inclinado con mancuernas", pecho, equipo = "Mancuerna",
            musculoPrincipal = "Pectorales", alias = listOf("dumbbell incline bench press"), gifUrl = "/gifs/4.gif"),
        EjercicioCatalogoResponse(5, "Remo con barra", null)
    )

    private fun nombres(consulta: String, grupo: Int? = null) =
        buscarEjercicios(biblioteca, consulta, grupo).map { it.nombre }

    @Test
    fun normalizaTildesMayusculasYPlurales() {
        assertEquals("biceps curl", normalizarBusqueda("  Bíceps—CURL "))
        assertEquals(listOf("mancuerna", "flexion"), terminosBusqueda("Mancuernas flexiones"))
    }

    @Test
    fun encuentraSinTildesEnPluralYEnOtroOrden() {
        assertEquals(listOf("Curl de bíceps con mancuerna"), nombres("BICEPS mancuernas"))
        assertEquals("Press de banca con barra", nombres("banca press").first())
    }

    @Test
    fun encuentraPorSinonimoNombreEnInglesYMusculo() {
        assertEquals(listOf("Press de banca con barra"), nombres("press plano"))
        assertEquals(2, nombres("bench press").size)
        assertEquals(3, nombres("pectorales").size)
    }

    @Test
    fun grupoOpcional_ySinTextoDevuelveTodoOrdenado() {
        assertEquals(5, nombres("").size)
        assertEquals("Aperturas con mancuernas", nombres("").first())
        assertEquals(listOf("Curl de bíceps con mancuerna"), nombres("", grupo = 6))
        assertEquals(2, nombres("mancuerna", grupo = 1).size)
    }

    @Test
    fun sinCoincidencias_listaVacia() {
        assertTrue(nombres("sentadilla").isEmpty())
    }
}
