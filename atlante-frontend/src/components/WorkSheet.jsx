import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ai, imgUrl } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { sourceLabel } from '../lib/format';
import Icon from './ui/Icon';
import { PaletteBars } from './ui/Visual';

const SOURCE = { TMDB: 'The Movie Database', MET: 'The Metropolitan Museum of Art', AIC: 'Art Institute of Chicago', WIKIPEDIA: 'Wikimedia Commons', WIKIDATA: 'Wikidata' };

/**
 * Scheda di un'opera (da ricerca negli archivi o dalla collezione) con "Comparazione AI ·
 * trama e palette". saved/onSave/onRemove gestiscono il pulsante della collezione.
 */
export default function WorkSheet({ item, type, context, saved, onSave, onRemove, onClose }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [aiState, setAiState] = useState('empty');
  const [aiData, setAiData] = useState(null);
  const isArt = type === 'ARTWORK';
  const src = imgUrl(item.imageUrl);

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const save = async () => {
    if (!user) {
      navigate(`/accedi?next=${encodeURIComponent('/cerca')}`);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await (saved ? onRemove?.() : onSave?.());
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const compare = async () => {
    setAiState('loading');
    try {
      setAiData(await ai.discover(item.title, type));
      setAiState('done');
    } catch (e) {
      setError(e.message);
      setAiState('error');
    }
  };

  const palette = aiData?.queryPalette ?? item.palette;
  const sourceName = SOURCE[item.source] ?? sourceLabel(item);

  return (
    <div className="overlay" onClick={onClose}>
      <button className="btn btn--ghost btn--sm overlay__close" onClick={onClose}><Icon name="close" /> Chiudi</button>
      <article className="card sheet" onClick={(e) => e.stopPropagation()} role="dialog" aria-label={item.title}>
        <div className="sheet__top">
          <span className={`tag ${isArt ? 'tag--art' : 'tag--film'}`}>{isArt ? 'Opera d’arte' : 'Film'}</span>
          {context && <span className="tiny">{context}</span>}
        </div>
        <div className="sheet__main">
          <div className={'sheet__img' + (isArt ? ' frame--art' : ' frame--film')}>{src && <img src={src} alt={item.title} onError={(e) => { e.currentTarget.style.display = 'none'; }} />}</div>
          <div>
            <h1>{item.title}</h1>
            <p className={`sheet__by ${isArt ? 't-art' : 't-film'}`}>{item.creator}{item.year ? ` · ${item.year}` : ''}</p>
            <p className="sheet__facts">{isArt ? 'Pittura' : 'Lungometraggio'}<br />{sourceName}</p>
            {item.description && <p className="sheet__desc">{item.description}</p>}
            <button className={`btn sheet__save ${saved ? 'btn--ghost btn--fav-active' : 'btn--primary'}`} onClick={save} disabled={busy}>
              <Icon name="bookmark" /> {busy ? 'Un attimo…' : saved ? 'Nella collezione' : 'Salva nella collezione'}
            </button>
            {error && <p className="form-error" style={{ marginTop: 10 }}>{error}</p>}
            <p className="sheet__source">Fonte dati: {sourceName} · Archivio opere</p>
          </div>
        </div>

        <h2 className="sheet__ai-title"><Icon name="sparkles" /> Comparazione AI · trama e palette</h2>
        {aiState === 'empty' && (
          <>
            <p className="sheet__ai-sub">
              Cerca {isArt ? 'film' : 'opere'} affini per tema e per colore. Il suggerimento è un punto di partenza, non una prova.
            </p>
            <button className="btn btn--ghost btn--sm" style={{ marginTop: 16 }} onClick={compare}>
              <Icon name="sparkles" /> Genera la comparazione
            </button>
          </>
        )}
        {aiState === 'loading' && (
          <>
            <p className="sheet__ai-sub">Stiamo mettendo in relazione temi e colori…</p>
            <div className="ai-card__bars"><div className="skeleton" /><div className="skeleton" style={{ width: '40%' }} /></div>
          </>
        )}
        {aiState === 'error' && (
          <button className="btn btn--ghost btn--sm" style={{ marginTop: 16 }} onClick={compare}><Icon name="refresh" /> Riprova</button>
        )}
        {aiState === 'done' && aiData && (
          <>
            <p className="sheet__ai-sub">{isArt ? 'Opera' : 'Film'} selezionato: {item.title}</p>
            {palette && <PaletteBars palette={palette} label={isArt ? 'Opera d’arte' : 'Film'} tone={isArt ? 'art' : 'film'} large showHex />}
            <div className="suggestions">
              {aiData.suggestions.map((s, i) => {
                const img = imgUrl(s.imageUrl);
                return (
                  <div key={i} className="suggestion">
                    <div className="suggestion__img">{img && <img src={img} alt="" />}</div>
                    <div>
                      <span className="tag">{s.linkType === 'palette' ? 'Palette' : 'Trama'}</span>
                      <h4 className={isArt ? 't-film' : 't-art'}>{s.title}</h4>
                      <p>{s.creator}{s.year ? ` · ${s.year}` : ''} · {s.reason}</p>
                    </div>
                  </div>
                );
              })}
            </div>
            <p className="sheet__disclaimer">Suggerimenti generati con AI, non prove di un’influenza. Verifica dati e interpretazioni.</p>
          </>
        )}
      </article>
    </div>
  );
}
