import { pairOf } from '../lib/format';
import '../styles/marquee.css';

/** Una parola composta in tessere, come le lettere mobili dell'insegna. */
function TileWord({ word, tone, offset }) {
  return (
    <span className="mq-word">
      {[...word].map((ch, i) => (
        <span key={i} className={`mq-tile mq-tile--${tone}`} style={{ '--i': offset + i }}>{ch}</span>
      ))}
    </span>
  );
}

/** Una riga dell'insegna: il testo resta leggibile per gli screen reader, le tessere sono decorative. */
export function SignLine({ text, tone, start = 0, small = false }) {
  let offset = start;
  const words = text.toUpperCase().split(/\s+/).filter(Boolean);
  return (
    <span className={`mq-line ${small ? 'mq-line--small' : ''}`}>
      <span className="sr-only">{text}</span>
      <span className="mq-line__tiles" aria-hidden="true">
        {words.map((w, i) => {
          const el = <TileWord key={i} word={w} tone={tone} offset={offset} />;
          offset += w.length;
          return el;
        })}
      </span>
    </span>
  );
}

/** Titolo di pagina come striscia d'insegna: le pagine interne parlano la lingua del cartellone. */
export function PageSign({ text }) {
  return (
    <h1 className="mq-sign mq-sign--page">
      <span className="mq-sign__panel"><SignLine text={text} tone="ink" /></span>
    </h1>
  );
}

/**
 * L'insegna a lettere mobili: in cartellone un'opera CON un'altra (film o dipinto).
 * size="sm" è la versione in testa a ogni connessione.
 */
export default function Marquee({ conn, label, size = 'lg', as: Tag = 'div' }) {
  const pair = conn ? pairOf(conn) : null;
  const film = pair?.a;
  const art = pair?.b;
  const filmLen = film?.title.length ?? 0;
  return (
    <Tag className={`mq-sign ${size === 'sm' ? 'mq-sign--sm' : ''}`}>
      <span className="mq-sign__panel" key={conn?.id ?? 'empty'}>
        <SignLine text={label} tone="ink" small />
        {conn ? (
          <>
            <SignLine text={film.title} tone={pair?.toneA} start={4} />
            <SignLine text="con" tone="ink" start={4 + filmLen} small />
            <SignLine text={art.title} tone={pair?.toneB} start={8 + filmLen} />
          </>
        ) : (
          <SignLine text="Il programma arriva" tone="ink" start={4} />
        )}
      </span>
    </Tag>
  );
}
