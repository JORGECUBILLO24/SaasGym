package ni.edu.uam.GimnasioAPI.util;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FotoPerfilValidatorTest {

    // Cabeceras solas no son imágenes válidas.
    private static final String WEBP = "data:image/webp;base64,UklGRhoAAABXRUJQVlA4IA==";
    private static final String PNG = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==";
    private static final String JPEG = "data:image/jpeg;base64,/9j/4AAQSkZJRg==";

    @Test
    void validar_aceptaPngYJpegCompletos() throws Exception {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        for (String formato : new String[]{"png", "jpeg"}) {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, formato, salida);
            String url = "data:image/" + formato + ";base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
            assertEquals(url, FotoPerfilValidator.validar(url));
        }
    }

    @Test
    void validar_rechazaFirmasConCuerpoIncompleto() {
        assertThrows(RuntimeException.class, () -> FotoPerfilValidator.validar(PNG));
        assertThrows(RuntimeException.class, () -> FotoPerfilValidator.validar(JPEG));
        assertThrows(RuntimeException.class, () -> FotoPerfilValidator.validar(WEBP));
    }

    @Test
    void validar_nullDevuelveNull() {
        assertNull(FotoPerfilValidator.validar(null));
    }

    @Test
    void validar_vacioODeEspaciosDevuelveNull() {
        assertNull(FotoPerfilValidator.validar(""));
        assertNull(FotoPerfilValidator.validar("   "));
    }

    @Test
    void validar_rechazaUrlExternaQueNoEsDataUrl() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> FotoPerfilValidator.validar("https://ejemplo.com/foto.png"));
        assertEquals("La foto debe ser una imagen válida.", ex.getMessage());
    }

    @Test
    void validar_rechazaImagenDemasiadoGrande() {
        String grande = "data:image/webp;base64," + "A".repeat(700_001);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> FotoPerfilValidator.validar(grande));
        assertEquals("La imagen es demasiado grande.", ex.getMessage());
    }

    @Test
    void validar_rechazaSvg() {
        String svg = "data:image/svg+xml;base64,PHN2ZyBvbmxvYWQ9YWxlcnQoMSk+PC9zdmc+";
        assertThrows(RuntimeException.class, () -> FotoPerfilValidator.validar(svg));
    }

    @Test
    void validar_rechazaTextoQueNoEsImagen() {
        // "not-an-image" en base64 con tipo png: el contenido no tiene la firma PNG.
        String falso = "data:image/png;base64,bm90LWFuLWltYWdl";
        assertThrows(RuntimeException.class, () -> FotoPerfilValidator.validar(falso));
    }

    @Test
    void validar_rechazaBase64Invalido() {
        assertThrows(RuntimeException.class,
                () -> FotoPerfilValidator.validar("data:image/png;base64,not-an-image"));
    }

    @Test
    void validar_rechazaTipoDeclaradoQueNoCoincide() {
        // Bytes de PNG declarados como JPEG.
        assertThrows(RuntimeException.class,
                () -> FotoPerfilValidator.validar("data:image/jpeg;base64,iVBORw0KGgoAAAANSUhEUg=="));
    }
}
