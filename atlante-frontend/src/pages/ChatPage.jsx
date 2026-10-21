import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { chat as chatApi, connections as connectionsApi, users as usersApi } from '../api/client';
import { connectChat } from '../api/stomp';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { Avatar } from '../components/ui/Visual';
import { useApp } from '../context/AppContext';
import { useAuth } from '../context/AuthContext';
import { artOf, clock, dayLabel, filmOf, pairOf, ROLE_LABEL, shortWhen } from '../lib/format';

const TYPING_MS = 2500;
const STATUS = { SENT: 'Inviato', DELIVERED: 'Consegnato', READ: 'Letto' };
const TICK = { SENT: 'check', DELIVERED: 'checkAll', READ: 'checkAll' };
const THEIRS = { SENT: 'Inviato', DELIVERED: 'Consegnato', READ: 'Letto' };

// cache delle connessioni citate in chat (card condivise e "state parlando di")
const connCache = new Map();
function loadConnection(id) {
  if (!connCache.has(id)) connCache.set(id, connectionsApi.get(id).catch(() => null));
  return connCache.get(id);
}

function useConnection(id) {
  const [conn, setConn] = useState(null);
  useEffect(() => {
    let alive = true;
    if (id) loadConnection(id).then((c) => alive && setConn(c));
    else setConn(null);
    return () => { alive = false; };
  }, [id]);
  return conn;
}

function SharedCard({ id }) {
  const conn = useConnection(id);
  if (!conn) return <div className="shared"><small>Connessione #{id}</small></div>;
  return (
    <Link to={`/connessioni/${id}`} className="shared">
      <small>CONNESSIONE CONDIVISA · #{String(id).padStart(3, '0')}</small>
      <span><span className={'t-' + pairOf(conn).toneA}>{filmOf(conn).title}</span><Icon name="arrowsLR" className="inline-icon" /><span className={'t-' + pairOf(conn).toneB}>{artOf(conn).title}</span></span>
    </Link>
  );
}

function About({ id }) {
  const conn = useConnection(id);
  if (!conn) return null;
  return (
    <Link to={`/connessioni/${id}`} className="about">
      <Icon name="link" />
      <div>
        <small>STATE PARLANDO DI · #{String(id).padStart(3, '0')}</small>
        <span>{filmOf(conn).title}<Icon name="arrowsLR" className="inline-icon" />{artOf(conn).title}</span>
      </div>
    </Link>
  );
}

/** Allegato: sceglie una connessione dell'archivio da condividere nel messaggio. */
function AttachPicker({ onPick, onClose }) {
  const [q, setQ] = useState('');
  const [list, setList] = useState([]);
  useEffect(() => {
    const timer = setTimeout(() => {
      connectionsApi.search({ q: q.trim(), size: 8 }).then((d) => setList(d.content)).catch(() => setList([]));
    }, 200);
    return () => clearTimeout(timer);
  }, [q]);
  return (
    <div className="share-pop">
      <label className="field">
        <span className="field__label">Condividi una connessione</span>
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Titolo di un film o di un’opera" autoFocus />
      </label>
      <div className="share-pop__list">
        {list.map((c) => (
          <button key={c.id} onClick={() => onPick(c.id)}>
            <span className={'t-' + pairOf(c).toneA}>{filmOf(c).title}</span><Icon name="arrowsLR" className="inline-icon" /><span className={'t-' + pairOf(c).toneB}>{artOf(c).title}</span>
          </button>
        ))}
      </div>
      <button className="link-muted" style={{ marginTop: 8 }} onClick={onClose}>Annulla</button>
    </div>
  );
}

