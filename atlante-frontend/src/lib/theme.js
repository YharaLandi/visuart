// Tema dell'interfaccia: "dark" (facciata di sera, predefinito) o "light" (manifesto di giorno).
// La scelta resta sul dispositivo; index.html la applica prima del primo render.
const KEY = 'visuart_theme';

export function getTheme() {
  return localStorage.getItem(KEY) === 'light' ? 'light' : 'dark';
}

export function setTheme(theme) {
  localStorage.setItem(KEY, theme);
  document.documentElement.dataset.theme = theme;
}
