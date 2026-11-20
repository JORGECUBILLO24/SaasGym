package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.FacturaMembresiaResponse;
import ni.edu.uam.GimnasioAPI.dto.PagoRequest;
import ni.edu.uam.GimnasioAPI.models.MembresiaSocio;
import ni.edu.uam.GimnasioAPI.models.Pago;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.TipoMembresia;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.MembresiaSocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.PagoRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.TipoMembresiaRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.repositories.PersonalRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final SocioRepository socioRepository;
    private final TipoMembresiaRepository tipoMembresiaRepository;
    private final PagoRepository pagoRepository;
    private final MembresiaSocioRepository membresiaSocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonalRepository personalRepository;
    private final ConfiguracionAppService configuracionAppService;

    // Renovacion desde el panel (recepcion/admin): el socio viene en el request.
    @Transactional
    public FacturaMembresiaResponse registrarPagoYMembresia(PagoRequest request) {
        Socio socio = socioRepository.findById(request.getIdSocio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Socio no encontrado"));
        return procesarRenovacion(socio, request.getIdTipoMembresia(), request.getMetodoPago(), false);
    }

    // Renovacion desde la app: el socio se resuelve del token, solo puede pagar lo suyo.
    @Transactional
    public FacturaMembresiaResponse renovarMembresiaSocio(String email, Integer tipoMembresiaId, String metodoPago) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado"));
        Socio socio = socioRepository.findById(usuario.getId())
                .orElseThrow(() -> new AccesoDenegadoException("Perfil de socio no encontrado"));
        return procesarRenovacion(socio, tipoMembresiaId, metodoPago, true);
    }

    private FacturaMembresiaResponse procesarRenovacion(Socio socio, Integer tipoMembresiaId, String metodoPago,
                                                        boolean desdeApp) {
        String metodoPagoValidado = MembresiaService.validarMetodoPago(metodoPago);
        TipoMembresia tipoMembresia = tipoMembresiaRepository.findById(tipoMembresiaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de membresía no encontrado"));
        if (membresiaSocioRepository.findFirstBySocioUsuarioIdAndEstado(socio.getUsuarioId(), "Pendiente").isPresent()) {
            throw new ConflictoException("Ya tienes una renovación pendiente de confirmación.");
        }
        boolean requiereConfirmacion = desdeApp
                ? !"Efectivo".equalsIgnoreCase(metodoPagoValidado)
                : "Transferencia".equalsIgnoreCase(metodoPagoValidado);

        // Se cierra la membresía activa anterior. saveAndFlush garantiza que el UPDATE
        // (Activa -> Renovada) ocurra antes del INSERT de la nueva, respetando el índice único.
        // Si se renueva antes de tiempo, los días que quedaban se suman al nuevo periodo.
        LocalDate hoy = LocalDate.now();
        LocalDate baseVencimiento = hoy;
        Optional<MembresiaSocio> membresiaActiva = membresiaSocioRepository.findBySocioUsuarioIdAndEstado(socio.getUsuarioId(), "Activa");
        if (membresiaActiva.isPresent()) {
            MembresiaSocio vieja = membresiaActiva.get();
            if (vieja.getFechaVencimiento() != null && vieja.getFechaVencimiento().isAfter(hoy)) {
                baseVencimiento = vieja.getFechaVencimiento();
            }
            if (!requiereConfirmacion) {
                vieja.setEstado("Renovada");
                membresiaSocioRepository.saveAndFlush(vieja);
            }
        }

        // La membresía se cobra al precio del plan (IVA incluido): el socio paga el precio y
        // la factura desglosa el impuesto por dentro (subtotal + impuesto = total = precio).
        BigDecimal total = tipoMembresia.getPrecio().setScale(2, RoundingMode.HALF_UP);
        BigDecimal tasa = obtenerTasaImpuesto();
        BigDecimal factor = BigDecimal.ONE.add(tasa.divide(CIEN, 6, RoundingMode.HALF_UP));
        BigDecimal subtotal = total.divide(factor, 2, RoundingMode.HALF_UP);
        BigDecimal impuesto = total.subtract(subtotal).setScale(2, RoundingMode.HALF_UP);

        Pago pago = new Pago();
        pago.setSocio(socio);
        pago.setMonto(total);
        pago.setMetodoPago(metodoPagoValidado);
        pago.setNumeroFactura(generarNumeroFactura());
        pago.setFechaTransaccion(LocalDateTime.now());
        pago.setEstado(requiereConfirmacion ? "Pendiente" : "Confirmado");
        pago = pagoRepository.save(pago);

        MembresiaSocio nuevaMembresia = new MembresiaSocio();
        nuevaMembresia.setSocio(socio);
        nuevaMembresia.setTipoMembresia(tipoMembresia);
        nuevaMembresia.setPago(pago);
        nuevaMembresia.setFechaInicio(hoy);
        nuevaMembresia.setFechaVencimiento(baseVencimiento.plusDays(tipoMembresia.getDuracionDias()));
        nuevaMembresia.setEstado(requiereConfirmacion ? "Pendiente" : "Activa");
        MembresiaSocio guardada = membresiaSocioRepository.save(nuevaMembresia);

        if (!requiereConfirmacion) {
            socio.setEstadoAcceso("Activo");
            socioRepository.save(socio);
        }

        return FacturaMembresiaResponse.builder()
                .id(pago.getId())
                .numeroFactura(pago.getNumeroFactura())
                .socioId(socio.getUsuarioId())
                .socio(socio.getNombres() + " " + socio.getApellidos())
                .tipoMembresia(tipoMembresia.getNombre())
                .subtotal(subtotal)
                .impuesto(impuesto)
                .total(total)
                .metodoPago(pago.getMetodoPago())
                .fechaInicio(guardada.getFechaInicio())
                .fechaVencimiento(guardada.getFechaVencimiento())
                .fechaTransaccion(pago.getFechaTransaccion())
                .build();
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

    private String generarNumeroFactura() {
        String fecha = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT).format(LocalDateTime.now());
        return "MEM-" + fecha + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    @Transactional
    public void confirmarPago(UUID idPago) {
        confirmarPago(idPago, null);
    }

    @Transactional
    public void confirmarPago(UUID idPago, Authentication auth) {
        Pago pago = pagoRepository.findByIdForUpdate(idPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado"));
        if ("Confirmado".equalsIgnoreCase(pago.getEstado())) {
            return;
        }

        MembresiaSocio membresia = membresiaSocioRepository.findByPagoId(idPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("Membresía pendiente no encontrada"));
        Socio socio = membresia.getSocio();
        validarSucursalPersonal(auth, socio);

        membresiaSocioRepository.findBySocioUsuarioIdAndEstado(socio.getUsuarioId(), "Activa")
                .ifPresent(actual -> {
                    if (actual.getId().equals(membresia.getId())) {
                        return;
                    }
                    actual.setEstado("Renovada");
                    membresiaSocioRepository.save(actual);
                });

        pago.setEstado("Confirmado");
        pagoRepository.save(pago);
        membresia.setEstado("Activa");
        membresiaSocioRepository.save(membresia);
        socio.setEstadoAcceso("Activo");
        socioRepository.save(socio);
    }

    private void validarSucursalPersonal(Authentication auth, Socio socio) {
        if (auth == null || auth.getName() == null) {
            return;
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
        if (usuario == null || usuario.getRol() == null) {
            return;
        }
        String rol = usuario.getRol().getNombre();
        if (!"ROLE_ENTRENADOR".equals(rol) && !"ROLE_RECEPCIONISTA".equals(rol)) {
            return;
        }
        var personal = personalRepository.findById(usuario.getId()).orElse(null);
        UUID sucursalPersonal = personal != null && personal.getSucursal() != null
                ? personal.getSucursal().getId() : null;
        UUID sucursalSocio = socio.getSucursal() != null ? socio.getSucursal().getId() : null;
        if (sucursalPersonal == null || !sucursalPersonal.equals(sucursalSocio)) {
            throw new AccesoDenegadoException("El pago pertenece a otra sucursal.");
        }
    }
}
