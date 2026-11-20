package ni.edu.uam.GimnasioAPI.repositories;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import ni.edu.uam.GimnasioAPI.models.SolicitudPagoMembresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudPagoMembresiaRepository extends JpaRepository<SolicitudPagoMembresia, UUID> {

    List<SolicitudPagoMembresia> findBySocio_UsuarioIdOrderByFechaCreacionDesc(UUID socioId);

    List<SolicitudPagoMembresia> findByEstadoOrderByFechaCreacionAsc(String estado);

    List<SolicitudPagoMembresia> findTop100ByOrderByFechaCreacionDesc();

    boolean existsBySocio_UsuarioIdAndEstado(UUID socioId, String estado);

    /** Una misma referencia bancaria no puede usarse dos veces (salvo que la rechazaran). */
    @Query("select count(s) > 0 from SolicitudPagoMembresia s where s.cuentaBancaria.id = :cuentaId "
            + "and upper(s.referencia) = upper(:referencia) and s.estado <> 'Rechazada'")
    boolean referenciaEnUso(@Param("cuentaId") UUID cuentaId, @Param("referencia") String referencia);

    /** Bloquea la solicitud para que dos personas no la aprueben a la vez (doble membresía). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select s from SolicitudPagoMembresia s where s.id = :id")
    Optional<SolicitudPagoMembresia> findByIdForUpdate(@Param("id") UUID id);
}
