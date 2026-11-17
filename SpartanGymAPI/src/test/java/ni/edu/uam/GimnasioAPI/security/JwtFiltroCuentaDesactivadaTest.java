package ni.edu.uam.GimnasioAPI.security;

import jakarta.servlet.FilterChain;
import ni.edu.uam.GimnasioAPI.models.Rol;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Desactivar a un empleado debe cortarle el acceso aunque conserve un JWT
 * emitido antes de la desactivación.
 */
class JwtFiltroCuentaDesactivadaTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(jwtService, userDetailsService);

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private UserDetailsImpl cuenta(boolean activa) {
        Rol rol = new Rol();
        rol.setNombre("ROLE_RECEPCIONISTA");
        Usuario usuario = new Usuario();
        usuario.setEmail("recepcion@ejemplo.com");
        usuario.setRol(rol);
        usuario.setActivo(activa);
        return new UserDetailsImpl(usuario);
    }

    private void ejecutarCon(UserDetailsImpl detalles) throws Exception {
        when(jwtService.extractUsername("token")).thenReturn("recepcion@ejemplo.com");
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(detalles);
        when(jwtService.isTokenValid(any(), any())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        FilterChain cadena = mock(FilterChain.class);
        filtro.doFilter(request, new MockHttpServletResponse(), cadena);
        verify(cadena).doFilter(any(), any());
    }

    @Test
    void cuentaActiva_quedaAutenticada() throws Exception {
        ejecutarCon(cuenta(true));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void cuentaDesactivada_noSeAutentica() throws Exception {
        ejecutarCon(cuenta(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
