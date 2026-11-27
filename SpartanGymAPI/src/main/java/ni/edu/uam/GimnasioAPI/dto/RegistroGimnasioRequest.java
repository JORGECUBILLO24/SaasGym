package ni.edu.uam.GimnasioAPI.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroGimnasioRequest(
        @NotBlank @Size(max = 150) String nombreComercial,
        @NotBlank @Email @Size(max = 100) String correo,
        @Size(max = 30) String telefono,
        @NotBlank @Size(max = 80)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "El slug solo puede contener letras minúsculas, números y guiones.")
        String slug,
        @NotNull @Valid Administrador administrador,
        @NotNull @Valid SucursalPrincipal sucursalPrincipal
) {
    public record Administrador(
            @NotBlank @Size(max = 100) String nombres,
            @NotBlank @Size(max = 100) String apellidos,
            @NotBlank @Email @Size(max = 100) String correo,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 20) String telefono
    ) {}

    public record SucursalPrincipal(
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Size(max = 255) String ubicacion,
            @Size(max = 25) String telefono,
            @NotNull @Min(0) Integer capacidad,
            @NotBlank @Size(max = 20) String horarioApertura,
            @NotBlank @Size(max = 20) String horarioCierre
    ) {}
}
