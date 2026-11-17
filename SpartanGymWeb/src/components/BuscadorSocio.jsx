import { useId, useMemo, useRef, useState } from 'react';
import { Search, UserRound, X } from 'lucide-react';
import { normalizar, terminos } from '../utils/busquedaEjercicios';

const MAXIMO_VISIBLE = 8;

const nombreDe = (socio) => [socio?.nombres, socio?.apellidos].filter(Boolean).join(' ').trim() || socio?.email || 'Socio';

// La lista solo aparece al escribir: con cientos de socios, mostrarlos todos ocupa la pantalla.
export const filtrarSocios = (socios, consulta) => {
  const lista = terminos(consulta);
  if (!lista.length) return [];
  return (socios || [])
    .filter((socio) => {
      const texto = normalizar([socio.nombres, socio.apellidos, socio.email, socio.telefono, socio.cedula].filter(Boolean).join(' '));
      return lista.every((termino) => texto.includes(termino));
    })
    .sort((a, b) => nombreDe(a).localeCompare(nombreDe(b), 'es'));
};

/** Buscador de socios: escribe nombre, apellido, correo o teléfono y elige de los resultados. */
const BuscadorSocio = ({ socios, valorId, onSeleccionar, etiqueta = 'Socio', claseEtiqueta, claseCampo, requerido = true }) => {
  const id = useId();
  const entrada = useRef(null);
  const [consulta, setConsulta] = useState('');
  const [abierto, setAbierto] = useState(false);
  const [activo, setActivo] = useState(0);

  const seleccionado = useMemo(
    () => (socios || []).find((socio) => String(socio.id) === String(valorId)),
    [socios, valorId],
  );
  const resultados = useMemo(() => filtrarSocios(socios, consulta), [socios, consulta]);
  const visibles = resultados.slice(0, MAXIMO_VISIBLE);

  const elegir = (socio) => {
    onSeleccionar(socio);
    setConsulta('');
    setAbierto(false);
  };

  const alTeclear = (evento) => {
    if (evento.key === 'ArrowDown') {
      evento.preventDefault();
      setActivo((actual) => Math.min(actual + 1, visibles.length - 1));
    } else if (evento.key === 'ArrowUp') {
      evento.preventDefault();
      setActivo((actual) => Math.max(actual - 1, 0));
    } else if (evento.key === 'Enter' && visibles[activo]) {
      evento.preventDefault();
      elegir(visibles[activo]);
    } else if (evento.key === 'Escape') {
      setAbierto(false);
    }
  };

  return (
    <div className="exercise-picker">
      <span className={claseEtiqueta || 'mb-2 block text-xs font-black uppercase tracking-widest text-gray-500'}>{etiqueta}</span>
      {seleccionado ? (
        <div className="exercise-picker-chosen border border-white/10 bg-[#111]">
          <span className="exercise-picker-thumb is-icon"><UserRound size={16} aria-hidden="true" /></span>
          <span className="min-w-0 flex-1">
            <strong className="block truncate text-sm text-white">{nombreDe(seleccionado)}</strong>
            {seleccionado.email && <small className="block truncate text-xs text-gray-500">{seleccionado.email}</small>}
          </span>
          <button
            type="button"
            onClick={() => { onSeleccionar(null); window.requestAnimationFrame(() => entrada.current?.focus()); }}
            aria-label="Cambiar socio"
            className="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-white/[0.07] hover:text-white"
          >
            <X size={14} />
          </button>
        </div>
      ) : (
        <div className="relative">
          <Search size={15} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500" aria-hidden="true" />
          <input
            ref={entrada}
            type="text"
            role="combobox"
            required={requerido}
            aria-expanded={abierto && Boolean(consulta.trim())}
            aria-controls={`${id}-lista`}
            aria-autocomplete="list"
            value={consulta}
            onChange={(evento) => { setConsulta(evento.target.value); setAbierto(true); setActivo(0); }}
            onFocus={() => setAbierto(true)}
            onBlur={() => setAbierto(false)}
            onKeyDown={alTeclear}
            placeholder="Escribe nombre, correo o teléfono"
            className={claseCampo || 'campo-sistema w-full rounded-xl border border-white/10 bg-[#050505] py-3 pl-10 pr-4 text-sm text-white outline-none transition-all focus:border-red-600'}
            style={{ paddingLeft: '2.5rem' }}
          />
          {abierto && consulta.trim() && (
            <div className="exercise-picker-panel border border-white/10 bg-[#111]" onMouseDown={(evento) => evento.preventDefault()}>
              <ul id={`${id}-lista`} role="listbox" aria-label="Socios encontrados">
                {visibles.map((socio, indice) => (
                  <li
                    key={socio.id}
                    role="option"
                    aria-selected={indice === activo}
                    onMouseEnter={() => setActivo(indice)}
                    onClick={() => elegir(socio)}
                    className="exercise-picker-option"
                  >
                    <span className="exercise-picker-thumb is-icon"><UserRound size={15} aria-hidden="true" /></span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-semibold text-white">{nombreDe(socio)}</span>
                      {socio.email && <span className="block truncate text-xs text-gray-500">{socio.email}</span>}
                    </span>
                  </li>
                ))}
              </ul>
              <p className="exercise-picker-foot text-gray-500">
                {!resultados.length
                  ? 'Ningún socio coincide.'
                  : resultados.length > MAXIMO_VISIBLE
                    ? `Mostrando ${MAXIMO_VISIBLE} de ${resultados.length}. Escribe más para afinar.`
                    : `${resultados.length} ${resultados.length === 1 ? 'socio' : 'socios'}`}
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default BuscadorSocio;
