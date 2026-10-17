import { Link } from 'react-router-dom';
import { artOf, filmOf, n, serial, splitNote } from '../lib/format';
import { Motif, PaletteBars, TitlePair } from './ui/Visual';

/** Card connessione: categoria e numero, motivo, titoli, due palette, citazione, autore, numeri. */
export default function ConnectionCard({ conn, selected = false }) {
  const film = filmOf(conn);
  const art = artOf(conn);
  return (
    <Link to={`/connessioni/${conn.id}`} className={`card conn-card ${selected ? 'conn-card--selected' : ''}`}>
      <p className="conn-card__num">{conn.category.toUpperCase()}&nbsp; / &nbsp;{serial(conn.id)}</p>
      <div className="conn-card__motif"><Motif film={film} art={art} size="md" /></div>
      <TitlePair film={film} art={art} />
      <p className="conn-card__meta">
        {film.year ?? '—'} · {film.creator} / {art.year ?? '—'} · {art.creator}
      </p>
      <PaletteBars palette={film.palette} label="Film" tone="film" />
      <PaletteBars palette={art.palette} label="Opera d’arte" tone="art" />
      <p className="conn-card__quote">«{splitNote(conn.criticalNote).quote}»</p>
      <p className="conn-card__author">{conn.authorName ?? 'Archivio VisuArt'} · Interpretazione personale</p>
      <p className="conn-card__stats">
        {n(conn.likeCount)} mi piace &nbsp;·&nbsp; {n(conn.commentCount)} commenti &nbsp;·&nbsp; {n(conn.viewCount)} viste
      </p>
    </Link>
  );
}
