package ni.edu.uam.GimnasioAPI.dto;

import lombok.Builder;
import lombok.Data;
import ni.edu.uam.GimnasioAPI.models.SolicitudPagoMembresia;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SolicitudPagoResponse {
    private UUID id;
    private UUID socioId;
    private String socio;
    private String sucursal;
    private Integer tipoMembresiaId;
    private String tipoMembresia;
    private BigDecimal monto;
    private UUID cuentaBancariaId;
    private String banco;
    private String tipoCuenta;
    private String numeroCuenta;
    private String moneda;
    private String referencia;
    private String comprobanteUrl;
    private String estado;
    private String motivoRechazo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaRevision;
    private String revisadoPor;
    private String numeroFactura;

    public static SolicitudPagoResponse desde(SolicitudPagoMembresia s, boolean conComprobante) {
        return SolicitudPagoResponse.builder()
                .id(s.getId())
                .socioId(s.getSocio().getUsuarioId())
                .socio(s.getSocio().getNombres() + " " + s.getSocio().getApellidos())
                .sucursal(s.getSocio().getSucursal() != null ? s.getSocio().getSucursal().getNombre() : null)
                .tipoMembresiaId(s.getTipoMembresia().getId())
                .tipoMembresia(s.getTipoMembresia().getNombre())
                .monto(s.getMonto())
                .cuentaBancariaId(s.getCuentaBancaria().getId())
                .banco(s.getCuentaBancaria().getBanco())
                .tipoCuenta(s.getCuentaBancaria().getTipoCuenta())
                .numeroCuenta(s.getCuentaBancaria().getNumeroCuenta())
                .moneda(s.getCuentaBancaria().getMoneda())
                .referencia(s.getReferencia())
                .comprobanteUrl(conComprobante ? s.getComprobanteUrl() : null)
                .estado(s.getEstado())
                .motivoRechazo(s.getMotivoRechazo())
                .fechaCreacion(s.getFechaCreacion())
                .fechaRevision(s.getFechaRevision())
                .revisadoPor(s.getRevisadoPor())
                .numeroFactura(s.getPago() != null ? s.getPago().getNumeroFactura() : null)
                .build();
    }
}
