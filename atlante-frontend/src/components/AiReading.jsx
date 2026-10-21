import { useEffect, useId, useState } from 'react';
import { ai } from '../api/client';
import Icon from './ui/Icon';

function errorText(e) {
  if (e?.status === 429) return 'Troppe richieste in poco tempo. Riprova tra un minuto.';
  return 'La lettura AI non è disponibile in questo momento.';
}

/**
 * "Lettura AI": supporto alla critica, mai al posto della voce dell'autore.
 * Stati: vuoto, caricamento, generata (richiudibile), errore.
 */
export default function AiReading({ connectionId, film, art }) {
  const [state, setState] = useState('empty');
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [open, setOpen] = useState(true);
  const [generatedAt, setGeneratedAt] = useState(null);
  const bodyId = useId();

  useEffect(() => {
    setState('empty');
    setData(null);
    setOpen(true);
  }, [connectionId]);

  const generate = async () => {
    setState('loading');
    try {
      setData(await ai.analyze(connectionId));
      setGeneratedAt(new Date());
      setOpen(true);
      setState('done');
    } catch (e) {
      setError(errorText(e));
      setState('error');
    }
  };

  const time = generatedAt?.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' });

  return (
    <section className="ai-card" aria-labelledby={`${bodyId}-title`}>
      <div className="ai-card__head">
        <h2 className="ai-card__title" id={`${bodyId}-title`}><Icon name="sparkles" /> Lettura AI</h2>
        {state === 'done' && (
          <button type="button" className="ai-card__toggle" onClick={() => setOpen((o) => !o)}
            aria-expanded={open} aria-controls={bodyId}>
            {open ? 'Nascondi' : 'Mostra'} <Icon name={open ? 'chevronDown' : 'chevronRight'} size={12} />
          </button>
        )}
      </div>

      <div id={bodyId} aria-live="polite" hidden={state === 'done' && !open}>
        {state === 'empty' && (
          <div className="ai-card__row">
            <p className="ai-card__intro">
              Un confronto di temi e palette, generato su richiesta. La critica resta il punto di partenza.
            </p>
            <button className="btn btn--ghost btn--sm" onClick={generate}>
              <Icon name="sparkles" /> Genera una lettura
            </button>
          </div>
        )}

        {state === 'loading' && (
          <>
            <p className="ai-card__intro">Stiamo mettendo in relazione temi e colori…</p>
            <div className="ai-card__bars" aria-hidden="true">
              <div className="skeleton" style={{ width: '40%' }} /><div className="skeleton" />
              <div className="skeleton" /><div className="skeleton" style={{ width: '70%' }} />
            </div>
          </>
        )}

        {state === 'error' && (
          <div className="ai-card__row">
            <p className="ai-card__intro">{error} La critica non cambia.</p>
            <button className="btn btn--ghost btn--sm" onClick={generate}>
              <Icon name="refresh" /> Riprova
            </button>
          </div>
        )}

        {state === 'done' && data && (
          <>
            {data.filmThemes.length > 0 && (
              <>
                <p className="ai-card__group t-film">Film · {film.title}</p>
                <div className="ai-card__chips">
                  {data.filmThemes.map((t) => <span key={t} className="ai-chip">{t}</span>)}
                </div>
              </>
            )}
            {data.artworkThemes.length > 0 && (
              <>
                <p className="ai-card__group t-art">Opera d’arte · {art.title}</p>
                <div className="ai-card__chips">
                  {data.artworkThemes.map((t) => <span key={t} className="ai-chip">{t}</span>)}
                </div>
              </>
            )}
            <p className="ai-card__text">{data.comparison}</p>
            <p className="ai-card__note">
              Generata con AI{time ? ` alle ${time}` : ''}. Può contenere imprecisioni e non documenta un’influenza storica.
            </p>
          </>
        )}
      </div>
    </section>
  );
}
