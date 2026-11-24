package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.exceptions.*;
import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.TipoMembresiaRequest;
import ni.edu.uam.GimnasioAPI.models.*;
import ni.edu.uam.GimnasioAPI.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MembresiaService {

    private static final Set<String> METODOS_PAGO_VALIDOS = Set.of("Efectivo", "Tarjeta", "Transferencia");

    private final SocioRepository socioRepository;
    private final TipoMembresiaRepository tipoMembresiaRepository;
    private final PagoRepository pagoRepository;
    private final MembresiaSocioRepository membresiaSocioRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<TipoMembresia> listarTipos() {
        return tipoMembresiaRepository.findAll();
    }

    @Transactional
    public TipoMembresia crearTipo(TipoMembresiaRequest request) {
        TipoMembresia tipo = new TipoMembresia();
        aplicarDatos(tipo, request);
        return tipoMembresiaRepository.save(tipo);
    }

    @Transactional
    public TipoMembresia actualizarTipo(Integer id, TipoMembresiaRequest request) {
        TipoMembresia tipo = tipoMembresiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan de membresía no encontrado"));
        aplicarDatos(tipo, request);
        return tipoMembresiaRepository.save(tipo);
    }

    private void aplicarDatos(TipoMembresia tipo, TipoMembresiaRequest request) {
        String nombre = request.getNombre() == null ? "" : request.getNombre().trim();
        if (nombre.isEmpty() || nombre.length() > 50) {
            throw new ReglaNegocioException("El nombre del plan debe tener entre 1 y 50 caracteres.");
        }
        if (request.getDuracionDias() == null || request.getDuracionDias() < 1) {
            throw new ReglaNegocioException("La duración debe ser al menos un día.");
        }
        if (request.getPrecio() == null || request.getPrecio().compareTo(BigDecimal.ZERO) < 0 || request.getPrecio().scale() > 2) {
            throw new ReglaNegocioException("El precio debe ser positivo y tener hasta dos decimales.");
        }
        String beneficios = request.getBeneficios() == null ? "" : request.getBeneficios().trim();
        if (beneficios.length() > 2000) {
            throw new ReglaNegocioException("Los beneficios no deben superar 2000 caracteres.");
        }
        tipo.setNombre(nombre);
        tipo.setDuracionDias(request.getDuracionDias());
        tipo.setPrecio(request.getPrecio());
        tipo.setBeneficios(beneficios);
    }

    @Transactional
    public TipoMembresia actualizarImagen(Integer id, String imagenUrl) {
        TipoMembresia tipo = tipoMembresiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan de membresía no encontrado"));
        tipo.setImagenUrl(ni.edu.uam.GimnasioAPI.util.FotoPerfilValidator.validar(imagenUrl));
        return tipoMembresiaRepository.save(tipo);
    }

    @Transactional
    public void eliminarTipo(Integer id) {
        tipoMembresiaRepository.deleteById(id);
    }

    static String validarMetodoPago(String metodoPago) {
        String normalizado = metodoPago == null ? "" : metodoPago.trim();
        return METODOS_PAGO_VALIDOS.stream()
                .filter(metodo -> metodo.equalsIgnoreCase(normalizado))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "El método de pago debe ser Efectivo, Tarjeta o Transferencia."
                ));
    }
}
