package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.dto.ConfirmarPedidoRequest;
import ni.edu.uam.GimnasioAPI.dto.PedidoSocioRequest;
import ni.edu.uam.GimnasioAPI.dto.ProductoComponenteRequest;
import ni.edu.uam.GimnasioAPI.dto.ProductoInventarioRequest;
import ni.edu.uam.GimnasioAPI.dto.VentaProductoDetalleRequest;
import ni.edu.uam.GimnasioAPI.dto.VentaProductoResponse;
import ni.edu.uam.GimnasioAPI.exceptions.ReglaNegocioException;
import ni.edu.uam.GimnasioAPI.integracion.IntegracionPostgresBase;
import ni.edu.uam.GimnasioAPI.models.ProductoInventario;
import ni.edu.uam.GimnasioAPI.models.Rol;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.ProductoInventarioRepository;
import ni.edu.uam.GimnasioAPI.repositories.RolRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SucursalRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pedido del socio desde la app: aparta el stock y queda pendiente con un código; recepción lo
 * valida (y entra a caja) o lo cancela (y el stock vuelve).
 */
@EnabledIfEnvironmentVariable(named = "SAASGYM_IT_DB_URL", matches = ".+")
class PedidoProductoIntegrationTest extends IntegracionPostgresBase {

    @Autowired private VentaProductoService ventaService;
    @Autowired private InventarioService inventarioService;
    @Autowired private ProductoInventarioRepository productoRepository;
    @Autowired private SucursalRepository sucursalRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private SocioRepository socioRepository;
    @Autowired private JdbcTemplate jdbc;

    private Sucursal sucursal;
    private UUID socioId;
    private Authentication socio;
    private final Authentication recepcion = new UsernamePasswordAuthenticationToken("recepcion-it@ejemplo.com", null, List.of());
    private ProductoInventario agua;
    private ProductoInventario leche;
    private ProductoInventario batido;

    @BeforeEach
    void preparar() {
        Sucursal nueva = new Sucursal();
        nueva.setNombre("Sucursal pedidos " + UUID.randomUUID().toString().substring(0, 8));
        nueva.setUbicacion("Prueba");
        nueva.setCapacidad(50);
        nueva.setHorarioApertura("06:00");
        nueva.setHorarioCierre("22:00");
        sucursal = sucursalRepository.save(nueva);

        Rol rol = rolRepository.findByNombre("ROLE_SOCIO").orElseGet(() -> {
            Rol r = new Rol();
            r.setNombre("ROLE_SOCIO");
            return rolRepository.save(r);
        });
        Usuario usuario = new Usuario();
        usuario.setEmail("socio-pedidos-" + UUID.randomUUID().toString().substring(0, 8) + "@ejemplo.com");
        usuario.setPasswordHash("$2a$10$hash-de-prueba");
        usuario.setRol(rol);
        usuario = usuarioRepository.save(usuario);
        Socio perfil = new Socio();
        perfil.setUsuario(usuario);
        perfil.setNombres("Socio");
        perfil.setApellidos("Pedidos");
        perfil.setSucursal(sucursal);
        socioRepository.save(perfil);
        socioId = usuario.getId();
        socio = new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, List.of());

