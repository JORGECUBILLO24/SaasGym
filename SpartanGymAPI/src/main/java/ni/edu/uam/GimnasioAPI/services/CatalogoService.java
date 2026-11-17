package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.models.Ejercicio;
import ni.edu.uam.GimnasioAPI.models.GrupoMuscular;
import ni.edu.uam.GimnasioAPI.repositories.EjercicioRepository;
import ni.edu.uam.GimnasioAPI.repositories.GrupoMuscularRepository;
import ni.edu.uam.GimnasioAPI.util.BusquedaTexto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final GrupoMuscularRepository grupoMuscularRepository;
    private final EjercicioRepository ejercicioRepository;

    public GrupoMuscular crearGrupoMuscular(GrupoMuscular grupo) {
        return grupoMuscularRepository.save(grupo);
    }

    public List<GrupoMuscular> obtenerTodosLosGrupos() {
        return grupoMuscularRepository.findAll();
    }

    public Ejercicio crearEjercicio(Ejercicio ejercicio) {
        GrupoMuscular grupo = grupoMuscularRepository.findById(ejercicio.getGrupoMuscular().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo muscular no encontrado"));

        ejercicio.setGrupoMuscular(grupo);
        return ejercicioRepository.save(ejercicio);
    }

    public List<Ejercicio> obtenerTodosLosEjercicios() {
        return ejercicioRepository.findAll();
    }

    /**
     * Búsqueda del entrenador en la biblioteca. Ignora tildes, mayúsculas y plurales simples,
     * busca en nombre, sinónimos (incluido el nombre en inglés), músculos y equipo, y ordena
     * primero lo que coincide en el nombre.
     */
    @Transactional(readOnly = true)
    public List<Ejercicio> buscarEjercicios(String consulta, Integer grupoMuscularId, String equipo,
                                            String dificultad, int limite) {
        List<String> terminos = BusquedaTexto.terminos(consulta);
        String equipoBuscado = BusquedaTexto.normalizar(equipo);
        String dificultadBuscada = BusquedaTexto.normalizar(dificultad);
        int tope = Math.max(1, Math.min(limite, 100));

        return ejercicioRepository.findAll().stream()
                .filter(e -> grupoMuscularId == null
                        || (e.getGrupoMuscular() != null && grupoMuscularId.equals(e.getGrupoMuscular().getId())))
                .filter(e -> equipoBuscado.isEmpty() || equipoBuscado.equals(BusquedaTexto.normalizar(e.getEquipo())))
                .filter(e -> dificultadBuscada.isEmpty()
                        || dificultadBuscada.equals(BusquedaTexto.normalizar(e.getDificultad())))
                .map(e -> new Resultado(e, puntuar(e, terminos)))
                .filter(r -> terminos.isEmpty() || r.puntos() > 0)
                // En empate gana el nombre más corto: suele ser la variante base ("Press de banca con barra"
                // antes que "... con agarre amplio en máquina Smith").
                .sorted(Comparator.comparingInt(Resultado::puntos).reversed()
                        .thenComparingInt(r -> r.ejercicio().getNombre().length())
                        .thenComparing(r -> r.ejercicio().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .limit(tope)
                .map(Resultado::ejercicio)
                .toList();
    }

    private int puntuar(Ejercicio e, List<String> terminos) {
        if (terminos.isEmpty()) {
            return 0;
        }
        String nombre = BusquedaTexto.normalizar(e.getNombre());
        String consulta = String.join(" ", terminos);
        if (nombre.equals(consulta)) {
            return 100;
        }
        if (nombre.startsWith(consulta)) {
            return 80;
        }
        if (BusquedaTexto.contieneTodos(nombre, terminos)) {
            return 60;
        }
        String alias = BusquedaTexto.normalizar(String.join(" ", lista(e.getAlias())));
        if (BusquedaTexto.contieneTodos(alias, terminos)) {
            return 40;
        }
        String resto = BusquedaTexto.normalizar(String.join(" ", Stream.concat(
                        Stream.of(nombre, alias, e.getMusculoPrincipal(), e.getEquipo(), e.getZona(),
                                e.getGrupoMuscular() == null ? null : e.getGrupoMuscular().getNombre()),
                        lista(e.getMusculosSecundarios()).stream())
                .filter(Objects::nonNull)
                .toList()));
        return BusquedaTexto.contieneTodos(resto, terminos) ? 20 : 0;
    }

    private static List<String> lista(List<String> valores) {
        return valores == null ? List.of() : valores;
    }

    private record Resultado(Ejercicio ejercicio, int puntos) {
    }
}
