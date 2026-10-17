import { useEffect, useState } from 'react';
import { ai } from '../api/client';
import Icon from './ui/Icon';

/** Blocco "Lettura AI" nei quattro stati della libreria: vuoto, caricamento, generata, errore. */
export default function AiReading({ connectionId, film, art }) {
  const [state, setState] = useState('empty');
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    setState('empty');
    setData(null);
  }, [connectionId]);

  const generate = async () => {
    setState('loading');
    try {
      setData(await ai.analyze(connectionId));
      setState('done');
    } catch (e) {
      setError(e.message);
      setState('error');
    }
  };

  const LABEL = { empty: 'VUOTO', loading: 'CARICAMENTO', done: 'GENERATA', error: 'ERRORE' };

  return (
    <section className="ai-card" aria-live="polite">
      <div className="ai-card__head">
        <span className="ai-card__title"><Icon name="sparkles" /> Lettura AI</span>
        <span className="ai-card__state">{LABEL[state]}</span>
      </div>

      {state === 'empty' && (
        <>
          <p className="ai-card__intro">
            Esplora affinità di temi, trama e palette. La tua interpretazione resta il punto di partenza.
          </p>
          <button className="btn btn--ghost btn--sm" style={{ marginTop: 18 }} onClick={generate}>
            <Icon name="sparkles" /> Genera una lettura
          </button>
        </>
      )}

      {state === 'loading' && (
        <>
          <p className="ai-card__intro">Stiamo mettendo in relazione temi e colori…</p>
          <div className="ai-card__bars">
            <div className="skeleton" /><div className="skeleton" /><div className="skeleton" style={{ width: '32%' }} />
          </div>
        </>
      )}

      {state === 'error' && (
        <>
          <p className="ai-card__intro" style={{ color: 'var(--text)' }}>{error || 'backend non raggiungibile'}</p>
          <p className="ai-card__note" style={{ fontSize: 12 }}>La critica è al sicuro. Riprova tra qualche istante.</p>
          <button className="btn btn--ghost btn--sm" style={{ marginTop: 18 }} onClick={generate}>
            <Icon name="refresh" /> Riprova
          </button>
        </>
      )}

      {state === 'done' && data && (
        <>
          {data.filmThemes.length > 0 && (
            <>
              <p className="ai-card__group t-film">Film · {film.title}</p>
              <div className="ai-card__chips">
                {data.filmThemes.map((t, i) => (
                  <span key={t} className={`ai-chip ${i === 0 ? 'ai-chip--film' : ''}`}>{t}</span>
                ))}
              </div>
            </>
          )}
          {data.artworkThemes.length > 0 && (
            <>
              <p className="ai-card__group t-art">Opera d’arte · {art.title}</p>
              <div className="ai-card__chips">
                {data.artworkThemes.map((t, i) => (
                  <span key={t} className={`ai-chip ${i === 0 ? 'ai-chip--art' : ''}`}>{t}</span>
                ))}
              </div>
            </>
          )}
          <p className="ai-card__text">{data.comparison}</p>
          <p className="ai-card__note">
            Testo generato con AI. Può contenere imprecisioni: non documenta un’influenza storica. Verifica sempre le fonti.
          </p>
        </>
      )}
    </section>
  );
}
