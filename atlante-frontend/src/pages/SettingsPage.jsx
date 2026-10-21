import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { account as accountApi } from '../api/client';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { Avatar } from '../components/ui/Visual';
import { useAuth } from '../context/AuthContext';
import { getTheme, setTheme } from '../lib/theme';

const PASSWORD_RULE = /^(?=.*[A-Z])(?=.*\d).{12,}$/;

function Section({ id, icon, title, intro, danger = false, children }) {
  return (
    <section id={id} className={`card settings-section ${danger ? 'settings-section--danger' : ''}`}>
      <h2><Icon name={icon} /> {title}</h2>
      {intro && <p className="settings-section__intro">{intro}</p>}
      {children}
    </section>
  );
}

/** Impostazioni account: profilo, sicurezza, riepilogo via mail, eliminazione. */
export default function SettingsPage() {
  const { user, persist, saveUser, clear } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState({ displayName: user.displayName ?? '', bio: user.bio ?? '' });
  const [pwd, setPwd] = useState({ current: '', next: '' });
  const [deletePwd, setDeletePwd] = useState('');
  const [msg, setMsg] = useState({});
  const [busy, setBusy] = useState(null);
  const [theme, setThemeState] = useState(getTheme);
  const toggleTheme = () => {
    const next = theme === 'light' ? 'dark' : 'light';
    setTheme(next);
    setThemeState(next);
  };

  const run = async (key, action, ok) => {
    setBusy(key);
    setMsg((m) => ({ ...m, [key]: null }));
    try {
      const text = await action();
      setMsg((m) => ({ ...m, [key]: { ok: true, text: text ?? ok } }));
    } catch (e) {
      setMsg((m) => ({ ...m, [key]: { ok: false, text: e.message } }));
    } finally {
      setBusy(null);
    }
  };

  const feedback = (k) => msg[k] && <p className={msg[k].ok ? 'form-ok' : 'form-error'}>{msg[k].text}</p>;

  const saveProfile = (e) => {
    e.preventDefault();
    run('profile', async () => {
      const me = await accountApi.update(profile.displayName, profile.bio);
      saveUser({ ...user, displayName: me.displayName, bio: me.bio });
    }, 'Profilo aggiornato');
  };

  const applyAvatar = (me) => saveUser({ ...user, hasAvatar: me.hasAvatar, avatarVersion: Date.now() });

  const uploadAvatar = (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      setMsg((m) => ({ ...m, avatar: { ok: false, text: 'La foto supera i 2 MB: scegline una più leggera.' } }));
      return;
    }
    run('avatar', async () => applyAvatar(await accountApi.uploadAvatar(file)), 'Foto aggiornata');
  };

  const removeAvatar = () => {
    run('avatar', async () => applyAvatar(await accountApi.removeAvatar()), 'Foto rimossa: torna la tua iniziale');
  };

  const changePassword = (e) => {
    e.preventDefault();
    if (!PASSWORD_RULE.test(pwd.next)) {
      setMsg((m) => ({ ...m, password: { ok: false, text: 'Almeno 12 caratteri, una maiuscola e un numero.' } }));
      return;
    }
    run('password', async () => {
      persist(await accountApi.changePassword(pwd.current, pwd.next));
      setPwd({ current: '', next: '' });
    }, 'Password cambiata: gli altri dispositivi sono stati disconnessi.');
  };

  const deleteAccount = (e) => {
    e.preventDefault();
    if (!window.confirm('Eliminare definitivamente account, critiche, like, commenti e chat?')) return;
    run('delete', async () => {
      await accountApi.remove(deletePwd);
      clear();
      navigate('/');
    });
  };

  return (
    <Layout>
      <div className="container settings-page">
        <header className="page-head" style={{ paddingTop: 40 }}>
          <p className="eyebrow">Il tuo spazio nell’archivio</p>
          <h1 className="h-page">Impostazioni</h1>
          <p className="lead">Come ti presenti nell’archivio, come proteggi il tuo account, cosa ricevi per mail.</p>
        </header>

        <div className="settings-layout">
          <nav className="settings-nav" aria-label="Sezioni impostazioni">
            <a href="#profilo"><Icon name="user" /> Profilo</a>
            <a href="#sicurezza"><Icon name="shield" /> Sicurezza</a>
            <a href="#aspetto"><Icon name="circle-half" /> Aspetto</a>
            <a href="#notifiche"><Icon name="envelope" /> Mail</a>
            <a href="#account" className="danger"><Icon name="trash" /> Elimina account</a>
          </nav>

          <div className="settings-stack">
            <Section id="profilo" icon="user" title="Profilo" intro="Foto, nome e frase compaiono sulle tue critiche, nei dialoghi e nella tua scheda.">
              <div className="avatar-upload">
                <Avatar name={user.displayName} username={user.username} has={user.hasAvatar} version={user.avatarVersion} size="lg" />
                <div className="avatar-upload__actions">
                  <label className="btn btn--ghost btn--sm">
                    <Icon name="upload" /> {user.hasAvatar ? 'Cambia foto' : 'Carica una foto'}
                    <input type="file" accept="image/jpeg,image/png,image/webp" className="sr-only" onChange={uploadAvatar} disabled={busy === 'avatar'} />
                  </label>
                  {user.hasAvatar && (
                    <button type="button" className="btn btn--ghost btn--sm" onClick={removeAvatar} disabled={busy === 'avatar'}>
                      <Icon name="trash" /> Rimuovi
                    </button>
                  )}
                </div>
                <p className="avatar-upload__hint">JPG, PNG o WebP fino a 2 MB. Senza foto resta la tua iniziale.</p>
                {feedback('avatar')}
              </div>
              <form onSubmit={saveProfile} className="settings-form">
                <label className="field">
                  <span className="field__label">Nome e cognome</span>
                  <input value={profile.displayName} onChange={(e) => setProfile({ ...profile, displayName: e.target.value })} maxLength={120} required />
                </label>
                <label className="field">
                  <span className="field__label">La tua frase</span>
                  <input value={profile.bio} onChange={(e) => setProfile({ ...profile, bio: e.target.value })} maxLength={300} placeholder="Cerco le immagini che continuano oltre la loro cornice." />
                </label>
                <p className="hint" style={{ marginTop: 0 }}>Email: {user.email} · username: @{user.username}</p>
                <div className="settings__row">
                  <button className="btn btn--primary btn--sm" disabled={busy === 'profile'}>Salva il profilo</button>
                  {feedback('profile')}
                </div>
              </form>
            </Section>

            <Section id="sicurezza" icon="shield" title="Sicurezza" intro="Cambiando password chiudi le sessioni aperte sugli altri dispositivi.">
              <form onSubmit={changePassword} className="settings-form">
                <label className="field">
                  <span className="field__label">Password attuale</span>
                  <input type="password" value={pwd.current} onChange={(e) => setPwd({ ...pwd, current: e.target.value })} required autoComplete="current-password" />
                </label>
                <label className="field">
                  <span className="field__label">Nuova password</span>
                  <input type="password" value={pwd.next} onChange={(e) => setPwd({ ...pwd, next: e.target.value })} required autoComplete="new-password" />
                </label>
                <p className="hint" style={{ marginTop: 0 }}>Almeno 12 caratteri, una maiuscola e un numero.</p>
                <div className="settings__row">
                  <button className="btn btn--ghost btn--sm" disabled={busy === 'password'}>Cambia password</button>
                  {feedback('password')}
                </div>
              </form>
            </Section>

            <Section id="aspetto" icon="circle-half" title="Aspetto" intro="Scegli tra la galleria scura e la carta avorio. La scelta resta su questo dispositivo.">
              <div className="settings__row" style={{ marginTop: 20 }}>
                <button type="button" role="switch" aria-checked={theme === 'light'} className="switch" onClick={toggleTheme}>
                  <span className="switch__track">
                    <span className="switch__thumb"><Icon name={theme === 'light' ? 'sun' : 'moon-stars'} /></span>
                  </span>
                  Tema carta avorio
                </button>
              </div>
              <div className="theme-options" aria-hidden="true">
                <span className="theme-swatch"><i style={{ background: '#0b0d10' }} /> Galleria scura</span>
                <span className="theme-swatch"><i style={{ background: '#f6f1e7' }} /> Carta avorio</span>
              </div>
            </Section>

            <Section id="notifiche" icon="envelope" title="Mail" intro="Ricevi un riepilogo con critiche, mi piace, commenti, opere salvate e messaggi.">
              <div className="settings__row">
                <button
                  className="btn btn--ghost btn--sm"
                  disabled={busy === 'email'}
                  onClick={() => run('email', async () => (await accountApi.emailStats()).message)}
                >
                  <Icon name="envelope" /> Invia il riepilogo a {user.email}
                </button>
                {feedback('email')}
              </div>
            </Section>

            <Section
              id="account"
              icon="trash"
              title="Elimina account"
              danger
              intro="Spariscono per sempre le tue critiche, i mi piace, i commenti, la collezione e le chat."
            >
              <form onSubmit={deleteAccount} className="settings-form">
                <label className="field">
                  <span className="field__label">Conferma con la password</span>
                  <input type="password" value={deletePwd} onChange={(e) => setDeletePwd(e.target.value)} required autoComplete="current-password" />
                </label>
                <div className="settings__row">
                  <button className="btn btn--ghost btn--sm btn--danger" disabled={busy === 'delete'}>Elimina definitivamente</button>
                  {feedback('delete')}
                </div>
              </form>
            </Section>
          </div>
        </div>
      </div>
    </Layout>
  );
}
