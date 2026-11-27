package ni.edu.uam.GimnasioAPI.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantLookupService {

    private final JdbcTemplate jdbcTemplate;

    public Optional<UUID> buscarGimnasioActivoPorSlug(String slug) {
        List<UUID> ids = jdbcTemplate.query(
                "SELECT id FROM gimnasios WHERE LOWER(BTRIM(slug)) = LOWER(BTRIM(?)) AND activo = TRUE",
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
                slug
        );
        return ids.stream().findFirst();
    }

    public boolean gimnasioActivo(UUID gimnasioId) {
        List<Boolean> estados = jdbcTemplate.query(
                "SELECT activo FROM gimnasios WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getBoolean("activo"),
                gimnasioId
        );
        return estados.stream().findFirst().orElse(false);
    }

    public List<UUID> listarGimnasiosActivos() {
        return jdbcTemplate.query(
                "SELECT id FROM gimnasios WHERE activo = TRUE ORDER BY fecha_alta, id",
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class)
        );
    }

    public boolean correoUsuarioEnUso(String correo) {
        Boolean existe = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM usuarios WHERE LOWER(email) = LOWER(?))",
                Boolean.class,
                correo
        );
        return Boolean.TRUE.equals(existe);
    }
}
