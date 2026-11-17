import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import VideoSpartan from './VideoSpartan';
import { usePrefersReducedMotion } from '../utils/useParallax';
import './GaleriaSpartan.css';

const numero = new Intl.NumberFormat('es');
const tituloEscena = (escena) => escena?.titulo?.trim() || 'Escena de entrenamiento';

export default function GaleriaSpartan({ escenas = [] }) {
  const [params] = useSearchParams();
  const { hash } = useLocation();
  const navegar = useNavigate();
  const modo = import.meta.env.DEV ? params.get('galeria') : null;

  let muestra = escenas;
  if (import.meta.env.DEV) {
    if (modo === 'una') muestra = escenas.slice(0, 1);
    if (modo === 'vacia') muestra = [];
    if (modo === 'muchas') {
      muestra = Array.from({ length: 1000 }, (_, i) => ({
        ...escenas[i % escenas.length],
        titulo: `Entrenamiento ${numero.format(i + 1)}`,
      }));
    }
    if (modo === 'extrema') {
      muestra = escenas.map((escena, indice) => {
        if (indice === 0) {
          return {
            ...escena,
            titulo: 'Entrenamiento de fuerza y acondicionamiento físico con barra olímpica: técnica, control y progresión',
          };
        }
        if (indice === 1) {
          return {
            ...escena,
            src: null,
            video: null,
            titulo: 'Movilidad y recuperación — Đặng Thị Ngọc Hân',
          };
        }
        return { ...escena, titulo: '', alt: '' };
      });
    }
  }

  return (
    <div className="sp-gallery-container">
      <VisorGaleria key={modo ?? 'normal'} escenas={muestra} />
      {import.meta.env.DEV && modo !== null && (
        <div className="sp-gallery-debug" role="group" aria-label="Datos de prueba de la galería">
          {[
            ['normal', 'Demo data'],
            ['extrema', 'Worst case'],
            ['vacia', 'Empty'],
            ['una', 'One'],
            ['muchas', '1,000'],
          ].map(([valor, etiqueta]) => (
            <button
              key={valor}
              type="button"
              aria-pressed={modo === valor}
              onClick={() => {
                const siguiente = new URLSearchParams(params);
                siguiente.set('galeria', valor);
                navegar({ search: siguiente.toString(), hash }, { replace: true, preventScrollReset: true });
              }}
            >
              {etiqueta}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function ImagenEscena({ src, alt, parallaxOffset = 0 }) {
  const [fallo, setFallo] = useState(false);
  if (!src || fallo) return <p className="sp-gallery-unavailable">Imagen no disponible.</p>;

  return (
    <div className="sp-gallery-media-wrap">
      <img
        src={src}
        alt={alt}
        loading="lazy"
        decoding="async"
        className="sp-gallery-img"
        style={parallaxOffset !== 0 ? { transform: `translate3d(${parallaxOffset}px, 0, 0) scale(1.08)` } : undefined}
        onError={() => setFallo(true)}
      />
    </div>
  );
}

function VisorGaleria({ escenas }) {
  const visor = useRef(null);
  const [actual, setActual] = useState(0);
  const [scrollProgress, setScrollProgress] = useState(0);
  const reducedMotion = usePrefersReducedMotion();

  const seleccion = Math.min(actual, Math.max(0, escenas.length - 1));

  useEffect(() => {
    const contenedor = visor.current;
    if (!contenedor) return;

    const observador = new IntersectionObserver(
      (entradas) => {
        for (const entrada of entradas) {
          if (entrada.isIntersecting) {
            setActual(Number(entrada.target.dataset.escena));
          }
        }
      },
      { root: contenedor, threshold: 0.65 }
    );

    for (const escena of contenedor.children) observador.observe(escena);
    return () => observador.disconnect();
  }, [escenas.length]);

  const manejarScroll = useCallback(() => {
    if (reducedMotion || !visor.current) return;
    const { scrollLeft, clientWidth } = visor.current;
    if (clientWidth > 0) {
      setScrollProgress(scrollLeft / clientWidth);
    }
  }, [reducedMotion]);

  function irA(indice, instantaneo = false) {
    const contenedor = visor.current;
    const destino = contenedor?.children[indice];
    if (!destino) return;
    contenedor.scrollTo({
      left: destino.offsetLeft,
      behavior: reducedMotion || instantaneo ? 'instant' : 'smooth',
    });
  }

  if (escenas.length === 0) {
    return (
      <div className="sp-gallery-viewer">
        <div className="sp-gallery-empty" role="status">
          Aún no hay escenas disponibles.
        </div>
      </div>
    );
  }

  return (
    <div className="sp-gallery-viewer">
      {escenas.length > 1 && escenas.length <= 6 && (
        <nav className="sp-ios-segmented" role="tablist" aria-label="Escenas de la galería">
          <div
            className="sp-ios-segmented-pill"
            style={{
              width: `${100 / escenas.length}%`,
              transform: `translate3d(${seleccion * 100}%, 0, 0)`,
            }}
            aria-hidden="true"
          />
          {escenas.map((escena, idx) => (
            <button
              key={idx}
              type="button"
              role="tab"
              aria-selected={idx === seleccion}
              aria-controls="sp-reel"
              className={`sp-ios-segment ${idx === seleccion ? 'is-active' : ''}`}
              onClick={() => irA(idx)}
            >
              <span className="sp-ios-segment-num">{String(idx + 1).padStart(2, '0')}</span>
              <span className="sp-ios-segment-label">
                {escena.titulo ? escena.titulo.split('—')[0].trim() : `Escena ${idx + 1}`}
              </span>
            </button>
          ))}
        </nav>
      )}

      <div className="sp-gallery-card-frame">
        <div
          ref={visor}
          id="sp-reel"
          className="sp-gallery-track"
          tabIndex={0}
          role="region"
          aria-label="Galería de entrenamiento"
          aria-roledescription="carrusel"
          onScroll={manejarScroll}
          onKeyDown={(event) => {
            if (event.target !== event.currentTarget) return;
            const destinos = {
              ArrowLeft: seleccion - 1,
              ArrowRight: seleccion + 1,
              Home: 0,
              End: escenas.length - 1,
            };
            if (!(event.key in destinos)) return;
            event.preventDefault();
            irA(Math.min(escenas.length - 1, Math.max(0, destinos[event.key])), true);
          }}
        >
          {escenas.map((escena, indice) => {
            const offsetDiff = (scrollProgress - indice) * 45;

            return (
              <figure
                className="sp-gallery-slide"
                key={indice}
                data-escena={indice}
                role="group"
                aria-roledescription="escena"
                aria-label={`${numero.format(indice + 1)} de ${numero.format(escenas.length)}: ${tituloEscena(escena)}`}
                inert={indice !== seleccion}
              >
                {Math.abs(indice - seleccion) <= 1 && (
                  <ImagenEscena
                    key={escena.src ?? 'missing'}
                    src={escena.src}
                    alt={escena.alt || tituloEscena(escena)}
                    parallaxOffset={offsetDiff}
                  />
                )}
                {escena.video && indice === seleccion && (
                  <VideoSpartan className="sp-gallery-video" src={escena.video} poster={escena.src} />
                )}
              </figure>
            );
          })}
        </div>

        <div className="sp-gallery-glass-hud">
          <div className="sp-gallery-caption-content">
            <p className="sp-gallery-title" aria-live="polite" aria-atomic="true" dir="auto">
              {tituloEscena(escenas[seleccion])}
            </p>
          </div>

          <div className="sp-gallery-actions">
            <span
              className="sp-gallery-count"
              aria-label={`Escena ${numero.format(seleccion + 1)} de ${numero.format(escenas.length)}`}
            >
              {numero.format(seleccion + 1)} <span aria-hidden="true">/</span> {numero.format(escenas.length)}
            </span>
            {escenas.length > 1 && (
              <div className="sp-ios-btn-group">
                <button
                  type="button"
                  className="sp-ios-circle-btn"
                  aria-label="Escena anterior"
                  aria-controls="sp-reel"
                  disabled={seleccion === 0}
                  onClick={(event) => irA(seleccion - 1, event.detail === 0)}
                >
                  <ArrowLeft size={18} aria-hidden="true" />
                </button>
                <button
                  type="button"
                  className="sp-ios-circle-btn"
                  aria-label="Escena siguiente"
                  aria-controls="sp-reel"
                  disabled={seleccion === escenas.length - 1}
                  onClick={(event) => irA(seleccion + 1, event.detail === 0)}
                >
                  <ArrowRight size={18} aria-hidden="true" />
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
