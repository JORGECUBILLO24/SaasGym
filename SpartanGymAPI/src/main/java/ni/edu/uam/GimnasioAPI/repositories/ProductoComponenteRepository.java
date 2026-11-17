package ni.edu.uam.GimnasioAPI.repositories;

import ni.edu.uam.GimnasioAPI.dto.LineaReceta;
import ni.edu.uam.GimnasioAPI.models.ProductoComponente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductoComponenteRepository extends JpaRepository<ProductoComponente, UUID> {

    boolean existsByInsumoId(UUID insumoId);

    @Query("select c.producto.nombre from ProductoComponente c where c.insumo.id = :insumoId")
    List<String> nombresQueUsan(@Param("insumoId") UUID insumoId);

    /**
     * Recetas como valores sueltos (producto, insumo, cantidad). No carga entidades a propósito:
     * la venta necesita saber qué insumos bloquear ANTES de leer su stock con SELECT ... FOR UPDATE;
     * si el insumo ya estuviera en el contexto, el bloqueo devolvería el stock leído sin bloqueo.
     */
    @Query("select new ni.edu.uam.GimnasioAPI.dto.LineaReceta("
            + "c.producto.id, c.insumo.id, c.cantidad) from ProductoComponente c where c.producto.id in :ids")
    List<LineaReceta> recetasDe(@Param("ids") Collection<UUID> ids);
}
