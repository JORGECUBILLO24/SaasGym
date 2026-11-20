package ni.edu.uam.GimnasioAPI.controllers;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.EstadoSocioResponse;
import ni.edu.uam.GimnasioAPI.dto.SocioResponse;
import ni.edu.uam.GimnasioAPI.services.SocioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/socios")
@RequiredArgsConstructor
public class SocioController {

    private final SocioService socioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'RECEPCIONISTA', 'ENTRENADOR')")
    public ResponseEntity<List<SocioResponse>> listar(Authentication auth) {
        return ResponseEntity.ok(socioService.listarSocios(auth));
    }

    // Mismo criterio que el resto de endpoints con socioId: el personal ve a cualquier socio,
    // un socio solo se ve a si mismo.
    @GetMapping("/estado/{socioId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'RECEPCIONISTA', 'ENTRENADOR') "
            + "or @accesoSocio.esElMismo(authentication, #socioId)")
    public ResponseEntity<EstadoSocioResponse> consultarEstado(@PathVariable UUID socioId) {
        return ResponseEntity.ok(socioService.consultarEstado(socioId));
    }
}
