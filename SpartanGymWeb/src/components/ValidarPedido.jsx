import { useState } from 'react';
import { CheckCircle2, PackageCheck, RefreshCw, Search, XCircle } from 'lucide-react';
import { ventasApi } from '../services/api';

const COLOR_ESTADO = {
  Pendiente: 'is-pending',
  Completada: 'is-done',
  Cancelada: 'is-cancelled',
};

/**
 * Recepción o administración buscan el código que el socio generó en la app, revisan el pedido
 * y lo completan al recibir el pago (efectivo o transferencia), o lo cancelan.
 */
const ValidarPedido = ({ formatearMonto, onCambio }) => {
  const [codigo, setCodigo] = useState('');
  const [pedido, setPedido] = useState(null);
  const [recibido, setRecibido] = useState('');
  const [referencia, setReferencia] = useState('');
  const [ocupado, setOcupado] = useState('');
  const [mensaje, setMensaje] = useState(null);

  const buscar = async (evento) => {
    evento?.preventDefault();
    if (!codigo.trim()) return;
    setOcupado('buscar');
    setMensaje(null);
    try {
      const encontrado = await ventasApi.buscarPedido(codigo);
      setPedido(encontrado);
      setRecibido('');
      setReferencia(encontrado.referenciaPago || '');
    } catch (error) {
      setPedido(null);
      setMensaje({ tipo: 'error', texto: error.message || 'No se encontró el pedido.' });
    } finally {
      setOcupado('');
    }
  };

  const esEfectivo = pedido?.metodoPago === 'Efectivo';
  const recibidoNumero = Number(recibido || 0);
  const cambio = esEfectivo && recibido ? recibidoNumero - Number(pedido.total || 0) : null;

  const confirmar = async () => {
    setOcupado('confirmar');
    setMensaje(null);
    try {
      const listo = await ventasApi.confirmarPedido(pedido.codigoPedido, {
        montoRecibido: esEfectivo && recibido ? recibidoNumero : null,
        referenciaPago: !esEfectivo ? referencia.trim() || null : null,
      });
      setPedido(listo);
      setMensaje({ tipo: 'exito', texto: `Pedido ${listo.codigoPedido} completado. Factura ${listo.numeroFactura}. Entrega los productos.` });
      onCambio?.();
    } catch (error) {
      setMensaje({ tipo: 'error', texto: error.message || 'No se pudo completar el pedido.' });
    } finally {
      setOcupado('');
    }
  };

  const cancelar = async () => {
    if (!window.confirm(`¿Cancelar el pedido ${pedido.codigoPedido}? Los productos vuelven al inventario.`)) return;
    setOcupado('cancelar');
    setMensaje(null);
    try {
      const cancelado = await ventasApi.cancelarPedido(pedido.codigoPedido);
      setPedido(cancelado);
      setMensaje({ tipo: 'exito', texto: `Pedido ${cancelado.codigoPedido} cancelado. El stock se devolvió al inventario.` });
      onCambio?.();
    } catch (error) {
      setMensaje({ tipo: 'error', texto: error.message || 'No se pudo cancelar el pedido.' });
    } finally {
      setOcupado('');
    }
  };

  return (
    <section className="order-check" aria-labelledby="order-check-title">
      <div className="order-check-head">
        <PackageCheck size={18} className="text-red-400" aria-hidden="true" />
        <div>
          <h2 id="order-check-title">Validar pedido de la app</h2>
          <p>El socio te muestra su código. Cobra y entrega, o cancela si no se retira.</p>
        </div>
      </div>

      <form onSubmit={buscar} className="order-check-search">
        <label className="sales-search-field">
          <Search size={15} aria-hidden="true" />
          <input
            value={codigo}
            onChange={(evento) => setCodigo(evento.target.value.toUpperCase())}
            placeholder="Código, ej. P-7K3M9Q"
            aria-label="Código del pedido"
            autoComplete="off"
          />
        </label>
        <button type="submit" disabled={!codigo.trim() || ocupado === 'buscar'} className="sales-refresh-button order-check-button">
          {ocupado === 'buscar' ? <RefreshCw size={14} className="animate-spin" /> : <Search size={14} />} Buscar
        </button>
      </form>

      {mensaje && (
        <p role={mensaje.tipo === 'error' ? 'alert' : 'status'} className={`sales-notice ${mensaje.tipo === 'exito' ? 'is-success' : 'is-error'}`}>
          {mensaje.tipo === 'exito' ? <CheckCircle2 size={15} /> : <XCircle size={15} />} {mensaje.texto}
        </p>
      )}

      {pedido && (
        <div className="order-check-card">
          <div className="order-check-row">
            <strong className="order-check-code">{pedido.codigoPedido}</strong>
            <span className={`order-check-state ${COLOR_ESTADO[pedido.estado] || ''}`}>{pedido.estado}</span>
          </div>
          <p className="order-check-meta">
            {pedido.clienteNombre} · {pedido.sucursal} · Paga con <strong>{pedido.metodoPago}</strong>
            {pedido.referenciaPago ? ` · Ref. ${pedido.referenciaPago}` : ''}
          </p>
          <ul className="order-check-items">
            {(pedido.detalles || []).map((detalle) => (
              <li key={detalle.id}>
                <span>{detalle.cantidad} × {detalle.productoNombre}</span>
                <span>{formatearMonto(detalle.totalLinea)}</span>
              </li>
            ))}
          </ul>
          <div className="order-check-total"><span>Total</span><strong>{formatearMonto(pedido.total)}</strong></div>

          {pedido.estado === 'Pendiente' ? (
            <>
              {esEfectivo ? (
                <label className="sales-field-wrap">
                  <span className="sales-field-label">Efectivo recibido · opcional, para calcular el cambio</span>
                  <input type="number" min="0" step="0.01" value={recibido} onChange={(evento) => setRecibido(evento.target.value)} placeholder="0.00" className="sales-field" />
                </label>
              ) : (
                <label className="sales-field-wrap">
                  <span className="sales-field-label">Referencia de la transferencia · revisa que llegó antes de entregar</span>
                  <input value={referencia} onChange={(evento) => setReferencia(evento.target.value)} placeholder="Número o comprobante" className="sales-field" />
                </label>
              )}
              {cambio !== null && (
                <p className={`order-check-change ${cambio < 0 ? 'is-short' : ''}`}>
                  {cambio < 0 ? `Faltan ${formatearMonto(-cambio)}` : `Cambio: ${formatearMonto(cambio)}`}
                </p>
              )}
              <div className="order-check-actions">
                <button type="button" onClick={cancelar} disabled={Boolean(ocupado)} className="order-check-cancel">Cancelar pedido</button>
                <button type="button" onClick={confirmar} disabled={Boolean(ocupado) || (cambio !== null && cambio < 0)} className="sales-submit-button order-check-confirm">
                  {ocupado === 'confirmar' ? <RefreshCw size={15} className="animate-spin" /> : <CheckCircle2 size={15} />}
                  {esEfectivo ? 'Recibí el efectivo · completar' : 'Transferencia recibida · completar'}
                </button>
              </div>
            </>
          ) : (
            <p className="order-check-meta">
              {pedido.estado === 'Completada' ? 'Ya se cobró y entregó' : 'Cancelado'}
              {pedido.validadoPor ? ` por ${pedido.validadoPor}` : ''}
              {pedido.numeroFactura ? ` · Factura ${pedido.numeroFactura}` : ''}
            </p>
          )}
        </div>
      )}
    </section>
  );
};

export default ValidarPedido;
