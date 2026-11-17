package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.models.VentaProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VentaProductoRepository extends JpaRepository<VentaProducto, UUID> {
    List<VentaProducto> findAllByOrderByFechaCreacionDesc();

    java.util.Optional<VentaProducto> findByCodigoPedidoIgnoreCase(String codigoPedido);

    boolean existsByCodigoPedido(String codigoPedido);

    List<VentaProducto> findBySocio_UsuarioIdAndCodigoPedidoIsNotNullOrderByFechaCreacionDesc(UUID socioId);
}
