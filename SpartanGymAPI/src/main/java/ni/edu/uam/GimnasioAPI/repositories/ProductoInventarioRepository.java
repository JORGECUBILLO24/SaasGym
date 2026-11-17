package ni.edu.uam.GimnasioAPI.repositories;

import jakarta.persistence.LockModeType;
import ni.edu.uam.GimnasioAPI.models.ProductoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductoInventarioRepository extends JpaRepository<ProductoInventario, UUID> {

    /**
     * Bloquea la fila hasta el fin de la transacción para que dos ventas simultáneas
     * no lean el mismo stock y lo descuenten dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductoInventario p where p.id = :id")
    Optional<ProductoInventario> findByIdParaActualizar(@Param("id") UUID id);
}
