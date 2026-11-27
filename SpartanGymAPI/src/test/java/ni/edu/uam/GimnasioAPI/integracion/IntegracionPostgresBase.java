package ni.edu.uam.GimnasioAPI.integracion;

import ni.edu.uam.GimnasioAPI.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

/**
 * Base de las pruebas de integración contra un PostgreSQL REAL (concurrencia, bloqueos,
 * restricciones de la base). Con mocks no se puede probar una carrera: lo que importa es cómo
 * se comportan las transacciones y los bloqueos de la base de datos.
 *
 * Solo corren si SAASGYM_IT_DB_URL está definida: cada subclase debe declarar también
 * @EnabledIfEnvironmentVariable, porque JUnit no hereda esa condición de esta clase base. La CI la define con un servicio postgres:16.
 * En local, apuntarla a una base DESCARTABLE con el esquema ya cargado, nunca a una de verdad:
 *
 *   SAASGYM_IT_DB_URL=jdbc:postgresql://localhost:5432/saasgym_it
 *   SAASGYM_IT_DB_USER=postgres  SAASGYM_IT_DB_PASSWORD=...
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SAASGYM_IT_DB_URL", matches = ".+")
public abstract class IntegracionPostgresBase {

    @Autowired private JdbcTemplate jdbcTemplate;
    protected UUID gimnasioPruebasId;

    @BeforeEach
    void establecerGimnasioDePrueba() {
        gimnasioPruebasId = jdbcTemplate.queryForObject(
                "SELECT id FROM gimnasios WHERE slug = 'spartan-gym'",
                UUID.class
        );
        TenantContext.setTenant(gimnasioPruebasId);
    }

    @AfterEach
    void limpiarContextoDeGimnasio() {
        TenantContext.clear();
    }

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getenv("SAASGYM_IT_DB_URL"));
        // La base de integración se arma con las mismas migraciones que producción.
        registro.add("spring.flyway.enabled", () -> "true");
        registro.add("spring.datasource.username", () -> System.getenv().getOrDefault("SAASGYM_IT_DB_USER", "postgres"));
        registro.add("spring.datasource.password", () -> System.getenv().getOrDefault("SAASGYM_IT_DB_PASSWORD", "postgres"));
    }
}
