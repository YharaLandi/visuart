import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { connections as connectionsApi, imgUrl, likes as likesApi } from '../api/client';
import AiReading from '../components/AiReading';
import Dialogues from '../components/Dialogues';
import Icon from '../components/ui/Icon';
import Marquee from '../components/Marquee';
import Layout from '../components/ui/Layout';
import { ErrorState, Loading } from '../components/ui/States';
import { Avatar, Motif, PaletteBars, TitlePair } from '../components/ui/Visual';
import { useApp } from '../context/AppContext';
import { useAuth } from '../context/AuthContext';
import { artOf, decade, filmOf, kindLabel, longDate, pairOf, ringStyle, n, readingMinutes, sourceLabel, splitNote } from '../lib/format';

function Frame({ work, tone }) {
  const src = imgUrl(work.imageUrl);
  const [broken, setBroken] = useState(false);
  const isFilm = work.type !== 'ARTWORK';
  return (
    <figure>
      <div className="frame__img">
        {src && !broken && <img src={src} alt={`${isFilm ? 'Fotogramma da' : 'Riproduzione di'} ${work.title}`} onError={() => setBroken(true)} />}
      </div>
      <figcaption className="frame__cap">
        <span className={'kind t-' + tone}>{kindLabel(work)}</span>
        <div>
          <p className="frame__title">{work.title}</p>
          <p>{work.creator}{work.year ? ` · ${work.year}` : ''}</p>
          <p>{isFilm ? 'Lungometraggio' : 'Pittura'} · {sourceLabel(work)}</p>
        </div>
      </figcaption>
    </figure>
  );
}

/** Barra in alto che segue la lettura della critica (solo citazione e testo). */
function ReadProgress() {
  const [progress, setProgress] = useState(0);
  useEffect(() => {
    let frame = 0;
    const measure = () => {
      frame = 0;
      const reading = document.querySelector('.reading');
      if (!reading) return;
      const rect = reading.getBoundingClientRect();
      const span = rect.height + window.innerHeight * 0.2;
      setProgress(Math.min(1, Math.max(0, (window.innerHeight * 0.8 - rect.top) / Math.max(span, 1))));
    };
    const onScroll = () => { frame ||= requestAnimationFrame(measure); };
    measure();
    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onScroll);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener('scroll', onScroll);
      window.removeEventListener('resize', onScroll);
    };
  }, []);
  return (
    <div className="read-progress" aria-hidden="true">
      <span style={{ transform: `scaleX(${progress})` }} />
    </div>
  );
}

/** Perché una connessione è vicina a quella aperta: opera in comune, stesso autore, stessa categoria. */
function affinity(current, other) {
  // un'opera in comune, in qualunque posizione: le coppie possono essere anche film–film o opera–opera
  const mine = [current.source.id, current.target.id];
  const shared = [other.source, other.target].find((w) => mine.includes(w.id));
  if (shared) return { score: 3, reason: `${shared.type === 'ARTWORK' ? 'Stessa opera' : 'Stesso film'} · ${shared.title}` };
  if (current.authorUsername && other.authorUsername === current.authorUsername) {
    return { score: 2, reason: `Anche di ${other.authorName ?? other.authorUsername}` };
  }
  if (other.category === current.category) return { score: 1, reason: `Stessa categoria · ${other.category}` };
  return { score: 0, reason: 'Tra le più lette' };
}

