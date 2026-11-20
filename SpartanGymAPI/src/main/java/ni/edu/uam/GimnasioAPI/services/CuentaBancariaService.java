package ni.edu.uam.GimnasioAPI.services;

import lombok.RequiredArgsConstructor;
import ni.edu.uam.GimnasioAPI.dto.CuentaBancariaRequest;
import ni.edu.uam.GimnasioAPI.dto.CuentaBancariaResponse;
import ni.edu.uam.GimnasioAPI.exceptions.RecursoNoEncontradoException;
import ni.edu.uam.GimnasioAPI.exceptions.ReglaNegocioException;
import ni.edu.uam.GimnasioAPI.models.CuentaBancaria;
import ni.edu.uam.GimnasioAPI.repositories.CuentaBancariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Cuentas bancarias del gimnasio que administración publica para recibir transferencias. */
@Service
@RequiredArgsConstructor
public class CuentaBancariaService {

    private static final Set<String> TIPOS = Set.of("Ahorro", "Corriente");
    private static final Set<String> MONEDAS = Set.of("NIO", "USD");
    private static final Pattern NUMERO = Pattern.compile("^[0-9][0-9 -]{3,38}[0-9]$");

    private final CuentaBancariaRepository repository;

    @Transactional(readOnly = true)
    public List<CuentaBancariaResponse> listar(boolean incluirInactivas) {
        List<CuentaBancaria> cuentas = incluirInactivas
                ? repository.findAllByOrderByActivaDescBancoAsc()
                : repository.findByActivaTrueOrderByBancoAscMonedaAsc();
        return cuentas.stream().map(CuentaBancariaResponse::desde).toList();
    }

    @Transactional
    public CuentaBancariaResponse crear(CuentaBancariaRequest request) {
        CuentaBancaria cuenta = new CuentaBancaria();
        aplicar(cuenta, request);
        return CuentaBancariaResponse.desde(repository.save(cuenta));
    }

    @Transactional
    public CuentaBancariaResponse actualizar(UUID id, CuentaBancariaRequest request) {
        CuentaBancaria cuenta = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria no encontrada"));
        aplicar(cuenta, request);
        return CuentaBancariaResponse.desde(repository.save(cuenta));
    }

    /** No se borra: las solicitudes viejas la siguen referenciando. Solo deja de mostrarse. */
    @Transactional
    public void desactivar(UUID id) {
        CuentaBancaria cuenta = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria no encontrada"));
        cuenta.setActiva(false);
        repository.save(cuenta);
    }

    private void aplicar(CuentaBancaria cuenta, CuentaBancariaRequest request) {
        if (request == null) {
            throw new ReglaNegocioException("Faltan los datos de la cuenta");
        }
        cuenta.setBanco(texto(request.getBanco(), "Escribe el nombre del banco", 80));
        cuenta.setTitular(texto(request.getTitular(), "Escribe el titular de la cuenta", 120));

        String tipo = elegir(request.getTipoCuenta(), TIPOS, "El tipo de cuenta debe ser Ahorro o Corriente");
        cuenta.setTipoCuenta(tipo);

        String moneda = request.getMoneda() == null ? "" : request.getMoneda().trim().toUpperCase(Locale.ROOT);
        if (!MONEDAS.contains(moneda)) {
            throw new ReglaNegocioException("La moneda debe ser NIO (córdobas) o USD (dólares)");
        }
        cuenta.setMoneda(moneda);

        String numero = request.getNumeroCuenta() == null ? "" : request.getNumeroCuenta().trim();
        if (!NUMERO.matcher(numero).matches()) {
            throw new ReglaNegocioException("El número de cuenta solo puede tener dígitos, espacios o guiones (5 a 40)");
        }
        cuenta.setNumeroCuenta(numero);
        if (request.getActiva() != null) {
            cuenta.setActiva(request.getActiva());
        }
    }

    private static String elegir(String valor, Set<String> permitidos, String mensaje) {
        String limpio = valor == null ? "" : valor.trim();
        return permitidos.stream().filter(p -> p.equalsIgnoreCase(limpio)).findFirst()
                .orElseThrow(() -> new ReglaNegocioException(mensaje));
    }

    private static String texto(String valor, String mensaje, int maximo) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException(mensaje);
        }
        if (limpio.length() > maximo) {
            throw new ReglaNegocioException("El texto no debe superar " + maximo + " caracteres");
        }
        return limpio;
    }
}
