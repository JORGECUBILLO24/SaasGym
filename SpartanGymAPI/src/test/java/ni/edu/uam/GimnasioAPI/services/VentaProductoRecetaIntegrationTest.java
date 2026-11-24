package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.dto.ProductoComponenteRequest;
import ni.edu.uam.GimnasioAPI.dto.ProductoInventarioRequest;
import ni.edu.uam.GimnasioAPI.dto.VentaProductoDetalleRequest;
import ni.edu.uam.GimnasioAPI.dto.VentaProductoRequest;
import ni.edu.uam.GimnasioAPI.exceptions.ReglaNegocioException;
import ni.edu.uam.GimnasioAPI.integracion.IntegracionPostgresBase;
import ni.edu.uam.GimnasioAPI.models.ProductoInventario;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.repositories.ProductoInventarioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SucursalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Un batido no tiene stock propio: al venderlo se descuenta la proteína y la leche que lleva.
 * Contra PostgreSQL real, porque lo delicado son los bloqueos de fila y la consulta de recetas.
 */
@EnabledIfEnvironmentVariable(named = "SAASGYM_IT_DB_URL", matches = ".+")
class VentaProductoRecetaIntegrationTest extends IntegracionPostgresBase {

    @Autowired private InventarioService inventarioService;
    @Autowired private VentaProductoService ventaService;
    @Autowired private ProductoInventarioRepository productoRepository;
    @Autowired private SucursalRepository sucursalRepository;
    @Autowired private JdbcTemplate jdbc;

    private Sucursal sucursal;
    private ProductoInventario proteina;
    private ProductoInventario leche;
    private ProductoInventario batido;

    @BeforeEach
    void preparar() {
        Sucursal nueva = new Sucursal();
        nueva.setNombre("Sucursal IT " + UUID.randomUUID().toString().substring(0, 8));
        nueva.setUbicacion("Prueba");
        nueva.setCapacidad(50);
        nueva.setHorarioApertura("06:00");
        nueva.setHorarioCierre("22:00");
        sucursal = sucursalRepository.save(nueva);

        proteina = inventarioService.crear(producto("Proteína a granel", 100, "g", false, List.of()));
        leche = inventarioService.crear(producto("Leche", 1000, "ml", false, List.of()));
        batido = inventarioService.crear(producto("Batido", 999, "unidad", true, List.of(
                new ProductoComponenteRequest(proteina.getId(), 30),
                new ProductoComponenteRequest(leche.getId(), 300))));
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM movimientos_financieros WHERE sucursal_id = ?", sucursal.getId());
        jdbc.update("DELETE FROM detalle_ventas_productos WHERE venta_id IN (SELECT id FROM ventas_productos WHERE sucursal_id = ?)", sucursal.getId());
        jdbc.update("DELETE FROM ventas_productos WHERE sucursal_id = ?", sucursal.getId());
        jdbc.update("DELETE FROM producto_componentes WHERE producto_id IN (SELECT id FROM productos_inventario WHERE sucursal_id = ?)", sucursal.getId());
        jdbc.update("DELETE FROM productos_inventario WHERE sucursal_id = ?", sucursal.getId());
        jdbc.update("DELETE FROM sucursales WHERE id = ?", sucursal.getId());
    }

    private ProductoInventarioRequest producto(String nombre, int stock, String unidad, boolean vendible,
                                               List<ProductoComponenteRequest> receta) {
        ProductoInventarioRequest request = new ProductoInventarioRequest();
        request.setNombre(nombre);
        request.setCategoria(vendible ? "Bebidas" : "Insumos");
        request.setPrecio(new BigDecimal("120.00"));
        request.setStock(stock);
        request.setUnidad(unidad);
        request.setVendible(vendible);
        request.setSucursalId(sucursal.getId());
        request.setComponentes(receta);
        return request;
    }

    private VentaProductoRequest venta(UUID productoId, int cantidad) {
        VentaProductoDetalleRequest detalle = new VentaProductoDetalleRequest();
        detalle.setProductoId(productoId);
        detalle.setCantidad(cantidad);
        VentaProductoRequest request = new VentaProductoRequest();
        request.setClienteEventual(true);
        request.setClienteNombre("Cliente IT");
        request.setSucursalId(sucursal.getId());
        request.setMetodoPago("Tarjeta");
        request.setDetalles(List.of(detalle));
        return request;
    }

    private int stock(ProductoInventario producto) {
        return productoRepository.findById(producto.getId()).orElseThrow().getStock();
    }

    @Test
    void venderUnBatido_descuentaSusInsumos() {
        assertThat(productoRepository.findById(batido.getId()).orElseThrow().getDisponible()).isEqualTo(3);
        assertThat(stock(batido)).as("un preparado no guarda stock propio").isZero();

        ventaService.vender(venta(batido.getId(), 2), null);

        assertThat(stock(proteina)).isEqualTo(40);
        assertThat(stock(leche)).isEqualTo(400);
        assertThat(productoRepository.findById(batido.getId()).orElseThrow().getDisponible()).isEqualTo(1);
    }

    @Test
    void sinInsumosSuficientes_noSeVendeNiSeDescuentaNada() {
        assertThatThrownBy(() -> ventaService.vender(venta(batido.getId(), 4), null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Proteína a granel")
                .hasMessageContaining("Batido");

        assertThat(stock(proteina)).isEqualTo(100);
        assertThat(stock(leche)).isEqualTo(1000);
    }

    @Test
    void unInsumo_noSeVendePorSeparado() {
        assertThatThrownBy(() -> ventaService.vender(venta(proteina.getId(), 1), null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("insumo");
    }

    @Test
    void ventasSimultaneas_noVendenMasBatidosDeLosQueAlcanzan() throws Exception {
        int hilos = 6;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                try {
                    ventaService.vender(venta(batido.getId(), 1), null);
                    exitos.incrementAndGet();
                } catch (ReglaNegocioException esperado) {
                    // Los que llegan cuando ya no alcanza la proteína fallan: es lo correcto.
                }
                return null;
            }));
        }
        salida.countDown();
        for (Future<?> futuro : futuros) {
            futuro.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(exitos.get()).isEqualTo(3);
        assertThat(stock(proteina)).isEqualTo(10);
        assertThat(stock(leche)).isEqualTo(100);
    }

    @Test
    void editarLaReceta_cambiaLoQueSeDescuenta() {
        inventarioService.actualizar(batido.getId(), producto("Batido", 0, "unidad", true, List.of(
                new ProductoComponenteRequest(proteina.getId(), 50))));

        ventaService.vender(venta(batido.getId(), 1), null);

        assertThat(stock(proteina)).isEqualTo(50);
        assertThat(stock(leche)).as("la leche ya no está en la receta").isEqualTo(1000);
    }

    @Test
    void reglasDeLaReceta() {
        assertThatThrownBy(() -> inventarioService.crear(producto("Batido doble", 0, "unidad", true, List.of(
                new ProductoComponenteRequest(batido.getId(), 2)))))
                .hasMessageContaining("también se prepara con receta");

        assertThatThrownBy(() -> inventarioService.eliminar(leche.getId()))
                .hasMessageContaining("es insumo de Batido");

        assertThatThrownBy(() -> inventarioService.actualizar(proteina.getId(), producto("Proteína a granel", 100, "g",
                false, List.of(new ProductoComponenteRequest(leche.getId(), 1)))))
                .hasMessageContaining("ya es insumo");
    }
}
