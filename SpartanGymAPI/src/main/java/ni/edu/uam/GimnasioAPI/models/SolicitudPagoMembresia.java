package ni.edu.uam.GimnasioAPI.models;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pago de membresía por transferencia que el socio reporta desde la app. No activa nada por sí
 * solo: queda Pendiente hasta que recepción o administración comprueban la transferencia en el
 * banco y la aprueban; recién ahí se registra el pago y se activa o extiende la membresía.
 */
@Data
@Entity
@Table(name = "solicitudes_pago_membresia")
public class SolicitudPagoMembresia {
    public static final String PENDIENTE = "Pendiente";
    public static final String APROBADA = "Aprobada";
    public static final String RECHAZADA = "Rechazada";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "socio_id", nullable = false)
    private Socio socio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_membresia_id", nullable = false)
    private TipoMembresia tipoMembresia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cuenta_bancaria_id", nullable = false)
    private CuentaBancaria cuentaBancaria;

    /** Precio del plan al momento de la solicitud: es lo que el socio transfirió. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    /** Número de referencia/comprobante que da el banco. */
    @Column(nullable = false, length = 40)
    private String referencia;

    /** Foto opcional del comprobante (data URL validada). */
    @Column(name = "comprobante_url", columnDefinition = "TEXT")
    private String comprobanteUrl;

    @Column(nullable = false, length = 20)
    private String estado = PENDIENTE;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "revisado_por", length = 150)
    private String revisadoPor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;
}
