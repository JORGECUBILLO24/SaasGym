package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.MovimientoFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, UUID> {
}
