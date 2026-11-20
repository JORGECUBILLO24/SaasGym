package ni.edu.uam.GimnasioAPI.security;

import ni.edu.uam.GimnasioAPI.exceptions.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitadorIntentosTest {

    private final AtomicLong ahora = new AtomicLong(1_000_000L);
    private final Clock reloj = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(ahora.get()); }
    };
    private final LimitadorIntentos.Regla regla = new LimitadorIntentos.Regla(3, 60);

    @Test
    void alSuperarElMaximoPideEsperar_yAlPasarLaVentanaVuelveAPermitir() {
        LimitadorIntentos limitador = new LimitadorIntentos(reloj);
        limitador.registrar("login:a", regla);
        limitador.registrar("login:a", regla);
        limitador.registrar("login:a", regla);

        assertThatThrownBy(() -> limitador.registrar("login:a", regla))
                .isInstanceOf(DemasiadosIntentosException.class)
                .satisfies(e -> assertThat(((DemasiadosIntentosException) e).getSegundosEspera()).isBetween(1L, 60L));

        ahora.addAndGet(61_000);
        assertThatCode(() -> limitador.registrar("login:a", regla)).doesNotThrowAnyException();
    }

    @Test
    void cadaClaveCuentaPorSeparado_yReiniciarBorraLosFallos() {
        LimitadorIntentos limitador = new LimitadorIntentos(reloj);
        for (int i = 0; i < 3; i++) {
            limitador.registrar("login:a", regla);
        }
        assertThatCode(() -> limitador.verificar("login:b", regla)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limitador.verificar("login:a", regla)).isInstanceOf(DemasiadosIntentosException.class);

        limitador.reiniciar("login:a");
        assertThatCode(() -> limitador.verificar("login:a", regla)).doesNotThrowAnyException();
    }

    @Test
    void deshabilitadoNoLimita() {
        LimitadorIntentos limitador = new LimitadorIntentos(reloj);
        limitador.setHabilitado(false);
        for (int i = 0; i < 10; i++) {
            limitador.registrar("x", regla);
        }
    }

    @Test
    void laIpEsLaUltimaDeXForwardedFor_queNoPuedeInventarElCliente() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.1.1.1, 203.0.113.7");
        assertThat(LimitadorIntentos.ipCliente(request)).isEqualTo("203.0.113.7");

        MockHttpServletRequest directo = new MockHttpServletRequest();
        directo.setRemoteAddr("198.51.100.2");
        assertThat(LimitadorIntentos.ipCliente(directo)).isEqualTo("198.51.100.2");
    }
}
