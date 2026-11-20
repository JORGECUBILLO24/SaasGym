package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.VentaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

public interface VentaProductoRepository extends JpaRepository<VentaProducto, UUID> {
    List<VentaProducto> findAllByOrderByFechaCreacionDesc();

    java.util.Optional<VentaProducto> findByCodigoPedidoIgnoreCase(String codigoPedido);

    boolean existsByCodigoPedido(String codigoPedido);

    List<VentaProducto> findBySocio_UsuarioIdAndCodigoPedidoIsNotNullOrderByFechaCreacionDesc(UUID socioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VentaProducto v where lower(v.codigoPedido) = lower(:codigo)")
    java.util.Optional<VentaProducto> findByCodigoPedidoForUpdate(@Param("codigo") String codigo);
}
