package com.saasgym.app.network

data class LoginRequest(
    val email: String,
    val password: String
)

data class PasswordRecoveryRequest(
    val email: String
)

data class MessageResponse(
    val mensaje: String? = null
)

data class AuthResponse(
    val id: String? = null,
    val token: String? = null,
    val email: String? = null,
    val rol: String? = null,
    val message: String? = null
)

data class DashboardResponse(
    val nombreCompleto: String? = null,
    val estadoAcceso: String? = null,
    val tipoMembresia: String? = null,
    val fechaVencimiento: String? = null,
    val ultimoPesoKg: Double? = null,
    val notasMedidas: String? = null,
    val objetivoRutina: String? = null,
    val nombreEntrenador: String? = null,
    val totalEjercicios: Int? = null
)

data class PerfilActualResponse(
    val id: String? = null,
    val email: String? = null,
    val rol: String? = null,
    val activo: Boolean? = null,
    val nombres: String? = null,
    val apellidos: String? = null,
    val telefono: String? = null,
    val estadoAcceso: String? = null,
    val tipo: String? = null,
    val especialidad: String? = null,
    val sucursalId: String? = null,
    val sucursal: String? = null,
    val fotoUrl: String? = null
)

data class AppConfigResponse(
    val gymName: String? = null,
    val appName: String? = null,
    val appLogo: String? = null,
    val appLogoNombre: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val currency: String? = null,
    val taxRate: String? = null,
    val theme: String? = null,
    val themeSource: String? = null,
    val accentColor: String? = null,
    val accentHoverColor: String? = null,
    val accentSoftColor: String? = null,
    val logoPrincipal: String? = null,
    val logoPrincipalNombre: String? = null,
    val logoAcceso: String? = null,
    val logoAccesoNombre: String? = null,
    val fondoLogin: String? = null,
    val emailAlerts: Boolean? = null,
    val smsAlerts: Boolean? = null,
    val dailyReports: Boolean? = null,
    val twoFactor: Boolean? = null,
    val sessionTimeout: String? = null
) {
    private fun logoValidoParaNombre(logo: String?, nombreAsociado: String?): String? {
        val nombreActual = gymName?.trim()?.takeIf { it.isNotEmpty() } ?: "SaasGym"
        val logoNombre = nombreAsociado?.trim().orEmpty()
        val coincide = if (logoNombre.isEmpty()) {
            nombreActual.equals("SaasGym", ignoreCase = true)
        } else {
            logoNombre.equals(nombreActual, ignoreCase = true)
        }
        return logo?.trim()?.takeIf { it.isNotEmpty() && coincide }
    }

    fun logoPrincipalActual(): String? = logoValidoParaNombre(logoPrincipal, logoPrincipalNombre)

    fun logoAccesoActual(): String? = logoValidoParaNombre(logoAcceso, logoAccesoNombre)

    fun nombreVisibleApp(): String = appName?.trim()?.takeIf { it.isNotEmpty() }
        ?: gymName?.trim()?.takeIf { it.isNotEmpty() }
        ?: "SaasGym"

    fun logoAppActual(): String? {
        val nombreActual = nombreVisibleApp()
        val nombreLogo = appLogoNombre?.trim().orEmpty()
        val coincide = if (nombreLogo.isEmpty()) {
            nombreActual.equals("SaasGym", ignoreCase = true)
        } else {
            nombreLogo.equals(nombreActual, ignoreCase = true)
        }
        return appLogo?.trim()?.takeIf { it.isNotEmpty() && coincide }
    }
}

data class AsistenciaQrTokenResponse(
    val token: String? = null,
    val sessionId: String? = null,
    val socioId: String? = null,
    val socio: String? = null,
    val generadoEn: String? = null,
    val expiraEn: String? = null,
    val ttlSegundos: Long? = null
)

data class AsistenciaQrRequest(
    val token: String
)

data class AsistenciaQrValidationResponse(
    val id: String? = null,
    val sessionId: String? = null,
    val socioId: String? = null,
    val socio: String? = null,
    val membresia: String? = null,
    val tipoMembresia: String? = null,
    val estadoAcceso: String? = null,
    val fechaVencimiento: String? = null,
    val estado: String? = null,
    val fechaHora: String? = null,
    val mensaje: String? = null
)

data class TipoMembresiaResponse(
    val id: Int? = null,
    val nombre: String? = null,
    val duracionDias: Int? = null,
    val precio: Double? = null,
    val imagenUrl: String? = null,
    val beneficios: String? = null
)

