package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.dto.AsistenciaQrRequest;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaQrValidationResponse;
import ni.edu.uam.GimnasioAPI.exceptions.AccesoDenegadoException;
import ni.edu.uam.GimnasioAPI.models.Asistencia;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.AsistenciaRepository;
import ni.edu.uam.GimnasioAPI.repositories.MembresiaSocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Consulta del QR que recepción muestra en pantalla: solo quien lo generó ve quién lo escaneó,
 * y la respuesta lleva la foto que el socio subió desde la app.
 */
class AsistenciaQrConsultaTest {

    private static final String SECRETO = "n7Qx2vLp9sTm4Ke8Rw1Yc6Hb3Zd5Fa0JuGiOeXtVnMy";
    private static final String RECEPCION_A = "recepcion-a@ejemplo.com";
    private static final String RECEPCION_B = "recepcion-b@ejemplo.com";
    private static final String CORREO_SOCIO = "socio@ejemplo.com";
    private static final UUID ID_SOCIO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String FOTO = "data:image/png;base64,iVBORw0KGgo=";

    private final AsistenciaRepository asistencias = mock(AsistenciaRepository.class);
    private final SocioRepository socios = mock(SocioRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final MembresiaSocioRepository membresias = mock(MembresiaSocioRepository.class);
    private final AtomicReference<Asistencia> guardada = new AtomicReference<>();

    @BeforeEach
    void preparar() {
        when(usuarios.findByEmailIgnoreCase(RECEPCION_A)).thenReturn(Optional.of(usuario(UUID.randomUUID())));
        when(usuarios.findByEmailIgnoreCase(RECEPCION_B)).thenReturn(Optional.of(usuario(UUID.randomUUID())));
        Usuario cuentaSocio = usuario(ID_SOCIO);
        cuentaSocio.setFotoUrl(FOTO);
        when(usuarios.findByEmailIgnoreCase(CORREO_SOCIO)).thenReturn(Optional.of(cuentaSocio));
        Socio socio = new Socio();
        socio.setUsuarioId(ID_SOCIO);
        socio.setUsuario(cuentaSocio);
        socio.setNombres("Ana");
        socio.setApellidos("Pérez");
        socio.setEstadoAcceso("Activo");
        when(socios.findById(ID_SOCIO)).thenReturn(Optional.of(socio));
        when(membresias.findBySocioUsuarioIdAndEstado(any(), anyString())).thenReturn(Optional.empty());
        when(asistencias.save(any())).thenAnswer(i -> {
            Asistencia a = i.getArgument(0);
            guardada.set(a);
            return a;
        });
        when(asistencias.findByQrTokenHash(anyString())).thenAnswer(i -> Optional.ofNullable(guardada.get()));
    }

    @Test
    void quienGeneroElQrVeAlSocioConSuFoto() {
        AsistenciaService servicio = servicio(new MemoryQrWebSessionStore());
        var qr = servicio.generarQrValidacionWeb(RECEPCION_A);
        servicio.registrarEntradaQrWeb(request(qr.getToken()), CORREO_SOCIO);

        AsistenciaQrValidationResponse respuesta = servicio.consultarEstadoQrValidacion(request(qr.getToken()), RECEPCION_A);

        assertThat(respuesta.getSocio()).isEqualTo("Ana Pérez");
        assertThat(respuesta.getFotoUrl()).isEqualTo(FOTO);
        assertThat(servicio.consultarEstadoQrValidacion(qr.getSessionId(), RECEPCION_A).getFotoUrl()).isEqualTo(FOTO);
    }

    @Test
    void otraPersonaDelPersonalNoVeQuienEscaneoElQr() {
        AsistenciaService servicio = servicio(new MemoryQrWebSessionStore());
        var qr = servicio.generarQrValidacionWeb(RECEPCION_A);
        servicio.registrarEntradaQrWeb(request(qr.getToken()), CORREO_SOCIO);

        assertThatThrownBy(() -> servicio.consultarEstadoQrValidacion(request(qr.getToken()), RECEPCION_B))
                .isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> servicio.consultarEstadoQrValidacion(qr.getSessionId(), RECEPCION_B))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void unTokenSinSesionNoRevelaAlSocio() {
        // Mismo secreto, pero la sesión ya no existe (Redis se vació o pasó la retención).
        AsistenciaService generador = servicio(new MemoryQrWebSessionStore());
        var qr = generador.generarQrValidacionWeb(RECEPCION_A);
        generador.registrarEntradaQrWeb(request(qr.getToken()), CORREO_SOCIO);
        AsistenciaService sinSesion = servicio(new MemoryQrWebSessionStore());

        AsistenciaQrValidationResponse respuesta = sinSesion.consultarEstadoQrValidacion(request(qr.getToken()), RECEPCION_A);

        assertThat(respuesta.getEstado()).isEqualTo("Expirado");
        assertThat(respuesta.getSocio()).isNull();
        assertThat(respuesta.getFotoUrl()).isNull();
    }

    private AsistenciaService servicio(QrWebSessionStore sesiones) {
        MockEnvironment entorno = new MockEnvironment();
        entorno.setActiveProfiles("default");
        AsistenciaService servicio = new AsistenciaService(asistencias, socios, usuarios, membresias, sesiones, entorno);
        ReflectionTestUtils.setField(servicio, "qrSecret", SECRETO);
        servicio.validarSecreto();
        return servicio;
    }

    private static Usuario usuario(UUID id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private static AsistenciaQrRequest request(String token) {
        AsistenciaQrRequest r = new AsistenciaQrRequest();
        r.setToken(token);
        return r;
    }
}
