package ni.edu.uam.GimnasioAPI.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Normalización para buscar sin depender de tildes, mayúsculas ni plurales simples. */
public final class BusquedaTexto {

    private BusquedaTexto() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Palabras de la consulta, en singular aproximado ("mancuernas" -> "mancuerna"). */
    public static List<String> terminos(String consulta) {
        String normalizada = normalizar(consulta);
        if (normalizada.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(normalizada.split(" "))
                .map(BusquedaTexto::singular)
                .filter(t -> !t.isEmpty())
                .toList();
    }

    /** True si cada término aparece en el texto (ya normalizado). */
    public static boolean contieneTodos(String textoNormalizado, List<String> terminos) {
        return !terminos.isEmpty() && terminos.stream().allMatch(textoNormalizado::contains);
    }

    private static String singular(String palabra) {
        if (palabra.length() > 4 && palabra.endsWith("es") && !palabra.endsWith("ses")) {
            return palabra.substring(0, palabra.length() - 2);
        }
        if (palabra.length() > 3 && palabra.endsWith("s")) {
            return palabra.substring(0, palabra.length() - 1);
        }
        return palabra;
    }
}
