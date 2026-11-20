package ni.edu.uam.GimnasioAPI.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FotoPerfilValidatorTest {

    // Cabeceras reales (los primeros bytes) de cada formato, en base64.
    private static final String WEBP = "data:image/webp;base64,UklGRhoAAABXRUJQVlA4IA==";
    private static final String PNG = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==";
    private static final String JPEG = "data:image/jpeg;base64,/9j/4AAQSkZJRg==";

    @Test
    void validar_aceptaDataUrlDeImagen() {
        assertEquals(WEBP, FotoPerfilValidator.validar(WEBP));
        assertEquals(PNG, FotoPerfilValidator.validar(PNG));
        assertEquals(JPEG, FotoPerfilValidator.validar(JPEG));
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
