package ni.edu.uam.GimnasioAPI.dto;

import lombok.Builder;
import lombok.Data;
import ni.edu.uam.GimnasioAPI.models.Asistencia;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asistencia tal como la ve el cliente. Antes el endpoint devolvía la entidad JPA, que
 * arrastraba Socio -> Usuario (email, foto, hash de contraseña) y el hash interno del QR.
 */
@Data
@Builder
public class AsistenciaResponse {
    private UUID id;
    private UUID socioId;
    private String socio;
    private LocalDateTime fechaHora;
    /** Solo en el check-in: Permitido, Sin membresia, Suspendido o Denegado. */
    private String estado;
    private String mensaje;

    public static AsistenciaResponse desde(Asistencia asistencia) {
        return AsistenciaResponse.builder()
                .id(asistencia.getId())
                .socioId(asistencia.getSocio().getUsuarioId())
                .socio(asistencia.getSocio().getNombres() + " " + asistencia.getSocio().getApellidos())
                .fechaHora(asistencia.getFechaHora())
                .build();
    }
}