        agua = inventarioService.crear(producto("Agua", 10, "unidad", true, List.of()));
        leche = inventarioService.crear(producto("Leche", 1000, "ml", false, List.of()));
        batido = inventarioService.crear(producto("Batido", 0, "unidad", true,
                List.of(new ProductoComponenteRequest(leche.getId(), 300))));
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM movimientos_financieros WHERE sucursal_id = ?", sucursal.getId());
        jdbc.update("DELETE FROM detalle_ventas_productos WHERE venta_id IN (SELECT id FROM ventas_productos WHERE sucursal_id = ?)", sucursal.getId());
        jdbc.update("DELETE FROM ventas_productos WHERE sucursal_id = ?", sucursal.getId());
        jdbc.update("DELETE FROM producto_componentes WHERE producto_id IN (SELECT id FROM productos_inventario WHERE sucursal_id = ?)", sucursal.getId());
        jdbc.update("DELETE FROM productos_inventario WHERE sucursal_id = ?", sucursal.getId());
        jdbc.update("DELETE FROM socios WHERE usuario_id = ?", socioId);
        jdbc.update("DELETE FROM usuarios WHERE id = ?", socioId);
        jdbc.update("DELETE FROM sucursales WHERE id = ?", sucursal.getId());
    }

    private ProductoInventarioRequest producto(String nombre, int stock, String unidad, boolean vendible,
                                               List<ProductoComponenteRequest> receta) {
        ProductoInventarioRequest request = new ProductoInventarioRequest();
        request.setNombre(nombre);
        request.setCategoria("Bebidas");
        request.setPrecio(new BigDecimal("50.00"));
        request.setStock(stock);
        request.setUnidad(unidad);
        request.setVendible(vendible);
        request.setSucursalId(sucursal.getId());
        request.setComponentes(receta);
        return request;
    }

    private PedidoSocioRequest pedido(String metodo, Object... productoYCantidad) {
        PedidoSocioRequest request = new PedidoSocioRequest();
        request.setMetodoPago(metodo);
        List<VentaProductoDetalleRequest> detalles = new java.util.ArrayList<>();
        for (int i = 0; i < productoYCantidad.length; i += 2) {
            VentaProductoDetalleRequest d = new VentaProductoDetalleRequest();
            d.setProductoId(((ProductoInventario) productoYCantidad[i]).getId());
            d.setCantidad((Integer) productoYCantidad[i + 1]);
            detalles.add(d);
        }
        request.setDetalles(detalles);
        return request;
    }

    private int stock(ProductoInventario producto) {
        return productoRepository.findById(producto.getId()).orElseThrow().getStock();
    }

    private int movimientos() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM movimientos_financieros WHERE sucursal_id = ?", Integer.class, sucursal.getId());
        return n == null ? 0 : n;
    }

    @Test
    void elPedido_apartaStock_yQuedaPendienteConCodigo_sinEntrarACaja() {
        VentaProductoResponse creado = ventaService.crearPedidoSocio(pedido("Efectivo", agua, 2, batido, 1), socio);

        assertThat(creado.getEstado()).isEqualTo("Pendiente");
        assertThat(creado.getCodigoPedido()).matches("P-[2-9A-HJ-NP-Z]{6}");
        assertThat(stock(agua)).isEqualTo(8);
        assertThat(stock(leche)).isEqualTo(700);
        assertThat(movimientos()).isZero();
        assertThat(ventaService.misPedidos(socio)).extracting(VentaProductoResponse::getCodigoPedido)
                .containsExactly(creado.getCodigoPedido());
    }

    @Test
    void confirmarEnEfectivo_completaLaCompra_calculaElCambio_yEntraACaja() {
        VentaProductoResponse creado = ventaService.crearPedidoSocio(pedido("Efectivo", agua, 1), socio);
        ConfirmarPedidoRequest pago = new ConfirmarPedidoRequest();
        pago.setMontoRecibido(new BigDecimal("100.00"));

        VentaProductoResponse confirmado = ventaService.confirmarPedido(creado.getCodigoPedido().toLowerCase(), pago, recepcion);

        assertThat(confirmado.getEstado()).isEqualTo("Completada");
        assertThat(confirmado.getCambio()).isEqualByComparingTo(new BigDecimal("100.00").subtract(confirmado.getTotal()));
        assertThat(confirmado.getValidadoPor()).isEqualTo("recepcion-it@ejemplo.com");
        assertThat(movimientos()).isEqualTo(1);
        assertThatThrownBy(() -> ventaService.confirmarPedido(creado.getCodigoPedido(), null, recepcion))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("completada");
    }

    @Test
    void efectivoInsuficiente_noConfirma() {
        VentaProductoResponse creado = ventaService.crearPedidoSocio(pedido("Efectivo", agua, 2), socio);
        ConfirmarPedidoRequest pago = new ConfirmarPedidoRequest();
        pago.setMontoRecibido(new BigDecimal("10.00"));

        assertThatThrownBy(() -> ventaService.confirmarPedido(creado.getCodigoPedido(), pago, recepcion))
                .hasMessageContaining("no cubre");
        assertThat(ventaService.buscarPedido(creado.getCodigoPedido(), recepcion).getEstado()).isEqualTo("Pendiente");
    }

    @Test
    void cancelar_devuelveElStockApartado_inclusoLosInsumosDeLaReceta() {
        VentaProductoResponse creado = ventaService.crearPedidoSocio(pedido("Transferencia", agua, 3, batido, 2), socio);
        assertThat(stock(leche)).isEqualTo(400);

        VentaProductoResponse cancelado = ventaService.cancelarPedido(creado.getCodigoPedido(), recepcion);

        assertThat(cancelado.getEstado()).isEqualTo("Cancelada");
        assertThat(stock(agua)).isEqualTo(10);
        assertThat(stock(leche)).isEqualTo(1000);
        assertThat(movimientos()).isZero();
    }

    @Test
    void cancelarPedidoAntiguoSinReservas_noAdivinaInsumosConRecetaActual() {
        VentaProductoResponse creado = ventaService.crearPedidoSocio(pedido("Transferencia", batido, 1), socio);
        assertThat(stock(leche)).isEqualTo(700);
        jdbc.update("DELETE FROM reservas_ventas_productos WHERE venta_id = ?", creado.getId());

        ventaService.cancelarPedido(creado.getCodigoPedido(), recepcion);

        assertThat(stock(leche)).isEqualTo(700);
    }

    @Test
    void soloEfectivoOTransferencia_yCodigoInexistente() {
        assertThatThrownBy(() -> ventaService.crearPedidoSocio(pedido("Tarjeta", agua, 1), socio))
                .hasMessageContaining("efectivo o por transferencia");
        assertThatThrownBy(() -> ventaService.buscarPedido("P-XXXXXX", recepcion))
                .hasMessageContaining("No hay ningún pedido");
    }
}
