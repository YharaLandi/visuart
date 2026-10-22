import { useEffect, useState } from 'react';

// Atmosfere stagionali: si accendono da sole in un periodo dell'anno e si spengono da sole
// alla fine, senza bisogno di un nuovo rilascio. Chi non le vuole può spegnerle dalle
// impostazioni (la scelta vale fino alla stagione successiva).
// index.html applica data-season prima del primo render, con la stessa regola.

const OFF_KEY = 'visuart_season_off';

/** Halloween: dal 20 ottobre al 31 ottobre compreso; il 1° novembre torna il tema normale. */
export const SEASONS = {
  halloween: { from: [10, 20], to: [10, 31] },
};

function inRange(date, { from, to }) {
  const md = (date.getMonth() + 1) * 100 + date.getDate();
  return md >= from[0] * 100 + from[1] && md <= to[0] * 100 + to[1];
}

/** La stagione del calendario in questo momento (anche se l'utente l'ha spenta). */
export function seasonOf(date = new Date()) {
  // ?stagione=halloween per vederla fuori periodo, ?stagione=nessuna per spegnerla in prova
  const forced = new URLSearchParams(window.location.search).get('stagione');
  if (forced) return SEASONS[forced] ? forced : null;
  return Object.keys(SEASONS).find((name) => inRange(date, SEASONS[name])) ?? null;
}

/** "halloween-2026": lo spegnimento vale solo per l'edizione di quest'anno. */
function edition(name, date = new Date()) {
  return `${name}-${date.getFullYear()}`;
}

export function isSeasonOff(name) {
  return localStorage.getItem(OFF_KEY) === edition(name);
}

/** La stagione attiva per questo utente, o null. */
export function activeSeason() {
  const name = seasonOf();
  return name && !isSeasonOff(name) ? name : null;
}

export function setSeasonOff(name, off) {
  if (off) localStorage.setItem(OFF_KEY, edition(name));
  else localStorage.removeItem(OFF_KEY);
  const now = activeSeason();
  if (now) document.documentElement.dataset.season = now;
  else delete document.documentElement.dataset.season;
  window.dispatchEvent(new Event('visuart:season'));
}

/** Film horror messi in evidenza durante la rassegna di Halloween. */
export const HALLOWEEN_TITLES = ['Shining', 'Suspiria', 'The Lighthouse'];

/**
 * Reparto fantasma: aperto solo nel mese di ottobre (?reparto=aperto per vederlo fuori mese).
 * Il 1° novembre la voce sparisce dal menu e la pagina dice che il reparto è chiuso.
 */
export function phantomOpen(date = new Date()) {
  const forced = new URLSearchParams(window.location.search).get('reparto');
  if (forced) return forced === 'aperto';
  // chi passa alla versione normale non vede il reparto
  return date.getMonth() === 9 && !isSeasonOff('halloween');
}

/** Si aggiorna quando l'utente cambia versione (rassegna o normale). */
export function useSeasonVersion() {
  const [v, setV] = useState(0);
  useEffect(() => {
    const bump = () => setV((x) => x + 1);
    window.addEventListener('visuart:season', bump);
    return () => window.removeEventListener('visuart:season', bump);
  }, []);
  return v;
}

const AGE_KEY = 'visuart_phantom_16';

/** Conferma dell'età: vale per l'edizione di quest'anno, poi si richiede. */
export function phantomAgeConfirmed() {
  return localStorage.getItem(AGE_KEY) === String(new Date().getFullYear());
}

export function confirmPhantomAge() {
  localStorage.setItem(AGE_KEY, String(new Date().getFullYear()));
}
