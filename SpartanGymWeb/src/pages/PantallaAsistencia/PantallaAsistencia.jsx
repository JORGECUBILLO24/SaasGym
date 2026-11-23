import { useEffect, useState } from 'react';
import { CheckCircle2, Loader2, Maximize, RefreshCw, Unplug, XCircle } from 'lucide-react';
import { obtenerNombreGimnasio, useConfiguracionApp } from '../../utils/configuracionApp';
import { useLogosApp } from '../../utils/logosApp';
import { abrirCanal, diasHasta, SEGUNDOS_RESULTADO } from './canalAsistencia';
import './PantallaAsistencia.css';


const useAhora = (cadaMs) => {
  const [ahora, setAhora] = useState(() => Date.now());
  useEffect(() => {
    const t = window.setInterval(() => setAhora(Date.now()), cadaMs);
    return () => window.clearInterval(t);
  }, [cadaMs]);
  return ahora;
};

const Reloj = () => {
  const ahora = useAhora(10_000);
  return (
    <span className="pa-reloj">
      {new Date(ahora).toLocaleTimeString('es-NI', { hour: '2-digit', minute: '2-digit' })}
    </span>
  );
};

const formatearFechaCorta = (fecha) => {
  if (!fecha) return '';
  const [anio, mes, dia] = String(fecha).slice(0, 10).split('-').map(Number);
  if (!anio || !mes || !dia) return String(fecha);
  return new Date(anio, mes - 1, dia).toLocaleDateString('es-NI', { day: 'numeric', month: 'long', year: 'numeric' });
};

const textoDias = (dias) => {
  if (dias === null) return null;
  if (dias < 0) return 'Vencida';
  if (dias === 0) return 'Vence hoy';
  return dias === 1 ? '1 día' : `${dias} días`;
};

const primerNombre = (nombre) => String(nombre || '').trim().split(/\s+/)[0] || '';

const Resultado = ({ acceso }) => {
  const dias = diasHasta(acceso.vencimiento);
  const quedanPocos = dias !== null && dias <= 5;
  const clase = acceso.permitido ? 'pa-resultado pa-resultado--ok' : 'pa-resultado pa-resultado--no';

  return (
    <main className={clase} role="status" aria-live="assertive">
      {acceso.permitido
        ? <CheckCircle2 className="pa-icono-resultado" strokeWidth={1.5} aria-hidden="true" />
        : <XCircle className="pa-icono-resultado" strokeWidth={1.5} aria-hidden="true" />}

      {acceso.permitido ? (
        <>
          <h1 className="pa-saludo">¡Hola, {primerNombre(acceso.nombre)}!</h1>
          <p className="pa-subtitulo">Tu asistencia quedó registrada. ¡Buen entrenamiento!</p>
          <dl className="pa-fichas">
            {acceso.plan && (
              <div className="pa-ficha"><dt>Plan</dt><dd>{acceso.plan}</dd></div>
            )}
            {acceso.vencimiento && (
              <div className="pa-ficha"><dt>Vence</dt><dd>{formatearFechaCorta(acceso.vencimiento)}</dd></div>
            )}
            {dias !== null && (
              <div className={quedanPocos ? 'pa-ficha pa-ficha--alerta' : 'pa-ficha'}>
                <dt>{quedanPocos ? 'Renueva pronto' : 'Te quedan'}</dt>
                <dd>{textoDias(dias)}</dd>
              </div>
            )}
          </dl>
        </>
      ) : (
        <>
          <h1 className="pa-saludo">No pudimos registrar tu entrada</h1>
          <p className="pa-subtitulo">
            {acceso.nombre ? `${acceso.nombre}: ` : ''}
            {acceso.mensaje || 'Tu acceso no está habilitado.'} Acércate a recepción.
          </p>
        </>
      )}

      <span className="pa-progreso" style={{ animationDuration: `${SEGUNDOS_RESULTADO}s` }} aria-hidden="true" />
    </main>
  );
};

