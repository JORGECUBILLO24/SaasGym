package ni.edu.uam.GimnasioAPI.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Fetch;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    // Nunca debe salir en un JSON ni en un log: varias entidades (Asistencia, Socio, ...)
    // arrastran a Usuario y se serializaban enteras, con el hash incluido.
    @JsonIgnore
    @ToString.Exclude
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    private Boolean activo = true;

    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    /**
     * Sube cada vez que cambia la contraseña: los JWT llevan este número y uno con un valor
     * viejo se rechaza, así cambiar la clave cierra todas las sesiones abiertas.
     */
    @JsonIgnore
    @ToString.Exclude
    @Column(name = "auth_version", nullable = false)
    private Integer authVersion = 0;

    /** Invalida todas las sesiones emitidas hasta ahora. */
    public void cerrarSesionesAbiertas() {
        authVersion = (authVersion == null ? 0 : authVersion) + 1;
    }

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
