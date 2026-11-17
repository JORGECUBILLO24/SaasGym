package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

import java.math.BigDecimal;

/** Lo que registra quien valida el pedido: efectivo recibido o referencia de la transferencia. */
@Data
public class ConfirmarPedidoRequest {
    private BigDecimal montoRecibido;
    private String referenciaPago;
}
