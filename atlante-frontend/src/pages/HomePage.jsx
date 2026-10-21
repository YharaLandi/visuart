import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { connections as connectionsApi, imgUrl } from '../api/client';
import Layout from '../components/ui/Layout';
import Icon from '../components/ui/Icon';
import { kindLabel, pairOf } from '../lib/format';
import Marquee from '../components/Marquee';

/** Le due bacheche sotto l'insegna: le due opere della coppia dietro il vetro. */
function Showcase({ conn }) {
  if (!conn) return <div className="mq-cases mq-cases--empty" aria-hidden="true"><span /><span /></div>;
  const { a: film, b: art, toneA, toneB, imgA, imgB } = pairOf(conn);
  const caseFor = (work, tone, kind, image) => (
    <figure className={`mq-case mq-case--${tone}`}>
      <span className={'mq-case__glass' + (work.type === 'ARTWORK' ? ' frame--art' : ' frame--film')}>{imgUrl(image) && <img src={imgUrl(image)} alt={work.title} />}</span>
      <figcaption>
        <b>{kind}</b> {work.creator}{work.year ? `, ${work.year}` : ''}
      </figcaption>
    </figure>
  );
  return (
    <Link to={`/connessioni/${conn.id}`} className="mq-cases" aria-label={`Apri la connessione: ${film.title} con ${art.title}`}>
      {caseFor(film, toneA, kindLabel(film, true), imgA)}
      {caseFor(art, toneB, kindLabel(art, true), imgB)}
      <span className="mq-cases__open">Leggi la critica{conn.authorName ? ` di ${conn.authorName}` : ''} <Icon name="arrowRight" /></span>
    </Link>
  );
}

function BillRow({ conn, onShow }) {
  const { a: film, b: art, toneA, toneB, imgA, imgB } = pairOf(conn);
  return (
    <li>
      <Link to={`/connessioni/${conn.id}`} className="mq-row"
        onMouseEnter={() => onShow(conn)} onFocus={() => onShow(conn)}>
        <span className="mq-row__stills" aria-hidden="true">
          <span className="mq-row__still">{imgUrl(imgA) && <img src={imgUrl(imgA)} alt="" loading="lazy" />}</span>
          <span className="mq-row__still">{imgUrl(imgB) && <img src={imgUrl(imgB)} alt="" loading="lazy" />}</span>
        </span>
        <span className="mq-row__pair">
          <span className={'mq-row__film t-' + toneA}>{film.title}</span>
          <span className="mq-row__con"> con </span>
          <span className={'mq-row__art t-' + toneB}>{art.title}</span>
        </span>
        <span className="mq-row__meta">{conn.category}{conn.authorName ? ` · ${conn.authorName}` : ''}</span>
        <span className="mq-row__go" aria-hidden="true"><Icon name="arrowRight" /></span>
      </Link>
    </li>
  );
}

export default function HomePage() {
  const [all, setAll] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [shown, setShown] = useState(null);
  const [q, setQ] = useState('');
  const [results, setResults] = useState(null);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    connectionsApi.list(undefined, 'popular').then(setAll).catch(() => setLoadFailed(true));
  }, []);

  const search = async (e) => {
    e.preventDefault();
    const term = q.trim();
    if (!term) { setResults(null); return; }
    setSearching(true);
    try {
      const page = await connectionsApi.search({ q: term, size: 12 });
      setResults({ term, list: page.content });
    } catch {
      setResults({ term, list: [], failed: true });
    } finally {
      setSearching(false);
    }
  };

  const featured = shown ?? all?.[0];
  const bill = results ? results.list : (all ?? []).slice(0, 8);

  return (
    <Layout>
      <div className="mq">
        <section className="mq-facade" aria-labelledby="mq-title">
          <h1 id="mq-title" className="sr-only">VisuArt: film e dipinti in cartellone insieme</h1>
          <div className="mq-wrap">
            <Marquee conn={featured} label={shown ? 'Anche in programma' : 'In cartellone'} />
            <div className="mq-under">
              <Showcase conn={featured} />
              <div className="mq-booth">
                <p className="mq-booth__lead">
                  Ogni sera un film e un dipinto, accostati da chi li ha guardati insieme.
                  Una critica firmata, i colori a confronto, la strada verso altre opere.
                </p>
                <form className="mq-search" role="search" onSubmit={search}>
                  <label htmlFor="mq-q">Cerca in cartellone</label>
                  <div className="mq-search__row">
                    <input id="mq-q" type="search" value={q} onChange={(e) => setQ(e.target.value)}
                      placeholder="Un film, un’opera, un regista, un pittore" autoComplete="off" />
                    <button type="submit" disabled={searching}>
                      <Icon name="search" /> {searching ? 'Cerco…' : 'Cerca'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </section>

        <section className="mq-wrap mq-bill" aria-labelledby="mq-bill">
          <div className="mq-bill__head">
            <h2 id="mq-bill">{results ? `In cartellone per «${results.term}»` : 'Il programma'}</h2>
            {results ? (
              <button type="button" className="mq-link" onClick={() => { setResults(null); setQ(''); }}>
                Torna al programma
              </button>
            ) : (
              <Link to="/connessioni" className="mq-link">Tutte le connessioni <Icon name="arrowRight" /></Link>
            )}
          </div>

          {loadFailed && !results && (
            <p className="mq-note" role="alert">Il programma non è arrivato. Ricarica la pagina tra poco.</p>
          )}
          {!all && !loadFailed && !results && <p className="mq-note">Montiamo le lettere…</p>}
          {results?.failed && <p className="mq-note" role="alert">La ricerca non ha risposto. Riprova tra poco.</p>}
          {results && !results.failed && results.list.length === 0 && (
            <p className="mq-note">Nessuna connessione per «{results.term}». Prova con il titolo di un film o il nome di un pittore.</p>
          )}

          {bill.length > 0 && (
            <ul className="mq-board" onMouseLeave={() => setShown(null)}>
              {bill.map((c) => <BillRow key={c.id} conn={c} onShow={setShown} />)}
            </ul>
          )}
        </section>

        <nav className="mq-wrap mq-doors" aria-label="Altri modi di guardare">
          <Link to="/grafo" className="mq-door">
            <span className="mq-door__t">Il grafo delle affinità</span>
            <span className="mq-door__d">Segui il filo da un’opera all’altra.</span>
          </Link>
          <Link to="/collezione" className="mq-door">
            <span className="mq-door__t">La tua collezione</span>
            <span className="mq-door__d">Le coppie che vuoi rivedere.</span>
          </Link>
          <Link to="/nuova-critica" className="mq-door">
            <span className="mq-door__t">Scrivi una connessione</span>
            <span className="mq-door__d">Accosta un film e un dipinto, firma il tuo sguardo.</span>
          </Link>
        </nav>

        <p className="mq-wrap mq-close">
          Le affinità non attestano un’influenza storica: raccontano un modo di vedere.
        </p>
      </div>
    </Layout>
  );
}
