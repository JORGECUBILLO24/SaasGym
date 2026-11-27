package ni.edu.uam.GimnasioAPI.controllers;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaQrRequest;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaQrTokenResponse;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaQrValidationResponse;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaRequest;
import ni.edu.uam.GimnasioAPI.dto.AsistenciaResponse;
import ni.edu.uam.GimnasioAPI.services.AsistenciaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/asistencias", "/api/asistencia"})
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<AsistenciaResponse> registrarCheckIn(@RequestBody AsistenciaRequest request) {
        return ResponseEntity.ok(asistenciaService.registrarEntradaConDecision(request));
    }

    @GetMapping("/qr-activo")
    @PreAuthorize("hasAuthority('ROLE_SOCIO')")
    public ResponseEntity<AsistenciaQrTokenResponse> obtenerQrActivo(Principal principal) {
        return ResponseEntity.ok(asistenciaService.generarQrAsistencia(principal.getName()));
    }

    @GetMapping("/qr-validacion")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<AsistenciaQrTokenResponse> obtenerQrValidacion(Principal principal) {
        return ResponseEntity.ok(asistenciaService.generarQrValidacionWeb(principal.getName()));
    }

    @PostMapping("/check-in-qr")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<AsistenciaQrValidationResponse> registrarCheckInQr(
            @RequestBody AsistenciaQrRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(asistenciaService.registrarEntradaQr(request, principal.getName()));
    }

    @PostMapping("/check-in-qr-web")
    @PreAuthorize("hasAuthority('ROLE_SOCIO')")
    public ResponseEntity<AsistenciaQrValidationResponse> registrarCheckInQrWeb(
            @RequestBody AsistenciaQrRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(asistenciaService.registrarEntradaQrWeb(request, principal.getName()));
    }

    // Solo el personal que muestra el QR en recepcion consulta su estado: la respuesta trae
    // nombre, membresia y estado de acceso del socio que lo escaneo.
    @PostMapping("/qr-validacion/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<AsistenciaQrValidationResponse> consultarEstadoQrValidacion(
            @RequestBody AsistenciaQrRequest request, Principal principal) {
        return ResponseEntity.ok(asistenciaService.consultarEstadoQrValidacion(request, principal.getName()));
    }

    @GetMapping("/estado/{sessionId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<AsistenciaQrValidationResponse> consultarEstadoQrValidacion(
            @PathVariable UUID sessionId, Principal principal) {
        return ResponseEntity.ok(asistenciaService.consultarEstadoQrValidacion(sessionId, principal.getName()));
    }

    // El entrenador solo puede consultar socios de su propia sucursal.
    @GetMapping("/socio/{socioId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'RECEPCIONISTA') "
            + "or @accesoSocio.esElMismo(authentication, #socioId) "
            + "or @accesoSocio.entrenadorDeSucursal(authentication, #socioId)")
    public ResponseEntity<List<AsistenciaResponse>> obtenerHistorialSocio(@PathVariable UUID socioId) {
        return ResponseEntity.ok(asistenciaService.obtenerHistorial(socioId).stream()
                .map(AsistenciaResponse::desde)
                .toList());
    }
}
