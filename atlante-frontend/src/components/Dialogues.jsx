import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { comments as commentsApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { n, timeAgo } from '../lib/format';
import { Avatar } from './ui/Visual';

/** "Dialoghi": commenti alla connessione, con risposta, modifica ed eliminazione. */
export default function Dialogues({ connectionId, onCountChange }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [list, setList] = useState([]);
  const [loadState, setLoadState] = useState('loading');
  const [text, setText] = useState('');
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [rowError, setRowError] = useState(null);
  const box = useRef(null);
  const loginPath = `/accedi?next=${encodeURIComponent(`/connessioni/${connectionId}`)}`;

  const load = useCallback(() => {
    setLoadState('loading');
    commentsApi.list(connectionId)
      .then((l) => { setList(l); setLoadState('ok'); })
      .catch(() => setLoadState('error'));
  }, [connectionId]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => { onCountChange?.(loadState === 'ok' ? list.length : null); }, [list.length, loadState, onCountChange]);

  const submit = async (e) => {
    e.preventDefault();
    if (!text.trim()) return;
    setBusy(true);
    setError(null);
    try {
      const created = await commentsApi.add(connectionId, text.trim());
      setList((prev) => [...prev, created]);
      setText('');
    } catch {
      setError('Il commento non è stato pubblicato. Il testo è ancora qui: riprova tra poco.');
    } finally {
      setBusy(false);
    }
  };

  const reply = (c) => {
    if (!user) return navigate(loginPath);
    setText(`@${c.authorName} `);
    box.current?.focus();
  };

  const saveEdit = async (e) => {
    e.preventDefault();
    setRowError(null);
    try {
      const updated = await commentsApi.edit(connectionId, editing.id, editing.text);
      setList((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
      setEditing(null);
    } catch {
      setRowError({ id: editing.id, text: 'Modifica non salvata. Riprova.' });
    }
  };

  const remove = async (c) => {
    if (!window.confirm('Eliminare questo commento?')) return;
    setRowError(null);
    try {
      await commentsApi.remove(connectionId, c.id);
      setList((prev) => prev.filter((x) => x.id !== c.id));
    } catch {
      setRowError({ id: c.id, text: 'Il commento non è stato eliminato. Riprova.' });
    }
  };

  const mine = (c) => user && c.authorUsername === user.username;
  const canDelete = (c) => mine(c) || user?.role === 'ADMIN';

  return (
    <section className="dialogues" id="dialoghi">
      <div className="dialogues__head">
        <h2>Dialoghi</h2>
        {loadState === 'ok' && <span className="tiny">{n(list.length)} commenti</span>}
      </div>

      {loadState === 'error' && (
        <div className="dialogues__notice" role="alert">
          <p>Non riusciamo a caricare i dialoghi.</p>
          <button type="button" className="btn btn--ghost btn--sm" onClick={load}>Riprova</button>
        </div>
      )}

      {loadState === 'ok' && list.length === 0 && (
        <p className="dialogues__empty">Nessun dialogo ancora. Il primo sguardo può essere il tuo.</p>
      )}

      {list.map((c) => (
        <article key={c.id} className="comment">
          <Avatar name={c.authorName} username={c.authorUsername} size="sm" />
          <div style={{ flex: 1 }}>
            <p className="comment__meta">
              <strong>{c.authorUsername ? <Link to={`/utenti/${c.authorUsername}`} className="author-link">{c.authorName}</Link> : c.authorName}</strong> <span>· {timeAgo(c.createdAt)}{c.editedAt ? ' · modificato' : ''}</span>
            </p>
            {editing?.id === c.id ? (
              <form onSubmit={saveEdit} style={{ marginTop: 8 }}>
                <label className="field">
                  <span className="sr-only">Modifica il commento</span>
                  <textarea rows={2} value={editing.text} onChange={(e) => setEditing({ ...editing, text: e.target.value })} autoFocus />
                </label>
                <div className="comment__actions">
                  <button type="submit" className="reply" disabled={!editing.text.trim()}>Salva</button>
                  <button type="button" onClick={() => setEditing(null)}>Annulla</button>
                </div>
              </form>
            ) : (
              <>
                <p className="comment__text">{c.text}</p>
                <div className="comment__actions">
                  <button className="reply" onClick={() => reply(c)}>Rispondi</button>
                  {mine(c) && <button onClick={() => setEditing({ id: c.id, text: c.text })}>Modifica</button>}
                  {canDelete(c) && <button onClick={() => remove(c)}>Elimina</button>}
                </div>
              </>
            )}
            {rowError?.id === c.id && <p className="form-error" role="alert">{rowError.text}</p>}
          </div>
        </article>
      ))}

      {user ? (
        <form className="comment-form" onSubmit={submit}>
          <label className="field">
            <span className="field__label">Aggiungi il tuo sguardo</span>
            <textarea
              ref={box}
              rows={3}
              placeholder="Quale dettaglio ti ha colpito?"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
          </label>
          {error && <p className="form-error" role="alert" style={{ marginTop: 8 }}>{error}</p>}
          <div className="comment-form__row">
            <button type="submit" className="btn btn--ghost btn--sm" disabled={busy || !text.trim()}>
              {busy ? 'Pubblico…' : 'Pubblica commento'}
            </button>
          </div>
        </form>
      ) : (
        <div className="comment-gate">
          <p>Per aggiungere il tuo sguardo serve un account.</p>
          <Link to={loginPath} className="btn btn--ghost btn--sm">Accedi per commentare</Link>
        </div>
      )}
    </section>
  );
}
