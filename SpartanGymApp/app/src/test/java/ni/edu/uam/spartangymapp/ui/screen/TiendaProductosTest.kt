package ni.edu.uam.spartangymapp.ui.screen

import ni.edu.uam.spartangymapp.network.ProductoCatalogoResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TiendaProductosTest {

    private val productos = listOf(
        ProductoCatalogoResponse(id = "1", nombre = "Proteína whey 2 lb", categoria = "Suplementos"),
        ProductoCatalogoResponse(id = "2", nombre = "Batido de proteína con leche", categoria = "Bebidas", descripcion = "30 g de proteína con 300 ml de leche"),
        ProductoCatalogoResponse(id = "3", nombre = "Shaker 600 ml", categoria = "Accesorios")
    )

    @Test
    fun productos_porNombreSinTildesOPorAlgoRelacionado() {
        assertEquals(listOf("1", "2"), filtrarProductos(productos, "PROTEINA").map { it.id })
        assertEquals(listOf("2"), filtrarProductos(productos, "leche").map { it.id })
        assertEquals(listOf("3"), filtrarProductos(productos, "accesorios").map { it.id })
        assertEquals(3, filtrarProductos(productos, "").size)
    }

    @Test
    fun personas_soloAparecenAlBuscar() {
        val personas = listOf(
            PersonaBuscable("a", "María Pérez", "maria@correo.com"),
            PersonaBuscable("b", "Jorge Cubillo", "jorge@correo.com")
        )
        assertTrue(filtrarPersonas(personas, "").isEmpty())
        assertEquals(listOf("a"), filtrarPersonas(personas, "maria perez").map { it.id })
        assertEquals(listOf("b"), filtrarPersonas(personas, "jorge@").map { it.id })
    }

    @Test
    fun simboloDeMoneda_yPrecio() {
        assertEquals("C$", simboloDeMoneda("NIO"))
        assertEquals("$", simboloDeMoneda("USD"))
        assertEquals("C$120.00", precioTexto(120.0, "NIO"))
    }
}
