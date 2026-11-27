package ni.edu.uam.GimnasioAPI.services;

import ni.edu.uam.GimnasioAPI.models.*;
import ni.edu.uam.GimnasioAPI.repositories.*;
import ni.edu.uam.GimnasioAPI.security.AlcanceSucursal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocioSucursalTest {
    @Test
    void recepcionDeA_noListaNiLeeSocioDeB() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        PersonalRepository personal = mock(PersonalRepository.class);
        SocioRepository socios = mock(SocioRepository.class);
        MembresiaSocioRepository membresias = mock(MembresiaSocioRepository.class);
        AlcanceSucursal alcance = new AlcanceSucursal(usuarios, personal, socios);
        SocioService servicio = new SocioService(socios, membresias, usuarios, alcance);

        Sucursal a = new Sucursal(); a.setId(UUID.randomUUID()); a.setNombre("A");
        Sucursal b = new Sucursal(); b.setId(UUID.randomUUID()); b.setNombre("B");
        Rol rol = new Rol(); rol.setNombre("ROLE_RECEPCIONISTA");
        Usuario recepcion = new Usuario(); recepcion.setId(UUID.randomUUID());
        recepcion.setEmail("recepcion@ejemplo.com"); recepcion.setRol(rol);
        Personal ficha = new Personal(); ficha.setSucursal(a);
        when(usuarios.findByEmailIgnoreCase(recepcion.getEmail())).thenReturn(Optional.of(recepcion));
        when(personal.findById(recepcion.getId())).thenReturn(Optional.of(ficha));

        Socio socioA = socio(a);
        Socio socioB = socio(b);
        when(socios.findAll()).thenReturn(List.of(socioA, socioB));
        when(socios.findById(socioB.getUsuarioId())).thenReturn(Optional.of(socioB));
        when(socios.findById(socioA.getUsuarioId())).thenReturn(Optional.of(socioA));
        var auth = new UsernamePasswordAuthenticationToken(recepcion.getEmail(), "", List.of());

        assertThat(servicio.listarSocios(auth)).extracting("id").containsExactly(socioA.getUsuarioId());
        assertThat(alcance.puedeAtenderSocio(auth, socioB.getUsuarioId())).isFalse();
        assertThat(alcance.puedeAtenderSocio(auth, socioA.getUsuarioId())).isTrue();
    }

    private static Socio socio(Sucursal sucursal) {
        Usuario usuario = new Usuario(); usuario.setId(UUID.randomUUID());
        usuario.setEmail(UUID.randomUUID() + "@ejemplo.com");
        Socio socio = new Socio(); socio.setUsuarioId(usuario.getId()); socio.setUsuario(usuario);
        socio.setNombres("Socio"); socio.setApellidos("Prueba"); socio.setSucursal(sucursal);
        return socio;
    }
}
