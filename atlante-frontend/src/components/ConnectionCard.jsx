import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { kindLabel, n, pairOf, serial, splitNote } from '../lib/format';
import { imgUrl } from '../api/client';
import { PaletteBars, TitlePair } from './ui/Visual';

/** Card connessione: categoria e numero, motivo, titoli, due palette, citazione, autore, numeri. */
export default function ConnectionCard({ conn, selected = false }) {
  const { a: film, b: art, toneA, toneB, imgA, imgB, palA, palB } = pairOf(conn);
  const navigate = useNavigate();
  const motifRef = useRef(null);
  const [seen, setSeen] = useState(false);
  useEffect(() => {
    const el = motifRef.current;
    if (!el || !('IntersectionObserver' in window)) {
      setSeen(true);
      return undefined;
    }
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setSeen(true);
        observer.disconnect();
      }
    }, { threshold: 0.6 });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);
  const openAuthor = (e) => {
    e.preventDefault();
    e.stopPropagation();
    navigate(`/utenti/${conn.authorUsername}`);
  };
  return (
    <Link to={`/connessioni/${conn.id}`} className={`card conn-card ${selected ? 'conn-card--selected' : ''}`}>
      <p className="conn-card__num">{conn.category.toUpperCase()}&nbsp; / &nbsp;{serial(conn.id)}</p>
      <div ref={motifRef} className={`conn-card__stills ${seen ? 'is-seen' : ''}`} aria-hidden="true">
        <span className="conn-card__still">{imgUrl(imgA) && <img src={imgUrl(imgA)} alt="" loading="lazy" />}</span>
        <span className="conn-card__still conn-card__still--b">{imgUrl(imgB) && <img src={imgUrl(imgB)} alt="" loading="lazy" />}</span>
      </div>
      <TitlePair film={film} art={art} />
      <p className="conn-card__meta">
        {film.year ?? '—'} · {film.creator} / {art.year ?? '—'} · {art.creator}
      </p>
      <PaletteBars palette={palA} label={kindLabel(film)} tone={toneA} />
      <PaletteBars palette={palB} label={kindLabel(art)} tone={toneB} />
      <p className="conn-card__quote">«{splitNote(conn.criticalNote).quote}»</p>
      <p className="conn-card__author">
        {conn.authorUsername
          ? <span role="link" tabIndex={0} className="author-link" onClick={openAuthor} onKeyDown={(e) => e.key === 'Enter' && openAuthor(e)}>{conn.authorName}</span>
          : 'Archivio VisuArt'} · Interpretazione personale
      </p>
      <p className="conn-card__stats">
        {n(conn.likeCount)} mi piace &nbsp;·&nbsp; {n(conn.commentCount)} commenti &nbsp;·&nbsp; {n(conn.viewCount)} viste
      </p>
    </Link>
  );
}
