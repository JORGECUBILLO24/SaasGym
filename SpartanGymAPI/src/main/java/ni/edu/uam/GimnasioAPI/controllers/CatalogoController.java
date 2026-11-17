package ni.edu.uam.GimnasioAPI.controllers;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.models.Ejercicio;
import ni.edu.uam.GimnasioAPI.models.GrupoMuscular;
import ni.edu.uam.GimnasioAPI.services.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    // Leer el catalogo es libre para cualquier sesion; modificarlo, solo para quien arma rutinas.
    @PostMapping("/grupos-musculares")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENTRENADOR')")
    public ResponseEntity<GrupoMuscular> crearGrupo(@RequestBody GrupoMuscular grupo) {
        return ResponseEntity.ok(catalogoService.crearGrupoMuscular(grupo));
    }

    @GetMapping("/grupos-musculares")
    public ResponseEntity<List<GrupoMuscular>> listarGrupos() {
        return ResponseEntity.ok(catalogoService.obtenerTodosLosGrupos());
    }

    @PostMapping("/ejercicios")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'ENTRENADOR')")
    public ResponseEntity<Ejercicio> crearEjercicio(@RequestBody Ejercicio ejercicio) {
        return ResponseEntity.ok(catalogoService.crearEjercicio(ejercicio));
    }

    @GetMapping("/ejercicios")
    public ResponseEntity<List<Ejercicio>> listarEjercicios() {
        return ResponseEntity.ok(catalogoService.obtenerTodosLosEjercicios());
    }
}