import { useEffect, useState } from 'react';
import { categories as categoriesApi } from '../api/client';

let cache = null;

// cosa unisce il film e il dipinto in ciascuna categoria: compare al passaggio e per gli screen reader
const HINTS = {
  fotografia: 'La coppia condivide il modo di inquadrare e di trattare la luce dell’immagine',
  composizione: 'Le due opere costruiscono lo spazio allo stesso modo',
  tema: 'Film e dipinto raccontano la stessa idea',
  montaggio: 'Il ritmo e la successione delle immagini richiamano il dipinto',
  luce: 'La luce è il legame tra le due opere',
  colore: 'Le palette del film e del dipinto si somigliano',
};

/** Chip "Tutte" + categorie dell'archivio; value è lo slug (null = tutte). */
export default function CategoryChips({ value, onChange, includeAll = true }) {
  const [cats, setCats] = useState(cache ?? []);

  useEffect(() => {
    if (cache) return;
    categoriesApi.list().then((c) => { cache = c; setCats(c); }).catch(() => {});
  }, []);

  return (
    <div className="chips" role="group" aria-label="Categoria">
      {includeAll && (
        <button className={`chip ${!value ? 'chip--active' : ''}`} onClick={() => onChange(null)} aria-pressed={!value}>
          Tutte
        </button>
      )}
      {cats.map((c) => (
        <button
          key={c.slug}
          className={`chip ${value === c.slug ? 'chip--active' : ''}`}
          onClick={() => onChange(c.slug)}
          aria-pressed={value === c.slug}
          title={HINTS[c.slug] ?? c.description}
        >
          {c.name}
        </button>
      ))}
    </div>
  );
}
