import { Fragment, useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { notifications as notificationsApi, users as usersApi } from '../../api/client';
import { connectNotifications } from '../../api/stomp';
import { useApp } from '../../context/AppContext';
import { useAuth } from '../../context/AuthContext';
import { ROLE_LABEL, timeAgo } from '../../lib/format';
import { getTheme, setTheme } from '../../lib/theme';
import Icon from './Icon';
import { Avatar, Brand, Footer } from './Visual';
import { isSeasonOff, phantomOpen, seasonOf, setSeasonOff, useSeasonVersion } from '../../lib/season';

const LINKS = [
  ['/connessioni', 'Più viste', 'compass', 'Più viste'],
  ['/grafo', 'Grafo', 'graph', 'Grafo'],
  ['/collezione', 'La mia collezione', 'bookmark', 'Collezione'],
  ['/chat', 'Chat', 'chat', 'Chat'],
];

// voce di ottobre: compare nel menu solo mentre il reparto fantasma è aperto
const PHANTOM_LINK = ['/reparto-fantasma', 'Reparto fantasma'];
const CURIOSITY_LINK = ['/curiosita', 'Curiosità'];

/**
 * Durante la rassegna di Halloween: un clic per tornare alla versione normale del sito
 * (e per rientrare nella rassegna). La scelta vale fino alla rassegna dell'anno dopo.
 */
// due icone nello stesso tratto (1.6 px, 20 px), in stile con le icone della barra
const SVG = { width: 20, height: 20, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.6, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': true };

/** Pipistrello: accende la rassegna di Halloween. */
function BatIcon() {
  return (
    <svg {...SVG}>
      <path d="M12 8.5c-.9 0-1.4-.9-1.6-1.9-.6.6-.9 1.4-.9 2.3C7.8 7.6 5.4 7 2.5 7.4c1.3 1 2 2.4 2 4 1-.6 2.3-.7 3.4-.2-.4 1-.3 2.2.4 3.1.9-1.2 2.3-1.7 3.7-1.4v0c1.4-.3 2.8.2 3.7 1.4.7-.9.8-2.1.4-3.1 1.1-.5 2.4-.4 3.4.2 0-1.6.7-3 2-4-2.9-.4-5.3.2-7 1.5 0-.9-.3-1.7-.9-2.3-.2 1-.7 1.9-1.6 1.9Z" />
    </svg>
  );
}

/** Sole: torna alla versione normale del sito. */
function SunIcon() {
  return (
    <svg {...SVG}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4" />
    </svg>
  );
}

function SeasonSwitch() {
  const season = seasonOf();
  const [off, setOff] = useState(() => (season ? isSeasonOff(season) : false));
  if (season !== 'halloween') return null;
  const toggle = () => { setSeasonOff(season, !off); setOff(!off); };
  return (
    <button type="button" className="icon-btn season-switch" onClick={toggle}
      aria-pressed={!off} title={off ? 'Torna alla rassegna di Halloween' : 'Passa alla versione normale'}>
      {off ? <BatIcon /> : <SunIcon />}
      <span className="sr-only">{off ? 'Torna alla rassegna di Halloween' : 'Passa alla versione normale'}</span>
    </button>
  );
}

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
  const [theme, setThemeState] = useState(getTheme);
  const toggleTheme = () => {
    const next = theme === 'light' ? 'dark' : 'light';
    setTheme(next);
    setThemeState(next);
  };

  return (
    <div className="menu-wrap" ref={ref}>
      <button className="avatar-btn" onClick={() => setOpen((v) => !v)} aria-label="Menu account" aria-expanded={open}>
        <Avatar name={user.displayName} username={user.username} has={user.hasAvatar} version={user.avatarVersion} />
      </button>
      {open && (
        <div className="menu" role="menu">
          <p className="menu__head">{user.displayName} · {ROLE_LABEL[user.role] ?? user.role}</p>
          <button className="menu__item" onClick={() => go('/profilo')}>Il mio profilo</button>
          <button className="menu__item" onClick={() => go('/collezione')}>La mia collezione</button>
          <button className="menu__item" onClick={() => go('/impostazioni')}>Impostazioni</button>
          <button className="menu__item menu__item--switch" role="menuitemcheckbox" aria-checked={theme === 'light'} onClick={toggleTheme}>
            Tema carta avorio
            <span className="switch__track" aria-hidden="true">
              <span className="switch__thumb"><Icon name={theme === 'light' ? 'sun' : 'moon-stars'} /></span>
            </span>
          </button>
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
// nomi visualizzati di chi scrive, per non chiederli a ogni messaggio
const nameCache = new Map();
function displayNameOf(username) {
  if (!nameCache.has(username)) {
    nameCache.set(username, usersApi.profile(username).then((p) => p.displayName).catch(() => username));
  }
  return nameCache.get(username);
}

const TOAST_MS = 6000;

/** Avvisi a comparsa per i messaggi in arrivo; cliccando si apre la conversazione. */
function MessageToasts({ toasts, onOpen, onClose }) {
  if (toasts.length === 0) return null;
  return (
    <div className="toasts" role="status" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.id} className="toast">
          <button className="toast__body" onClick={() => onOpen(t)}>
            <span className="toast__icon"><Icon name="chat" /></span>
            <span>
              <strong>Nuovo messaggio da {t.name}</strong>
              <small>{t.text || 'Ti ha condiviso una connessione'}</small>
            </span>
          </button>
          <button className="toast__close" onClick={() => onClose(t.id)} aria-label="Chiudi avviso"><Icon name="close" /></button>
        </div>
      ))}
    </div>
  );
}

function Bell() {
  const { refreshChatUnread } = useApp();
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [items, setItems] = useState([]);
  const [toasts, setToasts] = useState([]);
  const pathRef = useRef(location.pathname);
  pathRef.current = location.pathname;
  const meRef = useRef(user?.username);
  meRef.current = user?.username;
  const [open, setOpen] = useState(false);
  const ref = useClickOutside(open, setOpen);
  const refreshRef = useRef(refreshChatUnread);
  refreshRef.current = refreshChatUnread;

  useEffect(() => {
    notificationsApi.list().then(setItems).catch(() => {});
    return connectNotifications(
      (n) => setItems((prev) => [n, ...prev].slice(0, 30)),
      () => refreshRef.current?.(),
      (m) => {
        // solo i messaggi ricevuti, e non se quella conversazione è già aperta
        if (m.recipient !== meRef.current || pathRef.current === `/chat/${m.sender}`) return;
        displayNameOf(m.sender).then((name) => {
          const toast = { id: m.id, sender: m.sender, name, text: m.text };
          setToasts((prev) => [...prev.filter((t) => t.sender !== m.sender), toast].slice(-3));
          setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== toast.id)), TOAST_MS);
        });
      }
    );
  }, []);

  const openToast = (t) => {
    setToasts((prev) => prev.filter((x) => x.sender !== t.sender));
    navigate(`/chat/${t.sender}`);
  };
  const closeToast = (id) => setToasts((prev) => prev.filter((t) => t.id !== id));

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
      <MessageToasts toasts={toasts} onOpen={openToast} onClose={closeToast} />
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

