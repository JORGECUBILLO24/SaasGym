package ni.edu.uam.GimnasioAPI.security;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.models.Personal;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.PersonalRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Hasta dónde llega el personal de una sucursal.
 *
 * Regla de la casa (la misma que ya usaban los pedidos y el listado del entrenador):
 * superadmin y admin trabajan con todas las sucursales; recepción y entrenadores, solo con la
 * suya. Un miembro del personal sin sucursal asignada conserva el comportamiento anterior (ve
 * todo), para no dejar sin trabajo a nadie mientras administración termina de asignarlas.
 *
 * El header X-Sucursal-Id es solo un filtro de la interfaz: nunca se usa para autorizar.
 */
@Component("alcanceSucursal")
@RequiredArgsConstructor
public class AlcanceSucursal {

    private final UsuarioRepository usuarioRepository;
    private final PersonalRepository personalRepository;
    private final SocioRepository socioRepository;

    public static boolean esRolDeSucursal(String rol) {
        return "ROLE_RECEPCIONISTA".equals(rol) || "ROLE_ENTRENADOR".equals(rol);
    }

    /** Sucursal a la que queda limitado este usuario, o vacío si no tiene límite. */
    public Optional<UUID> sucursalRestringida(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || !esRolDeSucursal(usuario.getRol().getNombre())) {
            return Optional.empty();
        }
        return personalRepository.findById(usuario.getId())
                .map(Personal::getSucursal)
                .map(Sucursal::getId);
    }

    /** ¿Puede este usuario (personal) operar sobre algo de esa sucursal? */
    public boolean alcanza(Usuario usuario, UUID sucursalId) {
        Optional<UUID> suya = sucursalRestringida(usuario);
        return suya.isEmpty() || Objects.equals(suya.get(), sucursalId);
    }

    /** ¿Puede este usuario (personal) atender al socio indicado? */
    public boolean alcanzaSocio(Usuario usuario, Socio socio) {
        UUID sucursalSocio = socio == null || socio.getSucursal() == null ? null : socio.getSucursal().getId();
        return alcanza(usuario, sucursalSocio);
    }

    /** Para @PreAuthorize: el usuario autenticado es personal y el socio es de su sucursal. */
    public boolean puedeAtenderSocio(Authentication auth, UUID socioId) {
        if (auth == null || auth.getName() == null || socioId == null) {
            return false;
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
        if (usuario == null) {
            return false;
        }
        Optional<UUID> suya = sucursalRestringida(usuario);
        if (suya.isEmpty()) {
            return true;
        }
        return socioRepository.findById(socioId)
                .map(socio -> socio.getSucursal() != null && suya.get().equals(socio.getSucursal().getId()))
                .orElse(false);
    }
}
