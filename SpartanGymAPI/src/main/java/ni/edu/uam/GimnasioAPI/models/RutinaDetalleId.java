package ni.edu.uam.GimnasioAPI.models;

import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class RutinaDetalleId implements Serializable {
    private UUID rutina;
    private Long ejercicio;
}