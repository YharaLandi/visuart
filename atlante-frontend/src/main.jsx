import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { activeSeason } from './lib/season';

// atmosfera stagionale (es. Halloween): applicata prima del primo render, si spegne da sola a fine periodo
const season = activeSeason();
if (season) document.documentElement.dataset.season = season;

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>
);
