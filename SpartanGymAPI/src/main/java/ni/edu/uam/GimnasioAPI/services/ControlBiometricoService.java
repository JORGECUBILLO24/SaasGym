package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.ControlBiometricoRequest;
import ni.edu.uam.GimnasioAPI.models.ControlBiometrico;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.repositories.ControlBiometricoRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.PersonalRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ControlBiometricoService {

    private final ControlBiometricoRepository controlRepository;
    private final SocioRepository socioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonalRepository personalRepository;

    @Transactional
    public ControlBiometrico registrarProgreso(ControlBiometricoRequest request) {
        Socio socio = socioRepository.findById(request.getIdSocio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Error: Socio no encontrado."));

        ControlBiometrico control = new ControlBiometrico();
        control.setSocio(socio);
        control.setPesoKg(request.getPesoKg());
        control.setMedidasNotas(request.getMedidasNotas());
        control.setFechaRegistro(LocalDateTime.now());

        return controlRepository.save(control);
    }

    @Transactional
    public ControlBiometrico registrarProgreso(ControlBiometricoRequest request, Authentication auth) {
        Socio socio = socioRepository.findById(request.getIdSocio())
                .orElseThrow(() -> new RecursoNoEncontradoException("Error: Socio no encontrado."));
        validarSucursalPersonal(auth, socio);
        return registrarProgreso(request);
    }

    private void validarSucursalPersonal(Authentication auth, Socio socio) {
        if (auth == null || auth.getName() == null) {
            return;
        }
        var usuario = usuarioRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
        if (usuario == null || !"ROLE_ENTRENADOR".equals(usuario.getRol().getNombre())) {
            return;
        }
        var personal = personalRepository.findById(usuario.getId()).orElse(null);
        UUID sucursalPersonal = personal != null && personal.getSucursal() != null
                ? personal.getSucursal().getId() : null;
        UUID sucursalSocio = socio.getSucursal() != null ? socio.getSucursal().getId() : null;
        if (sucursalPersonal == null || !sucursalPersonal.equals(sucursalSocio)) {
            throw new AccesoDenegadoException("El socio no pertenece a tu sucursal.");
        }
    }

    public List<ControlBiometrico> obtenerHistorialProgreso(UUID socioId) {
        return controlRepository.findBySocioUsuarioIdOrderByFechaRegistroAsc(socioId);
    }
}