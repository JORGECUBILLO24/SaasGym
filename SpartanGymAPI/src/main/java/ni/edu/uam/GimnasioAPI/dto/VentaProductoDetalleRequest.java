package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class VentaProductoDetalleRequest {
    private UUID productoId;
    private Integer cantidad;
}
