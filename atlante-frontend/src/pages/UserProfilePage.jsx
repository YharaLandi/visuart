import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { connections as connectionsApi, users as usersApi } from '../api/client';
import ConnectionCard from '../components/ConnectionCard';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { ErrorState, Loading } from '../components/ui/States';
import { Avatar } from '../components/ui/Visual';
import { useAuth } from '../context/AuthContext';
import { monthYear, n, ROLE_LABEL } from '../lib/format';

const PAGE_SIZE = 6;

/** Profilo pubblico di un'altra persona: scheda, numeri e critiche pubblicate. */
export default function UserProfilePage() {
  const { username } = useParams();
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [status, setStatus] = useState('loading');
  const [list, setList] = useState([]);
  const [info, setInfo] = useState({ page: 0, totalPages: 0 });

  const load = useCallback(() => {
    setStatus('loading');
    Promise.all([
      usersApi.profile(username),
      connectionsApi.search({ author: username, sort: 'recent', size: PAGE_SIZE }),
    ])
      .then(([p, page]) => {
        setProfile(p);
        setList(page.content);
        setInfo({ page: 0, totalPages: page.totalPages });
        setStatus('ok');
      })
      .catch((e) => setStatus(e.status === 404 ? 'missing' : 'error'));
  }, [username]);

  useEffect(() => { load(); }, [load]);

  const more = () => {
    const next = info.page + 1;
    connectionsApi.search({ author: username, sort: 'recent', page: next, size: PAGE_SIZE }).then((page) => {
      setList((prev) => [...prev, ...page.content]);
      setInfo({ page: next, totalPages: page.totalPages });
    });
  };

  // il proprio profilo ha già la sua pagina
  if (user && user.username === username) return <Navigate to="/profilo" replace />;

  if (status === 'loading') return <Layout><div className="container"><Loading title="Apriamo il profilo…" /></div></Layout>;
  if (status === 'error') return <Layout><div className="container"><ErrorState onRetry={load} /></div></Layout>;
  if (status === 'missing') {
    return (
      <Layout>
        <div className="container state">
          <h3>Questa persona non è nell’archivio.</h3>
          <p>Forse ha eliminato il suo account.</p>
          <Link to="/connessioni" className="btn btn--ghost btn--sm">Torna alle connessioni</Link>
        </div>
      </Layout>
    );
  }

  const chatLink = user ? `/chat/${username}` : `/accedi?next=${encodeURIComponent(`/chat/${username}`)}`;

  return (
    <Layout>
      <div className="container">
        <header className="page-head" style={{ paddingTop: 40 }}>
          <p className="eyebrow">Uno sguardo nell’archivio</p>
          <h1 className="h-page">{profile.displayName}</h1>
        </header>

        <div className="profile">
          <section className="card profile-card">
            <Avatar name={profile.displayName} size="lg" />
            <h2>{profile.displayName}</h2>
            <p className="profile-card__email">@{profile.username}</p>
            <span className="badge">{ROLE_LABEL[profile.role] ?? profile.role}</span>
            {profile.bio && <p className="profile-card__bio">«{profile.bio}»</p>}
            {profile.createdAt && <p className="profile-card__since">Nell’archivio da {monthYear(profile.createdAt)}</p>}
            <Link to={chatLink} className="btn btn--primary btn--sm"><Icon name="chat" /> Scrivi a {profile.displayName.split(' ')[0]}</Link>
          </section>

          <div>
            <div className="stats">
              <div className="stat">
                <strong className="t-art">{n(profile.connections)}</strong>
                <span className="t-art">Critiche scritte</span>
              </div>
              <div className="stat">
                <strong className="t-film">{n(profile.likesReceived)}</strong>
                <span className="t-film">Mi piace ricevuti</span>
              </div>
            </div>
            <p className="role-line">{n(profile.comments)} commenti lasciati nei dialoghi dell’archivio.</p>
          </div>
        </div>

        <div className="results-head" style={{ marginTop: 56 }}>
          <h2>Le sue connessioni</h2>
          <span>{n(profile.connections)} CRITICHE</span>
        </div>
        {list.length === 0 ? (
          <div className="state"><h3>Nessuna critica, per ora.</h3><p>{profile.displayName} non ha ancora pubblicato connessioni.</p></div>
        ) : (
          <div className="cards-3">{list.map((c) => <ConnectionCard key={c.id} conn={c} />)}</div>
        )}
        {info.page + 1 < info.totalPages && (
          <div className="end-note"><button className="btn btn--ghost" onClick={more}>Mostra altre critiche</button></div>
        )}
      </div>
    </Layout>
  );
}
