# VisuArt — frontend

Interfaccia React di VisuArt, l'archivio di relazioni tra cinema e arti visive.

## Avvio

```bash
cp .env.example .env     # VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev              # http://localhost:5173
```

Il backend deve essere attivo (vedi `../atlante-backend/README.md`).

| Comando | Cosa fa |
|---|---|
| `npm run dev` | server di sviluppo con aggiornamento automatico |
| `npm run build` | build di produzione in `dist/` |
| `npm run preview` | anteprima della build |
| `npm run lint` | controllo del codice con oxlint |

## Struttura

```
src/
  api/          client REST (client.js) e WebSocket STOMP (stomp.js)
  context/      sessione (AuthContext) e stato condiviso: preferiti, messaggi non letti (AppContext)
  components/   card, lettura AI, dialoghi, scheda opera, ...
    ui/         design system: layout e navigazione, icone, motivo dei due cerchi, stati di servizio
  pages/        una pagina per rotta
  lib/          formattazioni (numeri, date, ruoli)
  styles/       visuart.css: token e stili di tutta l'app
```

## Rotte

| Rotta | Pagina | Accesso |
|---|---|---|
| `/` | Home | tutti |
| `/connessioni` · `/connessioni/:id` | Più viste · dettaglio | tutti |
| `/grafo` | Atlante delle affinità | tutti |
| `/cerca` | Ricerca negli archivi e scheda opera | tutti |
| `/utenti/:username` | Profilo pubblico | tutti |
| `/collezione` | La mia collezione | login |
| `/chat` · `/chat/:username` | Conversazioni | login |
| `/profilo` · `/impostazioni` | Profilo e impostazioni | login |
| `/nuova-critica` · `/connessioni/:id/modifica` | Scrivere o modificare una critica | login |
| `/admin` | Gestione utenti | admin |
| `/accedi` · `/registrati` · `/recupero-password` · `/reimposta-password` | Accesso | tutti |

## Design

Tema scuro da galleria: teal `#55e0d2` per il cinema, ambra `#f7b24d` per l'arte, rosa
solo per i preferiti. Titoli in Source Serif 4, testo in Manrope, icone Bootstrap Icons.
I token sono all'inizio di `src/styles/visuart.css`. Layout responsive: sotto gli 860 px
compaiono la barra di navigazione in basso e il pulsante "Nuova critica".

## Tempo reale

Da loggati resta aperta una sessione STOMP su `/ws` (JWT nel frame CONNECT): aggiorna
notifiche, numero di messaggi non letti e mostra un avviso quando arriva un messaggio.
La pagina chat apre una seconda sessione per invio, conferme di lettura e "sta scrivendo".
