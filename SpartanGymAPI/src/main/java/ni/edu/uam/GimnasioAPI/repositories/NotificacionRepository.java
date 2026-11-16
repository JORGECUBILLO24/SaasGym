package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {
    List<Notificacion> findByUsuarioIdOrUsuarioIsNullOrderByFechaCreacionDesc(UUID usuarioId);

    List<Notificacion> findByUsuarioIsNullOrderByFechaCreacionDesc();
}
