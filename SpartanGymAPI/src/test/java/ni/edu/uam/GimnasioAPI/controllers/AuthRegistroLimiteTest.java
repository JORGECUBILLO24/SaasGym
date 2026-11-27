package ni.edu.uam.GimnasioAPI.controllers;

import ni.edu.uam.GimnasioAPI.dto.AuthResponse;
import ni.edu.uam.GimnasioAPI.dto.RegisterRequest;
import ni.edu.uam.GimnasioAPI.security.CookieSesion;
import ni.edu.uam.GimnasioAPI.security.LimitadorIntentos;
import ni.edu.uam.GimnasioAPI.services.AuthService;
import ni.edu.uam.GimnasioAPI.services.GoogleLoginService;
import ni.edu.uam.GimnasioAPI.services.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthRegistroLimiteTest {
    @Test
    void registroCuentaIntentoPorIpAntesDeCrearCuenta() {
        AuthService servicio = mock(AuthService.class);
        LimitadorIntentos limitador = mock(LimitadorIntentos.class);
        AuthController controller = new AuthController(servicio, mock(PasswordResetService.class),
                mock(GoogleLoginService.class), limitador, mock(CookieSesion.class));
        when(servicio.register(any())).thenReturn(AuthResponse.builder().email("socio@ejemplo.com").build());

        controller.register(new RegisterRequest(), new MockHttpServletRequest(), new MockHttpServletResponse());

        var orden = inOrder(limitador, servicio);
        orden.verify(limitador).registrar(startsWith("registro-ip:"), eq(AuthController.REGISTRO_IP));
        orden.verify(servicio).register(any());
    }
}
