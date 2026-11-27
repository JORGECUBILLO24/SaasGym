package ni.edu.uam.GimnasioAPI.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "productos_inventario")
public class ProductoInventario {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "gimnasio_id", nullable = false, updatable = false)
    private java.util.UUID gimnasioId;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String unidad = "unidad";

    @Column(nullable = false)
    private Boolean vendible = true;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @BatchSize(size = 100)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductoComponente> componentes = new ArrayList<>();

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** Se prepara con insumos (batido, café...) en vez de tener stock propio. */
    public boolean isCompuesto() {
        return componentes != null && !componentes.isEmpty();
    }

    /**
     * Unidades que se pueden vender ahora. En un producto preparado es lo que alcanza con el
     * insumo más escaso: con 900 g de proteína y 30 g por batido salen 30 batidos.
     */
    public int getDisponible() {
        if (!isCompuesto()) {
            return stock == null ? 0 : stock;
        }
        int disponible = Integer.MAX_VALUE;
        for (ProductoComponente componente : componentes) {
            int existencia = componente.getInsumo().getStock() == null ? 0 : componente.getInsumo().getStock();
            disponible = Math.min(disponible, existencia / Math.max(1, componente.getCantidad()));
        }
        return disponible;
    }
}
