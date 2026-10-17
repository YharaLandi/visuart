# Atlante delle influenze — frontend

Interfaccia React del grafo interattivo. Richiede il backend Spring Boot già
avviato su `http://localhost:8080` (vedi `atlante-backend/README.md`).

## Avvio

```bash
npm install
npm run dev
```

Apre su `http://localhost:5173`. Il file `.env` punta già al backend locale
(`VITE_API_BASE_URL=http://localhost:8080`) — non serve toccarlo in sviluppo.

## Cosa vedrai

Il grafo carica automaticamente le 8 opere e 5 connessioni del seed demo.
Clicca su un'etichetta di connessione (sulla linea tra due nodi) per aprire il
pannello a destra con la nota critica. I chip in alto filtrano per categoria.

## Struttura

```
src/
  api/client.js         chiamate REST verso il backend, gestione token JWT
  context/AuthContext   stato di login (non ancora usato in UI, pronto per estensioni)
  components/
    GraphView           canvas React Flow, fetch di /api/graph
    WorkNode             nodo custom (cerchio colorato per film/opera d'arte)
    DetailPanel          pannello nota critica
    CategoryFilter        chip di filtro categoria
    NavBar               intestazione
  pages/AtlasPage        composizione della pagina principale
  lib/layout.js          layout circolare dei nodi (nessuna libreria esterna)
  styles/global.css      palette navy/teal/oro, Manrope + Source Serif 4
```

## Prossimi passi naturali

- Form di login/registrazione collegato ad `AuthContext` (già pronto lato client)
- Form di creazione connessione (CRUD lato UI, oggi solo lettura)
- Ricerca e import da TMDB/Met Museum direttamente dall'interfaccia
