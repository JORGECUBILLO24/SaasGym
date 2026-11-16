package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.ConfiguracionApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionAppRepository extends JpaRepository<ConfiguracionApp, Integer> {
}
