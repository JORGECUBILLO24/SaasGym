package ni.edu.uam.GimnasioAPI.services;

import jakarta.annotation.PostConstruct;
import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaQrRequest;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaQrTokenResponse;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaQrValidationResponse;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaRequest;
import ni.edu.uam.GimnasioAPI.models.Asistencia;
import ni.edu.uam.GimnasioAPI.models.MembresiaSocio;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.AsistenciaRepository;
import ni.edu.uam.GimnasioAPI.repositories.MembresiaSocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private static final String QR_SOCIO_PREFIX = "GIMNASIO_ASISTENCIA";
    private static final String QR_WEB_PREFIX = "GIMNASIO_VALIDACION_WEB";
    private static final long QR_TTL_SECONDS = 180;
    private static final long MARGEN_CONSULTA_QR_SEGUNDOS = 60;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int LARGO_MINIMO_SECRETO = 32;
    // Solo se usa en el perfil "local". Fuera de ese perfil, app.qr.secret es obligatorio.
    private static final String SECRETO_DESARROLLO = "secreto-qr-solo-para-desarrollo-local-no-usar-en-produccion";
    // Valor que antes se usaba por defecto y esta publicado en el repositorio: nunca es valido en produccion.
    private static final String SECRETO_PUBLICO_ANTIGUO = "spartan-gym-attendance-secret";

    private final AsistenciaRepository asistenciaRepository;
    private final SocioRepository socioRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembresiaSocioRepository membresiaSocioRepository;
    private final QrWebSessionStore sesionesQrWeb;
    private final Environment environment;

    @Value("${app.qr.secret:}")
    private String qrSecret;

    // Falla el arranque si falta ATTENDANCE_QR_SECRET fuera del perfil local, en vez de firmar
    // los QR con un valor fijo que cualquiera puede leer en el repositorio y usar para fabricar QR.
    @PostConstruct
    void validarSecreto() {
        boolean local = Arrays.asList(environment.getActiveProfiles()).contains("local");
        if (qrSecret == null || qrSecret.isBlank()) {
            if (local) {
                qrSecret = SECRETO_DESARROLLO;
                return;
            }
            throw new IllegalStateException(
                    "app.qr.secret no esta configurado (variable de entorno ATTENDANCE_QR_SECRET). "
                            + "Es obligatoria fuera del perfil 'local'.");
        }
        if (!local && (qrSecret.length() < LARGO_MINIMO_SECRETO || SECRETO_PUBLICO_ANTIGUO.equals(qrSecret))) {
            throw new IllegalStateException(
                    "ATTENDANCE_QR_SECRET es demasiado debil: debe tener al menos " + LARGO_MINIMO_SECRETO
                            + " caracteres y no puede ser el valor publico del repositorio.");
        }
    }

    @Transactional
    public Asistencia registrarEntrada(AsistenciaRequest request) {
        Socio socio = socioRepository.findById(request.getIdSocio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Error: Socio no encontrado."));

        return registrarAsistencia(socio);
    }

    /** Check-in manual: registra la entrada y devuelve la decision de acceso para mostrarla. */
    @Transactional
    public ni.edu.uam.GimnasioAPI.dto.AsistenciaResponse registrarEntradaConDecision(AsistenciaRequest request) {
        Asistencia asistencia = registrarEntrada(request);
        DecisionAcceso decision = decidirAcceso(asistencia.getSocio());
        ni.edu.uam.GimnasioAPI.dto.AsistenciaResponse respuesta = ni.edu.uam.GimnasioAPI.dto.AsistenciaResponse.desde(asistencia);
        respuesta.setEstado(decision.estado());
        respuesta.setMensaje(decision.mensaje());
        return respuesta;
    }

    public AsistenciaQrTokenResponse generarQrAsistencia(String emailUsuario) {
        Usuario usuario = buscarUsuarioAutenticado(emailUsuario);
        Socio socio = socioRepository.findById(usuario.getId())
                .orElseThrow(() -> new AccesoDenegadoException("El usuario autenticado no tiene perfil de socio."));
        Instant generado = Instant.now();
        Instant expira = generado.plusSeconds(QR_TTL_SECONDS);
        String nonce = UUID.randomUUID().toString();
        String datos = socio.getUsuarioId() + "|" + nonce + "|" + expira.getEpochSecond();
        String firma = firmar(datos);
        String token = QR_SOCIO_PREFIX + "|" + datos + "|" + firma;

        return AsistenciaQrTokenResponse.builder()
                .token(token)
                .socioId(socio.getUsuarioId())
                .socio(socio.getNombres() + " " + socio.getApellidos())
                .generadoEn(LocalDateTime.ofInstant(generado, ZoneId.systemDefault()))
                .expiraEn(LocalDateTime.ofInstant(expira, ZoneId.systemDefault()))
                .ttlSegundos(QR_TTL_SECONDS)
                .build();
    }

    public AsistenciaQrTokenResponse generarQrValidacionWeb(String emailUsuario) {
        buscarUsuarioAutenticado(emailUsuario);

        Instant generado = Instant.now();
        Instant expira = generado.plusSeconds(QR_TTL_SECONDS);
        UUID sessionId = UUID.randomUUID();
        String nonce = UUID.randomUUID().toString();
        String datos = sessionId + "|" + nonce + "|" + expira.getEpochSecond();
        String firma = firmar(datos);
        String token = QR_WEB_PREFIX + "|" + datos + "|" + firma;
        String tokenHash = hashToken(token);
        sesionesQrWeb.guardar(sessionId, tokenHash, expira);

        return AsistenciaQrTokenResponse.builder()
                .token(token)
                .sessionId(sessionId)
                .generadoEn(LocalDateTime.ofInstant(generado, ZoneId.systemDefault()))
                .expiraEn(LocalDateTime.ofInstant(expira, ZoneId.systemDefault()))
                .ttlSegundos(QR_TTL_SECONDS)
                .build();
    }

    @Transactional
    public AsistenciaQrValidationResponse registrarEntradaQr(AsistenciaQrRequest request, String emailUsuario) {
        buscarUsuarioAutenticado(emailUsuario);

        QrSocioPayload payload = validarTokenQrSocio(request != null ? request.getToken() : null);

        if (asistenciaRepository.existsByQrTokenHash(payload.tokenHash())) {
            throw new ConflictoException("QR ya utilizado. Solicita al socio generar uno nuevo.");
        }

        Socio socio = socioRepository.findById(payload.socioId())
                .orElseThrow(() -> new ReglaNegocioException("QR invalido: socio no encontrado."));

        try {
            Asistencia asistencia = registrarAsistencia(socio, payload.tokenHash());
            return construirRespuestaValidacion(asistencia, socio, "Asistencia validada.");
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException("QR ya utilizado. Solicita al socio generar uno nuevo.");
        }
    }

    @Transactional
    public AsistenciaQrValidationResponse registrarEntradaQrWeb(AsistenciaQrRequest request, String emailUsuario) {
        Usuario usuario = buscarUsuarioAutenticado(emailUsuario);
        Socio socio = socioRepository.findById(usuario.getId())
                .orElseThrow(() -> new AccesoDenegadoException("El usuario autenticado no tiene perfil de socio."));
        QrWebPayload payload = validarTokenQrWeb(request != null ? request.getToken() : null, true);
        QrWebSessionStore.Session sesion = buscarSesionQrWeb(payload.sessionId());

        if (!sesion.tokenHash().equals(payload.tokenHash())) {
            throw new ReglaNegocioException("QR invalido: sesion no coincide con el token.");
        }

        if (asistenciaRepository.existsByQrTokenHash(payload.tokenHash())) {
            throw new ConflictoException("QR ya utilizado. Solicita a recepcion generar uno nuevo.");
        }

        try {
            Asistencia asistencia = registrarAsistencia(socio, payload.tokenHash());
            return construirRespuestaValidacion(asistencia, socio, "Asistencia validada.", payload.sessionId());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException("QR ya utilizado. Solicita a recepcion generar uno nuevo.");
        }
    }

    public AsistenciaQrValidationResponse consultarEstadoQrValidacion(AsistenciaQrRequest request) {
        QrWebPayload payload = validarTokenQrWeb(request != null ? request.getToken() : null, false);
        // Pasado un margen corto tras el vencimiento, el QR ya no revela a quien lo uso: una foto
        // del QR de recepcion no sirve para consultar despues los datos del socio que entro.
        if (Instant.now().getEpochSecond() > payload.expiraEpoch() + MARGEN_CONSULTA_QR_SEGUNDOS) {
            return AsistenciaQrValidationResponse.builder()
                    .sessionId(payload.sessionId())
                    .estado("Expirado")
                    .mensaje("QR expirado. Genera uno nuevo.")
                    .build();
        }
        QrWebSessionStore.Session sesion = sesionesQrWeb.buscar(payload.sessionId()).orElse(null);

        if (sesion != null && !sesion.tokenHash().equals(payload.tokenHash())) {
            throw new ReglaNegocioException("QR invalido: sesion no coincide con el token.");
        }

        return consultarEstadoQrValidacion(payload.sessionId(), payload.tokenHash(), payload.expirado());
    }

    public AsistenciaQrValidationResponse consultarEstadoQrValidacion(UUID sessionId) {

        QrWebSessionStore.Session sesion = sesionesQrWeb.buscar(sessionId).orElse(null);
        if (sesion == null) {
            return AsistenciaQrValidationResponse.builder()
                    .sessionId(sessionId)
                    .estado("Expirado")
                    .mensaje("QR expirado o sesion no encontrada. Genera uno nuevo.")
                    .build();
        }

        return consultarEstadoQrValidacion(sessionId, sesion.tokenHash(), sesion.expirado());
    }

    public List<Asistencia> obtenerHistorial(UUID socioId) {
        return asistenciaRepository.findBySocioUsuarioIdOrderByFechaHoraDesc(socioId);
    }

    private Usuario buscarUsuarioAutenticado(String emailUsuario) {
        return usuarioRepository.findByEmailIgnoreCase(emailUsuario)
                .orElseThrow(() -> new NoAutenticadoException("Usuario autenticado no encontrado."));
    }

    private Asistencia registrarAsistencia(Socio socio) {
        return registrarAsistencia(socio, null);
    }

    private Asistencia registrarAsistencia(Socio socio, String qrTokenHash) {
        // Permitir el registro de asistencia sin bloquear por estado_acceso.
        // El recepcionista verá en pantalla si la membresía está activa o vencida.

        Asistencia asistencia = new Asistencia();
        asistencia.setSocio(socio);
        asistencia.setFechaHora(LocalDateTime.now());
        asistencia.setQrTokenHash(qrTokenHash);

        Asistencia asistenciaGuardada = asistenciaRepository.save(asistencia);
        if (qrTokenHash != null) {
            asistenciaRepository.flush();
        }
        return asistenciaGuardada;
    }

    private AsistenciaQrValidationResponse consultarEstadoQrValidacion(UUID sessionId, String tokenHash, boolean expirado) {
        return asistenciaRepository.findByQrTokenHash(tokenHash)
                .map((asistencia) -> construirRespuestaValidacion(
                        asistencia,
                        asistencia.getSocio(),
                        "Asistencia validada.",
                        sessionId
                ))
                .orElseGet(() -> AsistenciaQrValidationResponse.builder()
                        .sessionId(sessionId)
                        .estado(expirado ? "Expirado" : "Pendiente")
                        .mensaje(expirado
                                ? "QR expirado. Genera uno nuevo."
                                : "Esperando validacion desde la app movil.")
                        .build());
    }

    private AsistenciaQrValidationResponse construirRespuestaValidacion(Asistencia asistencia, Socio socio, String mensaje) {
        return construirRespuestaValidacion(asistencia, socio, mensaje, null);
    }

    private AsistenciaQrValidationResponse construirRespuestaValidacion(Asistencia asistencia, Socio socio, String mensaje, UUID sessionId) {
        MembresiaSocio membresiaActiva = membresiaSocioRepository
                .findBySocioUsuarioIdAndEstado(socio.getUsuarioId(), "Activa")
                .orElse(null);
        String tipoMembresia = membresiaActiva != null
                ? membresiaActiva.getTipoMembresia().getNombre()
                : "Sin membresia";
        DecisionAcceso decision = decidirAcceso(socio, membresiaActiva, mensaje);
        String estado = decision.estado();
        String mensajeFinal = decision.mensaje();

        return AsistenciaQrValidationResponse.builder()
                .id(asistencia.getId())
                .sessionId(sessionId)
                .socioId(socio.getUsuarioId())
                .socio(socio.getNombres() + " " + socio.getApellidos())
                .membresia(tipoMembresia)
                .tipoMembresia(tipoMembresia)
                .estadoAcceso(socio.getEstadoAcceso())
                .fechaVencimiento(membresiaActiva != null ? membresiaActiva.getFechaVencimiento() : null)
                .estado(estado)
                .fechaHora(asistencia.getFechaHora())
                .mensaje(mensajeFinal)
                .build();
    }

    /**
     * Decision de acceso unica para el check-in manual y por QR. La entrada se registra igual
     * (queda el intento en el historial), pero el estado solo dice "Permitido" si la cuenta
     * esta habilitada, el socio no esta suspendido y tiene una membresia activa y vigente.
     */
    public DecisionAcceso decidirAcceso(Socio socio) {
        MembresiaSocio membresiaActiva = membresiaSocioRepository
                .findBySocioUsuarioIdAndEstado(socio.getUsuarioId(), "Activa")
                .orElse(null);
        return decidirAcceso(socio, membresiaActiva, "Asistencia validada.");
    }

    private DecisionAcceso decidirAcceso(Socio socio, MembresiaSocio membresiaActiva, String mensajePermitido) {
        Usuario usuario = socio.getUsuario();
        if (usuario != null && !Boolean.TRUE.equals(usuario.getActivo())) {
            return new DecisionAcceso("Denegado",
                    "La cuenta del socio esta desactivada. No permitas el ingreso.");
        }
        if ("Suspendido".equalsIgnoreCase(socio.getEstadoAcceso())) {
            return new DecisionAcceso("Suspendido",
                    "Acceso suspendido por administracion. No permitas el ingreso.");
        }
        boolean vigente = membresiaActiva != null
                && (membresiaActiva.getFechaVencimiento() == null
                || !membresiaActiva.getFechaVencimiento().isBefore(java.time.LocalDate.now()));
        if (!vigente) {
            return new DecisionAcceso("Sin membresia",
                    "Entrada registrada sin membresía activa. El socio debe renovarla en recepción.");
        }
        return new DecisionAcceso("Permitido", mensajePermitido);
    }

    public record DecisionAcceso(String estado, String mensaje) {
        public boolean permitido() {
            return "Permitido".equals(estado);
        }
    }

    private QrSocioPayload validarTokenQrSocio(String token) {
        String[] partes = validarEstructuraToken(token, QR_SOCIO_PREFIX);

        UUID socioId;
        try {
            socioId = UUID.fromString(partes[1]);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("QR invalido: socio incorrecto.");
        }

        validarFirmaYExpiracion(partes[1] + "|" + partes[2] + "|" + partes[3], partes[3], partes[4], true);
        return new QrSocioPayload(socioId, hashToken(String.join("|", partes)));
    }

    private QrWebPayload validarTokenQrWeb(String token, boolean rechazarExpirado) {
        String[] partes = validarEstructuraToken(token, QR_WEB_PREFIX);

        UUID sessionId;
        try {
            sessionId = UUID.fromString(partes[1]);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("QR invalido: sesion incorrecta.");
        }

        boolean expirado = validarFirmaYExpiracion(
                partes[1] + "|" + partes[2] + "|" + partes[3],
                partes[3],
                partes[4],
                rechazarExpirado
        );
        return new QrWebPayload(sessionId, hashToken(String.join("|", partes)), expirado, Long.parseLong(partes[3]));
    }

    private String[] validarEstructuraToken(String token, String prefijoEsperado) {
        if (token == null || token.isBlank()) {
            throw new ReglaNegocioException("QR invalido: no se recibio token.");
        }

        // El limite -1 conserva los campos vacios del final: con split("\\|") a secas, "T|" o
        // "T||" daban las mismas 5 partes con firma valida pero OTRO hash, y el mismo QR se
        // podia volver a usar agregandole separadores al final.
        String[] partes = token.split("\\|", -1);
        if (partes.length != 5 || !prefijoEsperado.equals(partes[0])) {
            throw new ReglaNegocioException("QR invalido para este gimnasio.");
        }
        for (String parte : partes) {
            if (parte.isEmpty() || !parte.equals(parte.strip())) {
                throw new ReglaNegocioException("QR invalido para este gimnasio.");
            }
        }
        return partes;
    }

    private boolean validarFirmaYExpiracion(String datos, String expiraTexto, String firmaRecibida, boolean rechazarExpirado) {
        String firmaEsperada = firmar(datos);
        if (!MessageDigest.isEqual(firmaEsperada.getBytes(StandardCharsets.UTF_8), firmaRecibida.getBytes(StandardCharsets.UTF_8))) {
            throw new ReglaNegocioException("QR invalido o alterado.");
        }

        long expiraEpoch;
        try {
            expiraEpoch = Long.parseLong(expiraTexto);
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("QR invalido: expiracion incorrecta.");
        }

        boolean expirado = Instant.now().getEpochSecond() > expiraEpoch;
        if (expirado && rechazarExpirado) {
            throw new ReglaNegocioException("QR expirado. Solicita generar uno nuevo.");
        }
        return expirado;
    }

    private QrWebSessionStore.Session buscarSesionQrWeb(UUID sessionId) {
        QrWebSessionStore.Session sesion = sesionesQrWeb.buscar(sessionId).orElse(null);

        if (sesion == null) {
            throw new ReglaNegocioException("QR expirado o sesion no encontrada. Genera uno nuevo.");
        }

        if (sesion.expirado()) {
            throw new ReglaNegocioException("QR expirado. Solicita generar uno nuevo.");
        }

        return sesion;
    }


    private String firmar(String datos) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(qrSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] firma = mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(firma);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo firmar el QR de asistencia.", e);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo validar el QR de asistencia.", e);
        }
    }

    private record QrSocioPayload(UUID socioId, String tokenHash) {
    }

    private record QrWebPayload(UUID sessionId, String tokenHash, boolean expirado, long expiraEpoch) {
    }

}
