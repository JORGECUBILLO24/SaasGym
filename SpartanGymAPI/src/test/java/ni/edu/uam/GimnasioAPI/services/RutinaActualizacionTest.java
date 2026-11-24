package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.dto.RutinaRequest;
import ni.edu.uam.GimnasioAPI.exceptions.AccesoDenegadoException;
import ni.edu.uam.GimnasioAPI.models.*;
import ni.edu.uam.GimnasioAPI.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RutinaActualizacionTest {
    @Mock RutinaRepository rutinas;
    @Mock PersonalRepository personal;
    @Mock UsuarioRepository usuarios;
    @Mock SocioRepository socios;
    @Mock EjercicioRepository ejercicios;
    @Mock NotificacionService notificaciones;
    @Mock EjercicioCompletadoService completados;
    @Mock RutinaResponseMapper mapper;
    @Mock Authentication auth;
    @InjectMocks RutinaService servicio;

    @Test
    void entrenadorNoPuedeEditarRutinaDeOtraSucursal() {
        UUID id = UUID.randomUUID(), entrenadorId = UUID.randomUUID();
        Sucursal local = new Sucursal(); local.setId(UUID.randomUUID());
        Sucursal ajena = new Sucursal(); ajena.setId(UUID.randomUUID());
        Usuario usuario = new Usuario(); usuario.setId(entrenadorId);
        Personal entrenador = new Personal(); entrenador.setSucursal(local);
        Socio socio = new Socio(); socio.setSucursal(ajena);
        Rutina rutina = new Rutina(); rutina.setSocio(socio);
        when(rutinas.findById(id)).thenReturn(Optional.of(rutina));
        when(auth.getName()).thenReturn("entrenador@ejemplo.com");
        when(auth.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_ENTRENADOR")));
        when(usuarios.findByEmailIgnoreCase("entrenador@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(personal.findById(entrenadorId)).thenReturn(Optional.of(entrenador));

        assertThrows(AccesoDenegadoException.class,
                () -> servicio.actualizarRutina(id, new RutinaRequest(), auth));
        verify(rutinas, never()).saveAndFlush(any());
    }
}
