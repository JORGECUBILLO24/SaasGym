// Búsqueda de la biblioteca de ejercicios. Misma lógica que la API (BusquedaTexto y
// CatalogoService): ignora tildes, mayúsculas, plurales simples y el orden de las palabras,
// y también encuentra por sinónimo, nombre en inglés, músculo, equipo o grupo.

export const normalizar = (texto) => String(texto ?? '')
  .normalize('NFD')
  .replace(/\p{M}+/gu, '')
  .toLowerCase()
  .replace(/[^a-z0-9]+/g, ' ')
  .trim();

const singular = (palabra) => {
  if (palabra.length > 4 && palabra.endsWith('es') && !palabra.endsWith('ses')) return palabra.slice(0, -2);
  if (palabra.length > 3 && palabra.endsWith('s')) return palabra.slice(0, -1);
  return palabra;
};

export const terminos = (consulta) => {
  const normalizada = normalizar(consulta);
  return normalizada ? normalizada.split(' ').map(singular).filter(Boolean) : [];
};

const contieneTodos = (texto, lista) => lista.length > 0 && lista.every((termino) => texto.includes(termino));

const puntuar = (ejercicio, lista) => {
  const nombre = normalizar(ejercicio.nombre);
  const consulta = lista.join(' ');
  if (nombre === consulta) return 100;
  if (nombre.startsWith(consulta)) return 80;
  if (contieneTodos(nombre, lista)) return 60;
  const alias = normalizar((ejercicio.alias || []).join(' '));
  if (contieneTodos(alias, lista)) return 40;
  const resto = normalizar([
    nombre, alias, ejercicio.nombreOriginal, ejercicio.musculoPrincipal, ejercicio.equipo, ejercicio.zona,
    ejercicio.grupoMuscular?.nombre, ...(ejercicio.musculosSecundarios || []),
  ].filter(Boolean).join(' '));
  return contieneTodos(resto, lista) ? 20 : 0;
};

/**
 * Ejercicios que coinciden, mejor coincidencia primero. Sin texto devuelve todos (del grupo,
 * si se indica) en orden alfabético.
 */
export const buscarEjercicios = (ejercicios, consulta, { grupoMuscularId } = {}) => {
  const lista = terminos(consulta);
  const delGrupo = (ejercicios || []).filter((ejercicio) => !grupoMuscularId
    || String(ejercicio.grupoMuscular?.id ?? '') === String(grupoMuscularId));

  if (!lista.length) {
    return [...delGrupo].sort((a, b) => String(a.nombre).localeCompare(String(b.nombre), 'es'));
  }

  return delGrupo
    .map((ejercicio) => ({ ejercicio, puntos: puntuar(ejercicio, lista) }))
    .filter((resultado) => resultado.puntos > 0)
    // En empate va primero el que tiene GIF (se puede mostrar al socio) y después el nombre
    // más corto, que suele ser la variante base del ejercicio.
    .sort((a, b) => b.puntos - a.puntos
      || Number(Boolean(b.ejercicio.gifUrl)) - Number(Boolean(a.ejercicio.gifUrl))
      || String(a.ejercicio.nombre).length - String(b.ejercicio.nombre).length
      || String(a.ejercicio.nombre).localeCompare(String(b.ejercicio.nombre), 'es'))
    .map((resultado) => resultado.ejercicio);
};
