package ni.edu.uam.GimnasioAPI.exceptions;

import org.springframework.http.HttpStatus;

/** Se superó el límite de intentos (login, registro, recuperación). El cliente debe esperar. */
public class DemasiadosIntentosException extends ApiException {

    private final long segundosEspera;

    public DemasiadosIntentosException(long segundosEspera) {
        super(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS",
                "Demasiados intentos. Espera " + describir(segundosEspera) + " e inténtalo de nuevo.");
        this.segundosEspera = Math.max(1, segundosEspera);
    }

    public long getSegundosEspera() {
        return segundosEspera;
    }

    private static String describir(long segundos) {
        if (segundos <= 60) {
            return "un minuto";
        }
        long minutos = (segundos + 59) / 60;
        return minutos + " minutos";
    }
}
