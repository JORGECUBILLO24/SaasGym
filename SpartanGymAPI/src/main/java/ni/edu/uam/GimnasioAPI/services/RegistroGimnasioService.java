package ni.edu.uam.GimnasioAPI.services;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.RegistroGimnasioRequest;
import ni.edu.uam.GimnasioAPI.dto.RegistroGimnasioResponse;
import ni.edu.uam.GimnasioAPI.exceptions.ConflictoException;
import ni.edu.uam.GimnasioAPI.models.ConfiguracionApp;
import ni.edu.uam.GimnasioAPI.models.Gimnasio;
import ni.edu.uam.GimnasioAPI.models.Personal;
import ni.edu.uam.GimnasioAPI.models.Rol;
import ni.edu.uam.GimnasioAPI.models.Sucursal;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.ConfiguracionAppRepository;
import ni.edu.uam.GimnasioAPI.repositories.GimnasioRepository;
import ni.edu.uam.GimnasioAPI.repositories.PersonalRepository;
import ni.edu.uam.GimnasioAPI.repositories.RolRepository;
import ni.edu.uam.GimnasioAPI.repositories.SucursalRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.security.TenantContext;
import ni.edu.uam.GimnasioAPI.util.ValidacionCuenta;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistroGimnasioService {

    private final GimnasioRepository gimnasioRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final PersonalRepository personalRepository;
    private final ConfiguracionAppRepository configuracionAppRepository;
    private final TenantLookupService tenantLookupService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Transactional
    public RegistroGimnasioResponse registrar(RegistroGimnasioRequest request) {
        UUID gimnasioId = TenantContext.requireTenantId();
        String nombreComercial = ValidacionCuenta.requerido(request.nombreComercial(), "nombre comercial");
        String correo = ValidacionCuenta.email(request.correo());
        String slug = ValidacionCuenta.requerido(request.slug(), "slug").toLowerCase(java.util.Locale.ROOT);
        String correoAdministrador = ValidacionCuenta.email(request.administrador().correo());

        if (gimnasioRepository.existsByNombreComercialIgnoreCase(nombreComercial)) {
            throw new ConflictoException("Ya existe un gimnasio con ese nombre comercial.");
        }
        if (gimnasioRepository.existsBySlugIgnoreCase(slug)) {
            throw new ConflictoException("Ese slug ya está registrado.");
        }
        if (tenantLookupService.correoUsuarioEnUso(correoAdministrador)) {
            throw new ConflictoException("El correo del administrador ya está en uso.");
        }

        Gimnasio gimnasio = new Gimnasio();
        gimnasio.setId(gimnasioId);
        gimnasio.setNombreComercial(nombreComercial);
        gimnasio.setCorreo(correo);
        gimnasio.setTelefono(request.telefono());
        gimnasio.setSlug(slug);
        gimnasio.setFechaAlta(LocalDateTime.now());
        gimnasio.setActivo(true);
        gimnasioRepository.save(gimnasio);

        Sucursal sucursal = sucursalRepository.save(crearSucursal(request.sucursalPrincipal()));

        Rol rolAdministrador = rolRepository.findByNombre("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("El rol ROLE_ADMIN no está configurado."));
        Usuario administrador = new Usuario();
        administrador.setEmail(correoAdministrador);
        administrador.setPasswordHash(passwordEncoder.encode(
                ValidacionCuenta.password(request.administrador().password())
        ));
        administrador.setRol(rolAdministrador);
        administrador.setActivo(true);
        administrador = usuarioRepository.save(administrador);

        Personal personal = new Personal();
        personal.setUsuario(administrador);
        personal.setNombres(ValidacionCuenta.requerido(request.administrador().nombres(), "nombres"));
        personal.setApellidos(ValidacionCuenta.requerido(request.administrador().apellidos(), "apellidos"));
        personal.setTelefono(request.administrador().telefono());
        personal.setSucursal(sucursal);
        personalRepository.save(personal);

        crearConfiguracionInicial(gimnasio);

        return new RegistroGimnasioResponse(
                gimnasio.getId(),
                gimnasio.getNombreComercial(),
                gimnasio.getSlug(),
                administrador.getEmail(),
                administrador.getId(),
                sucursal.getId()
        );
    }

    private Sucursal crearSucursal(RegistroGimnasioRequest.SucursalPrincipal request) {
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(ValidacionCuenta.requerido(request.nombre(), "sucursal principal"));
        sucursal.setUbicacion(ValidacionCuenta.requerido(request.ubicacion(), "ubicación"));
        sucursal.setTelefono(request.telefono());
        sucursal.setCapacidad(request.capacidad());
        sucursal.setHorarioApertura(request.horarioApertura());
        sucursal.setHorarioCierre(request.horarioCierre());
        return sucursal;
    }

    private void crearConfiguracionInicial(Gimnasio gimnasio) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("gymName", gimnasio.getNombreComercial());
        defaults.put("email", gimnasio.getCorreo());
        if (gimnasio.getTelefono() != null) {
            defaults.put("phone", gimnasio.getTelefono());
        }
        defaults.put("currency", "USD");
        defaults.put("taxRate", "0");
        defaults.put("theme", "system");
        defaults.put("themeSource", "system");
        defaults.put("accentColor", "#e50914");
        defaults.put("accentHoverColor", "#b91c1c");
        defaults.put("accentSoftColor", "#450a0a");

        ConfiguracionApp configuracion = new ConfiguracionApp();
        configuracion.setId(1);
        try {
            configuracion.setConfiguracionJson(objectMapper.writeValueAsString(defaults));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear la configuración inicial del gimnasio.", e);
        }
        configuracionAppRepository.save(configuracion);
    }
}