/** Connessioni vicine a quella aperta, con il motivo dichiarato su ogni card. */
function Related({ current }) {
  const [list, setList] = useState([]);
  useEffect(() => {
    connectionsApi.list(undefined, 'views')
      .then((all) => setList(
        all.filter((c) => c.id !== current.id)
          .map((c) => ({ c, ...affinity(current, c) }))
          .sort((a, b) => b.score - a.score)
          .slice(0, 2),
      ))
      .catch(() => setList([]));
  }, [current]);
  if (list.length === 0) return null;
  return (
    <section className="related">
      <h2 className="h-section">Da qui si prosegue</h2>
      <div className="related__grid">
        {list.map(({ c, reason }) => (
          <Link key={c.id} to={`/connessioni/${c.id}`} className="related-card">
            <Motif film={filmOf(c)} art={artOf(c)} size="sm" />
            <div>
              <small>{reason}</small>
              <TitlePair film={filmOf(c)} art={artOf(c)} />
              <span className="link">Leggi la connessione <Icon name="arrowUpRight" size={11} /></span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

/** Opere aggiunte alla collezione da questa pagina, per toglierle senza toccare quelle salvate altrove. */
const SAVED_KEY = 'visuart_saved_via';
function savedVia(connId) {
  try { return JSON.parse(localStorage.getItem(SAVED_KEY) ?? '{}')[connId] ?? []; } catch { return []; }
}
function setSavedVia(connId, ids) {
  try {
    const all = JSON.parse(localStorage.getItem(SAVED_KEY) ?? '{}');
    if (ids.length) all[connId] = ids; else delete all[connId];
    localStorage.setItem(SAVED_KEY, JSON.stringify(all));
  } catch { /* preferenza locale, non essenziale */ }
}

export default function ConnectionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { favIds, toggleFavorite } = useApp();
  const [conn, setConn] = useState(null);
  const [status, setStatus] = useState('loading');
  const [like, setLike] = useState({ likeCount: 0, likedByMe: false });
  const [commentCount, setCommentCount] = useState(null);
  const [actionError, setActionError] = useState(null);

  const load = useCallback(() => {
    setStatus('loading');
    connectionsApi.get(id)
      .then((c) => {
        setConn(c);
        setLike({ likeCount: c.likeCount, likedByMe: c.likedByMe });
        setStatus('ok');
      })
      .catch((e) => setStatus(e.status === 404 ? 'missing' : 'error'));
  }, [id]);

  useEffect(() => { load(); }, [load, user]);
  // la visita alimenta l'ordinamento "più viste"
  useEffect(() => { connectionsApi.registerView(id).catch(() => {}); }, [id]);
  useEffect(() => { window.scrollTo(0, 0); }, [id]);

  const requireLogin = () => navigate(`/accedi?next=${encodeURIComponent(`/connessioni/${id}`)}`);

  const toggleLike = async () => {
    if (!user) return requireLogin();
    try {
      if (like.likedByMe) await likesApi.remove(id);
      else await likesApi.add(id);
    } catch {
      setActionError('Il mi piace non è stato registrato. Riprova.');
    } finally {
      likesApi.get(id).then(setLike).catch(() => {});
    }
  };

  if (status === 'loading') return <Layout><div className="container"><Loading title="Apriamo la connessione…" /></div></Layout>;
  if (status === 'error') return <Layout><div className="container"><ErrorState onRetry={load} /></div></Layout>;
  if (status === 'missing') {
    return (
      <Layout>
        <div className="container state">
          <h3>Questa connessione non è più nell’archivio.</h3>
          <p>Forse è stata rimossa da chi l’ha scritta.</p>
          <Link to="/connessioni" className="btn btn--ghost btn--sm">Tutte le connessioni</Link>
        </div>
      </Layout>
    );
  }

  const { a: film, b: art, toneA, toneB } = pairOf(conn);
  const bothSaved = favIds.has(film.id) && favIds.has(art.id);
  const toggleSave = async () => {
    if (!user) return requireLogin();
    setActionError(null);
    try {
      if (bothSaved) {
        // togli solo ciò che questo bottone ha aggiunto; se non risulta nulla, entrambe
        const tracked = savedVia(conn.id).filter((w) => favIds.has(w));
        for (const w of (tracked.length ? tracked : [film.id, art.id])) await toggleFavorite(w);
        setSavedVia(conn.id, []);
      } else {
        const added = [film.id, art.id].filter((w) => !favIds.has(w));
        for (const w of added) await toggleFavorite(w);
        setSavedVia(conn.id, added);
      }
    } catch {
      setActionError('La collezione non è stata aggiornata. Riprova.');
    }
  };

  const { quote, body } = splitNote(conn.criticalNote);
  const author = conn.authorName ?? 'Archivio VisuArt';
  const words = conn.criticalNote?.trim().split(/\s+/).length ?? 0;
  const periods = [decade(film.year), decade(art.year)].filter(Boolean).join(' / ');
  const isMine = user && conn.authorUsername === user.username;
  const removeConnection = async () => {
    if (!window.confirm('Eliminare questa critica? Spariscono anche i suoi mi piace e commenti.')) return;
    try {
      await connectionsApi.remove(conn.id);
      navigate('/connessioni');
    } catch {
      setActionError('La critica non è stata eliminata. Riprova.');
    }
  };

  return (
    <Layout>
      <div className="container">
        <ReadProgress />
        <Link to="/connessioni" className="back"><Icon name="arrowLeft" /> Tutte le connessioni</Link>

        <header className="detail-head">
          <Marquee conn={conn} size="sm" as="h1"
            label={`Connessione n. ${String(conn.id).padStart(3, '0')} · ${conn.category}`} />
          <div className="detail-actions">
            <button className={`btn btn--ghost btn--sm ${like.likedByMe ? 'btn--active' : ''}`} onClick={toggleLike} aria-pressed={like.likedByMe}>
              <Icon name={like.likedByMe ? 'thumbFill' : 'thumb'} /> Mi piace · {n(like.likeCount)}
            </button>
            <button className={`btn btn--ghost btn--sm ${bothSaved ? 'btn--active' : ''}`} onClick={toggleSave} aria-pressed={bothSaved}>
              <Icon name={bothSaved ? 'bookmarkFill' : 'bookmark'} /> {bothSaved ? 'Nella tua collezione' : (film.type === art.type ? (film.type === 'FILM' ? 'Salva i due film' : 'Salva le due opere') : 'Salva film e opera')}
            </button>
          </div>
          {actionError && <p className="form-error" role="alert">{actionError}</p>}
          <p className="detail-meta">
            {n(conn.viewCount)} visualizzazioni · {words < 150 ? 'lettura breve' : `${readingMinutes(conn.criticalNote)} min di lettura`}
          </p>
        </header>

        <div className="detail-frames" style={ringStyle(toneA, toneB)}>
          <Frame work={film} tone={toneA} />
          <span className="detail-frames__link" aria-hidden="true" />
          <Frame work={art} tone={toneB} />
        </div>

        <div className="detail-body">
          <article className="detail-critique">
            <p className="eyebrow eyebrow--light t-film">Lo sguardo di {author}</p>
            <div className="reading">
              <p className="reading__quote">«{quote}»</p>
              {body.length > 0 && (
                <div className="reading__text">{body.map((p, i) => <p key={i}>{p}</p>)}</div>
              )}
            </div>
            <p className="reading__disclaimer">
              Interpretazione personale di {author}. L’accostamento non implica un’influenza storica documentata.
            </p>
            <div className="author-row">
              <Avatar name={author} username={conn.authorUsername} size="sm" />
              <div>
                <strong>{conn.authorUsername ? <Link to={`/utenti/${conn.authorUsername}`} className="author-link">{author}</Link> : author}</strong>
                <small>{conn.authorRole === 'CURATOR' ? 'Curatore · ' : ''}Pubblicata il {longDate(conn.createdAt)}</small>
              </div>
              {conn.authorUsername && !isMine && (
                <Link
                  className="link"
                  to={user ? `/chat/${conn.authorUsername}?condividi=${conn.id}` : `/accedi?next=${encodeURIComponent(`/chat/${conn.authorUsername}`)}`}
                >
                  Scrivi a {author}
                </Link>
              )}
            </div>
            {/* le azioni del proprietario stanno in fondo alla critica, lontane da mi piace e salva */}
            {(isMine || user?.role === 'ADMIN') && (
              <div className="detail-owner">
                <p className="detail-owner__label">{isMine ? 'La tua critica' : 'Amministrazione'}</p>
                <Link to={`/connessioni/${conn.id}/modifica`} className="btn btn--ghost btn--sm"><Icon name="pencil" /> Modifica</Link>
                <button type="button" className="btn btn--danger btn--sm" onClick={removeConnection}><Icon name="trash" /> Elimina</button>
              </div>
            )}
          </article>

          <aside className="card color-card">
            <h2>La materia del colore</h2>
            <p className="color-card__intro">I colori dominanti di ciascuna immagine, da copiare o scaricare.</p>
            <PaletteBars palette={film.palette} label={kindLabel(film)} tone={toneA} title={film.title} large showHex />
            <PaletteBars palette={art.palette} label={kindLabel(art)} tone={toneB} title={art.title} large showHex />
            <div className="color-card__facts">
              {periods && <div className="fact"><span>Periodi</span><strong>{periods}</strong></div>}
              <div className="fact">
                <span>Dialoghi</span>
                <strong>{commentCount === null ? '—' : <a href="#dialoghi" className="author-link">{n(commentCount)} commenti</a>}</strong>
              </div>
              <Link to={`/grafo?opera=${film.id}`} className="btn btn--ghost btn--sm" style={{ alignSelf: 'flex-start' }}>
                <Icon name="graph" /> Opere vicine nel grafo
              </Link>
            </div>
          </aside>

          <div className="detail-after">
            <AiReading connectionId={conn.id} film={film} art={art} />
            <Dialogues connectionId={conn.id} onCountChange={setCommentCount} />
          </div>
        </div>

        <Related current={conn} />
      </div>
    </Layout>
  );
}
