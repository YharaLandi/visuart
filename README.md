# VisuArt · Cinema e arti visive

Un archivio di relazioni tra cinema e pittura. Ogni **connessione** accosta un film e
un'opera d'arte con una nota critica personale: si leggono, si commentano, si salvano
nella propria collezione, si esplorano in un grafo e se ne parla in chat.

Capstone Project Epicode.

## Funzionalità

- **Più viste** — le connessioni ordinate per visualizzazioni, con filtri per categoria e paginazione
- **Dettaglio connessione** — palette cromatiche a confronto, mi piace, preferiti, dialoghi (commenti)
- **Lettura AI** — temi e confronto cromatico generati con Gemini
- **Grafo delle affinità** — opere come nodi, connessioni come archi, profondità regolabile
- **Ricerca negli archivi** — TMDB per i film, Wikimedia / Art Institute of Chicago per i quadri, con comparazione AI
- **La mia collezione** — opere salvate
- **Chat in tempo reale** — WebSocket/STOMP, stato inviato/consegnato/letto, "sta scrivendo", connessioni condivise, suggerimento del prossimo messaggio con AI
- **Notifiche** — mi piace e commenti alle proprie connessioni, in tempo reale
- **Account** — registrazione, login con revoca dei token, recupero password via mail, profilo, impostazioni
- **Amministrazione** — ruoli Admin / Curatore / Utente, gestione utenti

## Stack

| Parte | Tecnologie |
|---|---|
| Backend | Java 25, Spring Boot 3.5, Spring Security + JWT, Spring Data JPA, WebSocket/STOMP, Spring Mail + Thymeleaf, springdoc OpenAPI |
| Frontend | React 19, Vite, React Router, React Flow, @stomp/stompjs, Bootstrap Icons |
| Database | H2 su file in sviluppo, PostgreSQL in produzione |
| Servizi esterni | Gemini (API compatibile OpenAI), TMDB, Art Institute of Chicago, Wikimedia, Gmail SMTP |

## Struttura

```
atlante-backend/    API Spring Boot          → vedi atlante-backend/README.md
atlante-frontend/   interfaccia React (Vite)
render.yaml         blueprint per il deploy su Render
```

## Avvio in locale

Requisiti: Java 25, Maven 3.9+, Node 20+.

```bash
# 1. backend
cd atlante-backend
cp secrets.example.yml secrets.yml   # inserisci le tue chiavi (TMDB, Gemini, Gmail)
mvn spring-boot:run                  # http://localhost:8080 · Swagger su /swagger-ui.html

# 2. frontend (in un altro terminale)
cd atlante-frontend
cp .env.example .env
npm install
npm run dev                          # http://localhost:5173
```

Al primo avvio vengono caricati opere e connessioni demo.
Account demo: `admin` / `password123` (Admin) e `curatore` / `password123` (Curatore).

## Test

```bash
cd atlante-backend && mvn test
```

Test di integrazione su autenticazione, like, permessi, chat WebSocket, notifiche,
ricerca paginata, recupero password e gestione degli errori del servizio AI.

## Deploy

Il file `render.yaml` crea su Render database PostgreSQL, backend (Docker) e frontend
statico. Dettagli e variabili d'ambiente in `atlante-backend/README.md`.
