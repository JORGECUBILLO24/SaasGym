package ni.edu.uam.GimnasioAPI.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "gimnasios")
public class Gimnasio {

    @Id
    private UUID id;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(nullable = false, length = 255)
    private String correo;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @Column(nullable = false)
    private Boolean activo = true;
}
