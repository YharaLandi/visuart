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

export function filmOf(conn) {
  return conn.source.type === 'FILM' ? conn.source : conn.target;
}

export function artOf(conn) {
  return conn.source.type === 'ARTWORK' ? conn.source : conn.target;
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
