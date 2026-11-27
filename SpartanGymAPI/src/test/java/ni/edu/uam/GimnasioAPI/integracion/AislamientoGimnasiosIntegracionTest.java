package ni.edu.uam.GimnasioAPI.integracion;

import ni.edu.uam.GimnasioAPI.models.Rol;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.security.JwtService;
import ni.edu.uam.GimnasioAPI.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@EnabledIfEnvironmentVariable(named = "SAASGYM_IT_DB_URL", matches = ".+")
class AislamientoGimnasiosIntegracionTest extends IntegracionPostgresBase {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtService jwtService;
    @Autowired private WebApplicationContext applicationContext;
    @Autowired private tools.jackson.databind.ObjectMapper objectMapper;

    @Test
    void administradorDeUnGimnasioNoPuedeConsultarSocioPagoNiVentaDeOtro() throws Exception {
        UUID gimnasioA = UUID.randomUUID();
        UUID gimnasioB = UUID.randomUUID();
        UUID usuarioAdminA = UUID.randomUUID();
        UUID socioB = UUID.randomUUID();
        UUID sucursalB = UUID.randomUUID();
        UUID pagoB = UUID.randomUUID();
        UUID ventaB = UUID.randomUUID();
        String sufijo = UUID.randomUUID().toString();
        String emailAdminA = "admin-" + sufijo + "@integracion.test";

        insertarGimnasio(gimnasioA, "Gym A " + sufijo, "gym-a-" + sufijo);
        insertarGimnasio(gimnasioB, "Gym B " + sufijo, "gym-b-" + sufijo);

        int rolAdmin = idRol("ROLE_ADMIN");
        int rolSocio = idRol("ROLE_SOCIO");
        jdbcTemplate.update("""
                INSERT INTO usuarios (id, gimnasio_id, email, password_hash, rol_id, activo)
                VALUES (?, ?, ?, 'hash', ?, TRUE)
                """, usuarioAdminA, gimnasioA, emailAdminA, rolAdmin);
        jdbcTemplate.update("""
                INSERT INTO usuarios (id, gimnasio_id, email, password_hash, rol_id, activo)
                VALUES (?, ?, ?, 'hash', ?, TRUE)
                """, socioB, gimnasioB, "socio-" + sufijo + "@integracion.test", rolSocio);
        jdbcTemplate.update("""
                INSERT INTO socios (usuario_id, gimnasio_id, nombres, apellidos)
                VALUES (?, ?, 'Socio', 'Gimnasio B')
                """, socioB, gimnasioB);
        jdbcTemplate.update("""
                INSERT INTO sucursales
                    (id, gimnasio_id, nombre, ubicacion, capacidad, horario_apertura, horario_cierre)
                VALUES (?, ?, 'Principal', 'Integración', 10, '06:00', '22:00')
                """, sucursalB, gimnasioB);
        jdbcTemplate.update("""
                INSERT INTO pagos (id, gimnasio_id, socio_id, monto, metodo_pago)
                VALUES (?, ?, ?, 25.00, 'Efectivo')
                """, pagoB, gimnasioB, socioB);
        jdbcTemplate.update("""
                INSERT INTO ventas_productos
                    (id, gimnasio_id, numero_factura, sucursal_id, socio_id, cliente_nombre,
                     metodo_pago, subtotal, impuesto, total)
                VALUES (?, ?, ?, ?, ?, 'Socio Gimnasio B', 'Efectivo', 25.00, 0.00, 25.00)
                """, ventaB, gimnasioB, "FACT-" + sufijo.replace("-", ""), sucursalB, socioB);

        Usuario adminA = new Usuario();
        adminA.setId(usuarioAdminA);
        adminA.setGimnasioId(gimnasioA);
        adminA.setEmail(emailAdminA);
        adminA.setPasswordHash("hash");
        Rol rol = new Rol();
        rol.setId(rolAdmin);
        rol.setNombre("ROLE_ADMIN");
        adminA.setRol(rol);
        String token = jwtService.generateToken(new UserDetailsImpl(adminA));
        MockMvc mockMvc = webAppContextSetup(applicationContext).apply(springSecurity()).build();
        String authorization = "Bearer " + token;

        mockMvc.perform(get("/api/socios/estado/{id}", socioB).header("Authorization", authorization))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/pagos/{id}/confirmar", pagoB).header("Authorization", authorization))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/ventas/productos/{id}", ventaB).header("Authorization", authorization))
                .andExpect(status().isNotFound());

        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM socios WHERE usuario_id = ? AND gimnasio_id = ?", Integer.class, socioB, gimnasioB
        ));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pagos WHERE id = ? AND gimnasio_id = ?", Integer.class, pagoB, gimnasioB
        ));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ventas_productos WHERE id = ? AND gimnasio_id = ?", Integer.class, ventaB, gimnasioB
        ));
    }

    @Test
    void registroCreaGimnasioAdministradorYSucursal_yNombreDuplicadoResponde409() throws Exception {
        String sufijo = UUID.randomUUID().toString();
        String nombre = "Gimnasio Registro " + sufijo;
        String slug = "registro-" + sufijo.replace("-", "");
        String correoAdmin = "admin-" + sufijo + "@integracion.test";
        String body = """
                {
                  "nombreComercial": "%s",
                  "correo": "contacto-%s@integracion.test",
                  "telefono": "88880000",
                  "slug": "%s",
                  "administrador": {
                    "nombres": "Admin",
                    "apellidos": "Integracion",
                    "correo": "%s",
                    "password": "ClaveSegura123",
                    "telefono": "88880001"
                  },
                  "sucursalPrincipal": {
                    "nombre": "Principal",
                    "ubicacion": "Integracion",
                    "telefono": "88880002",
                    "capacidad": 50,
                    "horarioApertura": "06:00",
                    "horarioCierre": "22:00"
                  }
                }
                """.formatted(nombre, sufijo, slug, correoAdmin);
        MockMvc mockMvc = webAppContextSetup(applicationContext).apply(springSecurity()).build();

        MvcResult resultado = mockMvc.perform(post("/api/gimnasios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gimnasioId").exists())
                .andReturn();

        UUID gimnasioId = UUID.fromString(
                objectMapper.readTree(resultado.getResponse().getContentAsString())
                        .get("gimnasioId").asString()
        );
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE gimnasio_id = ? AND email = ?",
                Integer.class, gimnasioId, correoAdmin
        ));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sucursales WHERE gimnasio_id = ?", Integer.class, gimnasioId
        ));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM configuracion_app WHERE gimnasio_id = ?", Integer.class, gimnasioId
        ));

        mockMvc.perform(post("/api/gimnasios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace(correoAdmin, "otro-" + correoAdmin)))
                .andExpect(status().isConflict());
    }

    private void insertarGimnasio(UUID id, String nombre, String slug) {
        jdbcTemplate.update("""
                INSERT INTO gimnasios (id, nombre_comercial, correo, slug)
                VALUES (?, ?, ?, ?)
                """, id, nombre, "contacto-" + slug + "@integracion.test", slug);
    }

    private int idRol(String nombre) {
        return jdbcTemplate.queryForObject("SELECT id FROM roles WHERE nombre = ?", Integer.class, nombre);
    }
}
