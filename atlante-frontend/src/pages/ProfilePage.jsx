import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { account as accountApi, connections as connectionsApi } from '../api/client';
import ConnectionCard from '../components/ConnectionCard';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { Avatar } from '../components/ui/Visual';
import { useAuth } from '../context/AuthContext';
import { monthYear, n, ROLE_DESCRIPTION, ROLE_LABEL } from '../lib/format';

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState(null);
  const [mine, setMine] = useState([]);

  useEffect(() => {
    accountApi.stats().then(setStats).catch(() => setStats(null));
    connectionsApi.search({ author: user.username, sort: 'recent', size: 50 })
      .then((p) => setMine(p.content)).catch(() => setMine([]));
  }, [user.username]);

  return (
    <Layout>
      <div className="container">
        <header className="page-head" style={{ paddingTop: 40 }}>
          <p className="eyebrow">Il tuo spazio nell’archivio</p>
          <h1 className="h-page">Il mio profilo</h1>
        </header>

        <div className="profile">
          <section className="card profile-card">
            <Avatar name={user.displayName} username={user.username} has={user.hasAvatar} version={user.avatarVersion} size="lg" />
            <h2>{user.displayName}</h2>
            <p className="profile-card__email">{user.email}</p>
            <span className="badge">{ROLE_LABEL[user.role] ?? user.role}</span>
            {user.bio ? (
              <p className="profile-card__bio">«{user.bio}»</p>
            ) : (
              <Link to="/impostazioni" className="profile-card__bio" style={{ display: 'block', color: 'var(--muted)' }}>
                Aggiungi una frase che racconti il tuo sguardo.
              </Link>
            )}
            {user.createdAt && <p className="profile-card__since">Nell’archivio da {monthYear(user.createdAt)}</p>}
            <div className="settings__row" style={{ marginTop: 18 }}>
              <button className="btn btn--ghost btn--sm" style={{ marginTop: 0 }} onClick={() => logout().then(() => navigate('/'))}>
                <Icon name="logout" /> Esci
              </button>
              <Link to="/impostazioni" className="btn btn--ghost btn--sm" style={{ marginTop: 0 }}>
                <Icon name="gear" /> Impostazioni
              </Link>
            </div>
          </section>

          <div>
            <div className="stats">
              <Link to="/collezione" className="stat">
                <strong className="t-film">{stats ? n(stats.favorites) : '—'}</strong>
                <span className="t-film">Opere salvate</span>
              </Link>
              <div className="stat">
                <strong className="t-art">{stats ? n(stats.connections) : '—'}</strong>
                <span className="t-art">Critiche scritte</span>
              </div>
            </div>
            <p className="role-line">{ROLE_LABEL[user.role]} · {ROLE_DESCRIPTION[user.role]}</p>
            {user.role === 'ADMIN' && (
              <Link to="/admin" className="btn btn--ghost btn--sm" style={{ marginTop: 16 }}>
                <Icon name="people" /> Gestione utenti e ruoli
              </Link>
            )}
          </div>
        </div>

        <div className="results-head" style={{ marginTop: 56 }}>
          <h2>Le mie critiche</h2>
          <span>{n(mine.length)} PUBBLICATE</span>
        </div>
        {mine.length === 0 ? (
          <div className="state">
            <h3>Non hai ancora pubblicato critiche.</h3>
            <p>Accosta due opere e racconta perché si parlano.</p>
            <Link to="/nuova-critica" className="btn btn--primary btn--sm"><Icon name="plus" /> Nuova critica</Link>
          </div>
        ) : (
          <div className="cards-3">
            {mine.map((c) => (
              <div key={c.id} className="my-critique">
                <ConnectionCard conn={c} />
                <div className="my-critique__actions">
                  <Link to={`/connessioni/${c.id}/modifica`} className="link"><Icon name="pencil" /> Modifica</Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}
