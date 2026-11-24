package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.EstadoSocioResponse;
import ni.edu.uam.GimnasioAPI.dto.SocioResponse;
import ni.edu.uam.GimnasioAPI.models.MembresiaSocio;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.repositories.MembresiaSocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.security.AlcanceSucursal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocioService {

    private final SocioRepository socioRepository;
    private final MembresiaSocioRepository membresiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlcanceSucursal alcanceSucursal;

    /**
     * Listado para el panel. Recepción y entrenadores solo ven los socios de su sucursal; admin y
     * superadmin, todos. El socio de otra sede igual puede entrar escaneando el QR de recepción.
     */
    @Transactional(readOnly = true)
    public List<SocioResponse> listarSocios(Authentication auth) {
        Usuario actor = auth == null || auth.getName() == null ? null
                : usuarioRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
        boolean esPersonalDeSucursal = actor != null && actor.getRol() != null
                && AlcanceSucursal.esRolDeSucursal(actor.getRol().getNombre());
        Optional<UUID> sucursal = esPersonalDeSucursal ? alcanceSucursal.sucursalRestringida(actor) : Optional.empty();
        return socioRepository.findAll().stream()
                .filter(socio -> sucursal.isEmpty()
                        || (socio.getSucursal() != null && sucursal.get().equals(socio.getSucursal().getId())))
                .map(this::mapearSocio)
                .toList();
    }

    public EstadoSocioResponse consultarEstado(UUID socioId) {
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Error: Socio no encontrado"));

        EstadoSocioResponse response = new EstadoSocioResponse();
        response.setNombres(socio.getNombres());
        response.setApellidos(socio.getApellidos());
        response.setEstadoAcceso(socio.getEstadoAcceso());

        // Buscamos si tiene alguna membresía Activa
        Optional<MembresiaSocio> membresiaActiva = membresiaRepository.findBySocioUsuarioIdAndEstado(socioId, "Activa");

        if (membresiaActiva.isPresent()) {
            response.setTieneMembresiaActiva(true);
            response.setTipoMembresia(membresiaActiva.get().getTipoMembresia().getNombre());
            response.setFechaVencimiento(membresiaActiva.get().getFechaVencimiento());
        } else {
            response.setTieneMembresiaActiva(false);
            response.setTipoMembresia("Ninguna / Vencida");
            response.setFechaVencimiento(null);
        }

        return response;
    }

    private SocioResponse mapearSocio(Socio socio) {
        Optional<MembresiaSocio> membresiaActiva = membresiaRepository
                .findBySocioUsuarioIdAndEstado(socio.getUsuarioId(), "Activa");

        return SocioResponse.builder()
                .id(socio.getUsuarioId())
                .nombres(socio.getNombres())
                .apellidos(socio.getApellidos())
                .email(socio.getUsuario().getEmail())
                .telefono(socio.getTelefono())
                .sucursalId(socio.getSucursal() != null ? socio.getSucursal().getId() : null)
                .sucursal(socio.getSucursal() != null ? socio.getSucursal().getNombre() : null)
                .estadoAcceso(socio.getEstadoAcceso())
                .fotoUrl(socio.getUsuario().getFotoUrl())
                .tipoMembresia(membresiaActiva.map(m -> m.getTipoMembresia().getNombre()).orElse("Sin membresia"))
                .estadoMembresia(membresiaActiva.map(MembresiaSocio::getEstado).orElse("Inactiva"))
                .fechaVencimiento(membresiaActiva.map(MembresiaSocio::getFechaVencimiento).orElse(null))
                .build();
    }
}
