package ni.edu.uam.GimnasioAPI.dto;

import java.util.Set;

public record ProgresoSemana(int planificados, int completados, double progreso, Set<Long> ejerciciosCompletadosIds) {
}
