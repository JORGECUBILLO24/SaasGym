package ni.edu.uam.GimnasioAPI.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodigoPedidoTest {

    @Test
    void aceptaElCodigoComoLoEscribanODicten() {
        assertThat(VentaProductoService.normalizarCodigoPedido("P-98GYW7")).isEqualTo("P-98GYW7");
        assertThat(VentaProductoService.normalizarCodigoPedido("p98gyw7")).isEqualTo("P-98GYW7");
        assertThat(VentaProductoService.normalizarCodigoPedido(" p 98 gyw7 ")).isEqualTo("P-98GYW7");
        assertThat(VentaProductoService.normalizarCodigoPedido("98GYW7")).isEqualTo("P-98GYW7");
        assertThat(VentaProductoService.normalizarCodigoPedido(null)).isEmpty();
    }
}
