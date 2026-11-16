package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.models.Ejercicio;
import ni.edu.uam.GimnasioAPI.models.GrupoMuscular;
import ni.edu.uam.GimnasioAPI.repositories.EjercicioRepository;
import ni.edu.uam.GimnasioAPI.repositories.GrupoMuscularRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final GrupoMuscularRepository grupoMuscularRepository;
    private final EjercicioRepository ejercicioRepository;

    // --- Grupos Musculares ---
    public GrupoMuscular crearGrupoMuscular(GrupoMuscular grupo) {
        return grupoMuscularRepository.save(grupo);
    }

    public List<GrupoMuscular> obtenerTodosLosGrupos() {
        return grupoMuscularRepository.findAll();
    }

    // --- Ejercicios ---
    public Ejercicio crearEjercicio(Ejercicio ejercicio) {
        // Validamos que el grupo muscular exista
        GrupoMuscular grupo = grupoMuscularRepository.findById(ejercicio.getGrupoMuscular().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo muscular no encontrado"));

        ejercicio.setGrupoMuscular(grupo);
        return ejercicioRepository.save(ejercicio);
    }

    public List<Ejercicio> obtenerTodosLosEjercicios() {
        return ejercicioRepository.findAll();
    }
}