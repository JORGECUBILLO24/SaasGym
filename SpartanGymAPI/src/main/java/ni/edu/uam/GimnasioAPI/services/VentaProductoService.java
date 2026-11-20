package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.FacturaDetalleResponse;
import ni.edu.uam.GimnasioAPI.dto.LineaReceta;
import ni.edu.uam.GimnasioAPI.dto.ConfirmarPedidoRequest;
import ni.edu.uam.GimnasioAPI.dto.PedidoSocioRequest;
import ni.edu.uam.GimnasioAPI.dto.VentaProductoDetalleRequest;
import ni.edu.uam.GimnasioAPI.dto.VentaProductoRequest;
import ni.edu.uam.GimnasioAPI.dto.VentaProductoResponse;
import ni.edu.uam.GimnasioAPI.models.MovimientoFinanciero;
import ni.edu.uam.GimnasioAPI.models.ProductoInventario;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.models.VentaProducto;
import ni.edu.uam.GimnasioAPI.models.VentaProductoDetalle;
import ni.edu.uam.GimnasioAPI.repositories.MovimientoFinancieroRepository;
import ni.edu.uam.GimnasioAPI.repositories.PersonalRepository;
import ni.edu.uam.GimnasioAPI.repositories.ProductoComponenteRepository;
import ni.edu.uam.GimnasioAPI.repositories.ProductoInventarioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SucursalRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.repositories.VentaProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import ni.edu.uam.GimnasioAPI.models.VentaProductoReserva;
import ni.edu.uam.GimnasioAPI.security.AlcanceSucursal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class VentaProductoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    static final String ESTADO_PENDIENTE = "Pendiente";
    static final String ESTADO_COMPLETADA = "Completada";
    static final String ESTADO_CANCELADA = "Cancelada";

    private final VentaProductoRepository ventaRepository;
    private final ProductoInventarioRepository productoRepository;
    private final ProductoComponenteRepository componenteRepository;
    private final SocioRepository socioRepository;
    private final PersonalRepository personalRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoFinancieroRepository movimientoRepository;
    private final ConfiguracionAppService configuracionAppService;
    private final AlcanceSucursal alcanceSucursal;
    private final NotificacionService notificacionService;
    private final EntityManager entityManager;

    private static final Map<String, Object> ESPERA_BLOQUEO = Map.of("jakarta.persistence.lock.timeout", 5000);

    /** Horas que un pedido de la app aparta el stock antes de cancelarse solo. */
    @Value("${app.pedidos.expiracion-horas:24}")
    private long horasExpiracionPedido = 24;

    /** Pedidos sin pagar que un socio puede tener a la vez. */
    @Value("${app.pedidos.max-pendientes-por-socio:3}")
    private long maxPendientesPorSocio = 3;

    /** Unidades máximas de un mismo producto por pedido de la app. */
    @Value("${app.pedidos.max-unidades-por-linea:20}")
    private int maxUnidadesPorLinea = 20;

    // Recepción ve las facturas de su sucursal; administración, todas.
    @Transactional(readOnly = true)
    public List<VentaProductoResponse> listar(Authentication auth) {
        java.util.Optional<UUID> suya = alcanceSucursal.sucursalRestringida(obtenerVendedor(auth));
        List<VentaProducto> ventas = suya.isPresent()
                ? ventaRepository.findBySucursal_IdOrderByFechaCreacionDesc(suya.get())
                : ventaRepository.findAllByOrderByFechaCreacionDesc();
        return ventas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VentaProductoResponse obtener(UUID id, Authentication auth) {
        VentaProducto venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));
        if (!alcanceSucursal.alcanza(obtenerVendedor(auth), venta.getSucursal().getId())) {
            throw new AccesoDenegadoException("Esa factura es de otra sucursal");
        }
        return toResponse(venta);
    }

    @Transactional
    public VentaProductoResponse vender(VentaProductoRequest request, Authentication auth) {
        return toResponse(registrar(request, auth, false));
    }

    /**
     * Registra la venta. Un pedido de la app queda "Pendiente": el stock se aparta ya (para no
     * vender lo mismo dos veces), pero el cobro y el ingreso en caja se registran al validarlo.
     */
    private VentaProducto registrar(VentaProductoRequest request, Authentication auth, boolean pedido) {
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("Agrega al menos un producto a la venta");
        }
        if (request.getSucursalId() == null) {
            throw new ReglaNegocioException("Selecciona una sucursal para facturar");
        }

        boolean clienteEventual = Boolean.TRUE.equals(request.getClienteEventual());
        if (!clienteEventual && request.getSocioId() == null) {
            throw new ReglaNegocioException("Selecciona un socio o marca la venta como cliente eventual");
        }

        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        if (!pedido && !alcanceSucursal.alcanza(obtenerVendedor(auth), sucursal.getId())) {
            throw new AccesoDenegadoException("Solo puedes facturar en tu sucursal");
        }
        Socio socio = clienteEventual || request.getSocioId() == null
                ? null
                : socioRepository.findById(request.getSocioId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Socio no encontrado"));
        Usuario vendedor = obtenerVendedor(auth);

        String clienteNombre = clienteEventual
                ? textoRequerido(request.getClienteNombre(), "Ingresa el nombre del cliente eventual")
                : socio.getNombres() + " " + socio.getApellidos();

        VentaProducto venta = new VentaProducto();
        venta.setNumeroFactura(generarNumeroFactura());
        venta.setClienteEventual(clienteEventual);
        venta.setSocio(socio);
        venta.setSucursal(sucursal);
        venta.setVendedor(vendedor);
        venta.setClienteNombre(clienteNombre);
        venta.setClienteDocumento(limpiarTexto(request.getClienteDocumento()));
        venta.setMetodoPago(textoOpcional(request.getMetodoPago(), "Efectivo"));
        venta.setObservaciones(limpiarTexto(request.getObservaciones()));
        venta.setFechaCreacion(LocalDateTime.now());

        // Un producto preparado (batido, café...) no tiene stock propio: descuenta de sus insumos.
        // Las recetas se leen como valores sueltos para saber qué filas bloquear antes de leer stock.
        List<UUID> idsVendidos = idsDeLaVenta(request.getDetalles());
        Map<UUID, List<LineaReceta>> recetas = componenteRepository.recetasDe(idsVendidos).stream()
                .collect(Collectors.groupingBy(LineaReceta::productoId));

        // Se bloquean TODAS las filas que se van a tocar (productos e insumos) antes de leer su
        // stock, siempre en el mismo orden. Sin esto, dos ventas simultaneas facturaban unidades
        // que ya no existian, y dos ventas en orden distinto terminaban en deadlock.
        // Primero las filas sin receta y despues las preparadas: al cargar un preparado, Hibernate
        // trae tambien sus insumos, y para entonces ya tienen que estar bloqueados.
        Set<UUID> sinReceta = new TreeSet<>();
        Set<UUID> preparados = new TreeSet<>();
        for (UUID id : idsVendidos) {
            (recetas.containsKey(id) ? preparados : sinReceta).add(id);
        }
        recetas.values().forEach(lineas -> lineas.forEach(linea -> sinReceta.add(linea.insumoId())));
        Set<UUID> aBloquear = new java.util.LinkedHashSet<>(sinReceta);
        aBloquear.addAll(preparados);
        Map<UUID, ProductoInventario> productos = bloquearProductos(aBloquear);

        // Lo que sale del inventario, sumado por fila: si se venden 2 batidos y 1 bote de la misma
        // proteina, se valida contra el total y no linea por linea.
        Map<UUID, Long> consumo = new LinkedHashMap<>();
        Map<UUID, String> usadoEn = new LinkedHashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (VentaProductoDetalleRequest detalleRequest : request.getDetalles()) {
            ProductoInventario producto = productos.get(detalleRequest.getProductoId());
            if (producto.getSucursal() != null && !producto.getSucursal().getId().equals(sucursal.getId())) {
                throw new ReglaNegocioException(producto.getNombre() + " pertenece a otra sucursal");
            }
            if (Boolean.FALSE.equals(producto.getVendible())) {
                throw new ReglaNegocioException(producto.getNombre() + " es un insumo y no se vende por separado");
            }
            int cantidad = detalleRequest.getCantidad() == null ? 0 : detalleRequest.getCantidad();
            if (cantidad <= 0) {
                throw new ReglaNegocioException("La cantidad debe ser mayor a cero");
            }
            if (pedido && cantidad > maxUnidadesPorLinea) {
                throw new ReglaNegocioException("Puedes pedir hasta " + maxUnidadesPorLinea + " unidades de "
                        + producto.getNombre() + " por pedido");
            }

            List<LineaReceta> receta = recetas.getOrDefault(producto.getId(), List.of());
            if (receta.isEmpty()) {
                consumo.merge(producto.getId(), (long) cantidad, Long::sum);
            } else {
                for (LineaReceta linea : receta) {
                    consumo.merge(linea.insumoId(), (long) cantidad * linea.cantidad(), Long::sum);
                    usadoEn.putIfAbsent(linea.insumoId(), producto.getNombre());
                }
            }

            BigDecimal totalLinea = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
            VentaProductoDetalle detalle = new VentaProductoDetalle();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setProductoNombre(producto.getNombre());
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setTotalLinea(totalLinea);
            venta.getDetalles().add(detalle);
            subtotal = subtotal.add(totalLinea);
        }

        for (Map.Entry<UUID, Long> salida : consumo.entrySet()) {
            ProductoInventario fila = productos.get(salida.getKey());
            long existencia = fila.getStock() == null ? 0 : fila.getStock();
            if (existencia < salida.getValue()) {
                String para = usadoEn.containsKey(fila.getId()) ? " para preparar " + usadoEn.get(fila.getId()) : "";
                throw new ReglaNegocioException("Stock insuficiente de " + fila.getNombre() + para
                        + ": hay " + existencia + " " + unidadDe(fila) + " y se necesitan " + salida.getValue());
            }
            fila.setStock((int) (existencia - salida.getValue()));
            productoRepository.save(fila);
            if (pedido) {
                VentaProductoReserva reserva = new VentaProductoReserva();
                reserva.setVenta(venta);
                reserva.setProductoId(fila.getId());
                reserva.setCantidad(Math.toIntExact(salida.getValue()));
                venta.getReservas().add(reserva);
            }
        }

        BigDecimal impuesto = subtotal.multiply(obtenerTasaImpuesto())
                .divide(CIEN, 2, RoundingMode.HALF_UP);
        venta.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        venta.setImpuesto(impuesto);
        venta.setTotal(subtotal.add(impuesto).setScale(2, RoundingMode.HALF_UP));
        if (pedido) {
            venta.setEstado(ESTADO_PENDIENTE);
            venta.setFechaExpiracion(venta.getFechaCreacion().plusHours(horasExpiracionPedido));
            venta.setCodigoPedido(generarCodigoPedido());
            venta.setReferenciaPago(limpiarTexto(request.getReferenciaPago()));
            venta.setVendedor(null);
        } else {
            aplicarPagoEfectivo(venta, request);
        }

        VentaProducto ventaGuardada = ventaRepository.save(venta);
        if (!pedido) {
            registrarMovimiento(ventaGuardada, auth);
        }
        return ventaGuardada;
    }

    // Venta desde la app del entrenador: siempre se factura en SU sucursal, aunque la app mande
    // otra, para que no descuente inventario de un gimnasio que no es el suyo.
    @Transactional
    public VentaProductoResponse venderComoEntrenador(VentaProductoRequest request, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new NoAutenticadoException("Usuario no autenticado");
        }
        Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado"));
        Sucursal sucursal = personalRepository.findById(usuario.getId())
                .map(personal -> personal.getSucursal())
                .orElse(null);
        if (sucursal == null) {
            throw new ReglaNegocioException("Tu perfil no tiene una sucursal asignada. Pide a administración que te la asigne.");
        }
        request.setSucursalId(sucursal.getId());
        return vender(request, auth);
    }

    // Compra de un solo producto (versiones anteriores de la app): se convierte en pedido en efectivo.
    @Transactional
    public VentaProductoResponse comprarComoSocio(VentaProductoDetalleRequest detalle, Authentication auth) {
        PedidoSocioRequest pedido = new PedidoSocioRequest();
        pedido.setMetodoPago("Efectivo");
        pedido.setDetalles(detalle == null ? List.of() : List.of(detalle));
        return crearPedidoSocio(pedido, auth);
    }

    /**
     * El socio arma su carrito en la app y elige pagar en efectivo o por transferencia. Se genera
     * un código que muestra en recepción; ahí lo validan y se completa la compra.
     */
    @Transactional
    public VentaProductoResponse crearPedidoSocio(PedidoSocioRequest pedido, Authentication auth) {
        Socio socio = socioAutenticado(auth);
        if (socio.getSucursal() == null) {
            throw new ReglaNegocioException("Tu perfil no tiene una sucursal asignada. Acércate a recepción.");
        }
        if (pedido == null || pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("Agrega al menos un producto al carrito");
        }
        long pendientes = ventaRepository.countBySocio_UsuarioIdAndEstadoAndCodigoPedidoIsNotNull(
                socio.getUsuarioId(), ESTADO_PENDIENTE);
        if (pendientes >= maxPendientesPorSocio) {
            throw new ReglaNegocioException("Ya tienes " + pendientes + " pedidos sin pagar. Págalos o cancélalos en recepción antes de hacer otro.");
        }
        String metodo = textoOpcional(pedido.getMetodoPago(), "");
        if (!"Efectivo".equalsIgnoreCase(metodo) && !"Transferencia".equalsIgnoreCase(metodo)) {
            throw new ReglaNegocioException("Elige pagar en efectivo o por transferencia");
        }

        VentaProductoRequest request = new VentaProductoRequest();
        request.setClienteEventual(false);
        request.setSocioId(socio.getUsuarioId());
        request.setSucursalId(socio.getSucursal().getId());
        request.setMetodoPago("Efectivo".equalsIgnoreCase(metodo) ? "Efectivo" : "Transferencia");
        request.setReferenciaPago(pedido.getReferenciaPago());
        request.setObservaciones("Pedido desde la app - paga y retira en recepción");
        request.setDetalles(pedido.getDetalles());
        return toResponse(registrar(request, auth, true));
    }

    @Transactional(readOnly = true)
    public List<VentaProductoResponse> misPedidos(Authentication auth) {
        Socio socio = socioAutenticado(auth);
        return ventaRepository.findBySocio_UsuarioIdAndCodigoPedidoIsNotNullOrderByFechaCreacionDesc(socio.getUsuarioId())
                .stream().map(this::toResponse).toList();
    }

    /** Recepción, administración o el entrenador buscan el pedido por el código del socio. */
    @Transactional(readOnly = true)
    public VentaProductoResponse buscarPedido(String codigo, Authentication auth) {
        return toResponse(pedidoVisible(codigo, auth));
    }

    @Transactional
    public VentaProductoResponse confirmarPedido(String codigo, ConfirmarPedidoRequest datos, Authentication auth) {
        // El pedido se bloquea y se relee ANTES de mirar su estado: dos confirmaciones a la vez
        // (dos cajas, doble clic) ya no pueden pasar las dos el "está pendiente" y registrar dos
        // ingresos en caja. La segunda espera y al releer ve el pedido ya completado.
        VentaProducto venta = bloquearPedido(pedidoVisible(codigo, auth));
        exigirPendiente(venta);
        if (venta.getFechaExpiracion() != null && LocalDateTime.now().isAfter(venta.getFechaExpiracion())) {
            throw new ReglaNegocioException("Este pedido venció sin pagarse. Pide al socio que haga uno nuevo.");
        }
        Usuario quien = obtenerVendedor(auth);

        if ("Efectivo".equalsIgnoreCase(venta.getMetodoPago()) && datos != null && datos.getMontoRecibido() != null) {
            if (datos.getMontoRecibido().compareTo(venta.getTotal()) < 0) {
                throw new ReglaNegocioException("El efectivo recibido no cubre el total del pedido");
            }
            venta.setMonedaPago(obtenerMonedaVenta(new VentaProductoRequest()));
            venta.setTipoCambio(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
            venta.setMontoRecibido(datos.getMontoRecibido().setScale(2, RoundingMode.HALF_UP));
            venta.setMontoRecibidoConvertido(venta.getMontoRecibido());
            venta.setCambio(venta.getMontoRecibido().subtract(venta.getTotal()).setScale(2, RoundingMode.HALF_UP));
        }
        if (datos != null && limpiarTexto(datos.getReferenciaPago()) != null) {
            venta.setReferenciaPago(limpiarTexto(datos.getReferenciaPago()));
        }

        venta.setEstado(ESTADO_COMPLETADA);
        venta.setVendedor(quien);
        venta.setFechaValidacion(LocalDateTime.now());
        venta.setValidadoPor(quien != null ? quien.getEmail() : auth.getName());
        VentaProducto guardada = ventaRepository.save(venta);
        registrarMovimiento(guardada, auth);
        return toResponse(guardada);
    }

    /** Cancela un pedido sin pagar y devuelve al inventario lo que tenía apartado. */
    @Transactional
    public VentaProductoResponse cancelarPedido(String codigo, Authentication auth) {
        VentaProducto venta = bloquearPedido(pedidoVisible(codigo, auth));
        exigirPendiente(venta);
        Usuario quien = obtenerVendedor(auth);
        liberarStockYCancelar(venta, quien != null ? quien.getEmail() : auth.getName(), null);
        return toResponse(ventaRepository.save(venta));
    }

    /**
     * Cancela un pedido de la app que venció sin pagarse (lo llama el scheduler, uno por uno y
     * cada uno en su propia transacción). Si mientras tanto lo pagaron o cancelaron, no hace nada.
     */
    @Transactional
    public boolean expirarPedido(UUID ventaId) {
        VentaProducto venta = ventaRepository.findById(ventaId).orElse(null);
        if (venta == null) {
            return false;
        }
        bloquearPedido(venta);
        if (!ESTADO_PENDIENTE.equalsIgnoreCase(venta.getEstado())) {
            return false;
        }
        liberarStockYCancelar(venta, "Sistema (pedido vencido)", "Vencido sin pagar");
        ventaRepository.save(venta);
        if (venta.getSocio() != null && venta.getSocio().getUsuario() != null) {
            try {
                notificacionService.registrarNotificacion(venta.getSocio().getUsuario(), "alerta",
                        "Pedido vencido",
                        "Tu pedido " + venta.getCodigoPedido() + " venció sin pagarse y se liberaron los productos.");
            } catch (RuntimeException e) {
                log.warn("No se pudo notificar el vencimiento del pedido {}: {}", venta.getCodigoPedido(), e.getMessage());
            }
        }
        return true;
    }

    public List<UUID> idsPedidosVencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        return ventaRepository.idsPedidosVencidos(ahora, ahora.minusHours(horasExpiracionPedido));
    }

    private void liberarStockYCancelar(VentaProducto venta, String quien, String nota) {
        Map<UUID, Long> aDevolver = new java.util.TreeMap<>();
        if (!venta.getReservas().isEmpty()) {
            // Pedidos nuevos: se devuelve exactamente lo que se apartó.
            venta.getReservas().forEach(reserva -> aDevolver.merge(reserva.getProductoId(), (long) reserva.getCantidad(), Long::sum));
        } else {
            // Pedidos creados antes de guardar las reservas: se reconstruye con la receta actual.
            List<UUID> ids = venta.getDetalles().stream().map(d -> d.getProducto().getId()).distinct().toList();
            Map<UUID, List<LineaReceta>> recetas = componenteRepository.recetasDe(ids).stream()
                    .collect(Collectors.groupingBy(LineaReceta::productoId));
            for (VentaProductoDetalle detalle : venta.getDetalles()) {
                List<LineaReceta> receta = recetas.getOrDefault(detalle.getProducto().getId(), List.of());
                if (receta.isEmpty()) {
                    aDevolver.merge(detalle.getProducto().getId(), (long) detalle.getCantidad(), Long::sum);
                } else {
                    receta.forEach(linea -> aDevolver.merge(linea.insumoId(), (long) detalle.getCantidad() * linea.cantidad(), Long::sum));
                }
            }
        }

        // Mismo orden de bloqueo que las ventas (ids ordenados) para no provocar deadlocks.
        for (Map.Entry<UUID, Long> linea : aDevolver.entrySet()) {
            ProductoInventario fila = bloquearYReleer(linea.getKey());
            if (fila == null) {
                continue; // El producto se eliminó: no hay a dónde devolver.
            }
            devolver(fila, Math.toIntExact(linea.getValue()));
            productoRepository.save(fila);
        }

        venta.getReservas().clear();
        venta.setEstado(ESTADO_CANCELADA);
        venta.setFechaValidacion(LocalDateTime.now());
        venta.setValidadoPor(quien);
        if (nota != null) {
            String obs = venta.getObservaciones() == null ? nota : venta.getObservaciones() + " - " + nota;
            venta.setObservaciones(obs.length() > 255 ? obs.substring(0, 255) : obs);
        }
    }

    /**
     * Bloquea la fila del pedido y vuelve a leerla de la base. Hace falta releer: el pedido ya
     * venía cargado en la sesión y una consulta con bloqueo devolvería esa copia sin refrescar.
     */
    private VentaProducto bloquearPedido(VentaProducto venta) {
        entityManager.refresh(venta, LockModeType.PESSIMISTIC_WRITE, ESPERA_BLOQUEO);
        return venta;
    }

    /** Bloquea un producto/insumo y relee su stock (puede venir cargado desde el detalle). */
    private ProductoInventario bloquearYReleer(UUID productoId) {
        ProductoInventario fila = entityManager.find(ProductoInventario.class, productoId);
        if (fila == null) {
            return null;
        }
        entityManager.refresh(fila, LockModeType.PESSIMISTIC_WRITE, ESPERA_BLOQUEO);
        return fila;
    }

    private static void devolver(ProductoInventario fila, int cantidad) {
        fila.setStock((fila.getStock() == null ? 0 : fila.getStock()) + cantidad);
    }

    private static void exigirPendiente(VentaProducto venta) {
        if (!ESTADO_PENDIENTE.equalsIgnoreCase(venta.getEstado())) {
            throw new ReglaNegocioException("Este pedido ya está " + venta.getEstado().toLowerCase(Locale.ROOT));
        }
    }

    // El personal de una sucursal (recepción, entrenador) solo ve los pedidos de la suya.
    private VentaProducto pedidoVisible(String codigo, Authentication auth) {
        String limpio = normalizarCodigoPedido(codigo);
        VentaProducto venta = ventaRepository.findByCodigoPedidoIgnoreCase(limpio)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ningún pedido con el código " + limpio));
        if (!alcanceSucursal.alcanza(obtenerVendedor(auth), venta.getSucursal().getId())) {
            throw new AccesoDenegadoException("Ese pedido es de otra sucursal (" + venta.getSucursal().getNombre() + ")");
        }
        return venta;
    }

    /** Acepta el código como lo dicten o escriban: "p 98gyw7", "P98GYW7" o "P-98GYW7". */
    static String normalizarCodigoPedido(String codigo) {
        String letras = codigo == null ? "" : codigo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (letras.length() == 7 && letras.startsWith("P")) {
            return "P-" + letras.substring(1);
        }
        return letras.length() == 6 ? "P-" + letras : letras;
    }

    private Socio socioAutenticado(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new NoAutenticadoException("Usuario no autenticado");
        }
        Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado"));
        return socioRepository.findById(usuario.getId())
                .orElseThrow(() -> new AccesoDenegadoException("Perfil de socio no encontrado"));
    }

    // Sin 0/O ni 1/I/L para que se pueda dictar y escribir sin confusiones.
    private static final String ALFABETO_CODIGO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final java.security.SecureRandom AZAR = new java.security.SecureRandom();

    private String generarCodigoPedido() {
        for (int intento = 0; intento < 20; intento++) {
            StringBuilder codigo = new StringBuilder("P-");
            for (int i = 0; i < 6; i++) {
                codigo.append(ALFABETO_CODIGO.charAt(AZAR.nextInt(ALFABETO_CODIGO.length())));
            }
            if (!ventaRepository.existsByCodigoPedido(codigo.toString())) {
                return codigo.toString();
            }
        }
        throw new IllegalStateException("No se pudo generar un código de pedido único");
    }

    private List<UUID> idsDeLaVenta(List<VentaProductoDetalleRequest> detalles) {
        List<UUID> ids = new ArrayList<>();
        for (VentaProductoDetalleRequest detalle : detalles) {
            if (detalle.getProductoId() == null) {
                throw new ReglaNegocioException("Selecciona un producto para cada linea de la venta");
            }
            if (!ids.contains(detalle.getProductoId())) {
                ids.add(detalle.getProductoId());
            }
        }
        return ids;
    }

    private Map<UUID, ProductoInventario> bloquearProductos(Set<UUID> idsOrdenados) {
        Map<UUID, ProductoInventario> productos = new LinkedHashMap<>();
        for (UUID id : idsOrdenados) {
            productos.put(id, productoRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado")));
        }
        return productos;
    }

    private static String unidadDe(ProductoInventario producto) {
        String unidad = producto.getUnidad();
        return unidad == null || unidad.isBlank() || "unidad".equals(unidad) ? "unid." : unidad;
    }

    private Usuario obtenerVendedor(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }

        return usuarioRepository.findByEmail(auth.getName()).orElse(null);
    }

    private BigDecimal obtenerTasaImpuesto() {
        Map<String, Object> configuracion = configuracionAppService.obtener();
        Object valor = configuracion.get("taxRate");
        try {
            return new BigDecimal(String.valueOf(valor == null ? "0" : valor));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String obtenerMonedaVenta(VentaProductoRequest request) {
        String monedaRequest = limpiarTexto(request.getMonedaVenta());
        if (monedaRequest != null && !monedaRequest.isBlank()) {
            return monedaRequest.toUpperCase(Locale.ROOT);
        }

        Object moneda = configuracionAppService.obtener().get("currency");
        return moneda == null ? "USD" : String.valueOf(moneda).toUpperCase(Locale.ROOT);
    }

    private void aplicarPagoEfectivo(VentaProducto venta, VentaProductoRequest request) {
        if (!"Efectivo".equalsIgnoreCase(venta.getMetodoPago())) {
            venta.setMonedaPago(null);
            venta.setTipoCambio(null);
            venta.setMontoRecibido(null);
            venta.setMontoRecibidoConvertido(null);
            venta.setCambio(null);
            return;
        }

        String monedaVenta = obtenerMonedaVenta(request);
        String monedaPago = textoOpcional(request.getMonedaPago(), monedaVenta).toUpperCase(Locale.ROOT);
        BigDecimal montoRecibido = request.getMontoRecibido();
        if (montoRecibido == null || montoRecibido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("Ingresa el monto recibido en efectivo");
        }

        BigDecimal tipoCambio = monedaPago.equals(monedaVenta)
                ? BigDecimal.ONE
                : request.getTipoCambio();
        if (tipoCambio == null || tipoCambio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("Ingresa un tipo de cambio valido para pago en efectivo");
        }

        BigDecimal montoConvertido = montoRecibido.multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cambio = montoConvertido.subtract(venta.getTotal()).setScale(2, RoundingMode.HALF_UP);
        if (cambio.compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El monto recibido no cubre el total de la factura");
        }

        venta.setMonedaPago(monedaPago);
        venta.setTipoCambio(tipoCambio.setScale(4, RoundingMode.HALF_UP));
        venta.setMontoRecibido(montoRecibido.setScale(2, RoundingMode.HALF_UP));
        venta.setMontoRecibidoConvertido(montoConvertido);
        venta.setCambio(cambio);
    }

    private void registrarMovimiento(VentaProducto venta, Authentication auth) {
        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setConcepto("Venta de productos " + venta.getNumeroFactura());
        movimiento.setMonto(venta.getTotal());
        movimiento.setTipo("ingreso");
        movimiento.setMetodo(venta.getMetodoPago());
        movimiento.setCategoria("Venta de productos");
        movimiento.setUsuario(auth != null && auth.getName() != null ? auth.getName() : "Sistema");
        movimiento.setSucursal(venta.getSucursal());
        movimiento.setFechaTransaccion(venta.getFechaCreacion());
        movimientoRepository.save(movimiento);
    }

    private String generarNumeroFactura() {
        String fecha = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT).format(LocalDateTime.now());
        return "FAC-" + fecha + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String textoRequerido(String valor, String mensaje) {
        String limpio = limpiarTexto(valor);
        if (limpio == null || limpio.isBlank()) {
            throw new ReglaNegocioException(mensaje);
        }
        return limpio;
    }

    private String textoOpcional(String valor, String respaldo) {
        String limpio = limpiarTexto(valor);
        return limpio == null || limpio.isBlank() ? respaldo : limpio;
    }

    private String limpiarTexto(String valor) {
        return valor == null ? null : valor.trim();
    }

    private VentaProductoResponse toResponse(VentaProducto venta) {
        Socio socio = venta.getSocio();
        Usuario vendedor = venta.getVendedor();

        return VentaProductoResponse.builder()
                .id(venta.getId())
                .numeroFactura(venta.getNumeroFactura())
                .clienteEventual(venta.getClienteEventual())
                .socioId(socio != null ? socio.getUsuarioId() : null)
                .socio(socio != null ? socio.getNombres() + " " + socio.getApellidos() : null)
                .sucursalId(venta.getSucursal().getId())
                .sucursal(venta.getSucursal().getNombre())
                .fechaExpiracion(ESTADO_PENDIENTE.equalsIgnoreCase(venta.getEstado()) ? venta.getFechaExpiracion() : null)
                .vendedorId(vendedor != null ? vendedor.getId() : null)
                .vendedor(vendedor != null ? vendedor.getEmail() : null)
                .clienteNombre(venta.getClienteNombre())
                .clienteDocumento(venta.getClienteDocumento())
                .metodoPago(venta.getMetodoPago())
                .monedaPago(venta.getMonedaPago())
                .tipoCambio(venta.getTipoCambio())
                .montoRecibido(venta.getMontoRecibido())
                .montoRecibidoConvertido(venta.getMontoRecibidoConvertido())
                .cambio(venta.getCambio())
                .subtotal(venta.getSubtotal())
                .impuesto(venta.getImpuesto())
                .total(venta.getTotal())
                .observaciones(venta.getObservaciones())
                .fechaCreacion(venta.getFechaCreacion())
                .estado(venta.getEstado())
                .codigoPedido(venta.getCodigoPedido())
                .referenciaPago(venta.getReferenciaPago())
                .fechaValidacion(venta.getFechaValidacion())
                .validadoPor(venta.getValidadoPor())
                .detalles(venta.getDetalles().stream().map(this::detalleResponse).toList())
                .build();
    }

    private FacturaDetalleResponse detalleResponse(VentaProductoDetalle detalle) {
        return FacturaDetalleResponse.builder()
                .id(detalle.getId())
                .productoId(detalle.getProducto().getId())
                .productoNombre(detalle.getProductoNombre())
                .cantidad(detalle.getCantidad())
                .precioUnitario(detalle.getPrecioUnitario())
                .totalLinea(detalle.getTotalLinea())
                .build();
    }
}