const EsperaQr = ({ estado }) => {
  const ahora = useAhora(1000);
  const restantes = estado.expiraMs ? Math.max(0, Math.ceil((estado.expiraMs - ahora) / 1000)) : 0;
  const total = Math.max(1, Number(estado.ttlSegundos) || restantes);

  const listo = estado.estadoQr === 'pendiente' && estado.qrImagen && restantes > 0;
  const porcentaje = total ? Math.min(100, (restantes / total) * 100) : 0;

  return (
    <main className="pa-cuerpo">
      <section>
        <p className="pa-etiqueta">Registro de asistencia</p>
        <h1 className="pa-titulo">Escanea y entra</h1>
        <ol className="pa-pasos">
          <li className="pa-paso"><span className="pa-paso-num">1</span>Abre la app del gimnasio</li>
          <li className="pa-paso"><span className="pa-paso-num">2</span>Toca «Escanear QR»</li>
          <li className="pa-paso"><span className="pa-paso-num">3</span>Apunta la cámara a este código</li>
        </ol>
        <p className="pa-nota">¿Sin la app o sin batería? Recepción te registra con tu nombre.</p>
      </section>

      <section className="pa-qr-columna" aria-label="Código QR de asistencia">
        <div className="pa-qr">
          {estado.qrImagen && <img src={estado.qrImagen} alt="Código QR para registrar la asistencia" />}
          {!listo && (
            <div className="pa-qr-velo">
              {estado.estadoQr === 'error'
                ? <>No se pudo generar el código.<span>Recepción lo está revisando.</span></>
                : <><Loader2 size={44} className="pa-girar" aria-hidden="true" />Preparando un código nuevo…</>}
            </div>
          )}
        </div>
        <div className="pa-tiempo" aria-hidden={!listo}>
          <div className="pa-tiempo-barra">
            <div className="pa-tiempo-relleno" style={{ width: `${listo ? porcentaje : 0}%` }} />
          </div>
          <div className="pa-tiempo-texto">
            <span>El código cambia solo</span>
            <span>{listo ? `${restantes} s` : ''}</span>
          </div>
        </div>
      </section>
    </main>
  );
};

/**
 * Pantalla de asistencia (segundo monitor de recepción, mirando a la entrada).
 * Recepción la abre desde Check-in con «Pantalla de asistencia» y la arrastra al otro monitor.
 */
const PantallaAsistencia = () => {
  const configuracion = useConfiguracionApp();
  const nombreGimnasio = obtenerNombreGimnasio(configuracion);
  const { principal: logo } = useLogosApp();
  const [estado, setEstado] = useState(null);
  // Recepción manda el último acceso solo durante unos segundos; luego vuelve a null.
  const accesoVisible = estado?.acceso ?? null;

  useEffect(() => {
    document.title = `Asistencia · ${nombreGimnasio}`;
  }, [nombreGimnasio]);

  useEffect(() => {
    const canal = abrirCanal();
    if (!canal) return undefined;
    canal.onmessage = (evento) => {
      if (evento.data?.tipo === 'estado') setEstado(evento.data.datos);
    };
    // Al abrir se pide el estado actual, y se reintenta por si recepción aún no estaba abierta.
    const pedir = () => canal.postMessage({ tipo: 'hola' });
    pedir();
    const reintento = window.setInterval(pedir, 5000);
    const despedirse = () => canal.postMessage({ tipo: 'adios' });
    window.addEventListener('pagehide', despedirse);
    return () => {
      window.clearInterval(reintento);
      window.removeEventListener('pagehide', despedirse);
      despedirse();
      canal.close();
    };
  }, []);

  const pantallaCompleta = () => {
    void document.documentElement.requestFullscreen?.().catch(() => undefined);
  };

  const iniciales = nombreGimnasio.split(/\s+/).map((p) => p[0]).join('').slice(0, 2).toUpperCase();

  return (
    <div className="pa">
      <span className="pa-halo pa-halo--a" aria-hidden="true" />
      <span className="pa-halo pa-halo--b" aria-hidden="true" />

      <header className="pa-encabezado">
        {logo
          ? <img src={logo} alt="" className="pa-logo" />
          : <span className="pa-iniciales" aria-hidden="true">{iniciales}</span>}
        <span className="pa-nombre">{nombreGimnasio}</span>
        <div className="pa-derecha">
          {!estado && <span className="pa-aviso"><Unplug size={18} aria-hidden="true" /> Esperando la recepción…</span>}
          <Reloj />
          <button type="button" onClick={pantallaCompleta} className="pa-boton-icono" title="Pantalla completa" aria-label="Pantalla completa">
            <Maximize size={20} />
          </button>
        </div>
      </header>

      {accesoVisible ? (
        <Resultado key={accesoVisible.pulso} acceso={accesoVisible} />
      ) : estado ? (
        <EsperaQr estado={estado} />
      ) : (
        <main className="pa-espera">
          <RefreshCw size={56} className="pa-girar" aria-hidden="true" />
          <strong>Conectando con recepción</strong>
          <span>Deja abierta la pantalla de Check-in en este mismo equipo.</span>
        </main>
      )}
    </div>
  );
};

export default PantallaAsistencia;