data class ProductoCatalogoResponse(
    val id: String? = null,
    val nombre: String? = null,
    val categoria: String? = null,
    val precio: Double? = null,
    val stock: Int? = null,
    val imagenUrl: String? = null,
    val sucursalId: String? = null,
    val sucursal: String? = null,
    val descripcion: String? = null
)

data class ControlBiometricoResponse(
    val id: String? = null,
    val fechaRegistro: String? = null,
    val pesoKg: Double? = null,
    val medidasNotas: String? = null
)

data class RegistroProgresoRequest(
    val idSocio: String,
    val pesoKg: Double,
    val medidasNotas: String? = null
)

/** El socio reporta la transferencia que hizo; recepción la confirma antes de activar el plan. */
data class SolicitudTransferenciaAppRequest(
    val tipoMembresiaId: Int,
    val cuentaBancariaId: String,
    val referencia: String,
    val metodoPago: String = "Transferencia"
)

data class SolicitudPagoAppResponse(
    val id: String? = null,
    val tipoMembresia: String? = null,
    val monto: Double? = null,
    val banco: String? = null,
    val tipoCuenta: String? = null,
    val numeroCuenta: String? = null,
    val moneda: String? = null,
    val referencia: String? = null,
    val estado: String? = null,
    val motivoRechazo: String? = null,
    val fechaCreacion: String? = null,
    val fechaRevision: String? = null
)

/** Cuenta del gimnasio que administración publica desde la web. */
data class CuentaBancariaApp(
    val id: String,
    val banco: String? = null,
    val tipoCuenta: String? = null,
    val numeroCuenta: String? = null,
    val titular: String? = null,
    val moneda: String? = null
)

data class FacturaMembresiaAppResponse(
    val numeroFactura: String? = null,
    val socio: String? = null,
    val tipoMembresia: String? = null,
    val subtotal: Double? = null,
    val impuesto: Double? = null,
    val total: Double? = null,
    val metodoPago: String? = null,
    val fechaInicio: String? = null,
    val fechaVencimiento: String? = null
)

data class CompraProductoAppRequest(
    val productoId: String,
    val cantidad: Int
)

data class FacturaVentaAppResponse(
    val numeroFactura: String? = null,
    val total: Double? = null,
    val sucursal: String? = null,
    val metodoPago: String? = null,
    val cambio: Double? = null
)

/** Carrito del socio: paga en recepción en efectivo o por transferencia con el código generado. */
data class PedidoSocioRequest(
    val metodoPago: String,
    val referenciaPago: String? = null,
    val detalles: List<CompraProductoAppRequest>
)

data class ConfirmarPedidoRequest(
    val montoRecibido: Double? = null,
    val referenciaPago: String? = null
)

data class DetallePedidoResponse(
    val productoNombre: String? = null,
    val cantidad: Int? = null,
    val precioUnitario: Double? = null,
    val totalLinea: Double? = null
)

data class PedidoResponse(
    val numeroFactura: String? = null,
    val codigoPedido: String? = null,
    val estado: String? = null,
    val metodoPago: String? = null,
    val referenciaPago: String? = null,
    val clienteNombre: String? = null,
    val sucursal: String? = null,
    val subtotal: Double? = null,
    val impuesto: Double? = null,
    val total: Double? = null,
    val cambio: Double? = null,
    val fechaCreacion: String? = null,
    val validadoPor: String? = null,
    val detalles: List<DetallePedidoResponse>? = null
)

/** Venta que registra el entrenador; la API la factura en la sucursal de su perfil. */
data class VentaEntrenadorRequest(
    val clienteEventual: Boolean,
    val socioId: String? = null,
    val clienteNombre: String? = null,
    val metodoPago: String,
    val montoRecibido: Double? = null,
    val detalles: List<CompraProductoAppRequest>
)

data class NotificacionResponse(
    val id: String? = null,
    val tipo: String? = null,
    val titulo: String? = null,
    val mensaje: String? = null,
    val leida: Boolean? = null,
    val fechaCreacion: String? = null
)

