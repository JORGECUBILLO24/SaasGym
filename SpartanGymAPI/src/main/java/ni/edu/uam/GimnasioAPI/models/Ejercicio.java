package ni.edu.uam.GimnasioAPI.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * Ejercicio del catálogo. Los de la biblioteca importada traen `codigo` y un GIF en
 * /gifs/{codigo}.gif; los creados a mano por el personal pueden dejar esos campos vacíos.
 */
@Data
@Entity
@Table(name = "ejercicios")
public class Ejercicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "grupo_muscular_id", nullable = false)
    private GrupoMuscular grupoMuscular;

    @Column(length = 10, unique = true)
    private String codigo;

    @Column(name = "nombre_original", length = 150)
    private String nombreOriginal;

    /** Sinónimos de búsqueda en español más el nombre original en inglés. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> alias;

    @Column(length = 40)
    private String zona;

    @Column(name = "musculo_principal", length = 60)
    private String musculoPrincipal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "musculos_secundarios")
    private List<String> musculosSecundarios;

    @Column(length = 40)
    private String equipo;

    @Column(length = 20)
    private String dificultad;

    @Column(length = 30)
    private String categoria;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> instrucciones;

    @Column(name = "gif_url", length = 255)
    private String gifUrl;
}
