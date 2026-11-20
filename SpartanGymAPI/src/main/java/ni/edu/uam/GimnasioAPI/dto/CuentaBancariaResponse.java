package ni.edu.uam.GimnasioAPI.dto;

import lombok.Builder;
import lombok.Data;
import ni.edu.uam.GimnasioAPI.models.CuentaBancaria;

import java.util.UUID;

@Data
@Builder
public class CuentaBancariaResponse {
    private UUID id;
    private String banco;
    private String tipoCuenta;
    private String numeroCuenta;
    private String titular;
    private String moneda;
    private Boolean activa;

    public static CuentaBancariaResponse desde(CuentaBancaria cuenta) {
        return CuentaBancariaResponse.builder()
                .id(cuenta.getId())
                .banco(cuenta.getBanco())
                .tipoCuenta(cuenta.getTipoCuenta())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .titular(cuenta.getTitular())
                .moneda(cuenta.getMoneda())
                .activa(cuenta.getActiva())
                .build();
    }
}
