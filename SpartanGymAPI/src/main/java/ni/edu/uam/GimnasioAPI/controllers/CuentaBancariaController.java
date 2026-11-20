package ni.edu.uam.GimnasioAPI.controllers;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.CuentaBancariaRequest;
import ni.edu.uam.GimnasioAPI.dto.CuentaBancariaResponse;
import ni.edu.uam.GimnasioAPI.services.CuentaBancariaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cuentas-bancarias")
@RequiredArgsConstructor
public class CuentaBancariaController {

    private final CuentaBancariaService service;

    // Cualquier sesión ve las cuentas activas (el socio las necesita para transferir);
    // administración puede pedir también las desactivadas con ?todas=true.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CuentaBancariaResponse>> listar(@RequestParam(defaultValue = "false") boolean todas,
                                                               Authentication auth) {
        boolean esAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPERADMIN".equals(a.getAuthority()));
        return ResponseEntity.ok(service.listar(todas && esAdmin));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CuentaBancariaResponse> crear(@RequestBody CuentaBancariaRequest request) {
        return ResponseEntity.ok(service.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CuentaBancariaResponse> actualizar(@PathVariable UUID id, @RequestBody CuentaBancariaRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
