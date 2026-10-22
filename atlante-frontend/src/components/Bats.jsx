import { useEffect, useState } from 'react';
import { activeSeason } from '../lib/season';

// sagoma del pipistrello: corpo e due ali; le ali sbattono con una scala verticale
const WING = 'M0 6 C 4 0, 9 1, 12 5 C 13 3, 15 3, 16 5 C 19 1, 24 0, 28 6 C 25 6, 23 8, 22 10 C 20 8, 18 9, 17 11 C 15.5 10, 12.5 10, 11 11 C 10 9, 8 8, 6 10 C 5 8, 3 6, 0 6 Z';

// rotte diverse: partenza, altezza, durata e ritardo variano per non sembrare un motivo ripetuto
const FLIGHTS = [
  { top: '14%', dur: 19, delay: 0, size: 54, dir: 1 },
  { top: '36%', dur: 26, delay: 3, size: 40, dir: -1 },
  { top: '62%', dur: 22, delay: 7, size: 48, dir: 1 },
  { top: '24%', dur: 31, delay: 11, size: 34, dir: -1 },
  { top: '78%', dur: 24, delay: 15, size: 42, dir: 1 },
];

/**
 * Pipistrelli che attraversano la pagina durante la rassegna di Halloween. Decorativi:
 * non ricevono clic, sono nascosti agli screen reader e spariscono con "riduci movimento".
 */
export default function Bats() {
  const [season, setSeason] = useState(activeSeason);
  useEffect(() => {
    const update = () => setSeason(activeSeason());
    window.addEventListener('visuart:season', update);
    return () => window.removeEventListener('visuart:season', update);
  }, []);
  if (season !== 'halloween') return null;
  return (
    <div className="bats" aria-hidden="true">
      {FLIGHTS.map((f, i) => (
        <span key={i} className={`bat ${f.dir < 0 ? 'bat--rtl' : ''}`}
          style={{ top: f.top, '--dur': `${f.dur}s`, '--delay': `${f.delay}s`, '--size': `${f.size}px` }}>
          <svg viewBox="0 0 28 12"><path d={WING} /></svg>
        </span>
      ))}
    </div>
  );
}
