package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class MovimientoFinancieroRequest {
    private String concepto;
    private BigDecimal monto;
    private String tipo;
    private String metodo;
    private String categoria;
    private UUID sucursalId;
}
