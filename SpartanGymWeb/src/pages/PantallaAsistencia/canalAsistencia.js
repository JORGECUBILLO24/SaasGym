/**
 * Comunicación recepción → pantalla de asistencia.
 *
 * La pantalla de asistencia es un segundo monitor que mira hacia la entrada: muestra el QR
 * grande para que el socio lo escanee con su app y, al registrarse, le da la bienvenida.
 * Usa BroadcastChannel, que funciona al instante entre ventanas del mismo navegador en el
 * mismo equipo. La ventana de recepción es la única que habla con la API; la pantalla solo
 * dibuja lo que recepción le envía.
 *
 * Mensajes:
 *   { tipo: 'hola' }                    pantalla → recepción (pide el estado actual)
 *   { tipo: 'adios' }                   pantalla → recepción (se cerró la ventana)
 *   { tipo: 'estado', datos: Estado }   recepción → pantalla
 *
 * Estado = {
 *   estadoQr: 'cargando' | 'pendiente' | 'validado' | 'expirado' | 'error',
 *   qrImagen: string (data URL), expiraMs: number, ttlSegundos: number,
 *   acceso: null | { nombre, plan, vencimiento, permitido, mensaje, pulso }
 *     (recepción lo manda durante SEGUNDOS_RESULTADO y luego lo pone en null),
 * }
 */

export const NOMBRE_CANAL = 'gimnasio:pantalla-asistencia';
export const RUTA_PANTALLA = '/pantalla-asistencia';

/** Cuánto tiempo se muestra la bienvenida (o el rechazo) antes de volver al QR. */
export const SEGUNDOS_RESULTADO = 7;

export const abrirCanal = () =>
  typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel(NOMBRE_CANAL);

/** Abre (o trae al frente) la ventana de la pantalla de asistencia. */
export const abrirPantallaAsistencia = () => {
  const ventana = window.open(RUTA_PANTALLA, 'gimnasio-pantalla-asistencia', 'popup,width=1280,height=800');
  ventana?.focus();
  return ventana;
};

/** Días que faltan para la fecha de vencimiento (negativo si ya pasó, null si no hay fecha). */
export const diasHasta = (fecha, hoy = new Date()) => {
  if (!fecha) return null;
  const texto = String(fecha).slice(0, 10);
  const [anio, mes, dia] = texto.split('-').map(Number);
  if (!anio || !mes || !dia) return null;
  const objetivo = Date.UTC(anio, mes - 1, dia);
  const base = Date.UTC(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  return Math.round((objetivo - base) / 86_400_000);
};
