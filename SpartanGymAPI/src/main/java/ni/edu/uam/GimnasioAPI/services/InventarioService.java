package ni.edu.uam.GimnasioAPI.services;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.ProductoComponenteRequest;
import ni.edu.uam.GimnasioAPI.dto.ProductoInventarioRequest;
import ni.edu.uam.GimnasioAPI.exceptions.RecursoNoEncontradoException;
import ni.edu.uam.GimnasioAPI.exceptions.ReglaNegocioException;
import ni.edu.uam.GimnasioAPI.models.AjusteStock;
import ni.edu.uam.GimnasioAPI.models.ProductoComponente;
import ni.edu.uam.GimnasioAPI.models.ProductoInventario;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.repositories.AjusteStockRepository;
import ni.edu.uam.GimnasioAPI.repositories.ProductoComponenteRepository;
import ni.edu.uam.GimnasioAPI.repositories.ProductoInventarioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SucursalRepository;
import ni.edu.uam.GimnasioAPI.util.FotoPerfilValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventarioService {
    private final ProductoInventarioRepository repository;
    private final ProductoComponenteRepository componenteRepository;
    private final SucursalRepository sucursalRepository;
    private final AjusteStockRepository ajusteStockRepository;

    @Transactional
    public ProductoInventario crear(ProductoInventarioRequest request) {
        ProductoInventario producto = new ProductoInventario();
        aplicar(producto, request, true);
        return repository.save(producto);
    }

    /**
     * Edita los datos del producto (nombre, precio, receta...) pero NO su stock: el stock solo
     * cambia por ventas o por un ajuste explicito (ajustarStock). Antes el formulario mandaba
     * el stock que tenia abierto y, si en ese rato se vendio algo, lo volvia a subir.
     * La fila se bloquea igual que en una venta para no pisar un descuento en curso.
     */
    @Transactional
    public ProductoInventario actualizar(UUID id, ProductoInventarioRequest request) {
        ProductoInventario producto = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
        aplicar(producto, request, false);
        return repository.save(producto);
    }

    /**
     * Suma o resta unidades al stock con un motivo (compra a proveedor, merma, conteo...).
     * Trabaja sobre la fila bloqueada, asi que no se pierde ninguna venta simultanea.
     */
    @Transactional
    public ProductoInventario ajustarStock(UUID id, Integer cantidad, String motivo, String usuario) {
        if (cantidad == null || cantidad == 0) {
            throw new ReglaNegocioException("Indica cuántas unidades sumar o restar");
        }
        if (Math.abs((long) cantidad) > 1_000_000) {
            throw new ReglaNegocioException("La cantidad del ajuste es demasiado grande");
        }
        String motivoLimpio = requerido(motivo, "Escribe el motivo del ajuste");
        if (motivoLimpio.length() > 200) {
            throw new ReglaNegocioException("El motivo no debe superar 200 caracteres");
        }
        ProductoInventario producto = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
        if (producto.isCompuesto()) {
            throw new ReglaNegocioException(producto.getNombre()
                    + " se prepara con receta: ajusta el stock de sus insumos");
        }
        long actual = producto.getStock() == null ? 0 : producto.getStock();
        long resultado = actual + cantidad;
        if (resultado < 0) {
            throw new ReglaNegocioException("El ajuste dejaría el stock en negativo: hay " + actual);
        }
        if (resultado > Integer.MAX_VALUE) {
            throw new ReglaNegocioException("El stock resultante es demasiado grande");
        }
        producto.setStock((int) resultado);
        ProductoInventario guardado = repository.save(producto);

        AjusteStock ajuste = new AjusteStock();
        ajuste.setProducto(guardado);
        ajuste.setCantidad(cantidad);
        ajuste.setStockResultante((int) resultado);
        ajuste.setMotivo(motivoLimpio);
        ajuste.setUsuario(usuario == null || usuario.isBlank() ? "Sistema" : usuario);
        ajusteStockRepository.save(ajuste);
        return guardado;
    }

    @Transactional
    public void eliminar(UUID id) {
        List<String> usan = componenteRepository.nombresQueUsan(id);
        if (!usan.isEmpty()) {
            throw new ReglaNegocioException("No se puede eliminar: es insumo de " + String.join(", ", usan)
                    + ". Quítalo primero de esas recetas.");
        }
        repository.deleteById(id);
    }

    private void aplicar(ProductoInventario producto, ProductoInventarioRequest request, boolean nuevo) {
        producto.setNombre(requerido(request.getNombre(), "Escribe el nombre del producto"));
        producto.setCategoria(requerido(request.getCategoria(), "Elige una categoría"));
        if (request.getPrecio() == null || request.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            throw new ReglaNegocioException("El precio no puede ser negativo");
        }
        producto.setPrecio(request.getPrecio());
        producto.setImagenUrl(FotoPerfilValidator.validar(request.getImagenUrl()));
        producto.setSucursal(obtenerSucursal(request.getSucursalId()));
        producto.setDescripcion(limpiar(request.getDescripcion()));
        String unidad = limpiar(request.getUnidad());
        producto.setUnidad(unidad == null ? "unidad" : unidad.toLowerCase(Locale.ROOT));
        producto.setVendible(request.getVendible() == null || request.getVendible());

        aplicarReceta(producto, request.getComponentes() == null ? List.of() : request.getComponentes());

        if (producto.isCompuesto()) {
            // Lo disponible sale de los insumos; un stock propio confundiría al vender.
            producto.setStock(0);
        } else if (nuevo) {
            // El stock inicial se fija al crear; despues solo cambia con ventas o ajustes.
            if (request.getStock() == null || request.getStock() < 0) {
                throw new ReglaNegocioException("El stock no puede ser negativo");
            }
            producto.setStock(request.getStock());
        } else if (producto.getStock() == null) {
            producto.setStock(0);
        }
    }

    private void aplicarReceta(ProductoInventario producto, List<ProductoComponenteRequest> lineas) {
        Map<UUID, Integer> receta = new LinkedHashMap<>();
        for (ProductoComponenteRequest linea : lineas) {
            if (linea == null || linea.getInsumoId() == null) {
                throw new ReglaNegocioException("Elige el insumo de cada línea de la receta");
            }
            if (linea.getCantidad() == null || linea.getCantidad() <= 0) {
                throw new ReglaNegocioException("La cantidad de cada insumo debe ser mayor a cero");
            }
            if (receta.put(linea.getInsumoId(), linea.getCantidad()) != null) {
                throw new ReglaNegocioException("Un insumo aparece dos veces en la receta");
            }
        }

        if (!receta.isEmpty() && producto.getId() != null && componenteRepository.existsByInsumoId(producto.getId())) {
            throw new ReglaNegocioException(producto.getNombre()
                    + " ya es insumo de otro producto; no puede tener receta propia");
        }

        // Se actualiza en el sitio: borrar y volver a insertar el mismo insumo choca con la
        // restricción única, porque Hibernate inserta antes de borrar.
        producto.getComponentes().removeIf(c -> !receta.containsKey(c.getInsumo().getId()));
        Set<UUID> existentes = new HashSet<>();
        for (ProductoComponente componente : producto.getComponentes()) {
            componente.setCantidad(receta.get(componente.getInsumo().getId()));
            validarInsumo(producto, componente.getInsumo());
            existentes.add(componente.getInsumo().getId());
        }
        for (Map.Entry<UUID, Integer> linea : receta.entrySet()) {
            if (existentes.contains(linea.getKey())) {
                continue;
            }
            ProductoInventario insumo = repository.findById(linea.getKey())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado"));
            validarInsumo(producto, insumo);
            ProductoComponente componente = new ProductoComponente();
            componente.setProducto(producto);
            componente.setInsumo(insumo);
            componente.setCantidad(linea.getValue());
            producto.getComponentes().add(componente);
        }
    }

    private void validarInsumo(ProductoInventario producto, ProductoInventario insumo) {
        if (producto.getId() != null && producto.getId().equals(insumo.getId())) {
            throw new ReglaNegocioException("Un producto no puede ser insumo de sí mismo");
        }
        if (insumo.isCompuesto()) {
            throw new ReglaNegocioException(insumo.getNombre()
                    + " también se prepara con receta; usa directamente sus insumos");
        }
        UUID sucursalProducto = producto.getSucursal() == null ? null : producto.getSucursal().getId();
        UUID sucursalInsumo = insumo.getSucursal() == null ? null : insumo.getSucursal().getId();
        if (sucursalProducto != null && sucursalInsumo != null && !Objects.equals(sucursalProducto, sucursalInsumo)) {
            throw new ReglaNegocioException(insumo.getNombre() + " pertenece a otra sucursal");
        }
    }

    private Sucursal obtenerSucursal(UUID sucursalId) {
        if (sucursalId == null) {
            return null;
        }
        return sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
    }

    private static String requerido(String valor, String mensaje) {
        String limpio = limpiar(valor);
        if (limpio == null) {
            throw new ReglaNegocioException(mensaje);
        }
        return limpio;
    }

    private static String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
