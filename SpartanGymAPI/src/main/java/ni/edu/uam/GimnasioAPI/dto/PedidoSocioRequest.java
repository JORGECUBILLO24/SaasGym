package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

import java.util.List;

/** Pedido que arma el socio en la app: paga en recepción en efectivo o por transferencia. */
@Data
public class PedidoSocioRequest {
    private String metodoPago;
    private String referenciaPago;
    private List<VentaProductoDetalleRequest> detalles;
}
