package ni.edu.uam.GimnasioAPI.controllers;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.MarcarEjercicioRequest;
import ni.edu.uam.GimnasioAPI.dto.RutinaRequest;
import ni.edu.uam.GimnasioAPI.exceptions.NoAutenticadoException;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.services.EjercicioCompletadoService;
import ni.edu.uam.GimnasioAPI.services.RutinaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rutinas")
@RequiredArgsConstructor
public class RutinaController {

    private final RutinaService rutinaService;
    private final EjercicioCompletadoService ejercicioCompletadoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_ENTRENADOR')")
    public ResponseEntity<List<Map<String, Object>>> listarRutinas(Authentication auth) {
        return ResponseEntity.ok(rutinaService.listarRutinas(auth));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_ENTRENADOR')")
    public ResponseEntity<Object> crearRutina(@RequestBody RutinaRequest request, Authentication auth) {
        fijarEntrenadorAutenticado(request, auth);
        if (Boolean.TRUE.equals(request.getEsGlobal())) {
            return ResponseEntity.ok(rutinaService.crearRutinaGlobal(request));
        }

        return ResponseEntity.ok(rutinaService.crearRutinaPersonalizada(request));
    }

    @PostMapping("/global")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN', 'ROLE_ENTRENADOR')")
    public ResponseEntity<Map<String, Object>> crearRutinaGlobal(@RequestBody RutinaRequest request, Authentication auth) {
        fijarEntrenadorAutenticado(request, auth);
        return ResponseEntity.ok(rutinaService.crearRutinaGlobal(request));
    }

    @PatchMapping("/{rutinaId}/imagen")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Void> actualizarImagen(@PathVariable UUID rutinaId, @RequestBody Map<String, String> request) {
        rutinaService.actualizarImagen(rutinaId, request.get("imagenUrl"));
        return ResponseEntity.noContent().build();
    }

    // El socio marca/desmarca su propio ejercicio de la rutina (socio sale del token, no del body).
    @PostMapping("/{rutinaId}/ejercicios/{ejercicioId}/completar")
    @PreAuthorize("hasAuthority('ROLE_SOCIO')")
    public ResponseEntity<Void> marcarEjercicioCompletado(
            @PathVariable UUID rutinaId,
            @PathVariable Long ejercicioId,
            @RequestBody MarcarEjercicioRequest request,
            Authentication auth
    ) {
        ejercicioCompletadoService.marcarCompletado(auth.getName(), rutinaId, ejercicioId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Un entrenador solo puede asignar rutinas en su propio nombre; el idEntrenador
     * del cuerpo se ignora para que no pueda usar el de otro miembro del personal.
     */
    private void fijarEntrenadorAutenticado(RutinaRequest request, Authentication auth) {
        boolean esEntrenador = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ENTRENADOR".equals(a.getAuthority()));
        if (!esEntrenador) {
            return;
        }
        UUID idActual = usuarioRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new NoAutenticadoException("Usuario autenticado no encontrado"))
                .getId();
        request.setIdEntrenador(idActual);
    }
}
