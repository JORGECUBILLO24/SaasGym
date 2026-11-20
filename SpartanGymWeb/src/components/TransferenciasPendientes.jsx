import { useCallback, useEffect, useState } from 'react';
import { CheckCircle2, Landmark, RefreshCw, XCircle } from 'lucide-react';
import { transferenciasApi } from '../services/api';
import { formatearMoneda, useConfiguracionApp } from '../utils/configuracionApp';

const formatearFecha = (valor) => {
  if (!valor) return '';
  const fecha = new Date(valor);
  return Number.isNaN(fecha.getTime()) ? String(valor) : fecha.toLocaleString('es-NI', { dateStyle: 'medium', timeStyle: 'short' });
};

/**
 * Pagos de membresía por transferencia que reportaron los socios desde la app. Recepción o
 * administración revisa en el banco que el dinero llegó y aprueba (se activa la membresía) o
 * rechaza con un motivo que el socio ve en la app.
 */
const TransferenciasPendientes = ({ onAprobada }) => {
  const configuracion = useConfiguracionApp();
  const [solicitudes, setSolicitudes] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [procesandoId, setProcesandoId] = useState('');
  const [mensaje, setMensaje] = useState(null);
  const [comprobante, setComprobante] = useState('');

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      setSolicitudes(await transferenciasApi.listar('Pendiente'));
    } catch (error) {
      setMensaje({ tipo: 'error', texto: error.message || 'No se pudieron cargar las transferencias.' });
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    Promise.resolve().then(cargar);
  }, [cargar]);

  const monto = (solicitud) => formatearMoneda(solicitud.monto, configuracion?.currency);

  const aprobar = async (solicitud) => {
    if (!window.confirm(`¿Confirmas que llegó la transferencia ${solicitud.referencia} por ${monto(solicitud)} a ${solicitud.banco}?`)) return;
    setProcesandoId(solicitud.id);
    setMensaje(null);
    try {
      await transferenciasApi.aprobar(solicitud.id);
      setMensaje({ tipo: 'exito', texto: `Membresía de ${solicitud.socio} activada.` });
      await cargar();
      onAprobada?.();
    } catch (error) {
      setMensaje({ tipo: 'error', texto: error.message || 'No se pudo aprobar.' });
    } finally {
      setProcesandoId('');
    }
  };

  const rechazar = async (solicitud) => {
    const motivo = window.prompt('¿Por qué no se confirma? El socio verá este mensaje en la app.', 'No encontramos la transferencia en el banco');
    if (motivo === null) return;
    setProcesandoId(solicitud.id);
    setMensaje(null);
    try {
      await transferenciasApi.rechazar(solicitud.id, motivo);
      setMensaje({ tipo: 'exito', texto: `Solicitud de ${solicitud.socio} rechazada.` });
      await cargar();
    } catch (error) {
      setMensaje({ tipo: 'error', texto: error.message || 'No se pudo rechazar.' });
    } finally {
      setProcesandoId('');
    }
  };

  return (
    <section className="rounded-2xl border border-white/10 bg-[#111111] p-5" aria-labelledby="transferencias-titulo">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="rounded-xl bg-red-600/10 p-2 text-red-500"><Landmark size={18} /></span>
          <div>
            <h2 id="transferencias-titulo" className="text-base font-bold text-white">Transferencias por confirmar</h2>
            <p className="text-xs text-gray-400">Revisa en el banco que el dinero llegó antes de aprobar.</p>
          </div>
        </div>
        <button type="button" onClick={cargar} disabled={cargando} className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-xs font-semibold text-gray-300 disabled:opacity-60">
          <RefreshCw size={14} className={cargando ? 'animate-spin' : ''} /> Actualizar
        </button>
      </div>

      {mensaje && <p role="status" className={`mb-3 text-xs font-semibold ${mensaje.tipo === 'exito' ? 'text-green-500' : 'text-red-500'}`}>{mensaje.texto}</p>}

      <ul className="divide-y divide-white/10">
        {solicitudes.map((solicitud) => (
          <li key={solicitud.id} className="flex flex-wrap items-start justify-between gap-3 py-3">
            <div className="min-w-0 space-y-0.5">
              <p className="text-sm font-bold text-white">{solicitud.socio} <span className="font-normal text-gray-400">· {solicitud.tipoMembresia}</span></p>
              <p className="text-xs text-gray-300">{monto(solicitud)} a {solicitud.banco} {solicitud.tipoCuenta} {solicitud.moneda} <span className="font-mono">{solicitud.numeroCuenta}</span></p>
              <p className="text-xs text-gray-400">Referencia <span className="font-mono font-semibold text-white">{solicitud.referencia}</span> · {formatearFecha(solicitud.fechaCreacion)}{solicitud.sucursal ? ` · ${solicitud.sucursal}` : ''}</p>
              {solicitud.comprobanteUrl && (
                <button type="button" onClick={() => setComprobante(solicitud.comprobanteUrl)} className="text-xs font-semibold text-red-500 underline">Ver comprobante</button>
              )}
            </div>
            <div className="flex gap-2">
              <button type="button" disabled={procesandoId === solicitud.id} onClick={() => aprobar(solicitud)} className="inline-flex items-center gap-1.5 rounded-xl bg-green-600 px-3 py-2 text-xs font-bold text-white disabled:opacity-60">
                <CheckCircle2 size={14} /> Aprobar
              </button>
              <button type="button" disabled={procesandoId === solicitud.id} onClick={() => rechazar(solicitud)} className="inline-flex items-center gap-1.5 rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-xs font-bold text-gray-300 disabled:opacity-60">
                <XCircle size={14} /> Rechazar
              </button>
            </div>
          </li>
        ))}
        {!solicitudes.length && <li className="py-4 text-sm text-gray-500">{cargando ? 'Cargando…' : 'No hay transferencias pendientes.'}</li>}
      </ul>

      {comprobante && (
        <div role="dialog" aria-modal="true" aria-label="Comprobante de transferencia" className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4" onClick={() => setComprobante('')}>
          <img src={comprobante} alt="Comprobante de transferencia" className="max-h-[85vh] max-w-full rounded-xl bg-white object-contain" />
        </div>
      )}
    </section>
  );
};

export default TransferenciasPendientes;
