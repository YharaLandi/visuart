import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { imgUrl, phantom as phantomApi } from '../api/client';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { Loading, ErrorState } from '../components/ui/States';
import { confirmPhantomAge, phantomAgeConfirmed, phantomOpen } from '../lib/season';
import '../styles/phantom.css';

export const TYPES = {
  FOLKLORE: 'Folklore',
  MITO: 'Mito',
  LEGGENDA: 'Leggenda',
  LETTERATURA: 'Romanzo e racconti',
  STORIA: 'Storia',
  CRONACA: 'Cronaca',
  TRUE_CRIME: 'Cronaca nera',
};

function yearLabel(y) {
  if (y == null) return null;
  return y < 0 ? `${-y} a.C.` : String(y);
}

/** Avviso +16: si entra solo confermando l'età, una volta all'anno su questo dispositivo. */
function AgeGate({ onEnter }) {
  const [ok, setOk] = useState(false);
  return (
    <section className="phantom-gate" aria-labelledby="gate-title">
      <p className="phantom-gate__badge" aria-hidden="true">16+</p>
      <h1 id="gate-title">Reparto fantasma</h1>
      <p>
        Da dove vengono i film e i quadri dell’orrore: leggende, miti, romanzi e fatti di cronaca. Il reparto contiene
        riferimenti a temi forti ed è riservato a chi ha almeno 16 anni.
      </p>
      <p className="phantom-gate__note">
        Sui casi di cronaca nera non raccontiamo nulla: per rispetto delle persone coinvolte trovi solo
        il nome del caso e il rimando a Wikipedia.
      </p>
      <label className="check">
        <input type="checkbox" checked={ok} onChange={(e) => setOk(e.target.checked)} />
        Ho almeno 16 anni
      </label>
      <div className="phantom-gate__actions">
        <button type="button" className="btn btn--primary" disabled={!ok} onClick={onEnter}>
          <Icon name="arrowRight" /> Entra nel reparto
        </button>
        <Link to="/" className="link-muted">Torna all’archivio</Link>
      </div>
    </section>
  );
}

/** La scheda di un film: da dove viene, in due o tre righe, e il rimando alla fonte. */
export function OriginDialog({ item, onClose }) {
  const ref = useRef(null);
  // la finestra si apre al montaggio; si chiude solo per scelta dell'utente (Esc, Chiudi, clic fuori),
  // e la chiusura avvisa il genitore tramite l'evento "close"
  useEffect(() => {
    const d = ref.current;
    if (d && !d.open) d.showModal();
  }, []);
  const { film } = item;
  const isArt = film.type === 'ARTWORK';
  const trueCrime = item.type === 'TRUE_CRIME';
  return (
    <dialog ref={ref} className="phantom-dialog" aria-labelledby="origin-title" onClose={onClose}
      onClick={(e) => { if (e.target === ref.current) ref.current.close(); }}>
      <div className="phantom-dialog__inner">
        <button type="button" className="phantom-dialog__close" onClick={() => ref.current.close()}>
          <Icon name="close" /> Chiudi
        </button>
        <span className={`phantom-dialog__poster ${isArt ? 'frame--art phantom-dialog__poster--art' : 'frame--film'}`}>
          {imgUrl(film.imageUrl) && <img src={imgUrl(film.imageUrl)} alt={`${isArt ? 'Riproduzione di' : 'Locandina di'} ${film.title}`} />}
        </span>
        <div className="phantom-dialog__body">
          <p className={`phantom-type phantom-type--${item.type.toLowerCase()}`}>{TYPES[item.type]}</p>
          <h2 id="origin-title">{film.title}</h2>
          <p className="phantom-dialog__by">{film.creator}{film.year ? ` · ${film.year}` : ''}</p>
          <p className="phantom-dialog__from">
            {isArt ? 'Dietro il quadro' : 'Nasce da'} <strong>{item.originTitle}</strong>{item.originYear != null ? ` (${yearLabel(item.originYear)})` : ''}
          </p>
          {trueCrime ? (
            <p className="phantom-dialog__note">
              È un caso di cronaca nera: qui non lo raccontiamo, per rispetto delle persone coinvolte.
              Se vuoi conoscerlo, leggi la voce su Wikipedia.
            </p>
          ) : (
            <p className="phantom-dialog__trivia">{item.trivia}</p>
          )}
          <a className="btn btn--ghost btn--sm" href={item.wikipediaUrl} target="_blank" rel="noopener noreferrer">
            Leggi su Wikipedia <Icon name="arrowUpRight" />
          </a>
        </div>
      </div>
    </dialog>
  );
}

export default function PhantomPage() {
  const open = phantomOpen();
  const [entered, setEntered] = useState(phantomAgeConfirmed);
  const [list, setList] = useState(null);
  const [failed, setFailed] = useState(false);
  const [current, setCurrent] = useState(null);

  useEffect(() => {
    if (!open || !entered) return;
    phantomApi.list().then(setList).catch(() => setFailed(true));
  }, [open, entered]);

  if (!open) {
    return (
      <Layout>
        <div className="container phantom">
          <section className="phantom-gate">
            <h1>Il reparto fantasma è chiuso</h1>
            <p>Riapre a ottobre, con le storie da cui nascono i film e i quadri dell’orrore. Tutto l’anno trovi le curiosità su film e quadri.</p>
            <Link to="/curiosita" className="btn btn--primary btn--sm">Vai alle curiosità</Link>
            <Link to="/" className="btn btn--ghost btn--sm"><Icon name="arrowLeft" /> Torna all’archivio</Link>
          </section>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="container phantom">
        {!entered ? (
          <AgeGate onEnter={() => { confirmPhantomAge(); setEntered(true); }} />
        ) : (
          <>
            <header className="page-head phantom-head">
              <p className="phantom-head__open">Aperto solo a ottobre · 16+</p>
              <h1 className="h-page">Reparto fantasma</h1>
              <p className="lead">Ogni film e ogni quadro dell’orrore viene da qualche parte: una leggenda, un mito, un romanzo, un fatto vero. Apri un’opera e scopri da dove.</p>
            </header>
            {failed && <ErrorState onRetry={() => { setFailed(false); phantomApi.list().then(setList).catch(() => setFailed(true)); }} />}
            {!list && !failed && <Loading title="Accendiamo le candele…" />}
            {list && (
              <ul className="phantom-grid">
                {list.map((item) => (
                  <li key={item.id}>
                    <button type="button" className="phantom-card" onClick={() => setCurrent(item)}
                      aria-label={`${item.film.title}: da dove viene`}>
                      <span className={`phantom-card__poster ${item.film.type === 'ARTWORK' ? 'frame--art' : 'frame--film'}`}>
                        {imgUrl(item.film.imageUrl) && <img src={imgUrl(item.film.imageUrl)} alt="" loading="lazy" />}
                      </span>
                      <span className={`phantom-type phantom-type--${item.type.toLowerCase()}`}>{TYPES[item.type]}</span>
                      <span className="phantom-card__title">{item.film.title}</span>
                      <span className="phantom-card__year">{item.film.year}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <p className="phantom-foot">
              Le origini sono curate dalla redazione. Il film è sempre un'interpretazione: non una ricostruzione dei fatti.
            </p>
          </>
        )}
        {current && <OriginDialog item={current} onClose={() => setCurrent(null)} />}
      </div>
    </Layout>
  );
}
