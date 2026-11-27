package ni.edu.uam.GimnasioAPI.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.exceptions.RespuestaError;
import ni.edu.uam.GimnasioAPI.services.TenantLookupService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    static final String SLUG_HEADER = "X-Gimnasio-Slug";
    public static final String PROVISIONING_ID_ATTRIBUTE = "tenantProvisioningId";
    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final TenantLookupService tenantLookupService;
    private final CookieSesion cookieSesion;
    private final ObjectMapper objectMapper;
    private final RespuestaError respuestaError;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            jakarta.servlet.FilterChain filterChain
    ) throws jakarta.servlet.ServletException, IOException {
        TenantContext.clear();
        try {
            HttpServletRequest scopedRequest;
            try {
                scopedRequest = resolverTenant(request, response);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                respuestaError.escribir(response, HttpStatus.UNAUTHORIZED,
                        "SESION_INVALIDA", "Tu sesión expiró o no es válida. Inicia sesión nuevamente.", request);
                return;
            }
            if (scopedRequest == null) {
                return;
            }
            filterChain.doFilter(scopedRequest, response);
        } finally {
            TenantContext.clear();
        }
    }

    private HttpServletRequest resolverTenant(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (esRegistroGimnasio(request)) {
            UUID provisionalId = UUID.randomUUID();
            request.setAttribute(PROVISIONING_ID_ATTRIBUTE, provisionalId);
            TenantContext.setTenant(provisionalId);
            return request;
        }

        if (esLogin(request)) {
            CachedBodyRequest cachedRequest = new CachedBodyRequest(request);
            String slug = slugEnBody(cachedRequest);
            return resolverPorSlug(slug, request, response, cachedRequest);
        }

        if (usaSlugParaAutenticacionPublica(request)) {
            return resolverPorSlug(
                    request.getHeader(SLUG_HEADER), request, response, request
            );
        }

        String slugPublico = request.getHeader(SLUG_HEADER);
        if (requiereSlugPublico(request) && slugPublico != null && !slugPublico.isBlank()) {
            return resolverPorSlug(slugPublico, request, response, request);
        }

        String token = tokenDe(request);
        if (token != null) {
            try {
                UUID gimnasioId = jwtService.extractGimnasioId(token);
                if (!tenantLookupService.gimnasioActivo(gimnasioId)) {
                    respuestaError.escribir(response, HttpStatus.UNAUTHORIZED,
                            "GIMNASIO_NO_DISPONIBLE", "El gimnasio de la sesión no está disponible.", request);
                    return null;
                }
                TenantContext.setTenant(gimnasioId);
                return request;
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                if (!requiereSlugPublico(request)) {
                    throw e;
                }
            }
        }

        if (!requiereSlugPublico(request)) {
            return request;
        }
        return resolverPorSlug(request.getHeader(SLUG_HEADER), request, response, request);
    }

    private HttpServletRequest resolverPorSlug(
            String slug,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpServletRequest scopedRequest
    ) throws IOException {
        if (slug == null || slug.isBlank()) {
            respuestaError.escribir(response, HttpStatus.BAD_REQUEST,
                    "GIMNASIO_REQUERIDO", "Indica el slug del gimnasio.", request);
            return null;
        }
        UUID gimnasioId = tenantLookupService.buscarGimnasioActivoPorSlug(slug).orElse(null);
        if (gimnasioId == null) {
            respuestaError.escribir(response, HttpStatus.NOT_FOUND,
                    "GIMNASIO_NO_ENCONTRADO", "No se encontró un gimnasio activo con ese slug.", request);
            return null;
        }
        TenantContext.setTenant(gimnasioId);
        return scopedRequest;
    }

    private String tokenDe(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(PREFIJO_BEARER)) {
            String bearer = authHeader.substring(PREFIJO_BEARER.length()).trim();
            return bearer.isEmpty() ? null : bearer;
        }
        return cookieSesion.leer(request);
    }

    private String slugEnBody(CachedBodyRequest request) throws IOException {
        try {
            JsonNode body = objectMapper.readTree(request.body());
            JsonNode slug = body == null ? null : body.get("gimnasioSlug");
            return slug == null || slug.isNull() ? null : slug.asText();
        } catch (JacksonException e) {
            return null;
        }
    }

    private static boolean esRegistroGimnasio(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && "/api/gimnasios/registro".equals(request.getRequestURI());
    }

    private static boolean esLogin(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && "/api/auth/login".equals(request.getRequestURI());
    }

    private static boolean usaSlugParaAutenticacionPublica(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.POST.matches(request.getMethod())
                && ("/api/auth/google".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/auth/forgot-password".equals(path)
                || "/api/auth/reset-password".equals(path));
    }

    private static boolean requiereSlugPublico(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (HttpMethod.GET.matches(request.getMethod())
                && ("/api/configuracion".equals(path) || "/api/sucursales/disponibles".equals(path))) {
            return true;
        }
        return HttpMethod.POST.matches(request.getMethod())
                && ("/api/auth/google".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/auth/forgot-password".equals(path)
                || "/api/auth/reset-password".equals(path));
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        private byte[] body() {
            return body.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    if (readListener == null) {
                        throw new IllegalArgumentException("ReadListener no puede ser nulo.");
                    }
                    try {
                        if (isFinished()) {
                            readListener.onAllDataRead();
                        } else {
                            readListener.onDataAvailable();
                        }
                    } catch (IOException e) {
                        readListener.onError(e);
                    }
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
