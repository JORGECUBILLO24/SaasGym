import { describe, expect, it } from 'vitest';
import { filtrarSocios } from './BuscadorSocio';

const socios = [
  { id: '1', nombres: 'María José', apellidos: 'Pérez', email: 'maria@correo.com', telefono: '8888-1111' },
  { id: '2', nombres: 'Jorge', apellidos: 'Cubillo', email: 'jorge@correo.com' },
  { id: '3', nombres: 'Ana', apellidos: 'Martínez', email: 'ana@correo.com' },
];

describe('filtrarSocios', () => {
  it('sin texto no lista a nadie', () => {
    expect(filtrarSocios(socios, '')).toEqual([]);
    expect(filtrarSocios(socios, '   ')).toEqual([]);
  });

  it('busca sin tildes por nombre, apellido, correo o teléfono', () => {
    expect(filtrarSocios(socios, 'maria perez').map((s: { id: string }) => s.id)).toEqual(['1']);
    expect(filtrarSocios(socios, 'MARTINEZ').map((s: { id: string }) => s.id)).toEqual(['3']);
    expect(filtrarSocios(socios, 'jorge@').map((s: { id: string }) => s.id)).toEqual(['2']);
    expect(filtrarSocios(socios, '8888').map((s: { id: string }) => s.id)).toEqual(['1']);
  });
});
