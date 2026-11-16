package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

@Data
public class ActualizarFotoRequest {
    private String fotoUrl; // null o "" = quitar la foto
}
