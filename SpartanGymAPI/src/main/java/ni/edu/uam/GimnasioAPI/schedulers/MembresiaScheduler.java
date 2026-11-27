package ni.edu.uam.GimnasioAPI.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ni.edu.uam.GimnasioAPI.models.MembresiaSocio;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.repositories.MembresiaSocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.security.TenantContext;
import ni.edu.uam.GimnasioAPI.services.NotificacionService;
import ni.edu.uam.GimnasioAPI.services.TenantLookupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j // Para poder imprimir mensajes bonitos en la consola
@Component
@RequiredArgsConstructor
public class MembresiaScheduler {

    private final MembresiaSocioRepository membresiaRepository;
    private final SocioRepository socioRepository;
    private final NotificacionService notificacionService;
    private final TenantLookupService tenantLookupService;
    private final PlatformTransactionManager transactionManager;

    // "0 0 0 * * *" significa: Todos los días a las 00:00 (Medianoche)
    // Para hacer pruebas rápidas podrías usar @Scheduled(fixedRate = 60000) que corre cada 1 minuto
    @Scheduled(cron = "0 0 0 * * *")
    public void verificarMembresiasVencidas() {
        for (UUID gimnasioId : tenantLookupService.listarGimnasiosActivos()) {
            TenantContext.setTenant(gimnasioId);
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(
                        status -> verificarMembresiasVencidasDelGimnasio(gimnasioId)
                );
            } catch (RuntimeException e) {
                log.error("No se pudieron revisar las membresías vencidas del gimnasio {}: {}",
                        gimnasioId, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void verificarMembresiasVencidasDelGimnasio(UUID gimnasioId) {
        log.info("Iniciando revisión automática de membresías vencidas del gimnasio {}...", gimnasioId);

        // Buscamos las que están "Activas" pero su fecha de vencimiento es ANTES de hoy
        List<MembresiaSocio> vencidas = membresiaRepository.findByEstadoAndFechaVencimientoBefore("Activa", LocalDate.now());

        int contador = 0;
        int fallidas = 0;
        for (MembresiaSocio membresia : vencidas) {
            // Cada socio se procesa aislado: si uno falla (dato corrupto, lo que sea), el
            // resto del lote de esa noche debe seguir igual. Antes una excepcion ac + su rollback
            // dejaba a todos los que venian despues en la lista sin vencer, sin que quedara
            // ningun rastro de que el proceso se corto a mitad de camino.
            try {
                // 1. Vencemos la membresía
                membresia.setEstado("Vencida");
                membresiaRepository.save(membresia);

                // 2. Le bloqueamos el torniquete al socio
                Socio socio = membresia.getSocio();
                // Una suspensión administrativa se mantiene aunque la membresía venza.
                if (!"Suspendido".equalsIgnoreCase(socio.getEstadoAcceso())) {
                    socio.setEstadoAcceso("Inactivo");
                    socioRepository.save(socio);
                }

                contador++;
                log.info("Membresía vencida para el socio: {} {}", socio.getNombres(), socio.getApellidos());

                notificacionService.registrarNotificacion(
                        socio.getUsuario(),
                        "alerta",
                        "Membresía vencida",
                        "Tu membresía ha vencido. Acércate a recepción para renovarla."
                );
                notificacionService.enviarCorreoVencimiento(
                        gimnasioId, socio.getUsuario().getEmail(), socio.getNombres()
                );
            } catch (Exception e) {
                fallidas++;
                log.error("No se pudo vencer la membresía {} (socio {})",
                        membresia.getId(),
                        membresia.getSocio() != null ? membresia.getSocio().getUsuarioId() : null,
                        e);
            }
        }

        if (fallidas > 0) {
            log.warn("Revisión terminada en gimnasio {} con errores: {} actualizadas, {} fallidas de {} encontradas.",
                    gimnasioId, contador, fallidas, vencidas.size());
        } else {
            log.info("Revisión terminada en gimnasio {}. Membresías actualizadas: {}",
                    gimnasioId, contador);
        }
    }
}