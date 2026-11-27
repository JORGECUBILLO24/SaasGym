package ni.edu.uam.GimnasioAPI.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "asistencias")
public class Asistencia {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "gimnasio_id", nullable = false, updatable = false)
    private java.util.UUID gimnasioId;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @Column(name = "fecha_hora", updatable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(name = "qr_token_hash", length = 64, unique = true)
    private String qrTokenHash;
}
