package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.AuthResponse;
import ni.edu.uam.GimnasioAPI.dto.LoginRequest;
import ni.edu.uam.GimnasioAPI.dto.RegisterRequest;
import ni.edu.uam.GimnasioAPI.models.Rol;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.RolRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.SucursalRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.security.JwtService;
import ni.edu.uam.GimnasioAPI.security.UserDetailsImpl;
import ni.edu.uam.GimnasioAPI.util.ValidacionCuenta;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SocioRepository socioRepository;
    private final SucursalRepository sucursalRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private final NotificacionService notificacionService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = ValidacionCuenta.email(request.getEmail());
        String password = ValidacionCuenta.password(request.getPassword());
        String nombres = ValidacionCuenta.requerido(request.getNombres(), "nombres");
        String apellidos = ValidacionCuenta.requerido(request.getApellidos(), "apellidos");
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictoException("El correo ya está en uso.");
        }

        Rol rolSocio = rolRepository.findByNombre("ROLE_SOCIO")
                .orElseThrow(() -> new IllegalStateException("Error: Rol ROLE_SOCIO no encontrado en BD."));

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRol(rolSocio);
        usuario.setActivo(true);

        usuario = usuarioRepository.save(usuario);

        Socio socio = new Socio();
        socio.setUsuario(usuario);
        socio.setNombres(nombres);
        socio.setApellidos(apellidos);
        socio.setTelefono(request.getTelefono());
        socio.setEstadoAcceso("Inactivo");
        if (request.getSucursalId() != null) {
            Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
            socio.setSucursal(sucursal);
        }

        socioRepository.save(socio);

        // Generamos el Token JWT
        UserDetailsImpl userDetails = new UserDetailsImpl(usuario);
        String token = jwtService.generateToken(userDetails);

        // Enviamos el correo en segundo plano
        notificacionService.enviarCorreoBienvenida(usuario.getEmail(), nombres);

        // Hacemos el ÚNICO return correcto al final
        return AuthResponse.builder()
                .id(usuario.getId().toString())
                .token(token)
                .email(usuario.getEmail())
                .rol(rolSocio.getNombre())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String email = ValidacionCuenta.email(request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras autenticación."));

        return iniciarSesion(usuario);
    }

    AuthResponse iniciarSesion(Usuario usuario) {
        UserDetailsImpl userDetails = new UserDetailsImpl(usuario);
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .id(usuario.getId().toString())
                .token(token)
                .email(usuario.getEmail())
                .rol(usuario.getRol().getNombre())
                .build();
    }
}
