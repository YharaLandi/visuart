import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { connections as connectionsApi, imgUrl, likes as likesApi } from '../api/client';
import AiReading from '../components/AiReading';
import Dialogues from '../components/Dialogues';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { ErrorState, Loading } from '../components/ui/States';
import { Avatar, Motif, PaletteBars, TitlePair } from '../components/ui/Visual';
import { useApp } from '../context/AppContext';
import { useAuth } from '../context/AuthContext';
import { artOf, decade, filmOf, longDate, n, readingMinutes, ROLE_LABEL, sourceLabel, splitNote } from '../lib/format';

function Frame({ work, kind }) {
  const src = imgUrl(work.imageUrl);
  const isFilm = kind === 'film';
  return (
    <figure>
      <div className="frame__img">{src && <img src={src} alt={work.title} />}</div>
      <figcaption className="frame__cap">
        <span className={`kind ${isFilm ? 't-film' : 't-art'}`}>{isFilm ? 'Film' : 'Opera d’arte'}</span>
        <div>
          <h3>{work.title}</h3>
          <p>{work.creator}{work.year ? ` · ${work.year}` : ''}</p>
          <p>{isFilm ? 'Lungometraggio' : 'Pittura'} · {sourceLabel(work)}</p>
        </div>
      </figcaption>
    </figure>
  );
}

/** Barra in alto che segue la lettura della critica. */
function ReadProgress() {
  const [progress, setProgress] = useState(0);
  useEffect(() => {
    let frame = 0;
    const measure = () => {
      frame = 0;
      const article = document.querySelector('.detail-body > article');
      if (!article) return;
      const rect = article.getBoundingClientRect();
      const span = rect.height - window.innerHeight * 0.6;
      setProgress(Math.min(1, Math.max(0, (window.innerHeight * 0.4 - rect.top) / Math.max(span, 1))));
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
    <div className="read-progress" role="progressbar" aria-label="Avanzamento lettura"
      aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progress * 100)}>
      <span style={{ transform: `scaleX(${progress})` }} />
    </div>
  );
}

