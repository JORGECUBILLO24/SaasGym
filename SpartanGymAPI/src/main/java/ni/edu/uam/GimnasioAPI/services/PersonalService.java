package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.ActualizarPerfilRequest;
import ni.edu.uam.GimnasioAPI.dto.AuthResponse;
import ni.edu.uam.GimnasioAPI.dto.PersonalResponse;
import ni.edu.uam.GimnasioAPI.dto.RegisterPersonalRequest;
import ni.edu.uam.GimnasioAPI.models.Personal;
import ni.edu.uam.GimnasioAPI.models.Rol;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.PersonalRepository;
import ni.edu.uam.GimnasioAPI.repositories.RolRepository;
import ni.edu.uam.GimnasioAPI.repositories.SucursalRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.security.AlcanceSucursal;
import ni.edu.uam.GimnasioAPI.security.JwtService;
import ni.edu.uam.GimnasioAPI.security.UserDetailsImpl;
import ni.edu.uam.GimnasioAPI.util.ValidacionCuenta;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonalService {

    private static final Set<String> ROLES_PERSONAL = Set.of(
            "ROLE_ADMIN",
            "ROLE_RECEPCIONISTA",
            "ROLE_ENTRENADOR"
    );

    private final UsuarioRepository usuarioRepository;
    private final PersonalRepository personalRepository;
    private final SucursalRepository sucursalRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public List<PersonalResponse> listarPersonal() {
        return personalRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PersonalResponse actualizarRol(UUID usuarioId, String rol) {
        String rolSolicitado = normalizarRol(rol);
        Rol nuevoRol = rolRepository.findByNombre(rolSolicitado)
                .orElseThrow(() -> new IllegalStateException("Rol " + rolSolicitado + " no configurado"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        protegerSuperadmin(usuario);

        usuario.setRol(nuevoRol);
        usuarioRepository.save(usuario);

        Personal personal = personalRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Personal no encontrado"));

        return toResponse(personal);
    }

    @Transactional
    public PersonalResponse desactivarPersonal(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Personal personal = personalRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Personal no encontrado"));
        protegerSuperadmin(usuario);

        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        return toResponse(personal);
    }

    @Transactional
    public AuthResponse registrarPersonal(RegisterPersonalRequest request) {
        String email = ValidacionCuenta.email(request.getEmail());
        String password = ValidacionCuenta.password(request.getPassword());
        String nombres = ValidacionCuenta.requerido(request.getNombres(), "nombres");
        String apellidos = ValidacionCuenta.requerido(request.getApellidos(), "apellidos");
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictoException("El correo ya está en uso.");
        }

        String rolSolicitado = normalizarRol(request.getRol());
        Rol rol = rolRepository.findByNombre(rolSolicitado)
                .orElseThrow(() -> new IllegalStateException("Rol " + rolSolicitado + " no configurado"));

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        Personal personal = new Personal();
        personal.setUsuario(usuario);
        personal.setNombres(nombres);
        personal.setApellidos(apellidos);
        personal.setTelefono(request.getTelefono());
        personal.setEspecialidad(request.getEspecialidad());
        personal.setSucursal(obtenerSucursalOpcional(request.getSucursalId()));

        personalRepository.save(personal);

        UserDetailsImpl userDetails = new UserDetailsImpl(usuario);
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .id(usuario.getId().toString())
                .token(token)
                .email(usuario.getEmail())
                .rol(rol.getNombre())
                .build();
    }

    @Transactional
    public PersonalResponse actualizarPerfilActual(Authentication auth, ActualizarPerfilRequest request) {
        if (auth == null || auth.getName() == null) {
            throw new NoAutenticadoException("Usuario no autenticado");
        }

        Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoAutenticadoException("Usuario no encontrado"));
        Personal personal = personalRepository.findById(usuario.getId())
                .orElseThrow(() -> new AccesoDenegadoException("Perfil de personal no encontrado"));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            usuario.setEmail(ValidacionCuenta.email(request.getEmail()));
        }
        if (request.getNombres() != null && !request.getNombres().isBlank()) {
            personal.setNombres(request.getNombres().trim());
        }
        if (request.getApellidos() != null && !request.getApellidos().isBlank()) {
            personal.setApellidos(request.getApellidos().trim());
        }
        if (request.getTelefono() != null) {
            personal.setTelefono(request.getTelefono().trim());
        }
        if (request.getEspecialidad() != null) {
            personal.setEspecialidad(request.getEspecialidad().trim());
        }
        // Recepción y entrenadores no eligen su propia sucursal: es lo que limita qué socios,
        // pedidos e inventario pueden tocar. Si la mandan se ignora; la asigna administración.
        String rolActual = usuario.getRol() != null ? usuario.getRol().getNombre() : "";
        if (request.getSucursalId() != null && !AlcanceSucursal.esRolDeSucursal(rolActual)) {
            personal.setSucursal(obtenerSucursalOpcional(request.getSucursalId()));
        }

        usuarioRepository.save(usuario);
        return toResponse(personalRepository.save(personal));
    }

    private void protegerSuperadmin(Usuario usuario) {
        if (usuario.getRol() != null && "ROLE_SUPERADMIN".equals(usuario.getRol().getNombre())) {
            throw new AccesoDenegadoException("La cuenta de superadministrador no se puede modificar desde aquí");
        }
    }

    private String normalizarRol(String rol) {
        String rolNormalizado = rol == null || rol.isBlank()
                ? "ROLE_ENTRENADOR"
                : rol.trim().toUpperCase();

        if (!rolNormalizado.startsWith("ROLE_")) {
            rolNormalizado = "ROLE_" + rolNormalizado;
        }

        if (!ROLES_PERSONAL.contains(rolNormalizado)) {
            throw new ReglaNegocioException("Rol de personal no permitido: " + rolNormalizado);
        }

        return rolNormalizado;
    }

    private Sucursal obtenerSucursalOpcional(UUID sucursalId) {
        if (sucursalId == null) {
            return null;
        }

        return sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
    }

    private PersonalResponse toResponse(Personal personal) {
        Usuario usuario = personal.getUsuario();

        return PersonalResponse.builder()
                .id(personal.getUsuarioId())
                .nombres(personal.getNombres())
                .apellidos(personal.getApellidos())
                .email(usuario.getEmail())
                .telefono(personal.getTelefono())
                .rol(usuario.getRol().getNombre())
                .especialidad(personal.getEspecialidad())
                .sucursalId(personal.getSucursal() != null ? personal.getSucursal().getId() : null)
                .sucursal(personal.getSucursal() != null ? personal.getSucursal().getNombre() : null)
                .activo(usuario.getActivo())
                .build();
    }
}
