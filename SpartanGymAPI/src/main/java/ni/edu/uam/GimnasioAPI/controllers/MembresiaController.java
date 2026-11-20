package ni.edu.uam.GimnasioAPI.controllers;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.TipoMembresiaRequest;
import ni.edu.uam.GimnasioAPI.models.TipoMembresia;
import ni.edu.uam.GimnasioAPI.services.MembresiaService;
import ni.edu.uam.GimnasioAPI.services.TransferenciaService;
import ni.edu.uam.GimnasioAPI.dto.SolicitudPagoResponse;
import ni.edu.uam.GimnasioAPI.dto.SolicitudTransferenciaRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membresias")
@RequiredArgsConstructor
public class MembresiaController {

    private final MembresiaService membresiaService;
    private final TransferenciaService transferenciaService;

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoMembresia>> listarTipos() {
        return ResponseEntity.ok(membresiaService.listarTipos());
    }

    @PostMapping("/tipos")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<TipoMembresia> crearTipo(@RequestBody TipoMembresiaRequest request) {
        return ResponseEntity.ok(membresiaService.crearTipo(request));
    }

    @PutMapping("/tipos/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<TipoMembresia> actualizarTipo(@PathVariable Integer id,
                                                         @RequestBody TipoMembresiaRequest request) {
        return ResponseEntity.ok(membresiaService.actualizarTipo(id, request));
    }

    @PatchMapping("/tipos/{id}/imagen")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<TipoMembresia> actualizarImagen(@PathVariable Integer id,
                                                           @RequestBody ImagenMembresiaRequest request) {
        return ResponseEntity.ok(membresiaService.actualizarImagen(id, request.imagenUrl()));
    }

    public record ImagenMembresiaRequest(String imagenUrl) {}

    @DeleteMapping("/tipos/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<Void> eliminarTipo(@PathVariable Integer id) {
        membresiaService.eliminarTipo(id);
        return ResponseEntity.noContent().build();
    }

    // Igual que /api/pagos/mi-membresia: el socio reporta su transferencia y queda pendiente
    // de confirmación. Antes este endpoint activaba la membresía sin ningún cobro.
    @PostMapping("/comprar")
    @PreAuthorize("hasAuthority('ROLE_SOCIO')")
    public ResponseEntity<SolicitudPagoResponse> comprarMembresia(
            @RequestBody SolicitudTransferenciaRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(transferenciaService.solicitar(authentication.getName(), request));
    }
}