/** Altre connessioni da leggere, escluse quella aperta. */
function Related({ currentId }) {
  const [list, setList] = useState([]);
  useEffect(() => {
    connectionsApi.search({ sort: 'views', size: 3 })
      .then((d) => setList(d.content.filter((c) => c.id !== currentId).slice(0, 2)))
      .catch(() => setList([]));
  }, [currentId]);
  if (list.length === 0) return null;
  return (
    <section className="related">
      <h2 className="h-section">Altri modi di abitare un’immagine</h2>
      <div className="related__grid">
        {list.map((c) => (
          <Link key={c.id} to={`/connessioni/${c.id}`} className="related-card">
            <Motif film={filmOf(c)} art={artOf(c)} size="sm" />
            <div>
              <small>{c.category} · #{String(c.id).padStart(3, '0')}</small>
              <TitlePair film={filmOf(c)} art={artOf(c)} />
              <span className="link">Leggi la connessione <Icon name="arrowUpRight" size={11} /></span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

export default function ConnectionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { favIds, toggleFavorite } = useApp();
  const [conn, setConn] = useState(null);
  const [status, setStatus] = useState('loading');
  const [like, setLike] = useState({ likeCount: 0, likedByMe: false });
  const [commentCount, setCommentCount] = useState(0);

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
          <p>Forse è stata rimossa dalla sua autrice.</p>
          <Link to="/connessioni" className="btn btn--ghost btn--sm">Tutte le connessioni</Link>
        </div>
      </Layout>
    );
  }

  const film = filmOf(conn);
  const art = artOf(conn);
  const bothSaved = favIds.has(film.id) && favIds.has(art.id);
  const toggleFav = async () => {
    if (!user) return requireLogin();
    if (bothSaved) {
      await toggleFavorite(film.id);
      await toggleFavorite(art.id);
    } else {
      if (!favIds.has(film.id)) await toggleFavorite(film.id);
      if (!favIds.has(art.id)) await toggleFavorite(art.id);
    }
  };

  const { quote, body } = splitNote(conn.criticalNote);
  const author = conn.authorName ?? 'Archivio VisuArt';
  const periods = [decade(art.year), decade(film.year)].filter(Boolean).join(' / ');
  const isMine = user && conn.authorUsername === user.username;
  const removeConnection = async () => {
    if (!window.confirm('Eliminare questa critica? Spariscono anche i suoi mi piace e commenti.')) return;
    await connectionsApi.remove(conn.id);
    navigate('/connessioni');
  };

  return (
    <Layout>
      <div className="container">
        <ReadProgress />
        <Link to="/connessioni" className="back"><Icon name="arrowLeft" /> Tutte le connessioni</Link>

        <header className="detail-head">
          <div className="page-head__row">
            <p className="eyebrow eyebrow--light">Connessione #{String(conn.id).padStart(3, '0')}</p>
            <span className="small" style={{ color: 'var(--text)', fontWeight: 600 }}>{conn.category}</span>
          </div>
          <TitlePair film={film} art={art} as="h1" />
          <div className="detail-actions">
            <button className={`btn btn--ghost btn--sm ${like.likedByMe ? 'btn--active' : ''}`} onClick={toggleLike} aria-pressed={like.likedByMe}>
              <Icon name="thumb" /> Mi piace · {n(like.likeCount)}
            </button>
            <button className={`btn btn--ghost btn--sm ${bothSaved ? 'btn--fav-active' : ''}`} onClick={toggleFav} aria-pressed={bothSaved}>
              <Icon name="heart" /> {bothSaved ? 'Nei preferiti' : 'Aggiungi ai preferiti'}
            </button>
          </div>
          <p className="detail-meta">{n(conn.viewCount)} visualizzazioni · {readingMinutes(conn.criticalNote)} min di lettura</p>
          {(isMine || user?.role === 'ADMIN') && (
            <div className="detail-owner">
              <Link to={`/connessioni/${conn.id}/modifica`} className="link"><Icon name="pencil" /> {isMine ? 'Modifica la tua critica' : 'Modifica (admin)'}</Link>
              <button className="link-muted" onClick={removeConnection}><Icon name="trash" /> Elimina</button>
            </div>
          )}
        </header>

        <div className="detail-frames">
          <Frame work={film} kind="film" />
          <Frame work={art} kind="art" />
        </div>

        <div className="detail-body">
          <article>
            <p className="eyebrow eyebrow--light t-film">Lo sguardo di {author}</p>
            <p className="reading__quote">«{quote}»</p>
            {body.length > 0 && (
              <div className="reading__text">{body.map((p, i) => <p key={i}>{p}</p>)}</div>
            )}
            <p className="reading__disclaimer">
              Interpretazione personale dell’autrice. L’accostamento non implica un’influenza storica documentata.
            </p>
            <div className="author-row">
              <Avatar name={author} size="sm" />
              <div>
                <strong>{conn.authorUsername ? <Link to={`/utenti/${conn.authorUsername}`} className="author-link">{author}</Link> : author}</strong>
                <small>{ROLE_LABEL[conn.authorRole] ?? 'Curatore'} · Pubblicato il {longDate(conn.createdAt)}</small>
              </div>
              {conn.authorUsername && !isMine && (
                <Link
                  className="link"
                  to={user ? `/chat/${conn.authorUsername}?condividi=${conn.id}` : `/accedi?next=${encodeURIComponent(`/chat/${conn.authorUsername}`)}`}
                >
                  Scrivi all’autore
                </Link>
              )}
            </div>

            <AiReading connectionId={conn.id} film={film} art={art} />
            <Dialogues connectionId={conn.id} onCountChange={setCommentCount} />
          </article>

          <aside className="card color-card">
            <h2>La materia del colore</h2>
            <p className="color-card__intro">Sei campioni estratti da ciascuna immagine.</p>
            <PaletteBars palette={film.palette} label="Film" tone="film" title={film.title} large showHex />
            <PaletteBars palette={art.palette} label="Opera d’arte" tone="art" title={art.title} large showHex />
            <div className="color-card__facts">
              <div className="fact"><span>Categoria</span><strong>{conn.category}</strong></div>
              {periods && <div className="fact"><span>Periodi</span><strong>{periods}</strong></div>}
              <div className="fact"><span>Linguaggi</span><strong>Cinema / Pittura</strong></div>
              <div className="fact"><span>Dialoghi</span><strong>{n(commentCount)} commenti</strong></div>
              <Link to={`/grafo?opera=${film.id}`} className="btn btn--ghost btn--sm" style={{ alignSelf: 'flex-start' }}>
                <Icon name="graph" /> Apri nel grafo
              </Link>
            </div>
          </aside>
        </div>

        <Related currentId={conn.id} />
      </div>
    </Layout>
  );
}
