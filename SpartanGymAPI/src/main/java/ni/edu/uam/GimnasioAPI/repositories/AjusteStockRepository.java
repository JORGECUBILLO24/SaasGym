package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.AjusteStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AjusteStockRepository extends JpaRepository<AjusteStock, UUID> {
    List<AjusteStock> findTop50ByProducto_IdOrderByFechaDesc(UUID productoId);
}
