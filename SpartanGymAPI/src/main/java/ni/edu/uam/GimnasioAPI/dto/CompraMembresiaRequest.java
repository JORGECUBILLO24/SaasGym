package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

@Data
public class CompraMembresiaRequest {
    private Integer tipoMembresiaId;
    private String metodoPago;
}
