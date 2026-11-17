import { describe, expect, it } from 'vitest';
import { buscarEjercicios, normalizar, terminos } from './busquedaEjercicios';

const pecho = { id: 1, nombre: 'Pecho' };
const biceps = { id: 6, nombre: 'Bíceps' };

const biblioteca = [
  { id: 1, nombre: 'Aperturas con mancuernas', grupoMuscular: pecho, equipo: 'Mancuerna', musculoPrincipal: 'Pectorales', alias: ['pec fly', 'dumbbell fly'] },
  { id: 2, nombre: 'Press de banca con barra', grupoMuscular: pecho, equipo: 'Barra', musculoPrincipal: 'Pectorales', alias: ['press plano', 'barbell bench press'] },
  { id: 3, nombre: 'Curl de bíceps con mancuerna', grupoMuscular: biceps, equipo: 'Mancuerna', musculoPrincipal: 'Bíceps', alias: ['curl alterno', 'dumbbell biceps curl'] },
  { id: 4, nombre: 'Press de banca inclinado con mancuernas', grupoMuscular: pecho, equipo: 'Mancuerna', musculoPrincipal: 'Pectorales', alias: ['dumbbell incline bench press'] },
];

const nombres = (lista: Array<{ nombre: string }>) => lista.map((ejercicio) => ejercicio.nombre);

describe('busquedaEjercicios', () => {
  it('normaliza tildes, mayúsculas y signos, y pasa a singular', () => {
    expect(normalizar('  Bíceps—CURL ')).toBe('biceps curl');
    expect(terminos('Mancuernas flexiones')).toEqual(['mancuerna', 'flexion']);
  });

  it('encuentra sin tildes, en plural y con las palabras en otro orden', () => {
    expect(nombres(buscarEjercicios(biblioteca, 'BICEPS mancuernas'))).toEqual(['Curl de bíceps con mancuerna']);
    expect(nombres(buscarEjercicios(biblioteca, 'banca press'))[0]).toBe('Press de banca con barra');
  });

  it('encuentra por sinónimo, nombre en inglés y músculo', () => {
    expect(nombres(buscarEjercicios(biblioteca, 'press plano'))).toEqual(['Press de banca con barra']);
    expect(nombres(buscarEjercicios(biblioteca, 'bench press'))).toHaveLength(2);
    expect(nombres(buscarEjercicios(biblioteca, 'pectorales'))).toHaveLength(3);
  });

  it('el filtro de grupo es opcional y sin texto devuelve todo en orden alfabético', () => {
    expect(buscarEjercicios(biblioteca, '')).toHaveLength(4);
    expect(nombres(buscarEjercicios(biblioteca, '', { grupoMuscularId: '6' }))).toEqual(['Curl de bíceps con mancuerna']);
    expect(buscarEjercicios(biblioteca, 'mancuerna', { grupoMuscularId: 1 })).toHaveLength(2);
  });

  it('sin coincidencias devuelve una lista vacía', () => {
    expect(buscarEjercicios(biblioteca, 'sentadilla')).toEqual([]);
  });
});
