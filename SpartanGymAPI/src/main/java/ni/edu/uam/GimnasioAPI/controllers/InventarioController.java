package ni.edu.uam.GimnasioAPI.controllers;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.ProductoCatalogoResponse;
import ni.edu.uam.GimnasioAPI.dto.ProductoInventarioRequest;
import ni.edu.uam.GimnasioAPI.models.ProductoInventario;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.repositories.ProductoInventarioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.services.InventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {
    private final ProductoInventarioRepository repository;
    private final InventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;
    private final SocioRepository socioRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'RECEPCIONISTA')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProductoInventario>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    // Catalogo visible para el socio (app): productos de su misma sucursal que se venden al publico.
    // Si el socio no tiene sucursal asignada, ve todos los productos.
    @GetMapping("/catalogo")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProductoCatalogoResponse>> catalogo(Authentication auth) {
        UUID sucursalId = sucursalDelSocio(auth);
        return ResponseEntity.ok(repository.findAll().stream()
                .filter(producto -> !Boolean.FALSE.equals(producto.getVendible()))
                .filter(producto -> sucursalId == null
                        || (producto.getSucursal() != null && sucursalId.equals(producto.getSucursal().getId())))
                .map(this::toCatalogo)
                .toList());
    }

    private UUID sucursalDelSocio(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return usuarioRepository.findByEmail(auth.getName())
                .flatMap(usuario -> socioRepository.findById(usuario.getId()))
                .map(Socio::getSucursal)
                .map(Sucursal::getId)
                .orElse(null);
    }

    private ProductoCatalogoResponse toCatalogo(ProductoInventario producto) {
        return ProductoCatalogoResponse.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .categoria(producto.getCategoria())
                .precio(producto.getPrecio())
                .stock(producto.getDisponible())
                .descripcion(producto.getDescripcion())
                .imagenUrl(producto.getImagenUrl())
                .sucursalId(producto.getSucursal() != null ? producto.getSucursal().getId() : null)
                .sucursal(producto.getSucursal() != null ? producto.getSucursal().getNombre() : null)
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ProductoInventario> crear(@RequestBody ProductoInventarioRequest request) {
        return ResponseEntity.ok(inventarioService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<ProductoInventario> actualizar(@PathVariable UUID id, @RequestBody ProductoInventarioRequest request) {
        return ResponseEntity.ok(inventarioService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        inventarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
