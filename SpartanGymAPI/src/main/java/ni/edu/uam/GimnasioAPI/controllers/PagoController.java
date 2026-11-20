package ni.edu.uam.GimnasioAPI.controllers;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.FacturaMembresiaResponse;
import ni.edu.uam.GimnasioAPI.dto.PagoRequest;
import ni.edu.uam.GimnasioAPI.dto.RechazoSolicitudRequest;
import ni.edu.uam.GimnasioAPI.dto.SolicitudPagoResponse;
import ni.edu.uam.GimnasioAPI.dto.SolicitudTransferenciaRequest;
import ni.edu.uam.GimnasioAPI.services.PagoService;
import ni.edu.uam.GimnasioAPI.services.TransferenciaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;
    private final TransferenciaService transferenciaService;

    // Cobro hecho en mostrador a nombre de un socio: lo registra el personal, no el socio.
    // Recepción solo cobra a socios de su sucursal.
    @PostMapping("/renovar")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'RECEPCIONISTA') "
            + "and @alcanceSucursal.puedeAtenderSocio(authentication, #request.idSocio)")
    public ResponseEntity<FacturaMembresiaResponse> registrarPago(@RequestBody PagoRequest request) {
        return ResponseEntity.ok(pagoService.registrarPagoYMembresia(request));
    }

    // El socio reporta desde la app la transferencia que hizo: queda pendiente hasta que
    // recepción la confirme. Ya no activa la membresía por sí sola.
    @PostMapping("/mi-membresia")
    @PreAuthorize("hasAuthority('ROLE_SOCIO')")
    public ResponseEntity<SolicitudPagoResponse> reportarTransferencia(@RequestBody SolicitudTransferenciaRequest request,
                                                                       Authentication auth) {
        return ResponseEntity.ok(transferenciaService.solicitar(auth.getName(), request));
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasAuthority('ROLE_SOCIO')")
    public ResponseEntity<List<SolicitudPagoResponse>> misSolicitudes(Authentication auth) {
        return ResponseEntity.ok(transferenciaService.misSolicitudes(auth.getName()));
    }

    // Confirma un pago que quedó pendiente (pagos creados con el flujo anterior de la app).
    @PostMapping("/{idPago}/confirmar")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Void> confirmarPago(@PathVariable UUID idPago, Authentication auth) {
        pagoService.confirmarPago(idPago, auth);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/solicitudes")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<SolicitudPagoResponse>> solicitudes(@RequestParam(required = false) String estado,
                                                                   Authentication auth) {
        return ResponseEntity.ok(transferenciaService.listarParaPersonal(auth, estado));
    }

    @PostMapping("/solicitudes/{id}/aprobar")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<FacturaMembresiaResponse> aprobar(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(transferenciaService.aprobar(id, auth));
    }

    @PostMapping("/solicitudes/{id}/rechazar")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<SolicitudPagoResponse> rechazar(@PathVariable UUID id,
                                                          @RequestBody RechazoSolicitudRequest request,
                                                          Authentication auth) {
        return ResponseEntity.ok(transferenciaService.rechazar(id, request == null ? null : request.getMotivo(), auth));
    }
}
