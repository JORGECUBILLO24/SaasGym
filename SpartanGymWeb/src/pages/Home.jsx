import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowDown, ArrowUpRight, Check, Menu, X } from 'lucide-react';
import atleta from '../assets/gym-training-poster.webp';
import videoTecnica from '../assets/gym-training-loop.mp4';
import escenaAestheticEspacio from '../assets/gym-aesthetic-workout.webp';
import escenaAestheticPhysique from '../assets/gym-youngla-physique.webp';
import escenaAestheticFuerza from '../assets/gym-aesthetic-coach.webp';
import VideoSpartan from './VideoSpartan';
import BarraEnsamblada from './BarraEnsamblada';
import GaleriaSpartan from './GaleriaSpartan';
import { beneficiosInicio } from '../data/homeBenefits';
import { useLogosApp } from '../utils/logosApp';
import { obtenerNombreGimnasio, useConfiguracionApp } from '../utils/configuracionApp';
import './Home.css';

const secciones = [
  ['instalaciones', 'El espacio'],
  ['beneficios', 'La experiencia'],
  ['fuerza', 'Ingeniería'],
  ['membresias', 'Membresía'],
];

const escenas = [
  {
    src: escenaAestheticEspacio,
    alt: 'Zona de mancuernas con iluminación tenue',
    titulo: 'Zona de peso libre',
  },
  {
    src: escenaAestheticPhysique,
    alt: 'Atleta entrenando con mancuernas',
    titulo: 'Hipertrofia',
  },
  {
    src: escenaAestheticFuerza,
    alt: 'Entrenador supervisando un ejercicio de fuerza',
    titulo: 'Entrenamiento guiado',
  },
];

function Marca({ src, footer = false, nombreGimnasio, mostrarLogo }) {
  const [logoFallido, setLogoFallido] = useState('');
  const mostrarImagen = mostrarLogo && src && logoFallido !== src;

  return (
    <a
      href="#inicio"
      className="sp-brand"
      aria-label={footer ? 'Volver al inicio' : `${nombreGimnasio}, inicio`}
    >
      {mostrarImagen ? (
        <img
          src={src}
          alt={nombreGimnasio}
          width="144"
          height="72"
          loading={footer ? 'lazy' : 'eager'}
          onError={() => setLogoFallido(src)}
        />
      ) : <span className="max-w-[18rem] text-xl font-black uppercase tracking-wide text-white">{nombreGimnasio}</span>}
    </a>
  );
}

