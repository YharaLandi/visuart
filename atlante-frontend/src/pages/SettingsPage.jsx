import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { account as accountApi } from '../api/client';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { useAuth } from '../context/AuthContext';

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
            <a href="#notifiche"><Icon name="envelope" /> Mail</a>
            <a href="#account" className="danger"><Icon name="trash" /> Elimina account</a>
          </nav>

          <div className="settings-stack">
            <Section id="profilo" icon="user" title="Profilo" intro="Nome e frase compaiono sulle tue critiche e nella tua scheda.">
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
