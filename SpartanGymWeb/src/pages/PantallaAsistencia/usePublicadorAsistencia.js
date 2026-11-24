import { useEffect, useRef, useState } from 'react';
import { abrirCanal } from './canalAsistencia';

/**
 * Lado de recepción: envía a la pantalla de asistencia cada cambio de `estado` y le responde
 * cuando la pantalla pide el estado actual. `estado` debe venir memorizado (useMemo) para no
 * enviar un mensaje en cada render.
 *
 * Devuelve si hay una pantalla abierta escuchando.
 */
export const usePublicadorAsistencia = (estado) => {
  const canalRef = useRef(null);
  const ultimoRef = useRef(estado);
  const [pantallaConectada, setPantallaConectada] = useState(false);

  useEffect(() => {
    const canal = abrirCanal();
    canalRef.current = canal;
    if (!canal) return undefined;
    canal.onmessage = (evento) => {
      const tipo = evento.data?.tipo;
      if (tipo === 'hola') {
        setPantallaConectada(true);
        canal.postMessage({ tipo: 'estado', datos: ultimoRef.current });
      } else if (tipo === 'adios') {
        setPantallaConectada(false);
      }
    };
    return () => {
      canalRef.current = null;
      canal.close();
    };
  }, []);

  useEffect(() => {
    ultimoRef.current = estado;
    canalRef.current?.postMessage({ tipo: 'estado', datos: estado });
  }, [estado]);

  return pantallaConectada;
};
