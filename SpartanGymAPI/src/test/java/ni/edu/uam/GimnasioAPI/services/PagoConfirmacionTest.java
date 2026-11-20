package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.models.MembresiaSocio;
import ni.edu.uam.GimnasioAPI.models.Pago;
import ni.edu.uam.GimnasioAPI.models.Socio;
import ni.edu.uam.GimnasioAPI.repositories.MembresiaSocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.PagoRepository;
import ni.edu.uam.GimnasioAPI.repositories.SocioRepository;
import ni.edu.uam.GimnasioAPI.repositories.TipoMembresiaRepository;
import ni.edu.uam.GimnasioAPI.repositories.UsuarioRepository;
import ni.edu.uam.GimnasioAPI.repositories.PersonalRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagoConfirmacionTest {

    @Test
    void confirmaPagoYPendienteDeMembresia() {
        PagoRepository pagos = mock(PagoRepository.class);
        MembresiaSocioRepository membresias = mock(MembresiaSocioRepository.class);
        SocioRepository socios = mock(SocioRepository.class);

        UUID idPago = UUID.randomUUID();
        UUID idSocio = UUID.randomUUID();
        Pago pago = new Pago();
        pago.setId(idPago);
        pago.setEstado("Pendiente");

        Socio socio = new Socio();
        socio.setUsuarioId(idSocio);
        socio.setEstadoAcceso("Inactivo");

        MembresiaSocio membresia = new MembresiaSocio();
        membresia.setId(UUID.randomUUID());
        membresia.setSocio(socio);
        membresia.setEstado("Pendiente");

        when(pagos.findByIdForUpdate(idPago)).thenReturn(Optional.of(pago));
        when(membresias.findByPagoId(idPago)).thenReturn(Optional.of(membresia));
        when(membresias.findBySocioUsuarioIdAndEstado(idSocio, "Activa")).thenReturn(Optional.empty());

        PagoService servicio = new PagoService(
                socios, mock(TipoMembresiaRepository.class), pagos, membresias,
                mock(UsuarioRepository.class), mock(PersonalRepository.class), mock(ConfiguracionAppService.class));

        servicio.confirmarPago(idPago);

        assertThat(pago.getEstado()).isEqualTo("Confirmado");
        assertThat(membresia.getEstado()).isEqualTo("Activa");
        assertThat(socio.getEstadoAcceso()).isEqualTo("Activo");
        verify(pagos).save(pago);
        verify(membresias).save(membresia);
        verify(socios).save(socio);
    }
}
