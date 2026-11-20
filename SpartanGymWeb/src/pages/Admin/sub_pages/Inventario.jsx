import { useEffect, useState } from 'react';
import {
  AlertTriangle, ArrowLeft, CheckCircle2, DollarSign, Edit, Image as ImageIcon,
  CookingPot, Layers, Package, Plus, Save, Search, Tag, Trash2, X,
} from 'lucide-react';
import TarjetaMetrica from '../../../components/TarjetaMetrica';
import {
  formatearMoneda,
  MONEDAS_DISPONIBLES,
  useConfiguracionApp,
} from '../../../utils/configuracionApp';
import { ajusteStockApi, inventarioApi, sucursalesApi } from '../../../services/api';
import { prepararImagen } from '../../../utils/imagen';
import { normalizar, terminos } from '../../../utils/busquedaEjercicios';

const crearProductoInicial = (sucursalId = '') => ({
  nombre: '', categoria: 'Suplementos', precio: '', stock: '', sucursalId, imagenUrl: '',
  descripcion: '', unidad: 'unidad', vendible: true, preparado: false, componentes: [],
});

const UNIDADES = [
  { valor: 'unidad', etiqueta: 'Unidades' },
  { valor: 'g', etiqueta: 'Gramos (g)' },
  { valor: 'ml', etiqueta: 'Mililitros (ml)' },
  { valor: 'servicio', etiqueta: 'Servicios' },
];

const abreviarUnidad = (unidad) => (!unidad || unidad === 'unidad' ? 'unid.' : unidad);

// Existencia que se puede vender: en un preparado la calcula la API según sus insumos.
const existenciaDe = (producto) => Number(producto?.disponible ?? producto?.stock ?? 0);

const resumenReceta = (producto) => (producto?.componentes || [])
  .map((componente) => `${componente.cantidad} ${abreviarUnidad(componente.insumo?.unidad)} ${componente.insumo?.nombre || ''}`.trim())
  .join(' · ');

const obtenerIdCorto = (id = '') => {
  const texto = String(id || '').trim();
  return texto ? texto.split('-')[0].toUpperCase() : 'SIN ID';
};

