import { useCallback, useEffect, useState } from 'react';
import { Banknote, Pencil, Plus, Save, Trash2, X } from 'lucide-react';
import { cuentasBancariasApi } from '../services/api';

const CUENTA_VACIA = { banco: '', tipoCuenta: 'Ahorro', numeroCuenta: '', titular: '', moneda: 'NIO', activa: true };

/**
 * Cuentas del gimnasio para recibir transferencias. Lo que se guarda aquí es lo que el socio ve
 * en la app al pagar su membresía.
 */
const CuentasBancarias = () => {
  const [cuentas, setCuentas] = useState([]);
  const [formulario, setFormulario] = useState(CUENTA_VACIA);
  const [editandoId, setEditandoId] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState(null);

  const cargar = useCallback(async () => {
    try {
      setCuentas(await cuentasBancariasApi.listar(true));
    } catch (error) {
      setMensaje({ tipo: 'error', texto: error.message || 'No se pudieron cargar las cuentas.' });
    }
  }, []);

  useEffect(() => {
    Promise.resolve().then(cargar);
  }, [cargar]);

  const cambiar = (evento) => {
    const { name, value, type, checked } = evento.target;
    setFormulario((actual) => ({ ...actual, [name]: type === 'checkbox' ? checked : value }));
  };

  const limpiar = () => {
    setFormulario(CUENTA_VACIA);
    setEditandoId('');
  };

  const guardar = async (evento) => {
    evento.preventDefault();
    setGuardando(true);
    setMensaje(null);
    try {
      if (editandoId) {
        await cuentasBancariasApi.actualizar(editandoId, formulario);
      } else {
        await cuentasBancariasApi.crear(formulario);
      }
      setMensaje({ tipo: 'exito', texto: editandoId ? 'Cuenta actualizada.' : 'Cuenta agregada. Ya aparece en la app.' });
      limpiar();
      await cargar();
    } catch (error) {
      setMensaje({ tipo: 'error', texto: error.message || 'No se pudo guardar la cuenta.' });
    } finally {
      setGuardando(false);
    }
  };

  const editar = (cuenta) => {
    setEditandoId(cuenta.id);
    setFormulario({
      banco: cuenta.banco,
      tipoCuenta: cuenta.tipoCuenta,
      numeroCuenta: cuenta.numeroCuenta,
      titular: cuenta.titular,
      moneda: cuenta.moneda,
      activa: cuenta.activa,
    });
  };

  const desactivar = async (cuenta) => {
    if (!window.confirm(`¿Ocultar la cuenta ${cuenta.banco} ${cuenta.numeroCuenta} de la app?`)) return;
    try {
      await cuentasBancariasApi.desactivar(cuenta.id);
      await cargar();
    } catch (error) {
      setMensaje({ tipo: 'error', texto: error.message || 'No se pudo desactivar la cuenta.' });
    }
  };

  return (
    <section className="rounded-2xl border border-white/10 bg-[#111111] p-5" aria-labelledby="cuentas-bancarias-titulo">
      <div className="mb-4 flex items-center gap-3">
        <span className="rounded-xl bg-red-600/10 p-2 text-red-500"><Banknote size={18} /></span>
        <div>
          <h2 id="cuentas-bancarias-titulo" className="text-base font-bold text-white">Cuentas para transferencias</h2>
          <p className="text-xs text-gray-400">Los socios eligen una de estas cuentas en la app para pagar su membresía.</p>
        </div>
      </div>

      <form onSubmit={guardar} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        <label className="block text-xs font-semibold text-gray-400">Banco
          <input name="banco" required maxLength={80} value={formulario.banco} onChange={cambiar} placeholder="Ej. BAC, Banpro, Lafise" className="mt-1 w-full rounded-xl border border-white/10 bg-[#090909] px-3 py-2.5 text-sm text-white outline-none focus:border-red-600" />
        </label>
        <label className="block text-xs font-semibold text-gray-400">Tipo de cuenta
          <select name="tipoCuenta" value={formulario.tipoCuenta} onChange={cambiar} className="mt-1 w-full rounded-xl border border-white/10 bg-[#090909] px-3 py-2.5 text-sm text-white outline-none focus:border-red-600">
            <option value="Ahorro">Ahorro</option>
            <option value="Corriente">Corriente</option>
          </select>
        </label>
        <label className="block text-xs font-semibold text-gray-400">Moneda
          <select name="moneda" value={formulario.moneda} onChange={cambiar} className="mt-1 w-full rounded-xl border border-white/10 bg-[#090909] px-3 py-2.5 text-sm text-white outline-none focus:border-red-600">
            <option value="NIO">Córdobas (NIO)</option>
            <option value="USD">Dólares (USD)</option>
          </select>
        </label>
        <label className="block text-xs font-semibold text-gray-400">Número de cuenta
          <input name="numeroCuenta" required inputMode="numeric" maxLength={40} pattern="[0-9][0-9 \-]{3,38}[0-9]" title="Solo dígitos, espacios o guiones" value={formulario.numeroCuenta} onChange={cambiar} placeholder="Ej. 123456789" className="mt-1 w-full rounded-xl border border-white/10 bg-[#090909] px-3 py-2.5 text-sm text-white outline-none focus:border-red-600" />
        </label>
        <label className="block text-xs font-semibold text-gray-400 sm:col-span-1 lg:col-span-2">Titular
          <input name="titular" required maxLength={120} value={formulario.titular} onChange={cambiar} placeholder="Nombre o razón social de la cuenta" className="mt-1 w-full rounded-xl border border-white/10 bg-[#090909] px-3 py-2.5 text-sm text-white outline-none focus:border-red-600" />
        </label>
        <div className="flex flex-wrap items-center gap-2 sm:col-span-2 lg:col-span-3">
          <button type="submit" disabled={guardando} className="inline-flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-60">
            {editandoId ? <Save size={15} /> : <Plus size={15} />} {editandoId ? 'Guardar cambios' : 'Agregar cuenta'}
          </button>
          {editandoId && (
            <button type="button" onClick={limpiar} className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-gray-300">
              <X size={15} /> Cancelar
            </button>
          )}
          {mensaje && <span role="status" className={`text-xs font-semibold ${mensaje.tipo === 'exito' ? 'text-green-500' : 'text-red-500'}`}>{mensaje.texto}</span>}
        </div>
      </form>

      <ul className="mt-5 divide-y divide-white/10">
        {cuentas.map((cuenta) => (
          <li key={cuenta.id} className={`flex flex-wrap items-center justify-between gap-3 py-3 ${cuenta.activa ? '' : 'opacity-50'}`}>
            <div className="min-w-0">
              <p className="text-sm font-bold text-white">{cuenta.banco} · {cuenta.tipoCuenta} · {cuenta.moneda}</p>
              <p className="font-mono text-xs text-gray-400">{cuenta.numeroCuenta} — {cuenta.titular}{cuenta.activa ? '' : ' (oculta)'}</p>
            </div>
            <div className="flex gap-1">
              <button type="button" onClick={() => editar(cuenta)} aria-label={`Editar cuenta ${cuenta.banco}`} className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-gray-400 hover:bg-white/10 hover:text-white"><Pencil size={15} /></button>
              {cuenta.activa && (
                <button type="button" onClick={() => desactivar(cuenta)} aria-label={`Ocultar cuenta ${cuenta.banco}`} className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-gray-400 hover:bg-red-500/10 hover:text-red-500"><Trash2 size={15} /></button>
              )}
            </div>
          </li>
        ))}
        {!cuentas.length && <li className="py-4 text-sm text-gray-500">Aún no hay cuentas. Agrega una para que los socios puedan pagar por transferencia.</li>}
      </ul>
    </section>
  );
};

export default CuentasBancarias;
