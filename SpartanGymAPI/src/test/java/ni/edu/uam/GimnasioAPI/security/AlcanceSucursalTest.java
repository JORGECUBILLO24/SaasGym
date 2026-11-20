package ni.edu.uam.GimnasioAPI.security;

import ni.edu.uam.GimnasioAPI.models.Personal;
import ni.edu.uam.GimnasioAPI.models.Rol;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.PersonalRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Recepción y entrenadores solo alcanzan socios de su sucursal; administración, todos. */
class AlcanceSucursalTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PersonalRepository personal = mock(PersonalRepository.class);
    private final SocioRepository socios = mock(SocioRepository.class);
    private final AlcanceSucursal alcance = new AlcanceSucursal(usuarios, personal, socios);

    private final Sucursal sucursalA = sucursal();
    private final Sucursal sucursalB = sucursal();

    private static Sucursal sucursal() {
        Sucursal s = new Sucursal();
        s.setId(UUID.randomUUID());
        return s;
    }

    private Usuario personal(String email, String rol, Sucursal sucursal) {
        Rol r = new Rol();
        r.setNombre(rol);
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setRol(r);
        when(usuarios.findByEmailIgnoreCase(email)).thenReturn(Optional.of(u));
        if (sucursal != null) {
            Personal p = new Personal();
            p.setUsuarioId(u.getId());
            p.setSucursal(sucursal);
            when(personal.findById(u.getId())).thenReturn(Optional.of(p));
        }
        return u;
    }

    private UUID socioEn(Sucursal sucursal) {
        Socio socio = new Socio();
        socio.setUsuarioId(UUID.randomUUID());
        socio.setSucursal(sucursal);
        when(socios.findById(socio.getUsuarioId())).thenReturn(Optional.of(socio));
        return socio.getUsuarioId();
    }

    private static UsernamePasswordAuthenticationToken auth(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of());
    }

    @Test
    void recepcionistaDeOtraSucursalNoAlcanzaAlSocio() {
        personal("recepcion-a@ejemplo.com", "ROLE_RECEPCIONISTA", sucursalA);
        assertThat(alcance.puedeAtenderSocio(auth("recepcion-a@ejemplo.com"), socioEn(sucursalB))).isFalse();
        assertThat(alcance.puedeAtenderSocio(auth("recepcion-a@ejemplo.com"), socioEn(sucursalA))).isTrue();
    }

    @Test
    void adminAlcanzaATodas() {
        personal("admin@ejemplo.com", "ROLE_ADMIN", sucursalA);
        assertThat(alcance.puedeAtenderSocio(auth("admin@ejemplo.com"), socioEn(sucursalB))).isTrue();
    }

    @Test
    void personalSinSucursalAsignadaConservaElAccesoGeneral() {
        personal("entrenador@ejemplo.com", "ROLE_ENTRENADOR", null);
        assertThat(alcance.puedeAtenderSocio(auth("entrenador@ejemplo.com"), socioEn(sucursalB))).isTrue();
    }
}
