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
import org.springframework.stereotype.Service;
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
    private final ConfiguracionAppService configuracionAppService;

    // Renovacion desde el panel (recepcion/admin): el socio viene en el request.
    @Transactional
    public FacturaMembresiaResponse registrarPagoYMembresia(PagoRequest request) {
        Socio socio = socioRepository.findById(request.getIdSocio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Socio no encontrado"));
        return procesarRenovacion(socio, request.getIdTipoMembresia(), request.getMetodoPago());
    }

    // Renovacion desde la app: el socio se resuelve del token, solo puede pagar lo suyo.
    @Transactional
    public FacturaMembresiaResponse renovarMembresiaSocio(String email, Integer tipoMembresiaId, String metodoPago) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado"));
        Socio socio = socioRepository.findById(usuario.getId())
                .orElseThrow(() -> new AccesoDenegadoException("Perfil de socio no encontrado"));
        return procesarRenovacion(socio, tipoMembresiaId, metodoPago);
    }

    private FacturaMembresiaResponse procesarRenovacion(Socio socio, Integer tipoMembresiaId, String metodoPago) {
        String metodoPagoValidado = MembresiaService.validarMetodoPago(metodoPago);
        TipoMembresia tipoMembresia = tipoMembresiaRepository.findById(tipoMembresiaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de membresía no encontrado"));

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
            vieja.setEstado("Renovada");
            membresiaSocioRepository.saveAndFlush(vieja);
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
        pago = pagoRepository.save(pago);

        MembresiaSocio nuevaMembresia = new MembresiaSocio();
        nuevaMembresia.setSocio(socio);
        nuevaMembresia.setTipoMembresia(tipoMembresia);
        nuevaMembresia.setPago(pago);
        nuevaMembresia.setFechaInicio(hoy);
        nuevaMembresia.setFechaVencimiento(baseVencimiento.plusDays(tipoMembresia.getDuracionDias()));
        nuevaMembresia.setEstado("Activa");
        MembresiaSocio guardada = membresiaSocioRepository.save(nuevaMembresia);

        socio.setEstadoAcceso("Activo");
        socioRepository.save(socio);

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
}