export default function Home() {
  const logos = useLogosApp();
  const configuracion = useConfiguracionApp();
  const nombreGimnasio = obtenerNombreGimnasio(configuracion);
  const mostrarLogo = Boolean(logos.principal);
  const [menuAbierto, setMenuAbierto] = useState(false);
  const botonMenu = useRef(null);

  return (
    <div className="spartan-editorial" id="inicio">
      <a className="sp-skip" href="#contenido-portada">
        Saltar al contenido
      </a>

      <header
        className="sp-header-wrapper"
        onBlur={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget)) setMenuAbierto(false);
        }}
        onKeyDown={(event) => {
          if (event.key === 'Escape' && menuAbierto) {
            setMenuAbierto(false);
            botonMenu.current?.focus();
          }
        }}
      >
        <div className="sp-header-capsule">
          <Marca src={logos.principal} nombreGimnasio={nombreGimnasio} mostrarLogo={mostrarLogo} />

          <nav
            id="sp-navegacion"
            className="sp-nav"
            data-open={menuAbierto}
            aria-label="Navegación principal"
          >
            {secciones.map(([id, nombre]) => (
              <a
                key={id}
                href={'#' + id}
                className="sp-nav-link"
                onClick={() => setMenuAbierto(false)}
              >
                {nombre}
              </a>
            ))}
          </nav>

          <div className="sp-header-actions">
            <Link className="sp-access-btn" to="/login">
              <span>Iniciar sesión</span>
              <ArrowUpRight size={15} aria-hidden="true" />
            </Link>

            <button
              ref={botonMenu}
              className="sp-menu"
              type="button"
              aria-controls="sp-navegacion"
              aria-expanded={menuAbierto}
              aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
              onClick={() => setMenuAbierto(!menuAbierto)}
            >
              {menuAbierto ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>
      </header>

      <main id="contenido-portada" tabIndex={-1}>
        <section className="sp-hero-section" aria-labelledby="sp-titulo">
          <div className="sp-hero-media-layer">
            <img
              className="sp-hero-image"
              src={atleta}
              alt="Atleta entrenando con barra"
              fetchPriority="high"
              decoding="async"
              width="720"
              height="1280"
            />
            <VideoSpartan className="sp-hero-image" src={videoTecnica} poster={atleta} automatico />
            <div className="sp-hero-vignette" aria-hidden="true" />
          </div>

          <div className="sp-wrap sp-hero-inner">
            <div className="sp-hero-copy sp-stagger-reveal">
              <p className="sp-kicker">{nombreGimnasio}</p>

              <h1 id="sp-titulo" className="sp-hero-heading">
                Hazte<br />
                más fuerte<span>.</span>
              </h1>

              <p className="sp-description">
                Peso libre, máquinas de fuerza y entrenadores en sala durante todo el horario.
                Llegas, entrenas y sabes qué toca la próxima vez.
              </p>

              <div className="sp-hero-cta-group">
                <Link className="sp-ios-btn-primary" to="/registro">
                  <span>Crear cuenta</span>
                  <ArrowUpRight size={18} aria-hidden="true" />
                </Link>

                <a className="sp-ios-btn-secondary" href="#instalaciones">
                  <ArrowDown size={16} aria-hidden="true" />
                  <span>Ver instalaciones</span>
                </a>
              </div>
            </div>
          </div>
        </section>

        <section
          id="instalaciones"
          className="sp-wrap sp-experience-section"
          aria-labelledby="sp-experience-title"
        >
          <div id="quienes-somos" className="sp-section-heading sp-reveal">
            <p className="sp-kicker">El espacio</p>
            <h2 id="sp-experience-title">
              Sala amplia,<br />
              equipo completo<span>.</span>
            </h2>
            <p className="sp-description">
              Racks, plataformas, mancuernas y zona de máquinas. Hay sitio para entrenar sin esperar turno.
            </p>
          </div>

          <div id="galeria" className="sp-reveal">
            <GaleriaSpartan escenas={escenas} />
          </div>

          <div id="beneficios" className="sp-bento-section">
            <div className="sp-bento-header sp-reveal">
              <p className="sp-kicker">Qué incluye</p>
              <h3 className="sp-bento-title">Lo que encuentras al entrar</h3>
            </div>

            <div className="sp-bento-grid">
              {beneficiosInicio.map((beneficio, idx) => {
                const Icono = beneficio.icono;
                const esPrincipal = idx === 0;

                return (
                  <Link
                    key={beneficio.ruta}
                    to={'/beneficios/' + beneficio.ruta}
                    className={`sp-bento-card ${esPrincipal ? 'sp-bento-card-featured' : ''}`}
                    aria-label={`Conoce más sobre ${beneficio.titulo}`}
                  >
                    {esPrincipal && (
                      <div className="sp-bento-bg-media">
                        <img
                          src={beneficio.imagen}
                          alt={beneficio.imagenAlt || beneficio.titulo}
                          loading="lazy"
                          className="sp-bento-img"
                        />
                        <div className="sp-bento-overlay" />
                      </div>
                    )}

                    <div className="sp-bento-content">
                      <div className="sp-bento-top-row">
                        <Icono size={22} className="sp-bento-icon" aria-hidden="true" />
                        <span className="sp-bento-badge">{beneficio.etiqueta}</span>
                      </div>

                      <div className="sp-bento-body">
                        <h4 className="sp-bento-card-title">{beneficio.titulo}</h4>
                        <p className="sp-bento-card-desc">{beneficio.resumen}</p>

                        {beneficio.puntos && (
                          <ul className="sp-bento-bullets" role="list">
                            {beneficio.puntos.slice(0, 2).map((punto, pIdx) => (
                              <li key={pIdx} className="sp-bento-bullet-item">
                                <Check size={15} className="shrink-0" aria-hidden="true" />
                                <span>{punto}</span>
                              </li>
                            ))}
                          </ul>
                        )}
                      </div>

                      <div className="sp-bento-footer">
                        <span className="sp-bento-link-text">Leer más</span>
                        <div className="sp-bento-arrow-circle">
                          <ArrowUpRight size={16} aria-hidden="true" />
                        </div>
                      </div>
                    </div>
                  </Link>
                );
              })}
            </div>
          </div>
        </section>

        <BarraEnsamblada />

        <section
          id="membresias"
          className="sp-wrap sp-membership-section"
          aria-labelledby="sp-membership-title"
        >
          <div className="sp-membership-glass-card sp-reveal">
            <div className="sp-membership-content">
              <p className="sp-kicker">Membresías</p>
              <h2 id="sp-membership-title" className="sp-membership-heading">
                Empieza<br />
                esta semana<span>.</span>
              </h2>
              <p className="sp-description">
                Crea tu cuenta en línea y activa el plan en recepción. En la app ves tus rutinas, tu asistencia y cuándo vence tu membresía.
              </p>

              <div className="sp-membership-perks">
                <div className="sp-perk-pill">
                  <Check size={16} aria-hidden="true" />
                  <span>Planes mensuales y anuales</span>
                </div>
                <div className="sp-perk-pill">
                  <Check size={16} aria-hidden="true" />
                  <span>Rutinas en la app</span>
                </div>
                <div className="sp-perk-pill">
                  <Check size={16} aria-hidden="true" />
                  <span>Entrada con QR</span>
                </div>
              </div>
            </div>

            <div className="sp-membership-action">
              <Link className="sp-ios-btn-primary sp-btn-large" to="/registro">
                <span>Crear mi cuenta</span>
                <ArrowUpRight size={20} aria-hidden="true" />
              </Link>
              <span className="sp-membership-note">
                Precios y horarios en recepción.
              </span>
            </div>
          </div>
        </section>
      </main>

      <footer className="sp-wrap sp-footer-modern">
        <div className="sp-footer-inner">
          <Marca src={logos.principal} footer nombreGimnasio={nombreGimnasio} mostrarLogo={mostrarLogo} />
          <p className="sp-footer-tagline">Entrena con constancia.</p>
          <a className="sp-footer-back-top" href="#inicio">
            <span>Volver arriba</span>
            <ArrowUpRight size={15} aria-hidden="true" />
          </a>
        </div>
        <div className="sp-footer-bottom">
          <p>© {new Date().getFullYear()} {nombreGimnasio}. Todos los derechos reservados.</p>
        </div>
      </footer>
    </div>
  );
}
