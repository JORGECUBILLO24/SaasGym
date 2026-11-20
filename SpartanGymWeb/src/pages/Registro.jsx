import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  Building2,
  Eye,
  EyeOff,
  Lock,
  Mail,
  Phone,
  User,
  UserPlus,
} from 'lucide-react';
import { useLogosApp } from '../utils/logosApp';
import { obtenerNombreGimnasio, useConfiguracionApp } from '../utils/configuracionApp';
import { authApi, authStorage, sucursalesApi } from '../services/api';
import showcaseImage from '../assets/gym-youngla-athlete.webp';

const Registro = () => {
  const navigate = useNavigate();
  const logos = useLogosApp();
  const configuracionApp = useConfiguracionApp();
  const nombreGimnasio = obtenerNombreGimnasio(configuracionApp);
  const logoMarca = logos.acceso || logos.principal;
  const mostrarLogoMarca = Boolean(logoMarca);
  const [formData, setFormData] = useState({
    nombres: '',
    apellidos: '',
    email: '',
    telefono: '',
    password: '',
    sucursalId: '',
  });
  const [sucursales, setSucursales] = useState([]);
  const [sucursalesError, setSucursalesError] = useState('');
  const [cargandoSucursales, setCargandoSucursales] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    sucursalesApi
      .listarDisponibles()
      .then((resultado) => {
        const datos = Array.isArray(resultado) ? resultado : [];
        setSucursales(datos);
        setSucursalesError(
          datos.length === 0
            ? 'La administración debe configurar una sucursal antes de habilitar el registro.'
            : ''
        );
        setFormData((actual) => ({
          ...actual,
          sucursalId: actual.sucursalId || datos[0]?.id || '',
        }));
      })
      .catch(() => setSucursalesError('No se pudieron cargar las sucursales desde la API.'))
      .finally(() => setCargandoSucursales(false));
  }, []);

  const handleChange = (event) => {
    setFormData({ ...formData, [event.target.name]: event.target.value });
    setError('');
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    if (sucursales.length === 0) return;
    setIsSubmitting(true);

    try {
      await authApi.registerSocio(formData);
      authStorage.clear();
      navigate('/login');
    } catch {
      setError('No se pudo completar el registro. Verifica los datos y que la API esté activa.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen w-full bg-[#070606] text-white flex flex-col lg:grid lg:grid-cols-2 overflow-x-hidden">
      <div className="w-full flex flex-col justify-between p-6 sm:p-8 lg:p-10 xl:p-12 min-h-screen z-10">
        <header className="flex items-center justify-between gap-4">
          <Link
            to="/"
            className="inline-flex items-center gap-2 text-xs font-semibold text-neutral-400 hover:text-white transition-colors px-3 py-1.5 rounded-full border border-white/10 hover:border-white/25 bg-white/[0.03]"
            aria-label="Volver al inicio"
          >
            <span>← Volver al inicio</span>
          </Link>

          <span className="text-[11px] font-bold uppercase tracking-widest text-neutral-500">
            Registro de Socios
          </span>
        </header>

        <div className="w-full max-w-lg mx-auto my-auto py-3">
          <div className="flex flex-col items-center text-center mb-4">
            <Link to="/" className="group inline-block" aria-label={nombreGimnasio}>
              <div className="relative inline-flex items-center justify-center">
                {mostrarLogoMarca ? (
                  <img src={logoMarca} alt={nombreGimnasio} width="180" height="90" className="relative h-20 sm:h-24 w-auto object-contain" />
                ) : <span className="relative max-w-[18rem] text-center text-2xl font-black uppercase tracking-wide text-white sm:text-3xl">{nombreGimnasio}</span>}
              </div>
            </Link>

            <h1 className="mt-3 text-2xl sm:text-3xl font-black uppercase tracking-tight text-white">
              Crea tu cuenta<span>.</span>
            </h1>
            <p className="mt-1 text-xs sm:text-sm text-neutral-400">
              Crea tu cuenta de socio en {nombreGimnasio}
            </p>
          </div>

          <div className="flex p-1 rounded-full bg-white/[0.06] border border-white/10 mb-4">
            <Link
              to="/login"
              className="flex-1 text-center py-2 text-xs font-semibold rounded-full text-neutral-400 hover:text-white transition-colors"
            >
              Iniciar sesión
            </Link>
            <span className="flex-1 text-center py-2 text-xs font-bold rounded-full bg-white text-black shadow-lg">
              Crear cuenta
            </span>
          </div>

          <form onSubmit={handleRegister} className="space-y-3">
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
              <CampoRegistro
                icon={User}
                label="Nombres"
                name="nombres"
                value={formData.nombres}
                onChange={handleChange}
                placeholder="Tus nombres"
                required
              />
              <CampoRegistro
                icon={User}
                label="Apellidos"
                name="apellidos"
                value={formData.apellidos}
                onChange={handleChange}
                placeholder="Tus apellidos"
                required
              />
            </div>

            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
              <CampoRegistro
                icon={Mail}
                label="Correo electrónico"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="tu.correo@ejemplo.com"
                type="email"
                required
              />
              <CampoRegistro
                icon={Phone}
                label="Teléfono"
                name="telefono"
                value={formData.telefono}
                onChange={handleChange}
                placeholder="+505 8888 8888"
                type="tel"
              />
            </div>

            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
              <CampoRegistro
                icon={Lock}
                label="Contraseña"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Mínimo 8 caracteres"
                type="password"
                minLength={8}
                required
              />

              <div>
                <label htmlFor="registro-sucursal" className="block text-xs font-bold uppercase tracking-wider text-neutral-400 mb-1">
                  Sucursal
                </label>
                <div className="group relative">
                  <Building2 className="absolute left-3.5 top-3 h-4 w-4 text-neutral-500 transition-colors group-focus-within:text-white" />
                  <select
                    id="registro-sucursal"
                    name="sucursalId"
                    value={formData.sucursalId}
                    onChange={handleChange}
                    required
                    className="w-full appearance-none rounded-2xl border border-white/10 bg-[#121111] py-2.5 pl-11 pr-8 text-xs sm:text-sm text-white outline-none transition-all focus:border-white/30 focus:bg-[#181616] focus:ring-1 focus:ring-white/20"
                  >
                    <option value="">Selecciona tu sucursal</option>
                    {sucursales.map((sucursal) => (
                      <option key={sucursal.id} value={sucursal.id}>
                        {sucursal.nombre}
                      </option>
                    ))}
                  </select>
                  <div className="pointer-events-none absolute inset-y-0 right-3.5 flex items-center text-neutral-500">
                    <span className="text-[10px]">▼</span>
                  </div>
                </div>
                {sucursalesError && (
                  <p id="sucursal-status" role="status" className="mt-2 text-xs leading-relaxed text-neutral-400">
                    {sucursalesError}
                  </p>
                )}
                {cargandoSucursales && (
                  <p id="sucursal-status" role="status" className="mt-2 text-xs text-neutral-400">
                    Cargando sucursales…
                  </p>
                )}
              </div>
            </div>

            {error && (
              <div role="alert" className="flex items-start gap-2.5 rounded-2xl border border-white/15 bg-white/[0.05] p-3 text-xs text-neutral-200">
                <AlertCircle className="h-4 w-4 shrink-0 text-white mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            <button
              disabled={isSubmitting || cargandoSucursales || sucursales.length === 0}
              type="submit"
              aria-describedby={sucursalesError || cargandoSucursales ? 'sucursal-status' : undefined}
              className="w-full h-11 mt-1 rounded-2xl bg-black border border-white/25 text-white font-bold text-xs sm:text-sm tracking-wide uppercase transition-all duration-200 hover:bg-neutral-900 hover:border-white/40 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed shadow-xl shadow-black/80 flex items-center justify-center gap-2 cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <span className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Registrando…</span>
                </>
              ) : (
                <>
                  <UserPlus className="h-4 w-4" />
                  <span>
                    {cargandoSucursales
                      ? 'Cargando sucursales…'
                      : sucursales.length === 0
                        ? 'Registro no disponible'
                        : 'Crear mi cuenta'}
                  </span>
                </>
              )}
            </button>
          </form>

          <div className="mt-4 text-center text-xs text-neutral-500">
            ¿Ya tienes una cuenta registrada?{' '}
            <Link to="/login" className="font-bold text-white hover:underline ml-1">
              Inicia sesión aquí
            </Link>
          </div>
        </div>

        <footer className="text-center text-[11px] text-neutral-600 mt-3">
          © {new Date().getFullYear()} {nombreGimnasio} • Todos los derechos reservados.
        </footer>
      </div>

      <div className="hidden lg:flex lg:flex-col lg:justify-end superficie-oscura relative min-h-screen overflow-hidden bg-black p-10 xl:p-14">
        <img
          src={showcaseImage}
          alt="Socios entrenando en el gimnasio"
          className="absolute inset-0 h-full w-full object-cover object-center brightness-90"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#070606] via-[#070606]/50 to-transparent" />

        <div className="relative z-10 max-w-lg border-l-2 border-[var(--accent-color,#e50914)] pl-6">
          <p className="mb-3 text-xs font-bold uppercase tracking-[0.16em] text-neutral-400">{nombreGimnasio}</p>
          <h2 className="auth-display text-5xl xl:text-6xl uppercase text-white">
            Crea tu cuenta<br />y empieza<span className="text-[var(--accent-color,#e50914)]">.</span>
          </h2>
          <p className="mt-4 text-sm leading-relaxed text-neutral-300">
            Regístrate, elige tu sucursal y activa tu plan en recepción. Tu entrenador te asigna la primera rutina.
          </p>
        </div>
      </div>
    </div>
  );
};

const CampoRegistro = ({
  icon: Icon,
  label,
  name,
  value,
  onChange,
  placeholder,
  type = 'text',
  required = false,
  minLength,
}) => {
  const [mostrar, setMostrar] = useState(false);
  const esPassword = type === 'password';
  const tipoInput = esPassword ? (mostrar ? 'text' : 'password') : type;

  return (
    <div>
      <label htmlFor={name} className="block text-xs font-bold uppercase tracking-wider text-neutral-400 mb-1.5">
        {label}
      </label>
      <div className="group relative">
        <Icon className="absolute left-3.5 top-3.5 h-4 w-4 text-neutral-500 transition-colors group-focus-within:text-white" />
        <input
          id={name}
          name={name}
          value={value}
          onChange={onChange}
          required={required}
          minLength={minLength}
          type={tipoInput}
          className={`w-full rounded-2xl border border-white/10 bg-[#121111] py-3 pl-11 ${
            esPassword ? 'pr-11' : 'pr-4'
          } text-sm text-white placeholder-neutral-600 outline-none transition-all focus:border-white/30 focus:bg-[#181616] focus:ring-1 focus:ring-white/20`}
          placeholder={placeholder}
        />
        {esPassword && (
          <button
            type="button"
            onClick={() => setMostrar((v) => !v)}
            className="absolute right-3.5 top-3.5 text-neutral-500 transition-colors hover:text-white"
            aria-label={mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          >
            {mostrar ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        )}
      </div>
    </div>
  );
};

export default Registro;
