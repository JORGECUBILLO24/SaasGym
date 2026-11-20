package ni.edu.uam.GimnasioAPI.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Sesión del panel web en una cookie HttpOnly: el JavaScript de la página no puede leer el
 * token, así que un script inyectado ya no puede robarlo (antes vivía en localStorage).
 *
 * La app Android sigue usando el header Authorization: ignora esta cookie.
 *
 * SameSite=Lax + Path=/api: el navegador no la manda en peticiones POST/PUT/DELETE que vengan
 * de otro sitio. Además, para esos métodos el filtro exige el header X-Requested-With, que un
 * formulario o enlace de otro sitio no puede poner (CSRF).
 *
 * El web llama a la API por el mismo dominio (Vercel reenvía /api a Render), así que la cookie
 * es de primera parte y Safari/iPhone no la bloquea.
 */
@Component
public class CookieSesion {

    public static final String NOMBRE = "spartan_sesion";
    public static final String HEADER_WEB = "X-Requested-With";

    @Value("${app.auth.cookie-secure:true}")
    private boolean segura = true;

    @Value("${jwt.expiration:86400000}")
    private long duracionMs = 86_400_000L;

    public void escribir(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(NOMBRE, token)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Lax")
                .path("/api")
                .maxAge(Duration.ofMillis(duracionMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void borrar(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(NOMBRE, "")
                .httpOnly(true)
                .secure(segura)
                .sameSite("Lax")
                .path("/api")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static String leer(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NOMBRE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** El panel web manda este header en todas sus peticiones; la app y otros clientes no. */
    public static boolean esClienteWeb(HttpServletRequest request) {
        return request != null && request.getHeader(HEADER_WEB) != null;
    }
}
