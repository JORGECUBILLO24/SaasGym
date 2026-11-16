package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MarcarEjercicioRequest {
    private Boolean completado;
    private LocalDate fecha;
}
