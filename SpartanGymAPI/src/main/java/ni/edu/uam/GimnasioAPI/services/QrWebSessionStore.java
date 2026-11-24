package ni.edu.uam.GimnasioAPI.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Shared abstraction for temporary web QR sessions (not the attendance ledger). */
public interface QrWebSessionStore {
    /**
     * Guarda la sesion del QR de recepcion. {@code creador} es el id del usuario del personal que
     * lo genero: solo esa persona puede consultar despues quien lo escaneo.
     */
    void guardar(UUID sessionId, String tokenHash, Instant expira, String creador);

    /** Sesion sin creador conocido (pruebas y datos anteriores a este cambio). */
    default void guardar(UUID sessionId, String tokenHash, Instant expira) {
        guardar(sessionId, tokenHash, expira, null);
    }

    Optional<Session> buscar(UUID sessionId);

    record Session(String tokenHash, Instant expira, String creador) {
        public Session(String tokenHash, Instant expira) {
            this(tokenHash, expira, null);
        }

        public boolean expirado() {
            return Instant.now().isAfter(expira);
        }
    }
}
