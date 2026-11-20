package ni.edu.uam.GimnasioAPI.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** Bitácora de cada cambio manual de stock: cuánto, por qué y quién lo hizo. */
@Data
@Entity
@Table(name = "ajustes_stock")
public class AjusteStock {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoInventario producto;

    /** Positivo suma unidades, negativo las resta. */
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(nullable = false, length = 150)
    private String usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}