data class EjercicioRutinaResponse(
    val ejercicioId: Long? = null,
    val ejercicio: String? = null,
    val grupoMuscular: String? = null,
    val grupoMuscularId: Int? = null,
    val gifUrl: String? = null,
    val instrucciones: List<String>? = null,
    val tipoEjercicio: String? = null,
    val diaProgramado: String? = null,
    val diaSemana: Int? = null,
    val series: Int? = null,
    val repeticiones: Int? = null,
    val pesoSugeridoKg: Double? = null,
    val velocidadNivel: Double? = null,
    val inclinacion: Double? = null,
    val duracionSegundos: Int? = null,
    val distanciaMetros: Double? = null,
    val tiempoDescansoSegundos: Int? = null,
    val notas: String? = null,
    val orden: Int? = null,
    val completadoEstaSemana: Boolean? = null
)

data class RutinaResumenResponse(
    val imagenUrl: String? = null,
    val id: String? = null,
    val socioId: String? = null,
    val socio: String? = null,
    val entrenador: String? = null,
    val fechaAsignacion: String? = null,
    val nombre: String? = null,
    val tipoRutina: String? = null,
    val generoObjetivo: String? = null,
    val esGlobal: Boolean? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val objetivo: String? = null,
    val notas: String? = null,
    val progresoSemana: Double? = null,
    val ejercicios: List<EjercicioRutinaResponse>? = emptyList()
)

data class SocioResumenResponse(
    val id: String? = null,
    val nombres: String? = null,
    val apellidos: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val estadoAcceso: String? = null,
    val tipoMembresia: String? = null,
    val sucursalId: String? = null,
    val sucursal: String? = null
)

data class GrupoMuscularResponse(
    val id: Int? = null,
    val nombre: String? = null
)

data class EjercicioCatalogoResponse(
    val id: Long? = null,
    val nombre: String? = null,
    val grupoMuscular: GrupoMuscularResponse? = null,
    val nombreOriginal: String? = null,
    val alias: List<String>? = null,
    val zona: String? = null,
    val musculoPrincipal: String? = null,
    val musculosSecundarios: List<String>? = null,
    val equipo: String? = null,
    val dificultad: String? = null,
    val gifUrl: String? = null
)

data class RutinaDetalleRequest(
    val idEjercicio: Long,
    val series: Int,
    val repeticiones: Int,
    val tipoEjercicio: String,
    val diaProgramado: String? = null,
    val pesoSugeridoKg: Double? = null,
    val tiempoDescansoSegundos: Int? = null,
    val notas: String? = null,
    val orden: Int
)

data class RutinaRequest(
    val idSocio: String? = null,
    val idEntrenador: String,
    val esGlobal: Boolean = false,
    val nombre: String,
    val tipoRutina: String,
    val generoObjetivo: String,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val objetivo: String,
    val notas: String? = null,
    val detalles: List<RutinaDetalleRequest>
)

data class MarcarEjercicioRequest(
    val completado: Boolean,
    val fecha: String? = null
)

data class EntrenadorDashboardResponse(
    val clientesAsignados: Int? = null,
    val rutinasCreadas: Int? = null,
    val evaluacionesPendientes: Int? = null,
    val rutinasRecientes: List<RutinaResumenResponse>? = emptyList()
)

data class EntrenadorClienteResponse(
    val id: String? = null,
    val nombres: String? = null,
    val apellidos: String? = null,
    val telefono: String? = null,
    val estadoAcceso: String? = null,
    val email: String? = null,
    val fotoUrl: String? = null
)

data class EntrenadorPerfilResponse(
    val id: String? = null,
    val fotoUrl: String? = null,
    val email: String? = null,
    val rol: String? = null,
    val activo: Boolean? = null,
    val nombres: String? = null,
    val apellidos: String? = null,
    val telefono: String? = null,
    val especialidad: String? = null,
    val rutinasCreadas: Int? = null,
    val sucursalId: String? = null,
    val sucursal: String? = null
)

data class AsistenciaHistorialResponse(
    val fechaHora: String? = null, val estado: String? = null, val mensaje: String? = null
)

data class ActualizarPerfilEntrenadorRequest(
    val nombres: String, val apellidos: String? = null, val telefono: String, val especialidad: String
)

data class CrearGrupoRequest(val nombre: String)

data class CrearEjercicioRequest(val nombre: String, val grupoMuscular: GrupoMuscularResponse)

data class PagoSocioResponse(
    val id: String? = null,
    val socioId: String? = null,
    val socio: String? = null,
    val monto: Double? = null,
    val metodoPago: String? = null,
    val fechaTransaccion: String? = null
)

data class ActualizarFotoRequest(
    val fotoUrl: String?
)

data class ActualizarFotoResponse(
    val fotoUrl: String? = null
)
