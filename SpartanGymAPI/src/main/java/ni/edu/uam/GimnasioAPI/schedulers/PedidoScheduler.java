package ni.edu.uam.GimnasioAPI.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(fixedDelayString = "${app.pedidos.revision-ms:300000}", initialDelayString = "${app.pedidos.revision-inicial-ms:60000}")
    public void cancelarPedidosVencidos() {
        List<UUID> vencidos;
        try {
            vencidos = ventaProductoService.idsPedidosVencidos();
        } catch (RuntimeException e) {
            log.warn("No se pudo revisar los pedidos vencidos: {}", e.getMessage());
            return;
        }
        int cancelados = 0;
        for (UUID id : vencidos) {
            // Cada pedido en su propia transacción: si uno falla, el resto sigue.
            try {
                if (ventaProductoService.expirarPedido(id)) {
                    cancelados++;
                }
            } catch (RuntimeException e) {
                log.warn("No se pudo cancelar el pedido vencido {}: {}", id, e.getMessage());
            }
        }
        if (cancelados > 0) {
            log.info("Pedidos vencidos cancelados: {}", cancelados);
        }
    }
}
