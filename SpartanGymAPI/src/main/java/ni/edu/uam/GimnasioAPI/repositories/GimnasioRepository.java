package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.Gimnasio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GimnasioRepository extends JpaRepository<Gimnasio, UUID> {
    boolean existsByNombreComercialIgnoreCase(String nombreComercial);

    boolean existsBySlugIgnoreCase(String slug);
}
