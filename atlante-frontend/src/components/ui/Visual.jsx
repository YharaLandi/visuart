import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { imgUrl } from '../../api/client';
import Icon from './Icon';
import { downloadPaletteCard } from '../../lib/paletteCard';

/** Logo: due cerchi (film teal, opera ambra) e "VisuArt" in serif corsivo. */
export function Brand({ to = '/', dim = false }) {
  return (
    <Link to={to} className={`brand ${dim ? 'brand--dim' : ''}`} aria-label="VisuArt, home">
      <span className="brand-mark"><i /><b /><i /></span>
      <span className="brand__name">VisuArt</span>
    </Link>
  );
}

/** Motivo ricorrente: cerchio del film, linea sfumata, cerchio dell'opera. */
export function Motif({ film, art, size = 'md', filmSrc, artSrc }) {
  const f = filmSrc ?? imgUrl(film?.imageUrl);
  const a = artSrc ?? imgUrl(art?.imageUrl);
  return (
    <span className={`motif motif--${size}`}>
      <span className="motif__c">{f && <img src={f} alt={film?.title ?? ''} />}</span>
      <span className="motif__line" />
      <span className="motif__c motif__c--art">{a && <img src={a} alt={art?.title ?? ''} />}</span>
    </span>
  );
}

export function paletteColors(palette) {
  return (palette ?? '').split(',').map((c) => c.trim()).filter(Boolean).slice(0, 6);
}

/**
 * Sei campioni della palette. Nella versione grande (dettaglio) i campioni si copiano con un clic
 * e si può copiare l'intera palette o scaricarla come immagine.
 */
export function PaletteBars({ palette, label, tone, title, large = false, showHex = false }) {
  const colors = paletteColors(palette);
  const [copied, setCopied] = useState(null);

  useEffect(() => {
    if (!copied) return undefined;
    const timer = setTimeout(() => setCopied(null), 1600);
    return () => clearTimeout(timer);
  }, [copied]);

  const copy = (text) => navigator.clipboard?.writeText(text).then(() => setCopied(text)).catch(() => {});
  const interactive = large && colors.length > 0;

  return (
    <div>
      {label && <p className={`palette__label ${tone === 'art' ? 't-art' : 't-film'}`}>{label}</p>}
      <div className={`palette ${large ? 'palette--lg' : ''}`}>
        {(colors.length ? colors : Array(6).fill('var(--surface-3)')).map((c, i) => (interactive ? (
          <button key={i} type="button" className="swatch" style={{ background: c }} data-hex={c}
            aria-label={`Copia ${c}`} onClick={() => copy(c)} />
        ) : (
          <span key={i} style={{ background: c }} title={colors.length ? c : undefined} />
        )))}
      </div>
      {interactive && (
        <div className="palette__actions">
          <button type="button" className="palette__copy" onClick={() => copy(colors.join(', '))}
            aria-label={`Copia la palette${label ? ` ${label}` : ''}`}>
            <Icon name="clipboard" /> Copia palette
          </button>
          <button type="button" className="palette__copy"
            onClick={() => downloadPaletteCard({ colors, label, title, tone }).then(() => setCopied('card'))}
            aria-label={`Scarica la card della palette${title ? ` di ${title}` : ''}`}>
            <Icon name="download" /> Scarica card
          </button>
        </div>
      )}
      {copied && (
        <p className="copy-toast" role="status">
          <Icon name="check" /> {copied === 'card' ? 'Card scaricata' : copied.includes(',') ? 'Palette copiata' : `Copiato ${copied}`}
        </p>
      )}
      {showHex && colors.length > 0 && <p className="palette__hex">{colors.join(' ')}</p>}
    </div>
  );
}

/** "In the Mood for Love / [↔] Nighthawks": titolo del film in teal, opera in ambra. */
export function TitlePair({ film, art, as: Tag = 'h3', className = '' }) {
  return (
    <Tag className={`title-pair ${className}`}>
      <span className="title-pair__film">{film?.title}</span>
      <span className="title-pair__art"><span className="title-pair__arrow"><Icon name="arrowsLR" /></span>{art?.title}</span>
    </Tag>
  );
}

export function Avatar({ name, size = '' }) {
  return <span className={`avatar ${size ? `avatar--${size}` : ''}`}>{name?.trim()?.[0]?.toUpperCase() ?? '?'}</span>;
}

export function Footer() {
  return (
    <footer className="footer container">
      <div className="footer__inner">
        <span>VisuArt © 2026 · Un archivio di sguardi</span>
        <span><a href="#progetto">Il progetto</a> · <a href="#fonti">Fonti</a> · <a href="#privacy">Privacy</a></span>
      </div>
    </footer>
  );
}
