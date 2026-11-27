package ni.edu.uam.GimnasioAPI.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ni.edu.uam.GimnasioAPI.security.TenantContext;
import ni.edu.uam.GimnasioAPI.services.TenantLookupService;
import ni.edu.uam.GimnasioAPI.services.VentaProductoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Cancela los pedidos de la app que vencieron sin pagarse y devuelve su stock. Sin esto, un
 * socio podía apartar todo el inventario con pedidos que nunca pagaba.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoScheduler {

    private final VentaProductoService ventaProductoService;
    private final TenantLookupService tenantLookupService;

    @Scheduled(fixedDelayString = "${app.pedidos.revision-ms:300000}", initialDelayString = "${app.pedidos.revision-inicial-ms:60000}")
    public void cancelarPedidosVencidos() {
        for (UUID gimnasioId : tenantLookupService.listarGimnasiosActivos()) {
            TenantContext.setTenant(gimnasioId);
            try {
                cancelarPedidosVencidosDelGimnasio(gimnasioId);
            } catch (RuntimeException e) {
                log.warn("No se pudieron revisar los pedidos vencidos del gimnasio {}: {}",
                        gimnasioId, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void cancelarPedidosVencidosDelGimnasio(UUID gimnasioId) {
        List<UUID> vencidos = ventaProductoService.idsPedidosVencidos();
        int cancelados = 0;
        for (UUID id : vencidos) {
            try {
                if (ventaProductoService.expirarPedido(id)) {
                    cancelados++;
                }
            } catch (RuntimeException e) {
                log.warn("No se pudo cancelar el pedido vencido {} del gimnasio {}: {}",
                        id, gimnasioId, e.getMessage());
            }
        }
        if (cancelados > 0) {
            log.info("Pedidos vencidos cancelados en el gimnasio {}: {}", gimnasioId, cancelados);
        }
    }
}
