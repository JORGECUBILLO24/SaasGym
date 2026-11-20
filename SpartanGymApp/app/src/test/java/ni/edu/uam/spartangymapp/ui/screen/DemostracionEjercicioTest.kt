package ni.edu.uam.spartangymapp.ui.screen

import ni.edu.uam.spartangymapp.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DemostracionEjercicioTest {

    @Test
    fun rutaRelativaDeLaApi_seCompletaConLaUrlBase() {
        val esperado = BuildConfig.API_BASE_URL.trimEnd('/') + "/gifs/0025.gif"
        assertEquals(esperado, urlDeGif("/gifs/0025.gif"))
        assertEquals(esperado, urlDeGif("gifs/0025.gif"))
    }

    @Test
    fun urlAbsoluta_seRespeta_yVaciaEsNula() {
        assertEquals("https://cdn.ejemplo.com/a.gif", urlDeGif("https://cdn.ejemplo.com/a.gif"))
        assertNull(urlDeGif(null))
        assertNull(urlDeGif("  "))
    }

    @Test
    fun fuerza_muestraSeriesRepeticionesPesoYTiempo() {
        val datos = datosEjercicio("Fuerza", 4, 12, 20.0, 90, null)
        assertEquals(
            listOf(
                DatoEjercicio("4", "series"),
                DatoEjercicio("12", "repeticiones"),
                DatoEjercicio("20", "kg"),
                DatoEjercicio("1:30", "tiempo")
            ),
            datos
        )
    }

    @Test
    fun cardio_noMuestraSeries_ySoloLoQueExiste() {
        assertEquals(
            listOf(DatoEjercicio("20:00", "tiempo"), DatoEjercicio("45s", "descanso")),
            datosEjercicio("Cardio", 3, 10, null, 1200, 45)
        )
    }

    @Test
    fun ultimoPasoDeRelleno_seQuita() {
        assertEquals(
            listOf("Acuéstate en el banco.", "Baja la barra al pecho."),
            pasosUtiles(listOf("Acuéstate en el banco.", "Baja la barra al pecho.", "Repite hasta completar las repeticiones."))
        )
    }

    @Test
    fun pasosUtiles_conservaUnUnicoPasoYDescartaVacios() {
        assertEquals(listOf("Repite el movimiento."), pasosUtiles(listOf("Repite el movimiento.")))
        assertEquals(listOf("Sujeta la barra."), pasosUtiles(listOf("  ", "Sujeta la barra.")))
    }

    @Test
    fun ultimoPasoConInformacion_seConserva() {
        val pasos = listOf("Sujeta la mancuerna.", "Completa la cantidad de repeticiones y luego cambia de brazo.")
        assertEquals(pasos, pasosUtiles(pasos))
        val alterno = listOf("Sube la rodilla.", "Repite con la otra pierna.")
        assertEquals(alterno, pasosUtiles(alterno))
    }
}
