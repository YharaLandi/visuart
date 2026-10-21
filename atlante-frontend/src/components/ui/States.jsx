import { Link } from 'react-router-dom';

/** Stati di servizio dalla libreria componenti: caricamento, errore, elenco vuoto. */
export function Loading({ title = 'Raccogliamo le connessioni…' }) {
  return (
    <div className="state" role="status">
      <h3>{title}</h3>
      <div style={{ maxWidth: 300, margin: '22px auto 0', display: 'grid', gap: 10 }}>
        <div className="skeleton" />
        <div className="skeleton" style={{ height: 8 }} />
      </div>
    </div>
  );
}

export function ErrorState({ onRetry, text = 'Le tue opere sono al sicuro. Verifica la connessione e riprova.' }) {
  return (
    <div className="state" role="alert">
      <h3>Non riusciamo a raggiungere l’archivio.</h3>
      <p>{text}</p>
      {onRetry && <button className="btn btn--ghost btn--sm" onClick={onRetry}>Riprova</button>}
    </div>
  );
}

export function EmptyState({ title = 'Nessun risultato, per ora.', text, action = 'Esplora l’archivio', to = '/cerca' }) {
  return (
    <div className="state">
      <h3>{title}</h3>
      {text && <p>{text}</p>}
      {action && <Link to={to} className="btn btn--ghost btn--sm">{action}</Link>}
    </div>
  );
}
