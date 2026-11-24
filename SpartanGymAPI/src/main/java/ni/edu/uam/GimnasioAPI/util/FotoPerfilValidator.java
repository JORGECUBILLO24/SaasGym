package ni.edu.uam.GimnasioAPI.util;

import ni.edu.uam.GimnasioAPI.exceptions.ReglaNegocioException;

import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
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
    private static final int DIMENSION_MAXIMA = 4096;

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
        if (!imagenCompletaYDimensionValida(tipo, bytes)) {
            throw new ReglaNegocioException("La imagen está dañada, incompleta o supera 4096 píxeles por lado.");
        }
        return fotoUrl;
    }

    private static boolean imagenCompletaYDimensionValida(String tipo, byte[] bytes) {
        if ("webp".equals(tipo)) {
            // ImageIO estándar no incluye lector WEBP. Verificamos longitud RIFF y dimensiones
            // de los contenedores VP8X/VP8/VP8L antes de aceptar el archivo.
            if (bytes.length < 30 || enteroLE(bytes, 4) != bytes.length - 8L) return false;
            long tamanoChunk = enteroLE(bytes, 16);
            if (tamanoChunk < 10 || 20L + tamanoChunk + (tamanoChunk % 2) > bytes.length) return false;
            int ancho, alto;
            if (empiezaCon(bytes, 12, 'V', 'P', '8', 'X') && bytes.length >= 30) {
                ancho = 1 + entero24LE(bytes, 24);
                alto = 1 + entero24LE(bytes, 27);
            } else if (empiezaCon(bytes, 12, 'V', 'P', '8', 'L') && bytes.length >= 25
                    && (bytes[20] & 0xff) == 0x2f) {
                ancho = 1 + ((bytes[21] & 0xff) | ((bytes[22] & 0x3f) << 8));
                alto = 1 + (((bytes[22] & 0xc0) >> 6) | ((bytes[23] & 0xff) << 2)
                        | ((bytes[24] & 0x0f) << 10));
            } else if (empiezaCon(bytes, 12, 'V', 'P', '8', ' ') && bytes.length >= 30
                    && empiezaCon(bytes, 23, 0x9d, 0x01, 0x2a)) {
                ancho = (bytes[26] & 0xff | (bytes[27] & 0x3f) << 8);
                alto = (bytes[28] & 0xff | (bytes[29] & 0x3f) << 8);
            } else return false;
            return ancho > 0 && alto > 0 && ancho <= DIMENSION_MAXIMA && alto <= DIMENSION_MAXIMA;
        }
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(bytes));
            return imagen != null && imagen.getWidth() > 0 && imagen.getHeight() > 0
                    && imagen.getWidth() <= DIMENSION_MAXIMA && imagen.getHeight() <= DIMENSION_MAXIMA;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static long enteroLE(byte[] bytes, int posicion) {
        return (bytes[posicion] & 0xffL) | ((bytes[posicion + 1] & 0xffL) << 8)
                | ((bytes[posicion + 2] & 0xffL) << 16) | ((bytes[posicion + 3] & 0xffL) << 24);
    }

    private static int entero24LE(byte[] bytes, int posicion) {
        return (bytes[posicion] & 0xff) | ((bytes[posicion + 1] & 0xff) << 8)
                | ((bytes[posicion + 2] & 0xff) << 16);
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
