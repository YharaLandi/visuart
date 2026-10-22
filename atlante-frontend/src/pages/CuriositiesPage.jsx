import { useEffect, useState } from 'react';
import { imgUrl, phantom as phantomApi } from '../api/client';
import Layout from '../components/ui/Layout';
import { ErrorState, Loading } from '../components/ui/States';
import { OriginDialog, TYPES } from './PhantomPage';
import '../styles/phantom.css';

const FILTERS = [['all', 'Tutte'], ['FILM', 'Film'], ['ARTWORK', 'Quadri']];

/**
 * Curiosità: da dove vengono film e quadri dell'archivio. Tutto l'anno, curate dalla redazione,
 * con la stessa scheda del reparto fantasma ma nei colori normali del sito.
 */
export default function CuriositiesPage() {
  const [list, setList] = useState(null);
  const [failed, setFailed] = useState(false);
  const [filter, setFilter] = useState('all');
  const [current, setCurrent] = useState(null);

  const load = () => {
    setFailed(false);
    phantomApi.curiosities().then(setList).catch(() => setFailed(true));
  };
  useEffect(load, []);

  const visible = (list ?? []).filter((i) => filter === 'all' || i.film.type === filter);

  return (
    <Layout>
      <div className="container phantom phantom--plain">
        <header className="page-head">
          <h1 className="h-page">Curiosità</h1>
          <p className="lead">Da dove vengono i film e i quadri dell’archivio: un racconto, un luogo, un’altra opera. Scelte e scritte dalla redazione.</p>
        </header>

        <div className="chips" role="group" aria-label="Tipo di opera">
          {FILTERS.map(([v, label]) => (
            <button key={v} type="button" className={`chip ${filter === v ? 'chip--active' : ''}`}
              aria-pressed={filter === v} onClick={() => setFilter(v)}>{label}</button>
          ))}
        </div>

        {failed && <ErrorState onRetry={load} />}
        {!list && !failed && <Loading title="Sfogliamo gli appunti della redazione…" />}
        {list && (
          <ul className="phantom-grid">
            {visible.map((item) => (
              <li key={item.id}>
                <button type="button" className="phantom-card" onClick={() => setCurrent(item)}
                  aria-label={`${item.film.title}: da dove viene`}>
                  <span className={`phantom-card__poster ${item.film.type === 'ARTWORK' ? 'frame--art' : 'frame--film'}`}>
                    {imgUrl(item.film.imageUrl) && <img src={imgUrl(item.film.imageUrl)} alt="" loading="lazy" />}
                  </span>
                  <span className="phantom-type">{TYPES[item.type]}</span>
                  <span className="phantom-card__title">{item.film.title}</span>
                  <span className="phantom-card__year">{item.film.creator}{item.film.year ? ` · ${item.film.year}` : ''}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {current && <OriginDialog item={current} onClose={() => setCurrent(null)} />}
      </div>
    </Layout>
  );
}
