package ni.edu.uam.GimnasioAPI.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // La app manda el token en Authorization; el panel web lo trae en la cookie HttpOnly.
        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        if (authHeader != null && authHeader.startsWith(PREFIJO_BEARER)) {
            jwt = authHeader.substring(PREFIJO_BEARER.length()).trim();
        } else {
            String deCookie = CookieSesion.leer(request);
            // CSRF: con la cookie, una petición que modifica datos solo vale si trae el header
            // del panel web. Un formulario o enlace de otro sitio no puede ponerlo.
            if (deCookie != null && (esLectura(request) || CookieSesion.esClienteWeb(request))) {
                jwt = deCookie;
            }
        }
        if (jwt == null || jwt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Una cuenta desactivada no debe seguir operando con un token emitido antes.
                if (userDetails.isEnabled() && jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token corrupto o expirado: se sigue sin autenticación y Spring Security
            // rechaza la petición si el endpoint es privado.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private static boolean esLectura(HttpServletRequest request) {
        String metodo = request.getMethod();
        return "GET".equalsIgnoreCase(metodo) || "HEAD".equalsIgnoreCase(metodo) || "OPTIONS".equalsIgnoreCase(metodo);
    }
}
