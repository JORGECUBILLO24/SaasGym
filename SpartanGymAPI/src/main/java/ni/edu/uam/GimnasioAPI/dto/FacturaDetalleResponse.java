package ni.edu.uam.GimnasioAPI.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class FacturaDetalleResponse {
    private UUID id;
    private UUID productoId;
    private String productoNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal totalLinea;
}
