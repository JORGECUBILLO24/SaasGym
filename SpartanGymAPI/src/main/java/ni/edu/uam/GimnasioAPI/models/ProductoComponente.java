package ni.edu.uam.GimnasioAPI.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Una línea de la receta: cuánto de un insumo consume una unidad del producto preparado. */
@Getter
@Setter
@Entity
@Table(name = "producto_componentes")
public class ProductoComponente {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "gimnasio_id", nullable = false, updatable = false)
    private java.util.UUID gimnasioId;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private ProductoInventario producto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "insumo_id")
    @JsonIgnoreProperties({"componentes", "imagenUrl", "sucursal", "descripcion", "fechaCreacion",
            "hibernateLazyInitializer", "handler"})
    private ProductoInventario insumo;

    @Column(nullable = false)
    private Integer cantidad;
}
