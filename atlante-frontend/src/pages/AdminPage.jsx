import { useCallback, useEffect, useState } from 'react';
import { admin as adminApi } from '../api/client';
import Layout from '../components/ui/Layout';
import { useAuth } from '../context/AuthContext';
import { ROLE_LABEL } from '../lib/format';
import Icon from '../components/ui/Icon';

export default function AdminPage() {
  const { user } = useAuth();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

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
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <Layout>
      <div className="container">
        <header className="page-head">
          <p className="eyebrow">Amministrazione</p>
          <h1 className="h-page">Gestione utenti</h1>
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
