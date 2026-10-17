import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { auth as authApi } from '../api/client';
import Icon from '../components/ui/Icon';
import { Shell } from '../components/ui/Layout';
import { Motif } from '../components/ui/Visual';
import { useAuth } from '../context/AuthContext';

const CAPTION = 'VisuArt · Uno spazio per guardare, non per scorrere.';
const PASSWORD_RULE = /^(?=.*[A-Z])(?=.*\d).{12,}$/;
const PASSWORD_HINT = 'Almeno 12 caratteri, una maiuscola e un numero.';

function safeNext(params) {
  const next = params.get('next');
  return next && next.startsWith('/') && !next.startsWith('//') ? next : '/connessioni';
}

function Dialog({ title, intro, children, onClose }) {
  return (
    <section className="dialog" aria-labelledby="dialog-title">
      <div className="dialog__top">
        <span className="eyebrow eyebrow--light">Il tuo spazio VisuArt</span>
        <button className="dialog__close" onClick={onClose} aria-label="Chiudi"><Icon name="close" /></button>
      </div>
      <div className="dialog__motif"><Motif size="xs" /></div>
      <h1 id="dialog-title">{title}</h1>
      <p className="dialog__intro">{intro}</p>
      {children}
    </section>
  );
}

function Field({ label, ...input }) {
  return (
    <label className="field">
      <span className="field__label">{label}</span>
      <input {...input} />
    </label>
  );
}

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const next = safeNext(params);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(email, password, remember);
      navigate(next, { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const rightText = next.startsWith('/collezione') ? 'La mia collezione · Accesso richiesto' : 'L’archivio delle connessioni';

  return (
    <Shell rightText={rightText} caption={CAPTION}>
      <Dialog
        title="Accesso"
        intro="Accedi per aprire la tua collezione, ritrovare le opere salvate e continuare i tuoi dialoghi."
        onClose={() => navigate('/')}
      >
        <form className="dialog__form" onSubmit={submit}>
          <Field label="Email" type="text" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" placeholder="email o username" autoFocus />
          <Field label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
          <div className="dialog__row">
            <label className="check"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} /> Ricordami</label>
            <Link to="/recupero-password" className="link" style={{ fontSize: 11 }}>Password dimenticata?</Link>
          </div>
          {error && <p className="form-error">{error}</p>}
          <div>
            <button className="btn btn--primary" disabled={busy}><Icon name="arrowRight" /> {busy ? 'Un attimo…' : 'Accedi'}</button>
          </div>
        </form>
        <div className="dialog__foot">
          <Link to={`/registrati?next=${encodeURIComponent(next)}`} className="link">Non hai un account? Registrati</Link>
        </div>
      </Dialog>
    </Shell>
  );
}

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [terms, setTerms] = useState(false);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!PASSWORD_RULE.test(form.password)) {
      setError(PASSWORD_HINT);
      return;
    }
    if (!terms) {
      setError('Per continuare accetta i Termini di utilizzo.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await register(form.email, form.password, form.name);
      navigate(safeNext(params), { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Shell rightText="L’archivio delle connessioni" caption={CAPTION}>
      <Dialog title="Registrazione" intro="Entra nell’archivio. Crea il tuo atlante di connessioni." onClose={() => navigate('/')}>
        <form className="dialog__form" onSubmit={submit}>
          <Field label="Nome e cognome" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required autoComplete="name" autoFocus />
          <Field label="Email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required autoComplete="email" />
          <div>
            <Field label="Password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required autoComplete="new-password" />
            <p className="hint" style={{ marginTop: 26 }}>{PASSWORD_HINT}</p>
          </div>
          <label className="check" style={{ marginTop: 8 }}>
            <input type="checkbox" checked={terms} onChange={(e) => setTerms(e.target.checked)} />
            Accetto i Termini di utilizzo e ho letto l’Informativa privacy.
          </label>
          {error && <p className="form-error">{error}</p>}
          <div>
            <button className="btn btn--primary" disabled={busy}><Icon name="arrowRight" /> {busy ? 'Un attimo…' : 'Crea il tuo account'}</button>
          </div>
        </form>
        <div className="dialog__foot">
          <Link to="/accedi" className="link">Hai già un account? Accedi</Link>
        </div>
      </Dialog>
    </Shell>
  );
}

export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      setSent((await authApi.forgotPassword(email)).message);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Shell rightText="L’archivio delle connessioni" caption={CAPTION}>
      <Dialog title="Recupero password" intro="Ti invieremo un link per scegliere una nuova password." onClose={() => navigate('/accedi')}>
        <form className="dialog__form" onSubmit={submit}>
          <Field label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" autoFocus />
          {error && <p className="form-error">{error}</p>}
          {sent && <p className="form-ok">{sent}</p>}
          <div>
            <button className="btn btn--primary" disabled={busy}><Icon name="arrowRight" /> {busy ? 'Invio…' : 'Invia link di recupero'}</button>
          </div>
        </form>
        <div className="dialog__foot">
          <Link to="/accedi" className="link"><Icon name="arrowLeft" /> Torna all’accesso</Link>
        </div>
        <p className="dialog__note">Se l’indirizzo è registrato, riceverai un’email. Controlla anche la cartella spam.</p>
      </Dialog>
    </Shell>
  );
}

export function ResetPasswordPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const token = params.get('token');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [done, setDone] = useState(false);
  const [error, setError] = useState(token ? null : 'Link incompleto: richiedi un nuovo recupero password.');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!PASSWORD_RULE.test(password)) return setError(PASSWORD_HINT);
    if (password !== confirm) return setError('Le due password non coincidono.');
    setBusy(true);
    setError(null);
    try {
      await authApi.resetPassword(token, password);
      setDone(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
    return undefined;
  };

  return (
    <Shell rightText="L’archivio delle connessioni" caption={CAPTION}>
      <Dialog title="Reimposta password" intro="Scegli una nuova password per proteggere il tuo archivio." onClose={() => navigate('/accedi')}>
        {done ? (
          <div className="dialog__form">
            <p className="form-ok">Password aggiornata. Tutte le sessioni aperte sono state chiuse.</p>
            <div><Link to="/accedi" className="btn btn--primary"><Icon name="arrowRight" /> Accedi</Link></div>
          </div>
        ) : (
          <form className="dialog__form" onSubmit={submit}>
            <Field label="Nuova password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="new-password" autoFocus />
            <div>
              <Field label="Conferma nuova password" type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required autoComplete="new-password" />
              <p className="hint" style={{ marginTop: 26 }}>{PASSWORD_HINT}</p>
            </div>
            {error && <p className="form-error">{error}</p>}
            <div>
              <button className="btn btn--primary" disabled={busy || !token}><Icon name="arrowRight" /> {busy ? 'Salvo…' : 'Salva la nuova password'}</button>
            </div>
          </form>
        )}
        <div className="dialog__foot">
          <Link to="/accedi" className="link"><Icon name="arrowLeft" /> Torna all’accesso</Link>
        </div>
      </Dialog>
    </Shell>
  );
}
