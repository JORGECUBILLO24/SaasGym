package ni.edu.uam.GimnasioAPI.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ni.edu.uam.GimnasioAPI.dto.FacturaMembresiaResponse;
import ni.edu.uam.GimnasioAPI.dto.SolicitudPagoResponse;
import ni.edu.uam.GimnasioAPI.dto.SolicitudTransferenciaRequest;
import ni.edu.uam.GimnasioAPI.exceptions.AccesoDenegadoException;
import ni.edu.uam.GimnasioAPI.exceptions.ConflictoException;
import ni.edu.uam.GimnasioAPI.exceptions.NoAutenticadoException;
import ni.edu.uam.GimnasioAPI.exceptions.RecursoNoEncontradoException;
import ni.edu.uam.GimnasioAPI.exceptions.ReglaNegocioException;
import ni.edu.uam.GimnasioAPI.models.CuentaBancaria;
import ni.edu.uam.GimnasioAPI.models.Pago;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.SolicitudPagoMembresia;
import ni.edu.uam.GimnasioAPI.models.TipoMembresia;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.CuentaBancariaRepository;
import ni.edu.uam.GimnasioAPI.repositories.PagoRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SolicitudPagoMembresiaRepository;
import ni.edu.uam.GimnasioAPI.repositories.TipoMembresiaRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.security.AlcanceSucursal;
import ni.edu.uam.GimnasioAPI.util.FotoPerfilValidator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Pago de membresía por transferencia bancaria.
 *
 * 1. Administración publica las cuentas del gimnasio (CuentaBancariaService).
 * 2. El socio elige plan y cuenta en la app, transfiere y reporta la referencia: queda Pendiente.
 * 3. Recepción/administración confirma en el banco y aprueba: recién ahí se registra el pago y se
 *    activa o extiende la membresía. Si no aparece, la rechaza con un motivo.
 *
 * Antes el socio elegía "Efectivo" en la app y la membresía se activaba al instante sin que
 * nadie cobrara nada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferenciaService {

    private static final Pattern REFERENCIA = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 ./_-]{2,39}$");

    private final SolicitudPagoMembresiaRepository solicitudRepository;
    private final CuentaBancariaRepository cuentaRepository;
    private final TipoMembresiaRepository tipoMembresiaRepository;
    private final SocioRepository socioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final PagoService pagoService;
    private final NotificacionService notificacionService;
    private final AlcanceSucursal alcanceSucursal;

    @Transactional
    public SolicitudPagoResponse solicitar(String emailSocio, SolicitudTransferenciaRequest request) {
        Socio socio = socioDe(emailSocio);
        if (request == null) {
            throw new ReglaNegocioException("Faltan los datos del pago");
        }
        String metodo = request.getMetodoPago() == null ? "" : request.getMetodoPago().trim();
        if (!metodo.isEmpty() && !"Transferencia".equalsIgnoreCase(metodo)) {
            throw new ReglaNegocioException("Paga en recepción o envía una transferencia para que la confirmen.");
        }
        if (request.getCuentaBancariaId() == null) {
            throw new ReglaNegocioException("Elige la cuenta bancaria a la que transferiste");
        }
        if (request.getTipoMembresiaId() == null) {
            throw new ReglaNegocioException("Elige el plan de membresía");
        }
        TipoMembresia tipo = tipoMembresiaRepository.findById(request.getTipoMembresiaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan de membresía no encontrado"));
        CuentaBancaria cuenta = cuentaRepository.findById(request.getCuentaBancariaId())
                .filter(c -> Boolean.TRUE.equals(c.getActiva()))
                .orElseThrow(() -> new ReglaNegocioException("Esa cuenta bancaria ya no está disponible"));

        String referencia = request.getReferencia() == null ? "" : request.getReferencia().trim();
        if (!REFERENCIA.matcher(referencia).matches()) {
            throw new ReglaNegocioException("Escribe el número de referencia de la transferencia (3 a 40 letras o números)");
        }
        if (solicitudRepository.existsBySocio_UsuarioIdAndEstado(socio.getUsuarioId(), SolicitudPagoMembresia.PENDIENTE)) {
            throw new ConflictoException("Ya tienes un pago en revisión. Espera a que recepción lo confirme.");
        }
        if (solicitudRepository.referenciaEnUso(cuenta.getId(), referencia)) {
            throw new ConflictoException("Esa referencia ya fue reportada. Revisa el número de tu comprobante.");
        }

        SolicitudPagoMembresia solicitud = new SolicitudPagoMembresia();
        solicitud.setSocio(socio);
        solicitud.setTipoMembresia(tipo);
        solicitud.setCuentaBancaria(cuenta);
        solicitud.setMonto(tipo.getPrecio());
        solicitud.setReferencia(referencia);
        solicitud.setComprobanteUrl(FotoPerfilValidator.validar(request.getComprobanteUrl()));
        solicitud.setEstado(SolicitudPagoMembresia.PENDIENTE);
        solicitud.setFechaCreacion(LocalDateTime.now());
        SolicitudPagoMembresia guardada = solicitudRepository.save(solicitud);
        log.info("Transferencia reportada por {} ({} {}, ref. {})", emailSocio, cuenta.getBanco(), cuenta.getMoneda(), referencia);
        return SolicitudPagoResponse.desde(guardada, false);
    }

    @Transactional(readOnly = true)
    public List<SolicitudPagoResponse> misSolicitudes(String emailSocio) {
        Socio socio = socioDe(emailSocio);
        return solicitudRepository.findBySocio_UsuarioIdOrderByFechaCreacionDesc(socio.getUsuarioId()).stream()
                .map(s -> SolicitudPagoResponse.desde(s, false))
                .toList();
    }

    /** Recepción ve las de socios de su sucursal; administración, todas. */
    @Transactional(readOnly = true)
    public List<SolicitudPagoResponse> listarParaPersonal(Authentication auth, String estado) {
        Usuario actor = actor(auth);
        List<SolicitudPagoMembresia> solicitudes = estado == null || estado.isBlank()
                ? solicitudRepository.findTop100ByOrderByFechaCreacionDesc()
                : solicitudRepository.findByEstadoOrderByFechaCreacionAsc(normalizarEstado(estado));
        return solicitudes.stream()
                .filter(s -> alcanceSucursal.alcanzaSocio(actor, s.getSocio()))
                .map(s -> SolicitudPagoResponse.desde(s, true))
                .toList();
    }

    @Transactional
    public FacturaMembresiaResponse aprobar(UUID solicitudId, Authentication auth) {
        Usuario actor = actor(auth);
        // Bloqueada: dos aprobaciones simultáneas no pueden activar dos membresías con un pago.
        SolicitudPagoMembresia solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de pago no encontrada"));
        exigirAlcance(actor, solicitud);
        exigirPendiente(solicitud);

        FacturaMembresiaResponse factura = pagoService.activarMembresiaPagada(
                solicitud.getSocio(), solicitud.getTipoMembresia(), "Transferencia", solicitud.getMonto());
        Pago pago = pagoRepository.findById(factura.getId()).orElse(null);

        solicitud.setEstado(SolicitudPagoMembresia.APROBADA);
        solicitud.setPago(pago);
        solicitud.setFechaRevision(LocalDateTime.now());
        solicitud.setRevisadoPor(actor.getEmail());
        solicitudRepository.save(solicitud);

        notificar(solicitud, "Pago aprobado",
                "Confirmamos tu transferencia (ref. " + solicitud.getReferencia() + "). Tu membresía "
                        + solicitud.getTipoMembresia().getNombre() + " está activa hasta el " + factura.getFechaVencimiento() + ".");
        log.info("{} aprobó la transferencia {} de {}", actor.getEmail(), solicitud.getReferencia(), solicitud.getSocio().getUsuario().getEmail());
        return factura;
    }

    @Transactional
    public SolicitudPagoResponse rechazar(UUID solicitudId, String motivo, Authentication auth) {
        Usuario actor = actor(auth);
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.length() < 3) {
            throw new ReglaNegocioException("Escribe el motivo del rechazo para que el socio sepa qué corregir");
        }
        if (motivoLimpio.length() > 255) {
            motivoLimpio = motivoLimpio.substring(0, 255);
        }
        SolicitudPagoMembresia solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de pago no encontrada"));
        exigirAlcance(actor, solicitud);
        exigirPendiente(solicitud);

        solicitud.setEstado(SolicitudPagoMembresia.RECHAZADA);
        solicitud.setMotivoRechazo(motivoLimpio);
        solicitud.setFechaRevision(LocalDateTime.now());
        solicitud.setRevisadoPor(actor.getEmail());
        SolicitudPagoMembresia guardada = solicitudRepository.save(solicitud);

        notificar(solicitud, "Pago no confirmado",
                "No pudimos confirmar tu transferencia (ref. " + solicitud.getReferencia() + "): " + motivoLimpio);
        return SolicitudPagoResponse.desde(guardada, true);
    }

    private void exigirPendiente(SolicitudPagoMembresia solicitud) {
        if (!SolicitudPagoMembresia.PENDIENTE.equals(solicitud.getEstado())) {
            throw new ConflictoException("Esta solicitud ya fue " + solicitud.getEstado().toLowerCase());
        }
    }

    private void exigirAlcance(Usuario actor, SolicitudPagoMembresia solicitud) {
        if (!alcanceSucursal.alcanzaSocio(actor, solicitud.getSocio())) {
            throw new AccesoDenegadoException("Ese socio pertenece a otra sucursal");
        }
    }

    private void notificar(SolicitudPagoMembresia solicitud, String titulo, String mensaje) {
        try {
            notificacionService.registrarNotificacion(solicitud.getSocio().getUsuario(), "pago", titulo, mensaje);
        } catch (RuntimeException e) {
            log.warn("No se pudo notificar al socio sobre la solicitud {}: {}", solicitud.getId(), e.getMessage());
        }
    }

    private static String normalizarEstado(String estado) {
        String limpio = estado.trim();
        for (String valido : List.of(SolicitudPagoMembresia.PENDIENTE, SolicitudPagoMembresia.APROBADA, SolicitudPagoMembresia.RECHAZADA)) {
            if (valido.equalsIgnoreCase(limpio)) {
                return valido;
            }
        }
        throw new ReglaNegocioException("Estado no válido: usa Pendiente, Aprobada o Rechazada");
    }

    private Socio socioDe(String email) {
        if (email == null) {
            throw new NoAutenticadoException("Usuario no autenticado");
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado"));
        return socioRepository.findById(usuario.getId())
                .orElseThrow(() -> new AccesoDenegadoException("Perfil de socio no encontrado"));
    }

    private Usuario actor(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new NoAutenticadoException("Usuario no autenticado");
        }
        return usuarioRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado"));
    }
}
