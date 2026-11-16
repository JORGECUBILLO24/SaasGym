package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.GrupoMuscular;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrupoMuscularRepository extends JpaRepository<GrupoMuscular, Integer> {
    Optional<GrupoMuscular> findByNombre(String nombre);
}
