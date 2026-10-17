import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { notifications as notificationsApi } from '../../api/client';
import { connectNotifications } from '../../api/stomp';
import { useApp } from '../../context/AppContext';
import { useAuth } from '../../context/AuthContext';
import { ROLE_LABEL, timeAgo } from '../../lib/format';
import Icon from './Icon';
import { Brand, Footer } from './Visual';

const LINKS = [
  ['/connessioni', 'Più viste', 'compass', 'Più viste'],
  ['/grafo', 'Grafo', 'graph', 'Grafo'],
  ['/collezione', 'La mia collezione', 'bookmark', 'Collezione'],
  ['/chat', 'Chat', 'chat', 'Chat'],
];

function useClickOutside(open, setOpen) {
  const ref = useRef(null);
  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onDown);
    return () => document.removeEventListener('mousedown', onDown);
  }, [open, setOpen]);
  return ref;
}

function AvatarMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useClickOutside(open, setOpen);
  const go = (to) => { setOpen(false); navigate(to); };

  return (
    <div className="menu-wrap" ref={ref}>
      <button className="avatar" onClick={() => setOpen((v) => !v)} aria-label="Menu account" aria-expanded={open}>
        {user.displayName?.[0]?.toUpperCase() ?? '?'}
      </button>
      {open && (
        <div className="menu" role="menu">
          <p className="menu__head">{user.displayName} · {ROLE_LABEL[user.role] ?? user.role}</p>
          <button className="menu__item" onClick={() => go('/profilo')}>Il mio profilo</button>
          <button className="menu__item" onClick={() => go('/collezione')}>La mia collezione</button>
          <button className="menu__item" onClick={() => go('/impostazioni')}>Impostazioni</button>
          {user.role === 'ADMIN' && (
            <button className="menu__item" onClick={() => go('/admin')}>Gestione utenti</button>
          )}
          <button className="menu__item" onClick={() => { setOpen(false); logout().then(() => navigate('/')); }}>Esci</button>
        </div>
      )}
    </div>
  );
}

function describe(n) {
  return n.type === 'LIKE'
    ? `${n.actor} ha messo mi piace alla tua connessione`
    : `${n.actor} ha commentato: «${n.preview ?? ''}»`;
}

/** Notifiche di like e commenti; la stessa sessione WebSocket aggiorna il badge della chat. */
function Bell() {
  const { refreshChatUnread } = useApp();
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [open, setOpen] = useState(false);
  const ref = useClickOutside(open, setOpen);
  const refreshRef = useRef(refreshChatUnread);
  refreshRef.current = refreshChatUnread;

  useEffect(() => {
    notificationsApi.list().then(setItems).catch(() => {});
    return connectNotifications(
      (n) => setItems((prev) => [n, ...prev].slice(0, 30)),
      () => refreshRef.current?.()
    );
  }, []);

  const unread = items.filter((n) => !n.read).length;
  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next && unread > 0) {
      notificationsApi.readAll().catch(() => {});
      setTimeout(() => setItems((prev) => prev.map((n) => ({ ...n, read: true }))), 1500);
    }
  };

  return (
    <div className="menu-wrap" ref={ref}>
      <button className="icon-btn bell" onClick={toggle} aria-label="Notifiche">
        <Icon name="bell" />
        {unread > 0 && <span className="bell__count">{unread > 9 ? '9+' : unread}</span>}
      </button>
      {open && (
        <div className="menu notif-menu">
          <p className="menu__head">Notifiche</p>
          {items.length === 0 && <p className="small">Ancora niente: i mi piace e i commenti arriveranno qui.</p>}
          {items.map((n) => (
            <button
              key={n.id}
              className={`notif ${n.read ? '' : 'notif--new'}`}
              onClick={() => { setOpen(false); if (n.connectionId) navigate(`/connessioni/${n.connectionId}`); }}
            >
              <span>{describe(n)}</span>
              <small>{timeAgo(n.createdAt)}</small>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export function NavBar({ minimal = false }) {
  const { user } = useAuth();
  const { chatUnread } = useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const next = encodeURIComponent(location.pathname + location.search);

  return (
    <header className="nav">
      <div className="container nav__inner">
        <Brand />
        {!minimal && (
          <nav className="nav__links" aria-label="Sezioni">
            {LINKS.map(([to, label]) => (
              <NavLink key={to} to={to} className={({ isActive }) => `nav__link ${isActive ? 'nav__link--active' : ''}`}>
                {label}
                {to === '/chat' && chatUnread > 0 && <span className="count">{chatUnread}</span>}
              </NavLink>
            ))}
          </nav>
        )}
        <div className="nav__actions">
          {!minimal && (
            <button className="icon-btn" onClick={() => navigate('/cerca')} aria-label="Cerca negli archivi">
              <Icon name="search" />
            </button>
          )}
          {!minimal && user && <span className="nav__bell-desktop"><Bell /></span>}
          {!minimal && (
            <Link to="/nuova-critica" className="btn btn--primary nav__cta">
              <Icon name="plus" /> Nuova critica
            </Link>
          )}
          {user ? <AvatarMenu /> : (
            <Link to={`/accedi?next=${next}`} className="avatar" aria-label="Accedi">
              <Icon name="user" size={18} />
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}

function TabBar() {
  return (
    <>
      <Link to="/nuova-critica" className="btn btn--primary fab"><Icon name="plus" /> Nuova critica</Link>
      <nav className="tabbar" aria-label="Sezioni">
        {LINKS.map(([to, , icon, short]) => (
          <NavLink key={to} to={to} className={({ isActive }) => (isActive ? 'on' : '')}>
            <Icon name={icon} />
            {short}
          </NavLink>
        ))}
      </nav>
    </>
  );
}

/** Pagina standard: navbar, contenuto, footer e (su mobile) barra in basso. */
export default function Layout({ children, minimalNav = false, footer = true, tabs = true }) {
  return (
    <div className="page">
      <NavBar minimal={minimalNav} />
      <main className="page__main">{children}</main>
      {footer && <Footer />}
      {tabs && <TabBar />}
    </div>
  );
}

/** Pagina "a finestra" (accesso, registrazione, nuova critica): logo attenuato e testo a destra. */
export function Shell({ rightText, children, caption }) {
  return (
    <div className="shell">
      <header className="search-top container">
        <Brand dim />
        {rightText && <span className="nav__right-text">{rightText}</span>}
      </header>
      <div className="shell__body">
        {children}
        {caption && <p className="shell__caption">{caption}</p>}
      </div>
    </div>
  );
}
