package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.dto.SolicitudTransferenciaRequest;
import ni.edu.uam.GimnasioAPI.exceptions.ReglaNegocioException;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.models.Usuario;
import ni.edu.uam.GimnasioAPI.repositories.*;
import ni.edu.uam.GimnasioAPI.security.AlcanceSucursal;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferenciaSeguridadTest {
    @Test
    void efectivoConCuentaIncluida_noCreaSolicitudNiPago() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        SocioRepository socios = mock(SocioRepository.class);
        SolicitudPagoMembresiaRepository solicitudes = mock(SolicitudPagoMembresiaRepository.class);
        PagoRepository pagos = mock(PagoRepository.class);
        Usuario usuario = new Usuario(); usuario.setId(UUID.randomUUID());
        Socio socio = new Socio(); socio.setUsuarioId(usuario.getId());
        when(usuarios.findByEmailIgnoreCase("socio@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(socios.findById(usuario.getId())).thenReturn(Optional.of(socio));
        TransferenciaService servicio = new TransferenciaService(solicitudes, mock(CuentaBancariaRepository.class),
                mock(TipoMembresiaRepository.class), socios, usuarios, pagos, mock(PagoService.class),
                mock(NotificacionService.class), mock(AlcanceSucursal.class));
        SolicitudTransferenciaRequest request = new SolicitudTransferenciaRequest();
        request.setCuentaBancariaId(UUID.randomUUID());
        request.setMetodoPago("Efectivo");

        assertThatThrownBy(() -> servicio.solicitar("socio@ejemplo.com", request))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("recepción");
        verifyNoInteractions(solicitudes, pagos);
    }
}
