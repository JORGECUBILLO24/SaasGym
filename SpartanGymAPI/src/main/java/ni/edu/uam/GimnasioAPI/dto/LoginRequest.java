package ni.edu.uam.GimnasioAPI.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private String gimnasioSlug;
}
