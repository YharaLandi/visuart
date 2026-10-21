import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { imgUrl } from '../api/client';
import WorkSheet from '../components/WorkSheet';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { Loading } from '../components/ui/States';
import { Motif } from '../components/ui/Visual';
import { useApp } from '../context/AppContext';

const FILTERS = [['all', 'Tutte'], ['FILM', 'Film'], ['ARTWORK', 'Opere']];

export default function CollectionPage() {
  const { favIds, refreshFavorites, toggleFavorite } = useApp();
  const [works, setWorks] = useState(null);
  const [filter, setFilter] = useState('all');
  const [open, setOpen] = useState(null);

  useEffect(() => {
    refreshFavorites().then(setWorks);
  }, [refreshFavorites]);

  // tiene la lista allineata se un preferito viene tolto da un'altra parte
  const visible = useMemo(() => (works ?? [])
    .filter((w) => favIds.has(w.id))
    .filter((w) => filter === 'all' || w.type === filter), [works, favIds, filter]);

  const remove = (w) => toggleFavorite(w.id);

  return (
    <Layout>
      <div className="container">
        <header className="page-head">
          <p className="eyebrow">Il tuo atlante personale</p>
          <h1 className="h-page">La mia collezione</h1>
          <p className="lead">Conserva le opere che ti fanno fermare. Il prossimo accostamento potrebbe cominciare qui.</p>
        </header>

        <div className="toolbar">
          <div className="chips">
            {FILTERS.map(([v, label]) => (
              <button key={v} className={`chip ${filter === v ? 'chip--active' : ''}`} onClick={() => setFilter(v)} aria-pressed={filter === v}>{label}</button>
            ))}
          </div>
          <span className="toolbar__count">
            {visible.length} opere{visible.length > 0 && <> · Ultime salvate <Icon name="arrowDown" /></>}
          </span>
        </div>

        {works === null && <Loading title="Apriamo la tua collezione…" />}

        {works !== null && visible.length === 0 && (
          <div className="empty">
            <Motif size="lg" />
            <h2>Il tuo sguardo parte da qui.</h2>
            <p>Non hai ancora salvato nessuna opera. Esplora l’archivio e scegli ciò che vuoi ritrovare.</p>
            <Link to="/cerca" className="btn btn--primary"><Icon name="search" /> Esplora gli archivi</Link>
          </div>
        )}

        {visible.length > 0 && (
          <div className="cards-3">
            {visible.map((w) => {
              const isArt = w.type === 'ARTWORK';
              const src = imgUrl(w.imageUrl);
              return (
                <article key={w.id} className="card coll-card">
                  <div className={'coll-card__img' + (isArt ? ' frame--art' : ' frame--film')}>{src && <img src={src} alt={w.title} />}</div>
                  <div className="coll-card__body">
                    <p className={`eyebrow ${isArt ? 't-art' : 't-film'}`}>{isArt ? 'Opera d’arte' : 'Film'} · Salvata</p>
                    <h3 className={isArt ? 't-art' : 't-film'}>{w.title}</h3>
                    <p>{w.creator}{w.year ? ` · ${w.year}` : ''}</p>
                    <div className="coll-card__actions">
                      <button onClick={() => setOpen(w)}>Apri la scheda <Icon name="arrowUpRight" /></button>
                      <button onClick={() => remove(w)}>Rimuovi</button>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>

      {open && (
        <WorkSheet
          item={open}
          type={open.type}
          context="LA MIA COLLEZIONE"
          saved={favIds.has(open.id)}
          onSave={() => toggleFavorite(open.id)}
          onRemove={() => toggleFavorite(open.id)}
          onClose={() => setOpen(null)}
        />
      )}
    </Layout>
  );
}
