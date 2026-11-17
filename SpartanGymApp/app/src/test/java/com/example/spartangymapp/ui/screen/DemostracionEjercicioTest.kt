package com.example.spartangymapp.ui.screen

import com.example.spartangymapp.BuildConfig
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
}
