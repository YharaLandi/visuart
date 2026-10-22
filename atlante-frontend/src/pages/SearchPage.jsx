import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { externalSearch, imgUrl } from '../api/client';
import WorkSheet from '../components/WorkSheet';
import Icon from '../components/ui/Icon';
import { Brand, Footer } from '../components/ui/Visual';
import { useApp } from '../context/AppContext';

function Result({ item, type, onOpen }) {
  const src = imgUrl(item.imageUrl);
  const isArt = type === 'ARTWORK';
  return (
    <button className="result" onClick={() => onOpen(item, type)}>
      <div className={'result__img' + (isArt ? ' frame--art' : ' frame--film')}>{src && <img src={src} alt="" onError={(e) => { e.currentTarget.style.display = 'none'; }} />}</div>
      <div className="result__cap">
        <span className={`kind ${isArt ? 't-art' : 't-film'}`}>{isArt ? 'Opera d’arte' : 'Film'}</span>
        <div>
          <h3>{item.title}</h3>
          <p>{item.creator ? `${item.creator} · ` : ''}{item.year ?? ''}</p>
        </div>
        <Icon name="arrowUpRight" />
      </div>
    </button>
  );
}

export default function SearchPage() {
  const navigate = useNavigate();
  const { favIds, refreshFavorites, toggleFavorite } = useApp();
  const [q, setQ] = useState('');
  const [query, setQuery] = useState('');
  const [films, setFilms] = useState([]);
  const [arts, setArts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(null);
  const [savedWorks, setSavedWorks] = useState({});
  const [showAll, setShowAll] = useState({ FILM: false, ARTWORK: false });

  // dalla terza lettera, con una piccola attesa per non tempestare gli archivi esterni
  useEffect(() => {
    const t = q.trim();
    if (t.length < 3) {
      setQuery('');
      return undefined;
    }
    const timer = setTimeout(() => setQuery(t), 400);
    return () => clearTimeout(timer);
  }, [q]);

  useEffect(() => {
    if (!query) {
      setFilms([]);
      setArts([]);
      return;
    }
    let alive = true;
    setLoading(true);
    Promise.allSettled([externalSearch.tmdb(query), externalSearch.artworks(query)]).then(([f, a]) => {
      if (!alive) return;
      setFilms(f.status === 'fulfilled' ? f.value : []);
      setArts(a.status === 'fulfilled' ? a.value : []);
      setLoading(false);
      setShowAll({ FILM: false, ARTWORK: false });
    });
    return () => { alive = false; };
  }, [query]);

  const key = (item) => `${item.source}:${item.externalId}`;

  const save = async () => {
    const { item, type } = open;
    const work = await externalSearch.importExternal({
      source: item.source, externalId: item.externalId, type,
      title: item.title, creator: item.creator, year: item.year, imageUrl: item.imageUrl,
    });
    if (!favIds.has(work.id)) await toggleFavorite(work.id);
    setSavedWorks((prev) => ({ ...prev, [key(item)]: work.id }));
    refreshFavorites();
  };

  const remove = async () => {
    const workId = savedWorks[key(open.item)];
    if (workId && favIds.has(workId)) await toggleFavorite(workId);
    setSavedWorks((prev) => ({ ...prev, [key(open.item)]: undefined }));
  };

  const total = films.length + arts.length;

  return (
    <div className="page">
      <header className="container search-top">
        <Brand />
        <button className="btn btn--ghost btn--sm" onClick={() => navigate(-1)}><Icon name="close" /> Chiudi</button>
      </header>
      <main className="page__main container search-hero">
        <h1 className="h-page">Trova il prossimo dialogo.</h1>
        <label className="field search-input">
          <span className="field__label">Cerca un titolo, un autore, un regista</span>
          <input type="search" value={q} onChange={(e) => setQ(e.target.value)} autoFocus placeholder="Es. Nighthawks, Kubrick, Vermeer" />
        </label>
        <p className="search-hint">
          Ricerca attiva da 3 caratteri
          {query && !loading && ` · ${total} risultati per “${query}”`}
          {loading && ' · ricerca in corso…'}
        </p>

        {query && !loading && (
          <>
            <div className="results-head"><h2 className="t-film">Film</h2><span>{films.length} RISULTATI</span></div>
            <div className="results">
              {(showAll.FILM ? films : films.slice(0, 6)).map((it) => <Result key={`f${it.externalId}`} item={it} type="FILM" onOpen={(item, type) => setOpen({ item, type })} />)}
            </div>
            {films.length > 6 && !showAll.FILM && <div className="end-note" style={{ marginTop: 24 }}><button className="btn btn--ghost btn--sm" onClick={() => setShowAll((v) => ({ ...v, FILM: true }))}>Mostra tutti i {films.length} film</button></div>}
            {films.length === 0 && <p className="small" style={{ marginTop: 14 }}>Nessun film: prova un altro titolo.</p>}

            <div className="results-head"><h2 className="t-art">Quadri</h2><span>{arts.length} RISULTATI</span></div>
            <div className="results">
              {(showAll.ARTWORK ? arts : arts.slice(0, 6)).map((it) => <Result key={`a${it.externalId}`} item={it} type="ARTWORK" onOpen={(item, type) => setOpen({ item, type })} />)}
            </div>
            {arts.length > 6 && !showAll.ARTWORK && <div className="end-note" style={{ marginTop: 24 }}><button className="btn btn--ghost btn--sm" onClick={() => setShowAll((v) => ({ ...v, ARTWORK: true }))}>Mostra tutti i {arts.length} quadri</button></div>}
            {arts.length === 0 && <p className="small" style={{ marginTop: 14 }}>Nessun quadro: prova un altro titolo.</p>}
          </>
        )}
      </main>
      <Footer />

      {open && (
        <WorkSheet
          item={open.item}
          type={open.type}
          context={`ARCHIVIO · “${query}”`}
          saved={Boolean(savedWorks[key(open.item)])}
          onSave={save}
          onRemove={remove}
          onClose={() => setOpen(null)}
        />
      )}
    </div>
  );
}
