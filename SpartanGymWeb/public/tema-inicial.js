// Aplica el tema y los colores guardados antes del primer pintado (evita el parpadeo).
// Está en un archivo aparte y no en línea para que la Content-Security-Policy no permita scripts en línea.
(() => {
  const clave = 'saasGym.settings';
  const temasValidos = ['light', 'dark', 'system'];
  const obtenerTemaSistema = () =>
    window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches
      ? 'light'
      : 'dark';

  let preferencia;

  try {
    const configuracion = JSON.parse(window.localStorage.getItem(clave) || '{}');
    const nombreGimnasio = typeof configuracion.gymName === 'string' && configuracion.gymName.trim()
      ? configuracion.gymName.trim()
      : 'SaasGym';
    document.title = nombreGimnasio;
    const favicon = document.querySelector('link[rel="icon"]');
    const iniciales = nombreGimnasio
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((palabra) => palabra[0])
      .join('')
      .replace(/[^a-z0-9]/gi, '')
      .toUpperCase() || 'GY';
    if (favicon) {
      const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="18" fill="#090909"/><path d="M8 32h8m32 0h8M16 25v14m32-14v14M21 32h22" stroke="#e50914" stroke-width="5" stroke-linecap="round"/><text x="32" y="57" text-anchor="middle" font-family="system-ui,sans-serif" font-size="12" font-weight="800" fill="#fff">${iniciales}</text></svg>`;
      favicon.href = `data:image/svg+xml,${encodeURIComponent(svg)}`;
    }
    const temaGuardado = temasValidos.includes(configuracion.theme) ? configuracion.theme : 'system';
    preferencia = temaGuardado === 'system' || configuracion.themeSource === 'user'
      ? temaGuardado
      : 'system';
    const colorValido = (valor, respaldo) => /^#[0-9a-f]{6}$/i.test(valor || '') ? valor : respaldo;
    const principal = colorValido(configuracion.accentColor, '#e50914');
    const hover = colorValido(configuracion.accentHoverColor, '#b91c1c');
    const suave = colorValido(configuracion.accentSoftColor, '#450a0a');
    const rgb = [
      Number.parseInt(principal.slice(1, 3), 16),
      Number.parseInt(principal.slice(3, 5), 16),
      Number.parseInt(principal.slice(5, 7), 16),
    ].join(', ');

    document.documentElement.style.setProperty('--accent-color', principal);
    document.documentElement.style.setProperty('--accent-hover-color', hover);
    document.documentElement.style.setProperty('--accent-soft-color', suave);
    document.documentElement.style.setProperty('--accent-rgb', rgb);
  } catch {
    preferencia = 'system';
  }

  document.documentElement.dataset.theme =
    preferencia === 'system' ? obtenerTemaSistema() : preferencia;
  document.documentElement.dataset.themePreference = preferencia;
})();
