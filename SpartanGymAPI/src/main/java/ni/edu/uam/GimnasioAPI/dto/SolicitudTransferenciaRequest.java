package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

import java.util.UUID;

/** Lo que manda el socio al reportar su transferencia. */
@Data
public class SolicitudTransferenciaRequest {
    private Integer tipoMembresiaId;
    private UUID cuentaBancariaId;
    private String referencia;
    /** Opcional: foto del comprobante como data URL (PNG, JPG o WEBP). */
    private String comprobanteUrl;
    /** Solo para responder un mensaje claro a versiones viejas de la app que mandan "Efectivo". */
    private String metodoPago;
}
