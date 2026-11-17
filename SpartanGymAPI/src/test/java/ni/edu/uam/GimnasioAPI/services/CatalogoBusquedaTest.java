package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.models.Ejercicio;
import ni.edu.uam.GimnasioAPI.models.GrupoMuscular;
import ni.edu.uam.GimnasioAPI.repositories.EjercicioRepository;
import ni.edu.uam.GimnasioAPI.repositories.GrupoMuscularRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogoBusquedaTest {

    private final EjercicioRepository ejercicios = mock(EjercicioRepository.class);
    private final CatalogoService servicio = new CatalogoService(mock(GrupoMuscularRepository.class), ejercicios);

    private static GrupoMuscular grupo(int id, String nombre) {
        GrupoMuscular g = new GrupoMuscular();
        g.setId(id);
        g.setNombre(nombre);
        return g;
    }

    private static Ejercicio ejercicio(String nombre, GrupoMuscular g, String equipo, String dificultad,
                                       String musculo, List<String> alias) {
        Ejercicio e = new Ejercicio();
        e.setNombre(nombre);
        e.setGrupoMuscular(g);
        e.setEquipo(equipo);
        e.setDificultad(dificultad);
        e.setMusculoPrincipal(musculo);
        e.setAlias(alias);
        return e;
    }

    @BeforeEach
    void datos() {
        GrupoMuscular pecho = grupo(1, "Pecho");
        GrupoMuscular biceps = grupo(6, "Bíceps");
        when(ejercicios.findAll()).thenReturn(List.of(
                ejercicio("Aperturas con mancuernas", pecho, "Mancuerna", "Principiante", "Pectorales",
                        List.of("pec fly", "dumbbell fly")),
                ejercicio("Press de banca con barra", pecho, "Barra", "Intermedio", "Pectorales",
                        List.of("press plano", "barbell bench press")),
                ejercicio("Curl de bíceps con mancuerna", biceps, "Mancuerna", "Principiante", "Bíceps",
                        List.of("curl alterno", "dumbbell biceps curl")),
                ejercicio("Press de banca inclinado con mancuernas", pecho, "Mancuerna", "Intermedio", "Pectorales",
                        List.of("press inclinado", "dumbbell incline bench press")),
                ejercicio("Press de banca con agarre amplio en máquina Smith", pecho, "Máquina Smith", "Intermedio",
                        "Pectorales", List.of("smith wide grip bench press"))
        ));
    }

    private List<String> nombres(List<Ejercicio> lista) {
        return lista.stream().map(Ejercicio::getNombre).toList();
    }

    @Test
    void ignoraTildesMayusculasYPlurales() {
        assertThat(nombres(servicio.buscarEjercicios("BICEPS mancuernas", null, null, null, 30)))
                .containsExactly("Curl de bíceps con mancuerna");
    }

    @Test
    void encuentraPorSinonimoYPorNombreEnIngles() {
        assertThat(nombres(servicio.buscarEjercicios("press plano", null, null, null, 30)))
                .containsExactly("Press de banca con barra");
        assertThat(nombres(servicio.buscarEjercicios("bench press", null, null, null, 30)))
                .containsExactlyInAnyOrder("Press de banca con barra", "Press de banca inclinado con mancuernas",
                        "Press de banca con agarre amplio en máquina Smith");
    }

    @Test
    void elNombreQueEmpiezaIgualVaPrimero() {
        assertThat(nombres(servicio.buscarEjercicios("press de banca", null, null, null, 30)))
                .first().isEqualTo("Press de banca con barra");
    }

    @Test
    void filtraPorGrupoEquipoYDificultad() {
        assertThat(nombres(servicio.buscarEjercicios(null, 1, "mancuerna", "intermedio", 30)))
                .containsExactly("Press de banca inclinado con mancuernas");
    }

    @Test
    void sinCoincidencias_listaVacia() {
        assertThat(servicio.buscarEjercicios("sentadilla", null, null, null, 30)).isEmpty();
    }
}
