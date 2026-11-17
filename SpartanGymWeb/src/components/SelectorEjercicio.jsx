import { useId, useMemo, useRef, useState } from 'react';
import { Dumbbell, Search, X } from 'lucide-react';
import { buscarEjercicios } from '../utils/busquedaEjercicios';
import { urlArchivoApi } from '../services/api';

const MAXIMO_VISIBLE = 40;

/**
 * Buscador de la biblioteca de ejercicios con vista previa del GIF. Busca sin tildes, en
 * cualquier orden y por sinónimo o nombre en inglés; el grupo muscular solo acota si se elige.
 */
const SelectorEjercicio = ({ ejercicios, grupoMuscularId, seleccionado, onSeleccionar, onQuitar, etiqueta = 'Ejercicio' }) => {
  const id = useId();
  const listaId = `${id}-lista`;
  const entrada = useRef(null);
  const [abierto, setAbierto] = useState(false);
  const [consulta, setConsulta] = useState('');
  const [activo, setActivo] = useState(0);

  const resultados = useMemo(
    () => buscarEjercicios(ejercicios, consulta, { grupoMuscularId }),
    [ejercicios, consulta, grupoMuscularId],
  );
  const visibles = resultados.slice(0, MAXIMO_VISIBLE);

  const abrir = () => {
    setAbierto(true);
    setActivo(0);
  };

  const elegir = (ejercicio) => {
    onSeleccionar(ejercicio);
    setConsulta('');
    setAbierto(false);
  };

  const alTeclear = (evento) => {
    if (evento.key === 'ArrowDown') {
      evento.preventDefault();
      if (!abierto) abrir();
      else setActivo((actual) => Math.min(actual + 1, visibles.length - 1));
    } else if (evento.key === 'ArrowUp') {
      evento.preventDefault();
      setActivo((actual) => Math.max(actual - 1, 0));
    } else if (evento.key === 'Enter' && abierto && visibles[activo]) {
      evento.preventDefault();
      elegir(visibles[activo]);
    } else if (evento.key === 'Escape') {
      setAbierto(false);
    }
  };

  const gifSeleccionado = urlArchivoApi(seleccionado?.gifUrl);

  return (
    <div className="exercise-picker">
      <span className="mb-2 block text-xs font-black uppercase tracking-widest text-gray-500">{etiqueta}</span>

      {seleccionado && !abierto ? (
        <div className="exercise-picker-chosen border border-white/10 bg-[#111]">
          <span className="exercise-picker-thumb">
            {gifSeleccionado ? <img src={gifSeleccionado} alt="" /> : <Dumbbell size={16} aria-hidden="true" />}
          </span>
          <span className="min-w-0 flex-1">
            <strong className="block truncate text-sm text-white" title={seleccionado.nombre}>{seleccionado.nombre}</strong>
            <small className="block truncate text-xs text-gray-500">{detalleDe(seleccionado)}</small>
          </span>
          <button
            type="button"
            onClick={() => { abrir(); window.requestAnimationFrame(() => entrada.current?.focus()); }}
            className="rounded-lg px-2.5 py-1.5 text-xs font-bold text-gray-300 transition-colors hover:bg-white/[0.07] hover:text-white"
          >
            Cambiar
          </button>
          {onQuitar && (
            <button type="button" onClick={onQuitar} aria-label="Quitar ejercicio" className="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-white/[0.07] hover:text-white">
              <X size={14} />
            </button>
          )}
        </div>
      ) : (
        <div className="relative">
          <Search size={15} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500" aria-hidden="true" />
          <input
            ref={entrada}
            type="text"
            role="combobox"
            aria-expanded={abierto}
            aria-controls={listaId}
            aria-autocomplete="list"
            aria-activedescendant={abierto && visibles[activo] ? `${id}-op-${activo}` : undefined}
            value={consulta}
            onChange={(evento) => { setConsulta(evento.target.value); setAbierto(true); setActivo(0); }}
            onFocus={abrir}
            onBlur={() => setAbierto(false)}
            onKeyDown={alTeclear}
            placeholder="Busca: sentadilla, curl bíceps, bench press…"
            className="campo-sistema w-full rounded-xl border border-white/10 bg-[#050505] py-3 pl-10 pr-4 text-sm text-white outline-none transition-all focus:border-red-600"
          />

          {abierto && (
            // mousedown evita que el blur del campo cierre la lista antes del clic.
            <div className="exercise-picker-panel border border-white/10 bg-[#111]" onMouseDown={(evento) => evento.preventDefault()}>
              <ul id={listaId} role="listbox" aria-label="Ejercicios encontrados">
                {visibles.map((ejercicio, indice) => {
                  const gif = urlArchivoApi(ejercicio.gifUrl);
                  return (
                    <li
                      key={ejercicio.id}
                      id={`${id}-op-${indice}`}
                      role="option"
                      aria-selected={indice === activo}
                      onMouseEnter={() => setActivo(indice)}
                      onClick={() => elegir(ejercicio)}
                      className="exercise-picker-option"
                    >
                      <span className="exercise-picker-thumb">
                        {gif ? <img src={gif} alt="" loading="lazy" /> : <Dumbbell size={16} aria-hidden="true" />}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-sm font-semibold text-white">{ejercicio.nombre}</span>
                        <span className="block truncate text-xs text-gray-500">{detalleDe(ejercicio)}</span>
                      </span>
                    </li>
                  );
                })}
              </ul>
              <p className="exercise-picker-foot text-gray-500">
                {!resultados.length
                  ? (grupoMuscularId ? 'Nada en este grupo. Prueba con «Todos los grupos».' : 'Sin resultados. Prueba con otra palabra o el nombre en inglés.')
                  : resultados.length > MAXIMO_VISIBLE
                    ? `Mostrando ${MAXIMO_VISIBLE} de ${resultados.length}. Escribe más para afinar.`
                    : `${resultados.length} ${resultados.length === 1 ? 'ejercicio' : 'ejercicios'}`}
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

const detalleDe = (ejercicio) => [ejercicio.grupoMuscular?.nombre, ejercicio.equipo, ejercicio.dificultad]
  .filter(Boolean)
  .join(' · ');

export default SelectorEjercicio;
