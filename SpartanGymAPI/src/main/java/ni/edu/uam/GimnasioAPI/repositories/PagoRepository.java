package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoRepository extends JpaRepository<Pago, UUID> {

    @Query("select coalesce(sum(p.monto), 0) from Pago p where p.estado = 'Confirmado'")
    BigDecimal sumMontoTotal();

    List<Pago> findAllByOrderByFechaTransaccionDesc();

    List<Pago> findBySocioUsuarioIdOrderByFechaTransaccionDesc(UUID socioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :idPago")
    Optional<Pago> findByIdForUpdate(@Param("idPago") UUID idPago);
}