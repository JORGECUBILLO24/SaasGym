package ni.edu.uam.GimnasioAPI.util;

import ni.edu.uam.GimnasioAPI.exceptions.ReglaNegocioException;

import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validación pura y aislada de las imágenes que llegan como data URL base64 (foto de perfil,
 * portadas de rutinas y membresías, productos, comprobantes), testeable sin la capa web ni la
 * base de datos. Los errores son ReglaNegocioException, que GlobalExceptionHandler traduce a 400.
 *
 * Antes solo se miraba el prefijo "data:image/" y el largo: pasaban SVG (que puede llevar
 * scripts) y cualquier texto que no fuera una imagen. Ahora solo se aceptan PNG, JPEG, WEBP y
 * GIF, el base64 se decodifica de verdad y los primeros bytes tienen que coincidir con el tipo
 * declarado.
 */
public final class FotoPerfilValidator {

    private static final int LONGITUD_MAXIMA = 700_000;

    private static final Pattern DATA_URL = Pattern.compile(
            "^data:image/(png|jpeg|jpg|webp|gif);base64,([A-Za-z0-9+/]+={0,2})$");

    private static final Map<String, String> TIPO_NORMALIZADO = Map.of(
            "png", "png", "jpeg", "jpeg", "jpg", "jpeg", "webp", "webp", "gif", "gif");

    private FotoPerfilValidator() {
    }

    public static String validar(String fotoUrl) {
        if (fotoUrl == null || fotoUrl.isBlank()) {
            return null;
        }
        if (!fotoUrl.startsWith("data:image/")) {
            throw new ReglaNegocioException("La foto debe ser una imagen válida.");
        }
        if (fotoUrl.length() > LONGITUD_MAXIMA) {
            throw new ReglaNegocioException("La imagen es demasiado grande.");
        }

        Matcher partes = DATA_URL.matcher(fotoUrl);
        if (!partes.matches()) {
            throw new ReglaNegocioException("Formato de imagen no permitido. Usa PNG, JPG, WEBP o GIF.");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(partes.group(2));
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("La imagen está dañada o incompleta.");
        }

        String tipo = TIPO_NORMALIZADO.get(partes.group(1).toLowerCase(Locale.ROOT));
        if (!coincideFirma(tipo, bytes)) {
            throw new ReglaNegocioException("El archivo no es una imagen " + tipo.toUpperCase(Locale.ROOT) + " válida.");
        }
        return fotoUrl;
    }

    /** Compara los primeros bytes del archivo con la firma del formato declarado. */
    static boolean coincideFirma(String tipo, byte[] b) {
        return switch (tipo) {
            case "png" -> empiezaCon(b, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "jpeg" -> empiezaCon(b, 0, 0xFF, 0xD8, 0xFF);
            case "gif" -> empiezaCon(b, 0, 'G', 'I', 'F', '8');
            case "webp" -> empiezaCon(b, 0, 'R', 'I', 'F', 'F') && empiezaCon(b, 8, 'W', 'E', 'B', 'P');
            default -> false;
        };
    }

    private static boolean empiezaCon(byte[] datos, int desde, int... firma) {
        if (datos.length < desde + firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((datos[desde + i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
