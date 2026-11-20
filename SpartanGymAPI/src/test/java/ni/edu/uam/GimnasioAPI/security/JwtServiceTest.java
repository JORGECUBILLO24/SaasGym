package ni.edu.uam.GimnasioAPI.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService crearServicio(String secretConfigurado, String... perfilesActivos) {
        MockEnvironment environment = new MockEnvironment();
        if (perfilesActivos.length > 0) {
            environment.setActiveProfiles(perfilesActivos);
        }
        JwtService service = new JwtService(environment);
        ReflectionTestUtils.setField(service, "secretKey", secretConfigurado);
        return service;
    }

    private void validarSecret(JwtService service) {
        ReflectionTestUtils.invokeMethod(service, "validarSecret");
    }

    @Test
    void validarSecret_lanzaExcepcionSiJwtSecretVacioFueraDePerfilLocal() {
        JwtService service = crearServicio("", "default");

        assertThrows(IllegalStateException.class, () -> validarSecret(service));
    }

    @Test
    void validarSecret_noLanzaExcepcionSiJwtSecretVacioEnPerfilLocal() {
        JwtService service = crearServicio("", "local");

        assertDoesNotThrow(() -> validarSecret(service));
    }

    @Test
    void validarSecret_noLanzaExcepcionSiJwtSecretConfiguradoFueraDePerfilLocal() {
        JwtService service = crearServicio("un-secreto-real-configurado-por-variable-de-entorno", "default");

        assertDoesNotThrow(() -> validarSecret(service));
    }

    @Test
    void cambiarLaContrasenaInvalidaLosTokensEmitidosAntes() {
        JwtService service = crearServicio("", "local");
        ReflectionTestUtils.setField(service, "jwtExpiration", 60_000L);
        validarSecret(service);
        ni.edu.uam.GimnasioAPI.models.Rol rol = new ni.edu.uam.GimnasioAPI.models.Rol();
        rol.setNombre("ROLE_SOCIO");
        ni.edu.uam.GimnasioAPI.models.Usuario usuario = new ni.edu.uam.GimnasioAPI.models.Usuario();
        usuario.setEmail("socio@ejemplo.com");
        usuario.setPasswordHash("x");
        usuario.setRol(rol);
        UserDetailsImpl detalles = new UserDetailsImpl(usuario);

        String token = service.generateToken(detalles);
        org.junit.jupiter.api.Assertions.assertTrue(service.isTokenValid(token, detalles));

        usuario.cerrarSesionesAbiertas();

        org.junit.jupiter.api.Assertions.assertFalse(service.isTokenValid(token, new UserDetailsImpl(usuario)));
        org.junit.jupiter.api.Assertions.assertTrue(service.isTokenValid(service.generateToken(new UserDetailsImpl(usuario)), new UserDetailsImpl(usuario)));
    }
}
