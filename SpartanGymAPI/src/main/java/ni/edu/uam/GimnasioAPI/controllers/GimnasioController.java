package ni.edu.uam.GimnasioAPI.controllers;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.RegistroGimnasioRequest;
import ni.edu.uam.GimnasioAPI.dto.RegistroGimnasioResponse;
import ni.edu.uam.GimnasioAPI.exceptions.ConflictoException;
import ni.edu.uam.GimnasioAPI.models.Gimnasio;
import ni.edu.uam.GimnasioAPI.repositories.GimnasioRepository;
import ni.edu.uam.GimnasioAPI.security.TenantContext;
import ni.edu.uam.GimnasioAPI.security.TenantContextFilter;
import ni.edu.uam.GimnasioAPI.services.RegistroGimnasioService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gimnasios")
@RequiredArgsConstructor
public class GimnasioController {

    private final GimnasioRepository gimnasioRepository;
    private final RegistroGimnasioService registroGimnasioService;

    @PostMapping("/registro")
    public ResponseEntity<RegistroGimnasioResponse> registrar(
            @RequestBody @jakarta.validation.Valid RegistroGimnasioRequest request,
            @RequestAttribute(TenantContextFilter.PROVISIONING_ID_ATTRIBUTE) UUID provisioningId) {
        if (!provisioningId.equals(TenantContext.requireTenantId())) {
            throw new IllegalStateException("El contexto de registro del gimnasio no coincide.");
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(registroGimnasioService.registrar(request));
        } catch (DataIntegrityViolationException e) {
            String detalle = e.getMostSpecificCause().getMessage();
            if (detalle != null && (detalle.contains("ux_gimnasios_nombre_comercial_ci")
                    || detalle.contains("ux_gimnasios_slug_ci"))) {
                throw new ConflictoException("El nombre comercial o slug ya está registrado.");
            }
            throw e;
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public List<Gimnasio> listarTodos() {
        return gimnasioRepository.findAll();
    }
}
