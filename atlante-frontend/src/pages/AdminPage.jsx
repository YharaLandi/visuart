import { useCallback, useEffect, useState } from 'react';
import { admin as adminApi } from '../api/client';
import Layout from '../components/ui/Layout';
import { useAuth } from '../context/AuthContext';
import { ROLE_LABEL } from '../lib/format';
import Icon from '../components/ui/Icon';
import { PageSign } from '../components/Marquee';

export default function AdminPage() {
  const { user } = useAuth();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [roles, setRoles] = useState([]);
  const [draft, setDraft] = useState({ name: '', description: '' });
  const [notice, setNotice] = useState(null);

  const loadRoles = useCallback(() => {
    adminApi.roles().then(setRoles).catch((e) => setError(e.message));
  }, []);
  useEffect(loadRoles, [loadRoles]);

  const load = useCallback(() => {
    adminApi.users(q, page).then(setData).catch((e) => setError(e.message));
  }, [q, page]);

  useEffect(() => {
    const timer = setTimeout(load, 250);
    return () => clearTimeout(timer);
  }, [load]);

  const act = async (action) => {
    setError(null);
    try {
      await action();
      load();
      loadRoles();
    } catch (e) {
      setError(e.message);
    }
  };

  const toggleRole = (u, name) => {
    const current = u.roles ?? [u.role];
    const next = current.includes(name) ? current.filter((r) => r !== name) : [...current, name];
    act(() => adminApi.assignRoles(u.id, next));
  };
  const custom = roles.filter((r) => !r.system);

  const createRole = (e) => {
    e.preventDefault();
    act(async () => {
      await adminApi.createRole(draft.name, draft.description);
      setDraft({ name: '', description: '' });
    });
  };

  return (
    <Layout>
      <div className="container">
        <header className="page-head">
          <PageSign text="Gestione utenti" />
          <p className="lead">Assegna i ruoli e modera l’archivio. Cambiare ruolo chiude le sessioni aperte dell’utente.</p>
        </header>

        <label className="field" style={{ marginTop: 28, maxWidth: 460 }}>
          <span className="field__label">Cerca nell’archivio utenti</span>
          <input type="search" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} placeholder="Username o email" />
        </label>
        {error && <p className="form-error" style={{ marginTop: 12 }}>{error}</p>}

        <div className="card" style={{ marginTop: 24, padding: '6px 18px 18px', overflowX: 'auto' }}>
          <table className="admin-table">
            <thead>
              <tr><th>Utente</th><th>Email</th><th>Ruolo</th><th /></tr>
            </thead>
            <tbody>
              {data?.content.map((u) => {
                const self = u.username === user.username;
                return (
                  <tr key={u.id}>
                    <td><strong style={{ fontWeight: 600 }}>{u.displayName}</strong><br /><span className="tiny">@{u.username}</span></td>
                    <td className="small">{u.email}</td>
                    <td>
                      <select
                        value={u.role}
                        disabled={self}
                        onChange={(e) => act(() => adminApi.changeRole(u.id, e.target.value))}
                        aria-label={`Ruolo di ${u.displayName}`}
                      >
                        {Object.entries(ROLE_LABEL).map(([v, label]) => <option key={v} value={v}>{label}</option>)}
                      </select>
                      {custom.length > 0 && (
                        <div className="chips" style={{ marginTop: 6 }} role="group" aria-label={`Altri ruoli di ${u.displayName}`}>
                          {custom.map((r) => (
                            <button key={r.id} type="button" className="chip" aria-pressed={(u.roles ?? []).includes(r.name)}
                              onClick={() => toggleRole(u, r.name)}>{r.name}</button>
                          ))}
                        </div>
                      )}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn--ghost btn--sm btn--danger"
                        disabled={self}
                        onClick={() => window.confirm(`Eliminare ${u.displayName} con critiche, like, commenti e chat?`) && act(() => adminApi.remove(u.id))}
                      >
                        Elimina
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <section style={{ marginTop: 40 }}>
          <h2 className="field__label">Newsletter</h2>
          <p className="small">Parte da sola ogni lunedì alle 9 per chi è iscritto. Qui puoi vederla o spedirla subito.</p>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center', marginTop: 12 }}>
            <button className="btn btn--ghost btn--sm" onClick={() => act(async () => {
              const html = await adminApi.newsletterPreview();
              window.open(URL.createObjectURL(new Blob([html], { type: 'text/html' })), '_blank', 'noopener');
            })}>Anteprima</button>
            <button className="btn btn--sm" onClick={() => window.confirm('Spedire adesso la newsletter a tutti gli iscritti?') && act(async () => {
              const r = await adminApi.newsletterSend();
              setNotice(`Newsletter spedita a ${r.recipients} iscritti.`);
            })}>Spedisci ora</button>
            {notice && <span className="small" role="status">{notice}</span>}
          </div>
        </section>

        <section style={{ marginTop: 40 }}>
          <h2 className="field__label">Ruoli</h2>
          <p className="small">I ruoli di sistema non si eliminano. Quelli creati qui si assegnano dalla tabella utenti.</p>
          <div className="card" style={{ marginTop: 12, padding: '6px 18px 18px', overflowX: 'auto' }}>
            <table className="admin-table">
              <thead><tr><th>Nome</th><th>Descrizione</th><th>Utenti</th><th /></tr></thead>
              <tbody>
                {roles.map((r) => (
                  <tr key={r.id}>
                    <td><strong style={{ fontWeight: 600 }}>{ROLE_LABEL[r.name] ?? r.name}</strong>{r.system && <><br /><span className="tiny">di sistema</span></>}</td>
                    <td className="small">{r.description}</td>
                    <td className="small">{r.users}</td>
                    <td style={{ textAlign: 'right' }}>
                      {!r.system && (
                        <button className="btn btn--ghost btn--sm btn--danger"
                          onClick={() => window.confirm(`Eliminare il ruolo ${r.name}? Chi lo ha lo perde.`) && act(() => adminApi.deleteRole(r.id))}>
                          Elimina
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <form onSubmit={createRole} style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end', marginTop: 16 }}>
            <label className="field" style={{ flex: '1 1 180px' }}>
              <span className="field__label">Nuovo ruolo</span>
              <input value={draft.name} maxLength={30} required placeholder="MODERATORE"
                onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
            </label>
            <label className="field" style={{ flex: '2 1 260px' }}>
              <span className="field__label">Descrizione</span>
              <input value={draft.description} maxLength={200}
                onChange={(e) => setDraft({ ...draft, description: e.target.value })} />
            </label>
            <button className="btn btn--sm" type="submit">Crea ruolo</button>
          </form>
        </section>

        {data && data.totalPages > 1 && (
          <div className="pager">
            <button className="chip" disabled={page === 0} onClick={() => setPage(page - 1)} aria-label="Pagina precedente"><Icon name="chevronLeft" /></button>
            <span>Pagina {page + 1} di {data.totalPages} · {data.totalElements} utenti</span>
            <button className="chip" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} aria-label="Pagina successiva"><Icon name="chevronRight" /></button>
          </div>
        )}
      </div>
    </Layout>
  );
}
