package ni.edu.uam.GimnasioAPI.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** Cuenta del gimnasio a la que los socios transfieren el pago de su membresía. */
@Data
@Entity
@Table(name = "cuentas_bancarias")
public class CuentaBancaria {
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "gimnasio_id", nullable = false, updatable = false)
    private java.util.UUID gimnasioId;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String banco;

    /** Ahorro o Corriente. */
    @Column(name = "tipo_cuenta", nullable = false, length = 20)
    private String tipoCuenta;

    @Column(name = "numero_cuenta", nullable = false, length = 40)
    private String numeroCuenta;

    @Column(nullable = false, length = 120)
    private String titular;

    /** NIO o USD. */
    @Column(nullable = false, length = 10)
    private String moneda;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
