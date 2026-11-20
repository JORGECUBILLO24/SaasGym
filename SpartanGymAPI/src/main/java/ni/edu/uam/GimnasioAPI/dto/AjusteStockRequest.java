package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

@Data
public class AjusteStockRequest {
    /** Unidades a sumar (positivo) o restar (negativo). */
    private Integer cantidad;
    private String motivo;
}
