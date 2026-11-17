import { CalendarCheck, Dumbbell, UsersRound } from 'lucide-react';
import ImagenEntrenador from '../assets/gym-youngla-coach.webp';
import ImagenInstalaciones from '../assets/gym-aesthetic-workout.webp';
import ImagenComunidad from '../assets/gym-youngla-athlete.webp';

export const beneficiosInicio = [
  {
    ruta: 'entrenamiento',
    titulo: 'Entrenadores en sala',
    etiqueta: 'Acompañamiento',
    resumen:
      'Siempre hay un entrenador en la sala. Te corrige la técnica, ajusta las cargas y te dice cómo seguir cuando una rutina se queda corta.',
    imagen: ImagenEntrenador,
    imagenAlt: 'Entrenador corrigiendo la técnica de un levantamiento',
    icono: Dumbbell,
    estadistica: 'Entrenadores en cada turno',
    puntos: [
      'Corrección de técnica en el momento.',
      'Rutinas asignadas que puedes seguir desde la app.',
      'Ajustes de carga según cómo progresas.',
    ],
  },
  {
    ruta: 'instalaciones',
    titulo: 'Equipo completo',
    etiqueta: 'Instalaciones',
    resumen:
      'Racks, plataformas, mancuernas y máquinas para cada grupo muscular. El equipo se revisa a diario y la sala se limpia entre turnos.',
    imagen: ImagenInstalaciones,
    imagenAlt: 'Sala de pesas con mancuernas y máquinas',
    icono: CalendarCheck,
    estadistica: 'Peso libre y máquinas',
    puntos: [
      'Zona de peso libre separada de las máquinas.',
      'Sala ventilada con espacio entre estaciones.',
      'Revisión diaria del equipo.',
    ],
  },
  {
    ruta: 'comunidad',
    titulo: 'Gente que vuelve',
    etiqueta: 'Comunidad',
    resumen:
      'Entrenas junto a socios que vienen a lo mismo que tú. Se respeta el turno en las máquinas, se recoge el material y se pide ayuda sin problema.',
    imagen: ImagenComunidad,
    imagenAlt: 'Socios entrenando en la sala de pesas',
    icono: UsersRound,
    estadistica: 'Socios constantes',
    puntos: [
      'Normas de sala claras y respetadas.',
      'Socios dispuestos a ayudarte con una serie.',
      'Asistencia registrada para ver tu constancia.',
    ],
  },
];
