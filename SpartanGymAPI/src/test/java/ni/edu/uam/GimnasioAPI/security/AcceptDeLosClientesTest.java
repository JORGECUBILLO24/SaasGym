package ni.edu.uam.GimnasioAPI.security;

import ni.edu.uam.GimnasioAPI.exceptions.ReglaNegocioException;
import ni.edu.uam.GimnasioAPI.services.ConfiguracionAppService;
import ni.edu.uam.GimnasioAPI.services.PasswordResetService;
import ni.edu.uam.GimnasioAPI.services.TenantLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;

/**
 * Fija el contrato con el header Accept EXACTO que mandan la web (api.js) y la app
 * (ErroresApi.kt). Con problem+json primero en la lista, Spring negociaba también las
 * respuestas exitosas como application/problem+json, y la web dejaba de parsear el JSON:
 * pasó en producción. Si alguien vuelve a cambiar el orden, estos tests lo detectan.
 */
@SpringBootTest
class AcceptDeLosClientesTest {

    /** Copiado literal de api.js y ErroresApi.kt: si cambia allá, hay que cambiarlo acá. */
    static final String ACCEPT_CLIENTES = "application/json, application/problem+json;q=0.9, */*;q=0.8";

    @Autowired private WebApplicationContext contexto;
    @MockitoBean private PasswordResetService passwordResetService;
    @MockitoBean private TenantLookupService tenantLookupService;
    @MockitoBean private ConfiguracionAppService configuracionAppService;
    @MockitoBean private JwtService jwtService;
    private static final UUID GIMNASIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTRO_GIMNASIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        when(tenantLookupService.buscarGimnasioActivoPorSlug("spartan-gym"))
                .thenReturn(Optional.of(GIMNASIO_ID));
        when(tenantLookupService.buscarGimnasioActivoPorSlug("otro-gym"))
                .thenReturn(Optional.of(OTRO_GIMNASIO_ID));
    }

    @Test
    void respuestaExitosa_esApplicationJsonYNoProblemJson() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .header("Accept", ACCEPT_CLIENTES)
                        .header(TenantContextFilter.SLUG_HEADER, "spartan-gym")
                        .contentType("application/json")
                        .content("{\"email\":\"socio@ejemplo.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void error_esProblemJsonConCodigo() throws Exception {
        doThrow(new ReglaNegocioException("El enlace de restablecimiento no es valido o ya fue usado"))
                .when(passwordResetService).restablecerPassword(anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                        .header("Accept", ACCEPT_CLIENTES)
                        .header(TenantContextFilter.SLUG_HEADER, "spartan-gym")
                        .contentType("application/json")
                        .content("{\"token\":\"falso\",\"password\":\"ClaveNueva123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void configuracionPublica_resuelveElSlugAunqueLaSolicitudTraigaSesionDeOtroGimnasio() throws Exception {
        when(jwtService.extractGimnasioId("sesion-gym-a")).thenReturn(GIMNASIO_ID);
        when(tenantLookupService.gimnasioActivo(GIMNASIO_ID)).thenReturn(true);
        when(configuracionAppService.obtener()).thenAnswer(invocation -> {
            org.junit.jupiter.api.Assertions.assertEquals(OTRO_GIMNASIO_ID, TenantContext.requireTenantId());
            return Map.of("gymName", "Otro Gym");
        });

        mockMvc.perform(get("/api/configuracion")
                        .header(TenantContextFilter.SLUG_HEADER, "otro-gym")
                        .header("Authorization", "Bearer sesion-gym-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gymName").value("Otro Gym"));

        verify(configuracionAppService).obtener();
    }
}
