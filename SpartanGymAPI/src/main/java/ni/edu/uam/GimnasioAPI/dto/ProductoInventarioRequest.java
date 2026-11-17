package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductoInventarioRequest {
    private String nombre;
    private String categoria;
    private BigDecimal precio;
    private Integer stock;
    private String imagenUrl;
    private UUID sucursalId;
    private String descripcion;
    /** Unidad en la que se cuenta el stock: unidad, g, ml, servicio... */
    private String unidad;
    /** false para insumos que solo se usan en recetas (leche, proteína a granel). */
    private Boolean vendible;
    /** Receta: si trae líneas, el producto se prepara con esos insumos y no lleva stock propio. */
    private List<ProductoComponenteRequest> componentes;
}
