package ni.edu.uam.GimnasioAPI.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, Limite> LIMITES = Map.of(
            "/api/auth/login", new Limite(10, 15 * 60 * 1000L),
            "/api/auth/register", new Limite(5, 60 * 60 * 1000L),
            "/api/auth/forgot-password", new Limite(5, 60 * 60 * 1000L),
            "/api/auth/reset-password", new Limite(5, 60 * 60 * 1000L),
            "/api/auth/google", new Limite(20, 15 * 60 * 1000L)
    );

    private final Map<String, Deque<Long>> solicitudes = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Limite limite = LIMITES.get(request.getRequestURI());
        if (limite == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clave = request.getRemoteAddr() + "|" + request.getRequestURI();
        long ahora = Instant.now().toEpochMilli();
        Deque<Long> ventana = solicitudes.computeIfAbsent(clave, ignored -> new ArrayDeque<>());

        synchronized (ventana) {
            while (!ventana.isEmpty() && ahora - ventana.peekFirst() >= limite.ventanaMs()) {
                ventana.removeFirst();
            }
            if (ventana.size() >= limite.maximos()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", String.valueOf(limite.ventanaMs() / 1000));
                response.getWriter().write("{\"codigo\":\"DEMASIADAS_SOLICITUDES\",\"mensaje\":\"Has realizado demasiados intentos. Espera antes de volver a intentarlo.\"}");
                return;
            }
            ventana.addLast(ahora);
        }

        filterChain.doFilter(request, response);
    }

    private record Limite(int maximos, long ventanaMs) {
    }
}
