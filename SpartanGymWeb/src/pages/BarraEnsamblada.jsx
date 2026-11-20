import { useCallback, useEffect, useRef, useState } from 'react';
import { RotateCcw } from 'lucide-react';
import equipo from '../assets/barra-discos.png';
import { usePrefersReducedMotion } from '../utils/useParallax';

// Pares de discos de 20 kg, del más cercano al manguito al exterior.
const pares = [
  { distancia: 296, altura: 166, peso: 40, label: 'Par 20kg x 2' },
  { distancia: 326, altura: 146, peso: 40, label: 'Par 20kg x 2' },
  { distancia: 352, altura: 126, peso: 40, label: 'Par 20kg x 2' },
];

export default function BarraEnsamblada() {
  const seccionRef = useRef(null);
  const reducedMotion = usePrefersReducedMotion();
  // paso = pares colocados (0 a 3); cada par suma 40 kg a la barra de 20 kg.
  const [paso, setPaso] = useState(() => (reducedMotion ? 3 : 0));
  const [animando, setAnimando] = useState(false);
  const timersRef = useRef([]);

  const limpiarTimers = useCallback(() => {
    timersRef.current.forEach(clearTimeout);
    timersRef.current = [];
  }, []);

  useEffect(() => {
    return limpiarTimers;
  }, [limpiarTimers]);

  const ejecutarEnsamble = useCallback(() => {
    limpiarTimers();
    setPaso(0);
    setAnimando(true);

    const t1 = setTimeout(() => setPaso(1), 350);
    const t2 = setTimeout(() => setPaso(2), 750);
    const t3 = setTimeout(() => {
      setPaso(3);
      setAnimando(false);
    }, 1150);

    timersRef.current = [t1, t2, t3];
  }, [limpiarTimers]);

  useEffect(() => {
    if (reducedMotion) {
      return;
    }

    const elemento = seccionRef.current;
    if (!elemento) return;

    let ejecutado = false;
    const observador = new IntersectionObserver(
      ([entrada]) => {
        if (entrada.isIntersecting && !ejecutado) {
          ejecutado = true;
          ejecutarEnsamble();
        }
      },
      { threshold: 0.25 }
    );

    observador.observe(elemento);
    return () => observador.disconnect();
  }, [reducedMotion, ejecutarEnsamble]);

  const pesoActual = 20 + paso * 40;

  return (
    <section ref={seccionRef} id="fuerza" className="sp-load" aria-labelledby="sp-lift-title">
      <div className="sp-wrap sp-load-frame">
        <div className="sp-load-header text-center">
          <p className="sp-kicker">Peso libre</p>
          <h2 id="sp-lift-title" className="sp-load-title">
            Disco a disco<span className="sp-dot-neutral">.</span>
          </h2>
          <p className="sp-description mx-auto text-center max-w-xl">
            Barras olímpicas y discos de 20 kg en cada plataforma. Subes la carga cuando la técnica lo permite.
          </p>

          <div className="sp-load-weight-hud" aria-live="polite">
            <div className="sp-weight-display">
              <span className="sp-weight-number">{pesoActual}</span>
              <span className="sp-weight-unit">KG</span>
            </div>
            <span className="sp-weight-status">
              {paso === 0
                ? 'Barra vacía'
                : paso === 1
                ? 'Primer par de discos'
                : paso === 2
                ? 'Segundo par de discos'
                : 'Tres pares de discos'}
            </span>
            <button
              type="button"
              className="sp-ios-replay-btn"
              onClick={ejecutarEnsamble}
              disabled={animando}
              aria-label="Repetir la animación de carga de la barra"
            >
              <RotateCcw size={14} className={animando ? 'animate-spin' : ''} aria-hidden="true" />
              <span>Repetir</span>
            </button>
          </div>
        </div>

        <div className="sp-load-stage">
          <svg
            className="sp-load-art"
            viewBox="0 0 1040 240"
            role="img"
            aria-label={`Barra olímpica de acero con carga actual de ${pesoActual} kg`}
          >
            <defs>
              {/* Máscara de recorte estricta: corta EXACTAMENTE la barra olímpica (y=97 a 195).
                  Esto elimina por completo cualquier disco que esté debajo en el archivo png original. */}
              <clipPath id="sp-bar-crop">
                <rect x="0" y="97" width="1774" height="98" />
              </clipPath>

              <image id="sp-equipo" href={equipo} width="1774" height="887" />

              <symbol id="sp-plate" viewBox="777 245 221 611">
                <use href="#sp-equipo" />
              </symbol>
            </defs>

            {/* Barra olímpica central: recortada al milímetro para que ningún elemento inferior sobresalga */}
            <svg
              x="105"
              y="97"
              width="830"
              height="46"
              viewBox="0 97 1774 98"
              className="sp-olympic-bar"
              overflow="hidden"
              style={{ overflow: 'hidden' }}
            >
              <use href="#sp-equipo" clipPath="url(#sp-bar-crop)" />
            </svg>

            {/* Pares de discos: se ensamblan secuencialmente desde los extremos hacia el collar */}
            {pares.map(({ distancia, altura }, indice) => {
              // El par está ensamblado si el paso actual es mayor que su índice
              const estaCargado = paso > indice || reducedMotion;

              // Distancia de desplazamiento cuando está fuera de la barra
              const offsetFuera = 220 + (2 - indice) * 50;

              return (
                <g key={distancia} data-par={indice} className="sp-load-pair">
                  {/* Disco izquierdo */}
                  <g
                    className="sp-load-disc sp-load-left"
                    style={{
                      transform: estaCargado ? 'translate3d(0, 0, 0)' : `translate3d(${-offsetFuera}px, 0, 0)`,
                      opacity: estaCargado ? 1 : 0,
                      transition: reducedMotion
                        ? 'none'
                        : 'transform 420ms cubic-bezier(0.18, 0.9, 0.32, 1.15), opacity 300ms ease',
                    }}
                  >
                    <use
                      href="#sp-plate"
                      x={520 - distancia - altura * 0.1}
                      y={120 - altura * 0.49}
                      width={altura * 0.36}
                      height={altura}
                    />
                  </g>

                  {/* Disco derecho */}
                  <g
                    className="sp-load-disc sp-load-right"
                    style={{
                      transform: estaCargado ? 'translate3d(0, 0, 0)' : `translate3d(${offsetFuera}px, 0, 0)`,
                      opacity: estaCargado ? 1 : 0,
                      transition: reducedMotion
                        ? 'none'
                        : 'transform 420ms cubic-bezier(0.18, 0.9, 0.32, 1.15), opacity 300ms ease',
                    }}
                  >
                    <g transform="translate(1040 0) scale(-1 1)">
                      <use
                        href="#sp-plate"
                        x={520 - distancia - altura * 0.1}
                        y={120 - altura * 0.49}
                        width={altura * 0.36}
                        height={altura}
                      />
                    </g>
                  </g>
                </g>
              );
            })}
          </svg>
        </div>

        <dl className="sp-load-specs">
          <div className="sp-ios-spec-card">
            <dt className="sp-ios-spec-desc">Barra</dt>
            <dd className="sp-ios-spec-val">20 kg</dd>
          </div>
          <div className="sp-ios-spec-card">
            <dt className="sp-ios-spec-desc">Discos</dt>
            <dd className="sp-ios-spec-val">20 kg c/u</dd>
          </div>
          <div className="sp-ios-spec-card">
            <dt className="sp-ios-spec-desc">Carga completa</dt>
            <dd className="sp-ios-spec-val">140 kg</dd>
          </div>
        </dl>
      </div>
    </section>
  );
}
