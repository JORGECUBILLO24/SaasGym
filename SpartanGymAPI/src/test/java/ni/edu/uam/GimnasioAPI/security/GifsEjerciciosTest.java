package ni.edu.uam.GimnasioAPI.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La app carga los GIFs con un cargador de imágenes que no envía el JWT. */
@SpringBootTest
class GifsEjerciciosTest {

    @Autowired private WebApplicationContext contexto;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void gifDeEjercicio_seSirveSinToken() throws Exception {
        mvc.perform(get("/gifs/0009.gif"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/gif"));
    }
}
