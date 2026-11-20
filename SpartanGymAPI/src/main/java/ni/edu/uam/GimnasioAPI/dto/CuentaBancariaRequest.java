package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

@Data
public class CuentaBancariaRequest {
    private String banco;
    private String tipoCuenta;
    private String numeroCuenta;
    private String titular;
    private String moneda;
    private Boolean activa;
}