const Inventario = () => {
  const configuracion = useConfiguracionApp();
  const monedaActual = MONEDAS_DISPONIBLES.find((moneda) => moneda.codigo === configuracion.currency) || MONEDAS_DISPONIBLES[1];
  const formatearMonto = (valor) => formatearMoneda(valor, configuracion.currency);
  const [productos, setProductos] = useState([]);
  const [sucursales, setSucursales] = useState([]);
  const [nuevoProducto, setNuevoProducto] = useState(() => crearProductoInicial());
  const [productoEditandoId, setProductoEditandoId] = useState('');
  const [vista, setVista] = useState('lista');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSaved, setIsSaved] = useState(false);
  const [errorApi, setErrorApi] = useState('');
  const [busqueda, setBusqueda] = useState('');
  // Sin búsqueda ni filtro no se lista nada: el catálogo completo ocupa toda la pantalla.
  const [filtro, setFiltro] = useState('');
  const [ajuste, setAjuste] = useState({ cantidad: '', motivo: '' });
  const [ajustando, setAjustando] = useState(false);
  const [mensajeAjuste, setMensajeAjuste] = useState('');

  // Editar un producto ya no cambia su stock (la API lo ignora): el stock se mueve con ventas o
  // con un ajuste explícito, que queda registrado con su motivo y no pisa ventas simultáneas.
  const aplicarAjuste = async (signo) => {
    const cantidad = Math.abs(Math.trunc(Number(ajuste.cantidad)));
    if (!cantidad) {
      setMensajeAjuste('Indica cuántas unidades sumar o restar.');
      return;
    }
    if (!ajuste.motivo.trim()) {
      setMensajeAjuste('Escribe el motivo (compra, merma, conteo...).');
      return;
    }
    setAjustando(true);
    setMensajeAjuste('');
    try {
      const actualizado = await ajusteStockApi.ajustar(productoEditandoId, signo * cantidad, ajuste.motivo.trim());
      setNuevoProducto((actual) => ({ ...actual, stock: actualizado?.stock ?? actual.stock }));
      setAjuste({ cantidad: '', motivo: '' });
      setMensajeAjuste(`Stock actualizado: ${actualizado?.stock ?? ''}`);
      await cargarDatos();
    } catch (error) {
      setMensajeAjuste(error.message || 'No se pudo ajustar el stock.');
    } finally {
      setAjustando(false);
    }
  };

  const cargarDatos = async () => {
    const [productosApi, sucursalesApiData] = await Promise.all([
      inventarioApi.listar(),
      sucursalesApi.listar(),
    ]);
    setProductos(productosApi);
    setSucursales(sucursalesApiData);
    setNuevoProducto((actual) => ({
      ...actual,
      sucursalId: actual.sucursalId || sucursalesApiData[0]?.id || '',
    }));
  };

  useEffect(() => {
    Promise.resolve().then(cargarDatos).catch(() => setErrorApi('No se pudo cargar el inventario desde la API.'));
  }, []);

  const abrirNuevoProducto = () => {
    setNuevoProducto(crearProductoInicial(sucursales[0]?.id || ''));
    setProductoEditandoId('');
    setIsSaved(false);
    setErrorApi('');
    setVista('editor');
  };

  const editarProducto = (producto) => {
    setProductoEditandoId(producto.id);
    setAjuste({ cantidad: '', motivo: '' });
    setMensajeAjuste('');
    setIsSaved(false);
    setErrorApi('');
    setNuevoProducto({
      nombre: producto.nombre || '',
      categoria: producto.categoria || 'Suplementos',
      precio: producto.precio ?? '',
      stock: producto.stock ?? '',
      sucursalId: producto.sucursal?.id || producto.sucursalId || '',
      imagenUrl: producto.imagenUrl || '',
      descripcion: producto.descripcion || '',
      unidad: producto.unidad || 'unidad',
      vendible: producto.vendible !== false,
      preparado: Boolean(producto.componentes?.length),
      componentes: (producto.componentes || []).map((componente) => ({
        insumoId: componente.insumo?.id || '',
        cantidad: componente.cantidad ?? '',
      })),
    });
    setVista('editor');
  };

  const volverAlCatalogo = () => {
    setVista('lista');
    setProductoEditandoId('');
    setIsSaved(false);
    setErrorApi('');
    setNuevoProducto(crearProductoInicial(sucursales[0]?.id || ''));
  };

  const handleChange = (evento) => {
    const { name, value, type, checked } = evento.target;
    setNuevoProducto((actual) => ({
      ...actual,
      [name]: type === 'checkbox' ? checked : value,
      // Al cambiar de sucursal la receta deja de valer: sus insumos son de la sucursal anterior.
      ...(name === 'sucursalId' && value !== actual.sucursalId ? { componentes: actual.preparado ? [{ insumoId: '', cantidad: '' }] : [] } : {}),
    }));
  };

  const cambiarPreparado = (preparado) => {
    setNuevoProducto((actual) => ({
      ...actual,
      preparado,
      componentes: preparado && !actual.componentes.length ? [{ insumoId: '', cantidad: '' }] : actual.componentes,
    }));
  };

  const cambiarComponente = (indice, campo, valor) => {
    setNuevoProducto((actual) => ({
      ...actual,
      componentes: actual.componentes.map((componente, i) => (i === indice ? { ...componente, [campo]: valor } : componente)),
    }));
  };

  const agregarComponente = () => {
    setNuevoProducto((actual) => ({ ...actual, componentes: [...actual.componentes, { insumoId: '', cantidad: '' }] }));
  };

  const quitarComponente = (indice) => {
    setNuevoProducto((actual) => ({ ...actual, componentes: actual.componentes.filter((_, i) => i !== indice) }));
  };

  const handleImagenChange = async (evento) => {
    const archivo = evento.target.files?.[0];
    if (!archivo) return;
    setErrorApi('');
    try {
      const imagenUrl = await prepararImagen(archivo, { maxLado: 720, calidad: 0.86, maxBytesEntrada: 3 * 1024 * 1024 });
      setNuevoProducto((actual) => ({ ...actual, imagenUrl }));
    } catch (error) {
      setErrorApi(error.message || 'No se pudo cargar la imagen.');
    } finally {
      evento.target.value = '';
    }
  };

  const handleSubmit = async (evento) => {
    evento.preventDefault();
    setIsSubmitting(true);
    setErrorApi('');
    try {
      const componentes = nuevoProducto.preparado
        ? nuevoProducto.componentes.map((componente) => ({ insumoId: componente.insumoId, cantidad: Number(componente.cantidad) }))
        : [];
      const payload = {
        nombre: nuevoProducto.nombre.trim(),
        categoria: nuevoProducto.categoria,
        precio: Number(nuevoProducto.precio),
        stock: componentes.length ? 0 : Number(nuevoProducto.stock),
        imagenUrl: nuevoProducto.imagenUrl,
        sucursalId: nuevoProducto.sucursalId || null,
        descripcion: nuevoProducto.descripcion.trim(),
        unidad: nuevoProducto.unidad,
        vendible: nuevoProducto.vendible,
        componentes,
      };
      await (productoEditandoId
        ? inventarioApi.actualizar(productoEditandoId, payload)
        : inventarioApi.crear(payload));
      await cargarDatos();
      setIsSaved(true);
      window.setTimeout(() => {
        setVista('lista');
        setProductoEditandoId('');
        setNuevoProducto(crearProductoInicial(sucursales[0]?.id || ''));
        setIsSaved(false);
      }, 900);
    } catch (error) {
      setErrorApi(error?.message || (productoEditandoId ? 'No se pudo actualizar el producto.' : 'No se pudo guardar el producto.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const eliminarProducto = async (id) => {
    setErrorApi('');
    try {
      await inventarioApi.eliminar(id);
      await cargarDatos();
    } catch (error) {
      setErrorApi(error?.message || 'No se pudo eliminar el producto.');
    }
  };

  // Un insumo en gramos o mililitros no está "bajo" con 5: se avisa cuando queda poco en su unidad.
  const enAlerta = (producto) => {
    const umbral = producto.unidad === 'g' || producto.unidad === 'ml' ? 500 : 5;
    return existenciaDe(producto) <= umbral;
  };

  const listando = Boolean(busqueda.trim()) || Boolean(filtro);
  const productosFiltrados = !listando ? [] : productos.filter((producto) => {
    const coincideCategoria = !filtro || filtro === 'todos'
      || (filtro === 'alertas' ? enAlerta(producto) : producto.categoria?.toLowerCase() === filtro);
    // Sin tildes y en cualquier orden; también por categoría, descripción, insumos o ID.
    const texto = normalizar([producto.nombre, producto.categoria, producto.descripcion, resumenReceta(producto), producto.id, obtenerIdCorto(producto.id)].filter(Boolean).join(' '));
    const coincideBusqueda = terminos(busqueda).every((termino) => texto.includes(termino));
    return coincideCategoria && coincideBusqueda;
  });

  const totalProductos = productos.length;
  const valorInventario = productos.reduce((total, producto) => total + (Number(producto.precio) * Number(producto.stock)), 0);
  const alertasStock = productos.filter(enAlerta).length;
  const totalCategorias = new Set(productos.map((producto) => producto.categoria).filter(Boolean)).size;

  // Insumos que puede llevar la receta: de la misma sucursal, sin receta propia y distintos del producto.
  const insumosDisponibles = productos.filter((producto) => {
    const sucursalProducto = producto.sucursal?.id || producto.sucursalId || '';
    return producto.id !== productoEditandoId
      && !producto.componentes?.length
      && (!nuevoProducto.sucursalId || !sucursalProducto || sucursalProducto === nuevoProducto.sucursalId);
  });

  const lineasReceta = nuevoProducto.componentes
    .map((componente) => ({ insumo: insumosDisponibles.find((item) => item.id === componente.insumoId), cantidad: Number(componente.cantidad) }))
    .filter((linea) => linea.insumo && linea.cantidad > 0);
  const disponiblePreparado = lineasReceta.length && lineasReceta.length === nuevoProducto.componentes.length
    ? Math.min(...lineasReceta.map((linea) => Math.floor(Number(linea.insumo.stock || 0) / linea.cantidad)))
    : null;

  if (vista === 'editor') {
    return (
      <div className="workspace-page pb-4 text-white">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex min-w-0 items-center gap-3">
            <button type="button" onClick={volverAlCatalogo} aria-label="Volver al inventario" className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-gray-300 transition-colors hover:bg-white/10 hover:text-white">
              <ArrowLeft size={19} />
            </button>
            <div>
              <h1 className="text-xl font-bold sm:text-2xl">{productoEditandoId ? 'Editar producto' : 'Añadir producto'}</h1>
              <p className="mt-1 text-sm text-gray-400">{productoEditandoId ? `ID ${obtenerIdCorto(productoEditandoId)}` : 'Completa los datos para incorporarlo al catálogo.'}</p>
            </div>
          </div>
          <span className="rounded-full bg-white/[0.06] px-3 py-1.5 text-xs text-gray-400">Inventario · {sucursales.find((sucursal) => sucursal.id === nuevoProducto.sucursalId)?.nombre || 'Sucursal'}</span>
        </div>

        {errorApi && <p role="alert" className="rounded-lg bg-red-500/10 px-3 py-2 text-sm text-red-300">{errorApi}</p>}

        <form onSubmit={handleSubmit} className="inventory-editor">
          <div className="inventory-editor-image">
            <label className="group relative flex min-h-32 cursor-pointer items-center gap-4 overflow-hidden rounded-xl border border-dashed border-white/15 bg-white/[0.025] p-4 transition-colors hover:border-red-500/50">
              <span className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-black/40 text-gray-500">
                {nuevoProducto.imagenUrl ? <img src={nuevoProducto.imagenUrl} alt="Vista previa del producto" className="h-full w-full object-cover" /> : <ImageIcon size={24} />}
              </span>
              <span className="min-w-0">
                <span className="block text-sm font-semibold text-white">Imagen del producto</span>
                <span className="mt-1 block text-xs text-gray-400">PNG, JPG o WEBP · hasta 3 MB</span>
                <span className="mt-2 inline-flex text-xs font-semibold text-red-300">{nuevoProducto.imagenUrl ? 'Cambiar imagen' : 'Seleccionar archivo'}</span>
              </span>
              <input type="file" accept="image/*" onChange={handleImagenChange} className="sr-only" />
            </label>
            {nuevoProducto.imagenUrl && (
              <button type="button" onClick={() => setNuevoProducto((actual) => ({ ...actual, imagenUrl: '' }))} className="mt-2 inline-flex items-center gap-1 text-xs text-gray-400 transition-colors hover:text-red-300">
                <X size={13} /> Quitar imagen
              </button>
            )}
          </div>

          <label className="inventory-editor-name block">
            <span className="mb-1.5 block text-xs font-semibold text-gray-400">Nombre del producto</span>
            <input type="text" name="nombre" required value={nuevoProducto.nombre} onChange={handleChange} placeholder="Ej. Proteína ISO 100" className="inventory-editor-input" />
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold text-gray-400">Categoría</span>
            <span className="relative block">
              <Tag size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
              <select name="categoria" value={nuevoProducto.categoria} onChange={handleChange} className="inventory-editor-input pl-9">
                <option value="Suplementos">Suplementos</option>
                <option value="Mercancía">Mercancía (Ropa)</option>
                <option value="Accesorios">Accesorios (Shakers, Cintos)</option>
                <option value="Bebidas">Bebidas / Snacks</option>
                <option value="Insumos">Insumos (para preparar)</option>
              </select>
            </span>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold text-gray-400">Sucursal</span>
            <select name="sucursalId" required value={nuevoProducto.sucursalId} onChange={handleChange} className="inventory-editor-input">
              <option value="">Selecciona sucursal</option>
              {sucursales.map((sucursal) => <option key={sucursal.id} value={sucursal.id}>{sucursal.nombre}</option>)}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold text-gray-400">Precio ({monedaActual.simbolo})</span>
            <input type="number" name="precio" min="0" step="0.01" required value={nuevoProducto.precio} onChange={handleChange} placeholder="0.00" className="inventory-editor-input" />
          </label>

          {nuevoProducto.preparado ? (
            <div className="block">
              <span className="mb-1.5 block text-xs font-semibold text-gray-400">Existencias</span>
              <p className="flex min-h-11 items-center rounded-[.65rem] border border-white/10 px-3 text-sm font-semibold text-gray-300">
                {disponiblePreparado === null ? 'Completa la receta' : `Alcanza para ${disponiblePreparado}`}
              </p>
            </div>
          ) : (
            productoEditandoId ? (
              <div className="block sm:col-span-2">
                <span className="mb-1.5 block text-xs font-semibold text-gray-400">Stock actual ({abreviarUnidad(nuevoProducto.unidad)})</span>
                <p className="flex min-h-11 items-center rounded-[.65rem] border border-white/10 px-3 text-sm font-semibold text-gray-200">{nuevoProducto.stock}</p>
                <div className="mt-2 grid gap-2 sm:grid-cols-[110px_1fr_auto_auto]">
                  <input type="number" min="1" step="1" aria-label="Unidades a ajustar" value={ajuste.cantidad} onChange={(e) => setAjuste((a) => ({ ...a, cantidad: e.target.value }))} placeholder="Cantidad" className="inventory-editor-input" />
                  <input type="text" maxLength={200} aria-label="Motivo del ajuste" value={ajuste.motivo} onChange={(e) => setAjuste((a) => ({ ...a, motivo: e.target.value }))} placeholder="Motivo: compra, merma, conteo..." className="inventory-editor-input" />
                  <button type="button" disabled={ajustando} onClick={() => aplicarAjuste(1)} className="rounded-[.65rem] bg-green-600 px-3 py-2 text-xs font-bold text-white disabled:opacity-50">+ Sumar</button>
                  <button type="button" disabled={ajustando} onClick={() => aplicarAjuste(-1)} className="rounded-[.65rem] bg-red-600 px-3 py-2 text-xs font-bold text-white disabled:opacity-50">− Restar</button>
                </div>
                {mensajeAjuste && <p className="mt-1.5 text-xs text-gray-400" role="status">{mensajeAjuste}</p>}
              </div>
            ) : (
              <label className="block">
                <span className="mb-1.5 block text-xs font-semibold text-gray-400">Stock inicial ({abreviarUnidad(nuevoProducto.unidad)})</span>
                <input type="number" name="stock" min="0" step="1" required value={nuevoProducto.stock} onChange={handleChange} placeholder={nuevoProducto.unidad === 'g' ? 'Ej. 2270' : 'Ej. 10'} className="inventory-editor-input" />
              </label>
            )
          )}

          {!nuevoProducto.preparado && (
            <label className="block">
              <span className="mb-1.5 block text-xs font-semibold text-gray-400">Se cuenta en</span>
              <select name="unidad" value={nuevoProducto.unidad} onChange={handleChange} className="inventory-editor-input">
                {UNIDADES.map((unidad) => <option key={unidad.valor} value={unidad.valor}>{unidad.etiqueta}</option>)}
              </select>
            </label>
          )}

          <label className="inventory-editor-check border border-white/10">
            <input type="checkbox" name="vendible" checked={nuevoProducto.vendible} onChange={handleChange} />
            <span>
              <strong className="text-white">Se vende al público</strong>
              <small className="text-gray-500">Desmárcalo en insumos como leche o proteína a granel: no aparecen en ventas ni en la app.</small>
            </span>
          </label>

          <label className="inventory-editor-wide block">
            <span className="mb-1.5 block text-xs font-semibold text-gray-400">Descripción · opcional</span>
            <textarea name="descripcion" rows={2} value={nuevoProducto.descripcion} onChange={handleChange} placeholder="Ej. 30 g de proteína con 300 ml de leche" className="inventory-editor-input resize-y" />
          </label>

          <fieldset className="inventory-editor-wide inventory-recipe border border-white/10">
            <legend className="sr-only">Receta</legend>
            <div className="inventory-recipe-head">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white/[0.05] text-gray-300"><CookingPot size={17} /></span>
              <span className="min-w-0 flex-1">
                <strong className="text-white">Se prepara con otros productos</strong>
                <small className="text-gray-500">Al venderlo se descuenta cada insumo de la receta. Ej. un batido descuenta proteína y leche.</small>
              </span>
              <label className="relative inline-flex shrink-0 cursor-pointer items-center">
                <input type="checkbox" aria-label="Se prepara con otros productos" checked={nuevoProducto.preparado} onChange={(evento) => cambiarPreparado(evento.target.checked)} className="toggle-switch-input peer sr-only" />
                <span className="toggle-switch-track h-6 w-11 rounded-full bg-gray-700 transition-all after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-gray-300 after:bg-white after:transition-all after:content-[''] peer-checked:after:translate-x-full peer-checked:after:border-white peer-focus-visible:ring-2 peer-focus-visible:ring-offset-2" />
              </label>
            </div>

            {nuevoProducto.preparado && (
              <div className="mt-3 space-y-2">
                {!insumosDisponibles.length && (
                  <p className="rounded-lg bg-orange-500/10 px-3 py-2 text-xs text-orange-200">
                    Esta sucursal no tiene insumos todavía. Crea primero productos como «Leche» o «Proteína a granel» y vuelve aquí.
                  </p>
                )}
                {nuevoProducto.componentes.map((componente, indice) => {
                  const insumo = insumosDisponibles.find((item) => item.id === componente.insumoId);
                  const usados = new Set(nuevoProducto.componentes.filter((_, i) => i !== indice).map((item) => item.insumoId));
                  return (
                    <div key={indice} className="inventory-recipe-row">
                      <select aria-label={`Insumo ${indice + 1}`} required value={componente.insumoId} onChange={(evento) => cambiarComponente(indice, 'insumoId', evento.target.value)} className="inventory-editor-input">
                        <option value="">Elige un insumo</option>
                        {insumosDisponibles.filter((item) => !usados.has(item.id)).map((item) => (
                          <option key={item.id} value={item.id}>{item.nombre} · {item.stock} {abreviarUnidad(item.unidad)}</option>
                        ))}
                      </select>
                      <label className="inventory-recipe-qty">
                        <input type="number" aria-label={`Cantidad de insumo ${indice + 1}`} min="1" step="1" required value={componente.cantidad} onChange={(evento) => cambiarComponente(indice, 'cantidad', evento.target.value)} placeholder="0" className="inventory-editor-input" />
                        <span>{abreviarUnidad(insumo?.unidad)}</span>
                      </label>
                      <button type="button" onClick={() => quitarComponente(indice)} aria-label={`Quitar insumo ${indice + 1}`} className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-red-500/10 hover:text-red-300"><Trash2 size={15} /></button>
                    </div>
                  );
                })}
                <button type="button" onClick={agregarComponente} disabled={nuevoProducto.componentes.length >= insumosDisponibles.length} className="inline-flex min-h-10 items-center gap-2 rounded-lg px-3 text-xs font-semibold text-gray-300 transition-colors hover:bg-white/[0.06] hover:text-white disabled:opacity-40">
                  <Plus size={14} /> Agregar insumo
                </button>
              </div>
            )}
          </fieldset>

          <div className="inventory-editor-actions">
            <button type="button" onClick={volverAlCatalogo} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-4 text-sm font-semibold text-gray-300 transition-colors hover:bg-white/[0.06] hover:text-white">
              <X size={16} /> Cancelar
            </button>
            <button type="submit" disabled={isSubmitting || isSaved} className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-5 text-sm font-bold text-white transition-colors disabled:cursor-wait disabled:opacity-60 ${isSaved ? 'bg-emerald-700' : 'bg-red-600 hover:bg-red-500'}`}>
              {isSubmitting ? 'Guardando…' : isSaved ? <><CheckCircle2 size={16} /> Guardado</> : <><Save size={16} /> {productoEditandoId ? 'Guardar cambios' : 'Añadir al inventario'}</>}
            </button>
          </div>
        </form>
      </div>
    );
  }

  return (
    <div className="workspace-page pb-4 text-white">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold sm:text-2xl">Inventario</h1>
          <p className="mt-1 text-sm text-gray-400">Productos, existencias y precios por sucursal.</p>
        </div>
        <button type="button" onClick={abrirNuevoProducto} className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-red-600 px-4 text-sm font-bold text-white transition-colors hover:bg-red-500">
          <Plus size={16} /> Añadir producto
        </button>
      </div>

      {errorApi && <p role="alert" className="rounded-lg bg-red-500/10 px-3 py-2 text-sm text-red-300">{errorApi}</p>}

      <div className="workspace-grid--metrics">
        <TarjetaMetrica titulo="Total de Productos" valor={totalProductos} icono={Package} color="text-blue-500" detalle="Artículos en catálogo" />
        <TarjetaMetrica titulo="Valor del Inventario" valor={formatearMonto(valorInventario)} icono={DollarSign} color="text-green-500" detalle="Capital invertido" />
        <TarjetaMetrica titulo="Alertas de Stock" valor={alertasStock} icono={AlertTriangle} color="text-orange-500" detalle="Existencias por agotarse" />
        <TarjetaMetrica titulo="Categorías" valor={totalCategorias} icono={Layers} color="text-gray-400" detalle="En el catálogo" />
      </div>

      <section className="min-w-0 rounded-xl border border-white/10 bg-[#090909] p-4 sm:p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-base font-bold">Catálogo de productos</h2>
            <p className="mt-1 text-xs text-gray-400">{listando ? `${productosFiltrados.length} resultados` : `${productos.length} productos · busca o elige un filtro`}</p>
          </div>
          <label className="relative w-full sm:max-w-xs">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input type="search" placeholder="Buscar producto o ID" value={busqueda} onChange={(evento) => setBusqueda(evento.target.value)} className="w-full rounded-lg border border-white/10 bg-[#111] py-2 pl-9 pr-3 text-sm text-white outline-none transition-colors focus:border-red-500/60" />
          </label>
        </div>

        <div className="mb-4 flex flex-wrap gap-2">
          <FilterBtn label="Todos" value="todos" current={filtro} setFiltro={setFiltro} />
          <FilterBtn label="Suplementos" value="suplementos" current={filtro} setFiltro={setFiltro} />
          <FilterBtn label="Bebidas / Snacks" value="bebidas" current={filtro} setFiltro={setFiltro} />
          <FilterBtn label="Accesorios" value="accesorios" current={filtro} setFiltro={setFiltro} />
          <FilterBtn label="Mercancía" value="mercancía" current={filtro} setFiltro={setFiltro} />
          <FilterBtn label="Insumos" value="insumos" current={filtro} setFiltro={setFiltro} />
          <FilterBtn label="Alertas de stock" value="alertas" current={filtro} setFiltro={setFiltro} />
        </div>

        <div className="overflow-x-auto rounded-lg border border-white/[0.07]">
          <table className="w-full min-w-[700px] border-collapse text-left">
            <thead className="bg-white/[0.025] text-[10px] font-bold uppercase tracking-wider text-gray-500">
              <tr>
                <th className="px-3 py-3">Producto</th><th className="px-3 py-3">Categoría</th><th className="px-3 py-3">Sucursal</th><th className="px-3 py-3 text-center">Stock</th><th className="px-3 py-3 text-right">Precio</th><th className="px-3 py-3 text-center">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.06] text-sm">
              {productosFiltrados.map((producto) => (
                <tr key={producto.id} className="transition-colors hover:bg-white/[0.025]">
                  <td className="px-3 py-3">
                    <div className="flex min-w-0 items-center gap-3">
                      <span className="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-white/[0.04] text-gray-500">
                        {producto.imagenUrl ? <img src={producto.imagenUrl} alt="" className="h-full w-full object-cover" /> : <Package size={16} />}
                      </span>
                      <span className="min-w-0">
                        <span className="flex items-center gap-2">
                          <span className="block truncate font-semibold text-white">{producto.nombre}</span>
                          {producto.vendible === false && <span className="shrink-0 rounded bg-white/[0.06] px-1.5 py-0.5 text-[10px] font-semibold text-gray-400">Insumo</span>}
                        </span>
                        {producto.componentes?.length
                          ? <span className="mt-0.5 block truncate text-[11px] text-gray-500" title={resumenReceta(producto)}>Lleva {resumenReceta(producto)}</span>
                          : <span className="mt-0.5 block font-mono text-[10px] text-gray-500" title={producto.id}>ID {obtenerIdCorto(producto.id)}</span>}
                      </span>
                    </div>
                  </td>
                  <td className="px-3 py-3 text-gray-400">{producto.categoria}</td>
                  <td className="px-3 py-3 text-gray-400">{producto.sucursal?.nombre || producto.sucursal || 'Sin sucursal'}</td>
                  <td className="px-3 py-3 text-center">
                    <span className={`text-xs font-semibold ${enAlerta(producto) ? 'text-orange-300' : 'text-gray-200'}`}>
                      {existenciaDe(producto) === 0
                        ? (producto.componentes?.length ? 'Sin insumos' : 'Agotado')
                        : producto.componentes?.length
                          ? `Alcanza para ${existenciaDe(producto)}`
                          : `${producto.stock} ${abreviarUnidad(producto.unidad)}`}
                    </span>
                  </td>
                  <td className="px-3 py-3 text-right font-semibold tabular-nums text-emerald-400">{formatearMonto(producto.precio)}</td>
                  <td className="px-3 py-3">
                    <div className="flex justify-center gap-1">
                      <button type="button" onClick={() => editarProducto(producto)} aria-label={`Editar ${producto.nombre}`} className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-white/[0.07] hover:text-white"><Edit size={15} /></button>
                      <button type="button" onClick={() => eliminarProducto(producto.id)} aria-label={`Eliminar ${producto.nombre}`} className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-gray-400 transition-colors hover:bg-red-500/10 hover:text-red-300"><Trash2 size={15} /></button>
                    </div>
                  </td>
                </tr>
              ))}
              {!productosFiltrados.length && <tr><td colSpan="6" className="px-4 py-10 text-center text-sm text-gray-500">{listando ? 'No se encontraron productos para esos filtros.' : 'Busca un producto por nombre o ID, o elige un filtro. «Todos» muestra el catálogo completo.'}</td></tr>}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};

const FilterBtn = ({ label, value, current, setFiltro }) => (
  <button type="button" onClick={() => setFiltro((actual) => (actual === value ? '' : value))} aria-pressed={current === value} className={`min-h-9 rounded-lg px-3 text-xs font-semibold transition-colors ${current === value ? 'bg-red-600/15 text-red-300' : 'text-gray-400 hover:bg-white/[0.06] hover:text-white'}`}>
    {label}
  </button>
);

export default Inventario;
