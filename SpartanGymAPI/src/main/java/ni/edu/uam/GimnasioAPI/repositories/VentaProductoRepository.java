package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.VentaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface VentaProductoRepository extends JpaRepository<VentaProducto, UUID> {
    List<VentaProducto> findAllByOrderByFechaCreacionDesc();

    List<VentaProducto> findBySucursal_IdOrderByFechaCreacionDesc(UUID sucursalId);

    java.util.Optional<VentaProducto> findByCodigoPedidoIgnoreCase(String codigoPedido);

    boolean existsByCodigoPedido(String codigoPedido);

    List<VentaProducto> findBySocio_UsuarioIdAndCodigoPedidoIsNotNullOrderByFechaCreacionDesc(UUID socioId);

    long countBySocio_UsuarioIdAndEstadoAndCodigoPedidoIsNotNull(UUID socioId, String estado);

    /**
     * Pedidos de la app que siguen pendientes y ya vencieron. Los creados antes de que existiera
     * fecha_expiracion se dan por vencidos cuando su creación es anterior a limiteSinFecha.
     */
    @Query("select v.id from VentaProducto v where v.estado = 'Pendiente' and v.codigoPedido is not null "
            + "and ((v.fechaExpiracion is not null and v.fechaExpiracion < :ahora) "
            + "or (v.fechaExpiracion is null and v.fechaCreacion < :limiteSinFecha))")
    List<UUID> idsPedidosVencidos(@Param("ahora") LocalDateTime ahora,
                                  @Param("limiteSinFecha") LocalDateTime limiteSinFecha);
}
