package ni.edu.uam.GimnasioAPI.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

/**
 * Lo que un pedido pendiente apartó del inventario, fila por fila (producto o insumo) y en la
 * cantidad exacta. Al cancelar o vencer el pedido se devuelve ESTO, no lo que diga la receta en
 * ese momento: si la receta cambió entre medio, devolver la receta nueva creaba stock que nunca
 * se había apartado y dejaba descontado el insumo original.
 *
 * El producto se guarda como id suelto (sin relación JPA) para que cargar el pedido no meta las
 * filas de inventario en la sesión antes de bloquearlas.
 */
@Data
@Entity
@Table(name = "reservas_ventas_productos")
public class VentaProductoReserva {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VentaProducto venta;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(nullable = false)
    private Integer cantidad;
}
