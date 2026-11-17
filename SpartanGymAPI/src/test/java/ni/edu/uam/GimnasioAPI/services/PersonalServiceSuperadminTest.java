package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.AccesoDenegadoException;
import ni.edu.uam.GimnasioAPI.models.Personal;
import ni.edu.uam.GimnasioAPI.models.Rol;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.PersonalRepository;
import ni.edu.uam.GimnasioAPI.repositories.RolRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un ADMIN podía cambiarle el rol al SUPERADMIN o desactivarlo; después quedaba
 * libre el restablecimiento de su contraseña. La cuenta superadmin queda fuera
 * de estos endpoints.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonalServiceSuperadminTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PersonalRepository personalRepository;
    @Mock private RolRepository rolRepository;

    @InjectMocks private PersonalService servicio;

    private Usuario usuario(String rol) {
        Rol r = new Rol();
        r.setNombre(rol);
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setEmail("cuenta@ejemplo.com");
        u.setRol(r);
        u.setActivo(true);
        return u;
    }

    private void registrar(Usuario u) {
        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        Personal p = new Personal();
        p.setUsuario(u);
        when(personalRepository.findById(u.getId())).thenReturn(Optional.of(p));
        when(personalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void cambiarRolDelSuperadmin_esAccesoDenegado() {
        Usuario superadmin = usuario("ROLE_SUPERADMIN");
        registrar(superadmin);
        Rol admin = new Rol();
        admin.setNombre("ROLE_ADMIN");
        when(rolRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(admin));

        assertThrows(AccesoDenegadoException.class, () -> servicio.actualizarRol(superadmin.getId(), "ADMIN"));
        assertEquals("ROLE_SUPERADMIN", superadmin.getRol().getNombre());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desactivarAlSuperadmin_esAccesoDenegado() {
        Usuario superadmin = usuario("ROLE_SUPERADMIN");
        registrar(superadmin);

        assertThrows(AccesoDenegadoException.class, () -> servicio.desactivarPersonal(superadmin.getId()));
        assertTrue(superadmin.getActivo());
    }
}
