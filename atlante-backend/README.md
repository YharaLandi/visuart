# Atlante delle Influenze — backend

Connessioni critiche tra cinema e arte visiva. Capstone Epicode, backend Spring Boot.

Struttura ripresa dai progetti settimanali: autenticazione JWT, "mi piace" e controllo
dei proprietari come in *SocialNetwork1.0*; chat in tempo reale (STOMP/WebSocket) e client
per il modello linguistico come in *Progetto-Settimanale-6*.

## Requisiti

Java 17+ (il pom usa 25), Maven. PostgreSQL solo per il profilo prod (in dev si usa H2 in memoria).

## Chiavi e password

Non stanno nel codice. In locale copia `secrets.example.yml` in `secrets.yml` (ignorato da git)
e inserisci chiave TMDB, chiave Gemini e password per le app di Gmail. In produzione gli stessi
valori arrivano dalle variabili d'ambiente (`TMDB_API_KEY`, `LLM_API_KEY`, `MAIL_PASSWORD`).

## Avvio rapido (dev)

```bash
mvn spring-boot:run
```

Parte su http://localhost:8080 con database H2 su file (`./data`, i dati restano tra un riavvio e l'altro; per ripartire da zero cancella la cartella) e dati demo caricati al primo avvio:
8 opere (4 film, 4 opere d'arte) e 5 connessioni critiche.

Login demo (password `password123`): `admin@atlante.local` (ADMIN) e `curatore@atlante.local` (CURATOR).
Il campo `email` del login accetta anche lo username (`admin`, `curatore`).

Console H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/atlante;AUTO_SERVER=TRUE`, user `sa`, password vuota).

Test: `mvn test` (integrazione con H2 e un finto servizio AI locale, nessuna rete richiesta).

## Endpoint REST

Documentazione interattiva: **http://localhost:8080/swagger-ui.html** (pulsante *Authorize* per
incollare il JWT del login e provare anche gli endpoint protetti).


| Metodo | Percorso | Auth |
|---|---|---|
| POST | /api/auth/register (`email`, `password`, `displayName`, `username` opzionale) | no |
| POST | /api/auth/login | no |
| POST | /api/auth/logout , /api/auth/refresh | JWT (revoca il token in uso) |
| POST | /api/auth/forgot-password , /api/auth/reset-password | no |
| GET/PUT/DELETE | /api/users/me | JWT (DELETE chiede la password e cancella tutto in cascata) |
| PUT | /api/users/me/password | JWT (chiude le altre sessioni, restituisce un token nuovo) |
| GET | /api/users/me/stats , POST /api/users/me/stats/email | JWT |
| GET | /api/notifications , /api/notifications/unread-count , POST /read-all | JWT |
| GET/PATCH/DELETE | /api/admin/users , /{id}/role , /{id} | ADMIN |
| GET | /api/works?type=FILM&q=... | no |
| POST | /api/works | JWT |
| PUT/DELETE | /api/works/{id} | ADMIN (un'opera usata in connessioni non si elimina) |
| GET | /api/connections?category=luce&sort=views | no |
| GET | /api/connections/search?q=&category=&type=&author=&sort=views|recent|oldest&page=0&size=12 | no |
| GET | /api/connections/{id} | no |
| POST | /api/connections | JWT |
| PUT/DELETE | /api/connections/{id} | JWT, solo autore o ADMIN (403 altrimenti) |
| GET | /api/connections/{id}/likes | no (`likedByMe` solo con JWT) |
| POST | /api/connections/{id}/likes | JWT (409 se già messo) |
| DELETE | /api/connections/{id}/likes | JWT (404 se non c'era) |
| GET/POST | /api/connections/{id}/comments | no / JWT |
| PUT/DELETE | /api/connections/{id}/comments/{commentId} | JWT, autore (DELETE anche ADMIN) |
| GET | /api/users/search?q= | JWT (solo username e nome, mai l'email) |
| GET/POST/DELETE | /api/favorites | JWT |
| GET | /api/graph?workId=1&depth=2&category= | no |
| GET | /api/categories | no |
| GET | /api/connections/{id}/ai-analysis | no |
| GET | /api/ai/discover?q=...&type=FILM | no |
| GET | /api/search/tmdb?q=... , /api/search/met?q=... | no |
| POST | /api/import/tmdb/{id} , /api/import/met/{objectId} | JWT |

Ogni connessione include `likeCount` e `likedByMe`.

Errori: JSON `{"error": "..."}` con 400 (validazione), 401 (credenziali), 403 (non sei il
proprietario), 404, 409 (like duplicato). Errori del servizio AI: 429 (con `Retry-After`)
oppure 502.

## Sessioni

Ogni JWT emesso viene registrato (solo come hash) e vale solo se non è stato revocato:
logout, refresh, cambio o reset della password e cambio di ruolo da parte di un admin
chiudono le sessioni interessate, sia REST sia WebSocket.

## Notifiche

Like e commenti alle proprie connessioni arrivano in tempo reale su `/user/queue/notifications`
(stessa connessione STOMP della chat) e restano consultabili via REST.

## Chat in tempo reale (WebSocket / STOMP)

Endpoint: `ws://localhost:8080/ws`. Il JWT va nell'header `Authorization: Bearer <token>`
del frame STOMP `CONNECT`; senza token valido la connessione viene rifiutata.

| Direzione | Destinazione | Payload |
|---|---|---|
| invia | `/app/chat.send` | `{recipient, text, tempId, connectionId?}` — `recipient` è lo username |
| invia | `/app/chat.read` | `{conversationId}` |
| invia | `/app/chat.typing` | `{recipient}` |
| ricevi | `/user/queue/messages` | messaggio (stato `SENT`/`DELIVERED`/`READ`); il mittente riceve l'eco con `tempId` |
| ricevi | `/user/queue/updates` | `{type: READ\|TYPING\|PRESENCE, conversationId, user, messageIds}` |
| ricevi | `/user/queue/errors` | `{error}` per i frame non validi |

REST di supporto (JWT): `POST /api/chat/conversations`, `GET /api/chat/conversations`,
`GET /api/chat/history?with=<username>&before=<idMessaggio>` (30 alla volta, `hasMore`),
`GET /api/chat/unread-count`, `POST /api/chat/messages` (stessa consegna del WebSocket) e
`PATCH /api/chat/conversations/{id}/delivered`. Un messaggio può contenere una connessione
condivisa (`connectionId`), mostrata come card.

Esempio con `@stomp/stompjs`:

```js
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: { Authorization: `Bearer ${token}` },
  onConnect: () => {
    client.subscribe('/user/queue/messages', (f) => console.log(JSON.parse(f.body)));
    client.publish({ destination: '/app/chat.send',
      body: JSON.stringify({ recipient: 'curatore', text: 'Ciao!', tempId: crypto.randomUUID() }) });
  },
});
client.activate();
```

## Intelligenza artificiale

Tutte le funzioni AI passano da `LlmService` (client REST verso `/chat/completions`, formato
compatibile OpenAI): analisi cromatica di una connessione, scoperta assistita e
`POST /api/chat/suggest` (`{conversationId}` → `{suggestion}`, il prossimo messaggio suggerito,
non salvato). Di default usa l'endpoint OpenAI-compatibile di Gemini; per cambiare provider
bastano `LLM_BASE_URL`, `LLM_MODEL` e `LLM_API_KEY` (`GEMINI_API_KEY` resta valida come alias).

## API esterne

TMDB richiede una chiave gratuita (https://www.themoviedb.org/settings/api):

```bash
export TMDB_API_KEY=la-tua-chiave
```

Il Met Museum non richiede chiave. Le immagini importate vengono salvate in
`./storage/images` (strategia snapshot: la demo non dipende dalla rete).

## Mail

Recupero password e report statistiche partono da Gmail (comunication.visuart@gmail.com) con
una *password per le app* nella variabile `MAIL_PASSWORD`. Senza, l'invio è disattivato e il
link di reset viene scritto nel log.

## Deploy su Render

Nella cartella principale c'è `render.yaml` (PostgreSQL + backend Docker + frontend statico):
su Render *New → Blueprint*, poi inserisci dalla dashboard `ADMIN_PASSWORD`, `CORS_ALLOWED_ORIGINS`,
`FRONTEND_URL`, `MAIL_PASSWORD`, `LLM_API_KEY`, `TMDB_API_KEY` e, per il frontend, `VITE_API_BASE_URL`.
`DATABASE_URL` di Render viene convertito automaticamente; health check su `/actuator/health`.

## Profilo prod

`JWT_SECRET` deve essere una stringa **Base64** di almeno 32 byte (`openssl rand -base64 48`).

```bash
export SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://localhost:5432/atlante \
       DB_USER=atlante DB_PASSWORD=... JWT_SECRET=... LLM_API_KEY=... \
       CORS_ALLOWED_ORIGINS=https://tuo-frontend.example
mvn spring-boot:run
```
