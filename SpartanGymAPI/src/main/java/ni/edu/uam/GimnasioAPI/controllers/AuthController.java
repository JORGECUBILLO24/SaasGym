package ni.edu.uam.GimnasioAPI.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.*;
import ni.edu.uam.GimnasioAPI.exceptions.DemasiadosIntentosException;
import ni.edu.uam.GimnasioAPI.security.CookieSesion;
import ni.edu.uam.GimnasioAPI.security.LimitadorIntentos;
import ni.edu.uam.GimnasioAPI.security.LimitadorIntentos.Regla;
import ni.edu.uam.GimnasioAPI.services.AuthService;
import ni.edu.uam.GimnasioAPI.services.GoogleLoginService;
import ni.edu.uam.GimnasioAPI.services.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // Fallos de contraseña por cuenta: 8 en 15 minutos y hay que esperar (nunca un bloqueo fijo).
    static final Regla LOGIN_CUENTA = new Regla(8, 15 * 60);
    // Por IP es amplio: todos los usuarios del web salen por las mismas IPs de Vercel.
    static final Regla LOGIN_IP = new Regla(300, 5 * 60);
    static final Regla GOOGLE_IP = new Regla(60, 5 * 60);
    static final Regla REGISTRO_IP = new Regla(20, 60 * 60);
    static final Regla RECUPERAR_CUENTA = new Regla(3, 15 * 60);
    static final Regla RECUPERAR_IP = new Regla(30, 15 * 60);
    static final Regla RESTABLECER_IP = new Regla(30, 15 * 60);

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final GoogleLoginService googleLoginService;
    private final LimitadorIntentos limitador;
    private final CookieSesion cookieSesion;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request,
                                              HttpServletRequest http, HttpServletResponse respuesta) {
        String claveCuenta = "login:" + LimitadorIntentos.normalizarEmail(request.getEmail());
        limitador.registrar("login-ip:" + LimitadorIntentos.ipCliente(http), LOGIN_IP);
        limitador.verificar(claveCuenta, LOGIN_CUENTA);

        AuthResponse sesion;
        try {
            sesion = authService.login(request);
        } catch (AuthenticationException e) {
            contarFallo(claveCuenta, LOGIN_CUENTA);
            throw e;
        }
        limitador.reiniciar(claveCuenta);
        return ResponseEntity.ok(entregarSesion(sesion, http, respuesta));
    }

    public record GoogleCredential(String credential) {}

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@RequestBody GoogleCredential request,
                                               HttpServletRequest http, HttpServletResponse respuesta) {
        limitador.registrar("google-ip:" + LimitadorIntentos.ipCliente(http), GOOGLE_IP);
        return ResponseEntity.ok(entregarSesion(googleLoginService.login(request.credential()), http, respuesta));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request,
                                                 HttpServletRequest http, HttpServletResponse respuesta) {
        limitador.registrar("registro-ip:" + LimitadorIntentos.ipCliente(http), REGISTRO_IP);
        return ResponseEntity.ok(entregarSesion(authService.register(request), http, respuesta));
    }

    /** Cierra la sesión del panel web: borra la cookie. La app solo descarta su token. */
    @PostMapping("/logout")
    public ResponseEntity<MensajeResponse> logout(HttpServletResponse respuesta) {
        cookieSesion.borrar(respuesta);
        return ResponseEntity.ok(new MensajeResponse("Sesión cerrada."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MensajeResponse> solicitarRestablecimiento(@Valid @RequestBody SolicitarRestablecimientoRequest request,
                                                                     HttpServletRequest http) {
        // El límite se aplica igual exista o no el correo, para no revelar cuáles están registrados.
        limitador.registrar("recuperar-ip:" + LimitadorIntentos.ipCliente(http), RECUPERAR_IP);
        limitador.registrar("recuperar:" + LimitadorIntentos.normalizarEmail(request.getEmail()), RECUPERAR_CUENTA);
        passwordResetService.solicitarRestablecimiento(request.getEmail());
        return ResponseEntity.ok(new MensajeResponse("Si el correo esta registrado, enviaremos un enlace seguro para restablecer la contraseña."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MensajeResponse> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request,
                                                               HttpServletRequest http) {
        limitador.registrar("restablecer-ip:" + LimitadorIntentos.ipCliente(http), RESTABLECER_IP);
        passwordResetService.restablecerPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok(new MensajeResponse("Contraseña actualizada correctamente."));
    }

    /**
     * El panel web recibe la sesión en una cookie HttpOnly y el token NO va en el cuerpo, para
     * que ningún script de la página pueda leerlo. La app Android sigue recibiéndolo en el cuerpo.
     */
    private AuthResponse entregarSesion(AuthResponse sesion, HttpServletRequest http, HttpServletResponse respuesta) {
        if (!CookieSesion.esClienteWeb(http) || sesion.getToken() == null) {
            return sesion;
        }
        cookieSesion.escribir(respuesta, sesion.getToken());
        return AuthResponse.builder()
                .id(sesion.getId())
                .email(sesion.getEmail())
                .rol(sesion.getRol())
                .build();
    }

    private void contarFallo(String clave, Regla regla) {
        try {
            limitador.registrar(clave, regla);
        } catch (DemasiadosIntentosException ignorada) {
            // Ya estaba al límite: el próximo intento recibirá el 429.
        }
    }
}
