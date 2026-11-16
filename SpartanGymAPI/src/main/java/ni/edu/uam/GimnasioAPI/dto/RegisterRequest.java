package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class RegisterRequest {
    private String nombres;
    private String apellidos;
    private String email;
    private String password;
    private String telefono;
    private UUID sucursalId;
}
