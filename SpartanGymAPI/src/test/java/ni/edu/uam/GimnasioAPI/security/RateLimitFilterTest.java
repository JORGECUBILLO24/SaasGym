package ni.edu.uam.GimnasioAPI.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void bloqueaIntentosExcesivosDeLogin() throws Exception {
        RateLimitFilter filtro = new RateLimitFilter();
        FilterChain chain = (request, response) -> { };

        MockHttpServletResponse ultima = null;
        for (int intento = 0; intento < 11; intento++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("203.0.113.10");
            ultima = new MockHttpServletResponse();
            filtro.doFilter(request, ultima, chain);
        }

        assertThat(ultima.getStatus()).isEqualTo(429);
        assertThat(ultima.getHeader("Retry-After")).isNotBlank();
        assertThat(ultima.getContentAsString()).contains("DEMASIADAS_SOLICITUDES");
    }

    @Test
    void noLimitaGetNiEndpointsNoProtegidos() throws Exception {
        RateLimitFilter filtro = new RateLimitFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setRemoteAddr("203.0.113.11");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, (req, res) -> { });

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
