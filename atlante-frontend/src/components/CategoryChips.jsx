import { useEffect, useState } from 'react';
import { categories as categoriesApi } from '../api/client';

let cache = null;

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
        >
          {c.name}
        </button>
      ))}
    </div>
  );
}
