package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.dto.RegisterRequest;
import ni.edu.uam.GimnasioAPI.exceptions.ConflictoException;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.*;
import ni.edu.uam.GimnasioAPI.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthRegistroPrivacidadTest {
    @Test
    void correoExistente_recibeMensajeGenerico() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        TenantLookupService tenantLookup = mock(TenantLookupService.class);
        when(tenantLookup.correoUsuarioEnUso("socio@ejemplo.com")).thenReturn(true);
        AuthService servicio = new AuthService(usuarios, mock(SocioRepository.class), mock(SucursalRepository.class),
                mock(RolRepository.class), mock(PasswordEncoder.class), mock(JwtService.class),
                mock(AuthenticationManager.class), mock(NotificacionService.class), tenantLookup);
        RegisterRequest request = new RegisterRequest();
        request.setEmail("socio@ejemplo.com");
        request.setPassword("ClaveSegura123!");
        request.setNombres("Socio");
        request.setApellidos("Prueba");

        assertThatThrownBy(() -> servicio.register(request)).isInstanceOf(ConflictoException.class)
                .hasMessageContaining("No pudimos crear la cuenta")
                .hasMessageNotContaining("correo ya");
    }
}
