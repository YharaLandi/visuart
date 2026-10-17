import { useEffect, useRef, useState } from 'react';
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
  const [text, setText] = useState('');
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const box = useRef(null);

  useEffect(() => {
    commentsApi.list(connectionId).then(setList).catch(() => setList([]));
  }, [connectionId]);

  useEffect(() => { onCountChange?.(list.length); }, [list.length, onCountChange]);

  const requireUser = () => {
    if (!user) navigate(`/accedi?next=${encodeURIComponent(`/connessioni/${connectionId}`)}`);
    return !!user;
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!requireUser() || !text.trim()) return;
    setBusy(true);
    setError(null);
    try {
      const created = await commentsApi.add(connectionId, text.trim());
      setList((prev) => [...prev, created]);
      setText('');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const reply = (c) => {
    if (!requireUser()) return;
    setText(`@${c.authorName} `);
    box.current?.focus();
  };

  const saveEdit = async (e) => {
    e.preventDefault();
    const updated = await commentsApi.edit(connectionId, editing.id, editing.text);
    setList((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
    setEditing(null);
  };

  const remove = async (c) => {
    if (!window.confirm('Eliminare questo commento?')) return;
    await commentsApi.remove(connectionId, c.id);
    setList((prev) => prev.filter((x) => x.id !== c.id));
  };

  const mine = (c) => user && c.authorUsername === user.username;
  const canDelete = (c) => mine(c) || user?.role === 'ADMIN';

  return (
    <section className="dialogues">
      <div className="dialogues__head">
        <h2>Dialoghi</h2>
        <span className="tiny">{n(list.length)} commenti</span>
      </div>

      {list.map((c) => (
        <article key={c.id} className="comment">
          <Avatar name={c.authorName} size="sm" />
          <div style={{ flex: 1 }}>
            <p className="comment__meta">
              <strong>{c.authorUsername ? <Link to={`/utenti/${c.authorUsername}`} className="author-link">{c.authorName}</Link> : c.authorName}</strong> <span>· {timeAgo(c.createdAt)}{c.editedAt ? ' · modificato' : ''}</span>
            </p>
            {editing?.id === c.id ? (
              <form onSubmit={saveEdit} style={{ marginTop: 8 }}>
                <label className="field">
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
          </div>
        </article>
      ))}

      <form className="comment-form" onSubmit={submit}>
        <label className="field">
          <span className="field__label">Aggiungi il tuo sguardo</span>
          <textarea
            ref={box}
            rows={3}
            placeholder="Quale dettaglio ti ha colpito?"
            value={text}
            onChange={(e) => setText(e.target.value)}
            onFocus={() => { if (!user) requireUser(); }}
          />
        </label>
        {error && <p className="form-error" style={{ marginTop: 8 }}>{error}</p>}
        <div className="comment-form__row">
          <button type="submit" className="btn btn--ghost btn--sm" disabled={busy || !text.trim()}>
            {busy ? 'Pubblico…' : 'Pubblica commento'}
          </button>
        </div>
      </form>
    </section>
  );
}