/** Sotto i 1080 px le voci della barra finiscono in un menu a tendina. */
function BurgerMenu({ links, chatUnread }) {
  const [open, setOpen] = useState(false);
  const ref = useClickOutside(open, setOpen);
  const location = useLocation();
  useEffect(() => { setOpen(false); }, [location.pathname]);
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open]);

  return (
    <div className="menu-wrap burger" ref={ref}>
      <button className="icon-btn burger__btn" onClick={() => setOpen((v) => !v)}
        aria-label={open ? 'Chiudi il menu' : 'Apri il menu'} aria-expanded={open} aria-controls="burger-menu">
        <Icon name={open ? 'close' : 'menu'} />
      </button>
      {open && (
        <nav id="burger-menu" className="menu burger__menu" aria-label="Sezioni">
          {links.map(([to, label]) => (
            <NavLink key={to} to={to} className={({ isActive }) => `menu__item burger__link ${isActive ? 'burger__link--active' : ''}`}>
              {label}
              {to === '/chat' && chatUnread > 0 && <span className="count">{chatUnread}</span>}
            </NavLink>
          ))}
          <NavLink to="/nuova-critica" className="menu__item burger__link burger__link--cta">
            <Icon name="plus" /> Nuova critica
          </NavLink>
        </nav>
      )}
    </div>
  );
}

export function NavBar({ minimal = false }) {
  useSeasonVersion();
  const { user } = useAuth();
  const { chatUnread } = useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const next = encodeURIComponent(location.pathname + location.search);
  const links = [...LINKS, CURIOSITY_LINK, ...(phantomOpen() ? [PHANTOM_LINK] : [])];

  return (
    <header className={`nav ${minimal ? 'nav--minimal' : ''}`}>
      <div className="container nav__inner">
        <Brand />
        {!minimal && (
          <nav className="nav__links" aria-label="Sezioni">
            {links.map(([to, label]) => (
              <NavLink key={to} to={to} className={({ isActive }) => `nav__link ${to === '/reparto-fantasma' ? 'nav__link--phantom' : ''} ${isActive ? 'nav__link--active' : ''}`}>
                {label}
                {to === '/chat' && chatUnread > 0 && <span className="count">{chatUnread}</span>}
              </NavLink>
            ))}
          </nav>
        )}
        <div className="nav__actions">
          {/* in home la ricerca è già nella biglietteria, sotto l'insegna */}
          {!minimal && location.pathname !== '/' && (
            <button className="icon-btn" onClick={() => navigate('/cerca')} aria-label="Cerca negli archivi">
              <Icon name="search" />
            </button>
          )}
          {!minimal && <SeasonSwitch />}
          {/* sempre attiva da loggati: porta anche gli avvisi dei messaggi; il pulsante si nasconde via CSS */}
          {user && <Bell />}
          {!minimal && (
            <Link to="/nuova-critica" className="btn btn--primary nav__cta">
              <Icon name="plus" /> Nuova critica
            </Link>
          )}
          {!minimal && <BurgerMenu links={links} chatUnread={chatUnread} />}
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
    <nav className="tabbar" aria-label="Sezioni">
      {LINKS.map(([to, , icon, short], i) => (
        <Fragment key={to}>
          {/* l'azione principale sta al centro della barra: non copre più i contenuti */}
          {i === 2 && (
            <NavLink to="/nuova-critica" className={({ isActive }) => `tabbar__new ${isActive ? 'on' : ''}`}>
              <Icon name="plus" />
              Scrivi
            </NavLink>
          )}
          <NavLink to={to} className={({ isActive }) => (isActive ? 'on' : '')}>
            <Icon name={icon} />
            {short}
          </NavLink>
        </Fragment>
      ))}
    </nav>
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
      <main className="shell__body">
        {children}
        {caption && <p className="shell__caption">{caption}</p>}
      </main>
    </div>
  );
}
