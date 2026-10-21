// Formattazioni condivise tra le schermate (numeri e date in italiano).

export const ROLE_LABEL = { ADMIN: 'Admin', CURATOR: 'Curatore', USER: 'Utente' };

export const ROLE_DESCRIPTION = {
  ADMIN: 'Gestisci utenti e ruoli, modera le critiche e cura la qualità dei metadati dell’archivio.',
  CURATOR: 'Curi i metadati, verifichi le fonti e accompagni il dialogo critico tra le opere.',
  USER: 'Esplora, salva opere e pubblica le tue connessioni. Ogni nuovo sguardo arricchisce l’archivio.',
};

export function n(value) {
  return new Intl.NumberFormat('it-IT').format(value ?? 0);
}

export function serial(id) {
  return `N° ${String(id).padStart(3, '0')}`;
}

/**
 * Le due opere di una connessione, in ordine di lettura. Una coppia può essere film–opera,
 * film–film oppure opera–opera: nelle coppie miste il film viene sempre prima (come in
 * "film CON dipinto"); nelle coppie dello stesso tipo resta l'ordine scelto da chi scrive.
 * Ogni opera porta la sua tinta: 'film' / 'art', e 'film2' / 'art2' per la seconda di due
 * opere dello stesso tipo, così titoli e palette si distinguono anche per colore.
 */
export function pairOf(conn) {
  const { source, target } = conn;
  const mixed = source.type !== target.type;
  const swap = mixed && source.type === 'ARTWORK';
  const [a, b] = swap ? [target, source] : [source, target];
  // immagine scelta da chi scrive (fotogramma, dettaglio del quadro), altrimenti quella dell'opera
  const [imgA, imgB] = swap
    ? [conn.targetImageUrl || target.imageUrl, conn.sourceImageUrl || source.imageUrl]
    : [conn.sourceImageUrl || source.imageUrl, conn.targetImageUrl || target.imageUrl];
  // palette scelta da chi scrive, altrimenti quella dell'opera
  const [palA, palB] = swap
    ? [conn.targetPalette || target.palette, conn.sourcePalette || source.palette]
    : [conn.sourcePalette || source.palette, conn.targetPalette || target.palette];
  const base = (w) => (w.type === 'ARTWORK' ? 'art' : 'film');
  return {
    a,
    b,
    toneA: base(a),
    toneB: mixed ? base(b) : `${base(b)}2`,
    imgA,
    imgB,
    palA,
    palB,
    mixed,
  };
}

/** Prima opera della coppia (il film, nelle coppie miste). */
export function filmOf(conn) {
  return pairOf(conn).a;
}

/** Seconda opera della coppia (il dipinto, nelle coppie miste). */
export function artOf(conn) {
  return pairOf(conn).b;
}

/** Etichetta del tipo di opera, al singolare. */
export function kindLabel(work, short = false) {
  if (work?.type === 'ARTWORK') return short ? 'Opera' : 'Opera d’arte';
  return 'Film';
}

/** Prima frase come citazione, il resto come paragrafi. */
export function splitNote(note = '') {
  const text = note.trim();
  const match = text.match(/^(.+?[.!?…])(\s+|$)([\s\S]*)$/);
  if (!match) return { quote: text, body: [] };
  const body = match[3].split(/\n\s*\n/).map((p) => p.trim()).filter(Boolean);
  return { quote: match[1], body };
}

export function readingMinutes(note = '') {
  return Math.max(1, Math.round(note.split(/\s+/).length / 200));
}

export function decade(year) {
  if (!year) return null;
  const d = Math.floor(year / 10) * 10;
  return `${d}–${d + 9}`;
}

export function timeAgo(iso) {
  if (!iso) return '';
  const minutes = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (minutes < 1) return 'ora';
  if (minutes < 60) return `${minutes} min fa`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} ore fa`;
  const days = Math.round(hours / 24);
  return days === 1 ? 'ieri' : `${days} giorni fa`;
}

export function clock(iso) {
  return iso ? new Date(iso).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }) : '';
}

/** Orario per la lista conversazioni: ora se oggi, "Ieri", altrimenti giorno breve. */
export function shortWhen(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const today = new Date();
  const diff = Math.floor((new Date(today.toDateString()) - new Date(d.toDateString())) / 86400000);
  if (diff === 0) return clock(iso);
  if (diff === 1) return 'Ieri';
  if (diff < 7) {
    const day = d.toLocaleDateString('it-IT', { weekday: 'short' });
    return day.charAt(0).toUpperCase() + day.slice(1, 3);
  }
  return d.toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit' });
}

export function dayLabel(iso) {
  const d = new Date(iso);
  const today = new Date();
  const diff = Math.floor((new Date(today.toDateString()) - new Date(d.toDateString())) / 86400000);
  const date = d.toLocaleDateString('it-IT', { day: 'numeric', month: 'long' }).toUpperCase();
  if (diff === 0) return `OGGI · ${date}`;
  if (diff === 1) return `IERI · ${date}`;
  return date;
}

export function longDate(iso) {
  return iso ? new Date(iso).toLocaleDateString('it-IT', { day: 'numeric', month: 'long', year: 'numeric' }) : '';
}

export function monthYear(iso) {
  return iso ? new Date(iso).toLocaleDateString('it-IT', { month: 'long', year: 'numeric' }) : '';
}

export function sourceLabel(work) {
  const map = { TMDB: 'The Movie Database', MET: 'The Metropolitan Museum of Art', AIC: 'Art Institute of Chicago', WIKIPEDIA: 'Wikimedia Commons', WIKIDATA: 'Wikidata', MANUAL: 'Archivio VisuArt' };
  return map[work?.externalSource] ?? 'Archivio VisuArt';
}

const RING = { film: 'var(--film)', art: 'var(--art)', film2: 'var(--film-2)', art2: 'var(--art-2)' };

/**
 * Variabili CSS per i cerchi e la linea che uniscono le due opere: ogni anello prende la tinta
 * della sua opera (due film = verde acqua e blu, due quadri = arancio e ocra).
 */
export function ringStyle(toneA, toneB) {
  return { '--film-ring': RING[toneA] ?? RING.film, '--art-ring': RING[toneB] ?? RING.art };
}

/** Tinte della coppia a partire dalle due opere, nell'ordine in cui sono mostrate. */
export function tonesOf(a, b) {
  const base = (w) => (w?.type === 'ARTWORK' ? 'art' : 'film');
  return [base(a), base(b) + (a?.type === b?.type ? '2' : '')];
}

