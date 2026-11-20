package ni.edu.uam.GimnasioAPI.security;

import jakarta.servlet.http.HttpServletRequest;
import ni.edu.uam.GimnasioAPI.exceptions.DemasiadosIntentosException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de intentos por ventana deslizante, en memoria (la API corre en una sola instancia).
 * Protege login, registro, login con Google y recuperación de contraseña de la fuerza bruta y
 * del envío masivo de correos.
 *
 * Nunca bloquea una cuenta para siempre: solo hace esperar unos minutos, así un atacante no
 * puede dejar fuera a un socio a propósito por más que eso.
 *
 * IP del cliente: la API está detrás del proxy de Render (y el web pasa además por Vercel). Se
 * toma la ÚLTIMA dirección de X-Forwarded-For, que es la que agrega el proxy de Render y no la
 * puede inventar el cliente. Los usuarios del web comparten la IP de salida de Vercel, por eso
 * el límite por IP es amplio y la protección fina va por correo.
 */
@Component
public class LimitadorIntentos {

    public record Regla(int maximo, long ventanaSegundos) {}

    private final Map<String, Deque<Long>> intentos = new ConcurrentHashMap<>();
    private final Clock reloj;

    @Value("${app.rate-limit.habilitado:true}")
    private boolean habilitado = true;

    private long ultimaLimpieza;

    public LimitadorIntentos() {
        this(Clock.systemUTC());
    }

    LimitadorIntentos(Clock reloj) {
        this.reloj = reloj;
    }

    void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    /** Cuenta un intento y falla si con él se pasa del máximo en la ventana. */
    public void registrar(String clave, Regla regla) {
        if (!habilitado || clave == null) {
            return;
        }
        long ahora = reloj.millis();
        limpiarDeVezEnCuando(ahora);
        Deque<Long> marcas = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (marcas) {
            descartarViejas(marcas, ahora, regla.ventanaSegundos());
            if (marcas.size() >= regla.maximo()) {
                long esperaMs = marcas.peekFirst() + regla.ventanaSegundos() * 1000 - ahora;
                throw new DemasiadosIntentosException((esperaMs + 999) / 1000);
            }
            marcas.addLast(ahora);
        }
    }

    /** Falla si la clave ya agotó sus intentos, sin contar uno nuevo. */
    public void verificar(String clave, Regla regla) {
        if (!habilitado || clave == null) {
            return;
        }
        Deque<Long> marcas = intentos.get(clave);
        if (marcas == null) {
            return;
        }
        long ahora = reloj.millis();
        synchronized (marcas) {
            descartarViejas(marcas, ahora, regla.ventanaSegundos());
            if (marcas.size() >= regla.maximo()) {
                long esperaMs = marcas.peekFirst() + regla.ventanaSegundos() * 1000 - ahora;
                throw new DemasiadosIntentosException((esperaMs + 999) / 1000);
            }
        }
    }

    /** Un login correcto borra los fallos acumulados de esa cuenta. */
    public void reiniciar(String clave) {
        if (clave != null) {
            intentos.remove(clave);
        }
    }

    public static String ipCliente(HttpServletRequest request) {
        if (request == null) {
            return "desconocida";
        }
        String reenviado = request.getHeader("X-Forwarded-For");
        if (reenviado != null && !reenviado.isBlank()) {
            String[] saltos = reenviado.split(",");
            String ultimo = saltos[saltos.length - 1].trim();
            if (!ultimo.isEmpty()) {
                return ultimo;
            }
        }
        return request.getRemoteAddr() == null ? "desconocida" : request.getRemoteAddr();
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static void descartarViejas(Deque<Long> marcas, long ahora, long ventanaSegundos) {
        long limite = ahora - ventanaSegundos * 1000;
        while (!marcas.isEmpty() && marcas.peekFirst() <= limite) {
            marcas.pollFirst();
        }
    }

    // Evita que el mapa crezca sin límite con claves que ya no se usan.
    private void limpiarDeVezEnCuando(long ahora) {
        if (ahora - ultimaLimpieza < 10 * 60_000L) {
            return;
        }
        ultimaLimpieza = ahora;
        long limite = ahora - 2 * 60 * 60_000L;
        intentos.entrySet().removeIf(entrada -> {
            Deque<Long> marcas = entrada.getValue();
            synchronized (marcas) {
                return marcas.isEmpty() || marcas.peekLast() < limite;
            }
        });
    }
}
