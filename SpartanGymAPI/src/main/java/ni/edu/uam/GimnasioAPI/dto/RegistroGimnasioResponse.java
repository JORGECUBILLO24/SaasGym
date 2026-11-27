package ni.edu.uam.GimnasioAPI.dto;

import java.util.UUID;

public record RegistroGimnasioResponse(
        UUID gimnasioId,
        String nombreComercial,
        String slug,
        String correoAdministrador,
        UUID administradorId,
        UUID sucursalPrincipalId
) {}
