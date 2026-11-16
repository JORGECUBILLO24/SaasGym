package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ControlBiometricoRequest {
    private UUID idSocio;
    private Double pesoKg;
    private String medidasNotas;
}