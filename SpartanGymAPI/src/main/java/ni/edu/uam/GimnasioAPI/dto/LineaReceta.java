package ni.edu.uam.GimnasioAPI.dto;

import java.util.UUID;

/** Una línea de receta leída como valores sueltos, sin cargar entidades. */
public record LineaReceta(UUID productoId, UUID insumoId, Integer cantidad) {
}