export default function ChatPage() {
  const { username: withParam } = useParams();
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { refreshChatUnread } = useApp();

  const [conversations, setConversations] = useState([]);
  const [filter, setFilter] = useState('');
  const [people, setPeople] = useState([]);
  const [page, setPage] = useState(null);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [fromAi, setFromAi] = useState(false);
  const [typing, setTyping] = useState(false);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState(null);
  const [suggestion, setSuggestion] = useState({ state: 'idle', text: '' });
  const [attach, setAttach] = useState(false);
  const [pendingShare, setPendingShare] = useState(params.get('condividi') ? Number(params.get('condividi')) : null);

  const socket = useRef(null);
  const convRef = useRef(null);
  const withRef = useRef(withParam);
  const typingTimer = useRef(null);
  const lastTyping = useRef(0);
  const bottom = useRef(null);
  const keepScroll = useRef(false);
  convRef.current = page?.conversationId ?? null;
  withRef.current = withParam;

  const refreshConversations = useCallback(() => {
    chatApi.conversations().then(setConversations).catch(() => {});
  }, []);

  // una sola sessione STOMP per la pagina
  useEffect(() => {
    refreshConversations();
    socket.current = connectChat({
      onStatus: setConnected,
      onError: setError,
      onMessage: (m) => {
        if (m.conversationId === convRef.current) {
          setMessages((prev) => (prev.some((x) => x.id === m.id) ? prev : [...prev, m]));
          if (m.recipient === user.username) socket.current?.markRead(m.conversationId);
          if (m.connectionId) setPage((p) => (p ? { ...p, lastSharedConnectionId: m.connectionId } : p));
        } else if (m.sender === withRef.current || m.recipient === withRef.current) {
          // primo messaggio di una conversazione appena nata: ricarica la cronologia
          chatApi.history(withRef.current).then((p) => { setPage(p); setMessages(p.messages); });
        }
        refreshConversations();
        refreshChatUnread();
      },
      onUpdate: (u) => {
        if (u.type === 'READ' && u.conversationId === convRef.current) {
          const ids = new Set(u.messageIds);
          setMessages((prev) => prev.map((m) => (ids.has(m.id) ? { ...m, status: 'READ' } : m)));
        }
        if (u.type === 'TYPING' && u.conversationId === convRef.current) {
          setTyping(true);
          clearTimeout(typingTimer.current);
          typingTimer.current = setTimeout(() => setTyping(false), TYPING_MS);
        }
        if (u.type === 'READ' || u.type === 'PRESENCE') {
          refreshConversations();
          refreshChatUnread();
        }
      },
    });
    return () => {
      clearTimeout(typingTimer.current);
      socket.current?.disconnect();
    };
  }, [user.username, refreshConversations, refreshChatUnread]);

  // apre la conversazione indicata nell'indirizzo (/chat/:username)
  useEffect(() => {
    setPage(null);
    setMessages([]);
    setTyping(false);
    setSuggestion({ state: 'idle', text: '' });
    setError(null);
    if (!withParam) return;
    chatApi.create(withParam)
      .then(() => chatApi.history(withParam))
      .then((p) => {
        setPage(p);
        setMessages(p.messages);
        if (p.conversationId && p.unread > 0) socket.current?.markRead(p.conversationId);
        refreshConversations();
      })
      .catch((e) => setError(e.message));
  }, [withParam, refreshConversations]);

  useEffect(() => {
    if (keepScroll.current) {
      keepScroll.current = false;
      return;
    }
    bottom.current?.scrollIntoView({ block: 'nearest' });
  }, [messages, typing]);

  // persone da cercare quando il filtro non trova conversazioni
  useEffect(() => {
    const t = filter.trim();
    if (t.length < 2) {
      setPeople([]);
      return undefined;
    }
    const timer = setTimeout(() => usersApi.search(t).then(setPeople).catch(() => setPeople([])), 250);
    return () => clearTimeout(timer);
  }, [filter]);

  const shown = useMemo(() => {
    const t = filter.trim().toLowerCase();
    if (!t) return conversations;
    return conversations.filter((c) =>
      [c.counterpart, c.counterpartName, c.lastText].some((v) => v?.toLowerCase().includes(t)));
  }, [conversations, filter]);
  const newPeople = people.filter((p) => !conversations.some((c) => c.counterpart === p.username));

  const active = conversations.find((c) => c.counterpart === withParam);
  const activeName = active?.counterpartName ?? withParam;

  const loadOlder = async () => {
    const p = await chatApi.history(withParam, messages[0]?.id);
    keepScroll.current = true;
    setMessages((prev) => [...p.messages, ...prev]);
    setPage((prev) => ({ ...prev, hasMore: p.hasMore }));
  };

  const send = (e) => {
    e?.preventDefault();
    const text = draft.trim();
    if ((!text && !pendingShare) || !withParam) return;
    socket.current?.sendMessage(withParam, text, pendingShare ?? undefined);
    setDraft('');
    setFromAi(false);
    setPendingShare(null);
    setSuggestion({ state: 'idle', text: '' });
    if (params.get('condividi')) setParams({}, { replace: true });
  };

  const onType = (value) => {
    setDraft(value);
    const now = Date.now();
    if (withParam && now - lastTyping.current > 1500) {
      lastTyping.current = now;
      socket.current?.typing(withParam);
    }
  };

  const suggest = async () => {
    setSuggestion({ state: 'loading', text: '' });
    try {
      const r = await chatApi.suggest(page.conversationId);
      setSuggestion({ state: 'ready', text: r.suggestion });
    } catch (err) {
      setSuggestion({ state: 'error', text: err.message });
    }
  };

  const accept = () => {
    setDraft(suggestion.text);
    setFromAi(true);
    setSuggestion((s) => ({ ...s, state: 'accepted' }));
  };

  // separatore di giorno prima del primo messaggio di ogni data
  const rows = [];
  let lastDay = null;
  messages.forEach((m) => {
    const day = new Date(m.sentAt).toDateString();
    if (day !== lastDay) {
      rows.push({ sep: dayLabel(m.sentAt), key: `d${m.id}` });
      lastDay = day;
    }
    rows.push({ m, key: m.id });
  });

  return (
    <Layout>
      <div className={`container chat ${withParam ? 'chat--open' : ''}`}>
        <aside className="chat__side">
          <h1 className="chat__title">Conversazioni</h1>
          <label className="field chat__search">
            <span className="field__label">Cerca una conversazione</span>
            <input value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Nome o messaggio" />
          </label>

          <div className="conv-list">
            {shown.map((c) => (
              <button
                key={c.conversationId}
                className={`conv ${c.counterpart === withParam ? 'conv--active' : ''}`}
                onClick={() => navigate(`/chat/${c.counterpart}`)}
              >
                <Avatar name={c.counterpartName ?? c.counterpart} username={c.counterpart} size="sm" />
                <div className="conv__body">
                  <div className="conv__row"><strong>{c.counterpartName ?? c.counterpart}</strong><small>{shortWhen(c.lastAt)}</small></div>
                  <p className="conv__last">{c.lastText === '' ? 'Connessione condivisa' : (c.lastText ?? 'Nessun messaggio')}</p>
                  <p className={`conv__status ${c.online ? 'conv__status--on' : ''}`}>
                    {c.online ? 'Online' : 'Non in linea'}
                    {c.unread > 0 && <span className="t-film"> · {c.unread} {c.unread === 1 ? 'nuovo messaggio' : 'nuovi messaggi'}</span>}
                  </p>
                </div>
              </button>
            ))}
            {conversations.length === 0 && !filter && (
              <p className="small">Nessuna conversazione. Cerca una persona qui sopra o scrivi all’autore di una connessione.</p>
            )}
          </div>

          {newPeople.length > 0 && (
            <>
              <p className="people-label">Persone nell’archivio</p>
              <div className="conv-list">
                {newPeople.map((p) => (
                  <button key={p.username} className="conv" onClick={() => { setFilter(''); navigate(`/chat/${p.username}`); }}>
                    <Avatar name={p.displayName} username={p.username} size="sm" />
                    <div className="conv__body">
                      <div className="conv__row"><strong>{p.displayName}</strong></div>
                      <p className="conv__last">@{p.username} · Inizia un dialogo</p>
                    </div>
                  </button>
                ))}
              </div>
            </>
          )}
        </aside>

        <section className="thread" aria-label="Conversazione">
          {!withParam ? (
            <div className="state">
              <h3>Nessun dialogo aperto.</h3>
              <p>Scegli una conversazione o cerca una persona per iniziarne una nuova.</p>
            </div>
          ) : (
            <>
              <div className="thread__head">
                {/* su mobile la colonna con "Conversazioni" è nascosta: il dialogo ha il suo titolo */}
                <h2 className="sr-only">Conversazione con {activeName}</h2>
                <button className="icon-btn thread__back" onClick={() => navigate('/chat')} aria-label="Torna alle conversazioni">
                  <Icon name="arrowLeft" />
                </button>
                <Avatar name={activeName} username={withParam} size="sm" />
                <div>
                  <strong><Link to={`/utenti/${withParam}`} className="author-link">{activeName}</Link></strong>
                  <small className={active?.online ? 'on' : ''}>
                    {active?.online ? 'Online' : 'Non in linea'}{active?.counterpartRole ? ` · ${ROLE_LABEL[active.counterpartRole]}` : ''}
                  </small>
                </div>
              </div>

              {page?.lastSharedConnectionId && <About id={page.lastSharedConnectionId} />}

              <div className="messages">
                {page?.hasMore && <button className="btn btn--ghost btn--sm older" onClick={loadOlder}>Carica messaggi precedenti</button>}
                {rows.map((r) => (r.sep ? (
                  <p key={r.key} className="day-sep">{r.sep}</p>
                ) : (
                  <div key={r.key} className={`bubble ${r.m.sender === user.username ? 'bubble--mine' : ''}`}>
                    {r.m.connectionId && <div style={{ marginBottom: 8 }}><SharedCard id={r.m.connectionId} /></div>}
                    {r.m.text && <p>{r.m.text}</p>}
                    <small className={r.m.sender === user.username && r.m.status === 'READ' ? 'read' : ''}>
                      {clock(r.m.sentAt)} · {r.m.sender === user.username ? STATUS[r.m.status] : THEIRS[r.m.status]}
                      {r.m.sender === user.username && <Icon name={TICK[r.m.status]} className="tick" />}
                    </small>
                  </div>
                )))}
                {messages.length === 0 && page && (
                  <p className="small" style={{ textAlign: 'center', marginTop: 30 }}>Scrivi il primo messaggio a {activeName}.</p>
                )}
                <div ref={bottom} />
              </div>

              {typing && (
                <p className="typing">
                  <Avatar name={activeName} username={withParam} size="xs" />{' '}
                  <span className="typing__dots" aria-hidden="true"><i /><i /><i /></span>{' '}
                  {activeName.split(' ')[0]} sta scrivendo
                </p>
              )}

              {messages.length > 0 && (
                <div className="ai-next">
                  <div className="ai-next__head">
                    <span><Icon name="sparkles" /> Il prossimo messaggio · AI</span>
                    {suggestion.state === 'accepted' && <span className="ai-next__ok"><Icon name="check" /> Accettato</span>}
                  </div>
                  {suggestion.state === 'idle' && (
                    <button className="btn btn--ghost btn--sm" style={{ marginTop: 12 }} onClick={suggest} disabled={!page?.conversationId}>
                      <Icon name="sparkles" /> Suggeriscimi una risposta
                    </button>
                  )}
                  {suggestion.state === 'loading' && <p>Sto leggendo la conversazione…</p>}
                  {suggestion.state === 'error' && <p className="form-error">{suggestion.text}</p>}
                  {(suggestion.state === 'ready' || suggestion.state === 'accepted') && <p>{suggestion.text}</p>}
                  {suggestion.state === 'ready' && (
                    <button className="btn btn--ghost btn--sm" style={{ marginTop: 12 }} onClick={accept}>
                      <Icon name="check" /> Accetta il suggerimento
                    </button>
                  )}
                  {suggestion.state === 'accepted' && <small>Suggerimento inserito nella bozza. Rileggilo prima di inviare.</small>}
                </div>
              )}

              <form className="composer" onSubmit={send}>
                {pendingShare && (
                  <div style={{ marginBottom: 10 }}>
                    <SharedCard id={pendingShare} />
                    <button type="button" className="link-muted" onClick={() => setPendingShare(null)}>Rimuovi allegato</button>
                  </div>
                )}
                <label className="field">
                  <span className="field__label">{fromAi ? 'Messaggio · bozza da suggerimento AI' : 'Messaggio'}</span>
                  <textarea
                    rows={3}
                    value={draft}
                    maxLength={2000}
                    placeholder="Scrivi un messaggio…"
                    onChange={(e) => onType(e.target.value)}
                    onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) send(e); }}
                  />
                </label>
                {attach && <AttachPicker onPick={(id) => { setPendingShare(id); setAttach(false); }} onClose={() => setAttach(false)} />}
                {error && <p className="form-error" style={{ marginTop: 8 }}>{error}</p>}
                <div className="composer__row">
                  <button type="button" className="icon-btn" onClick={() => setAttach((v) => !v)} aria-label="Condividi una connessione">
                    <Icon name="clip" />
                  </button>
                  <button type="submit" className="btn btn--primary" disabled={(!draft.trim() && !pendingShare) || !connected}>
                    <Icon name="arrowUp" /> Invia
                  </button>
                </div>
              </form>
            </>
          )}
        </section>
      </div>
    </Layout>
  );
}

