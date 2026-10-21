import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { connections as connectionsApi, works as worksApi } from '../api/client';
import CategoryChips from '../components/CategoryChips';
import Icon from '../components/ui/Icon';
import { Shell } from '../components/ui/Layout';
import { Motif } from '../components/ui/Visual';

const MAX = 3000;

/** Select con ricerca: "Cerca e seleziona un Film / un'Opera". */
function WorkCombo({ label, works, value, onChange, disabled = false }) {
  const [open, setOpen] = useState(false);
  const [q, setQ] = useState('');
  const ref = useRef(null);
  const selected = works.find((w) => w.id === value);

  useEffect(() => {
    if (!open) return undefined;
    const onDown = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', onDown);
    return () => document.removeEventListener('mousedown', onDown);
  }, [open]);

  const list = useMemo(() => {
    const t = q.trim().toLowerCase();
    return works.filter((w) => !t || w.title.toLowerCase().includes(t) || (w.creator ?? '').toLowerCase().includes(t)).slice(0, 30);
  }, [works, q]);

  return (
    <div className="picker" ref={ref}>
      <label className="field">
        <span className="field__label">{label}</span>
        <input
          role="combobox"
          aria-expanded={open}
          value={open ? q : selected ? `${selected.title}${selected.year ? ` · ${selected.year}` : ''}` : ''}
          placeholder="Scrivi per cercare…"
          disabled={disabled}
          onFocus={() => { setOpen(true); setQ(''); }}
          onChange={(e) => setQ(e.target.value)}
        />
        <Icon name="chevronDown" className="field__chev" />
      </label>
      {open && (
        <div className="picker__list" role="listbox">
          {list.map((w) => (
            <button key={w.id} type="button" className={w.id === value ? 'on' : ''} onClick={() => { onChange(w.id); setOpen(false); }}>
              {w.title}<small>{w.creator}{w.year ? ` · ${w.year}` : ''}</small>
            </button>
          ))}
          {list.length === 0 && <p className="small" style={{ padding: '10px 14px' }}>Nessuna opera: importala da “Cerca negli archivi”.</p>}
        </div>
      )}
    </div>
  );
}

export default function NewCritiquePage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const editing = Boolean(id);
  const [loaded, setLoaded] = useState(!editing);
  const [films, setFilms] = useState([]);
  const [arts, setArts] = useState([]);
  // le due opere della coppia: ognuna può essere un film o un'opera d'arte
  const [slots, setSlots] = useState([{ type: 'FILM', id: null }, { type: 'ARTWORK', id: null }]);
  const [category, setCategory] = useState(null);
  const [note, setNote] = useState('');
  const [personal, setPersonal] = useState(true);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    worksApi.list({ type: 'FILM' }).then(setFilms).catch(() => {});
    worksApi.list({ type: 'ARTWORK' }).then(setArts).catch(() => {});
  }, []);

  // in modifica le due opere restano quelle della critica: si cambiano categoria e testo
  useEffect(() => {
    if (!editing) return;
    connectionsApi.get(id)
      .then((c) => {
        setSlots([{ type: c.source.type, id: c.source.id }, { type: c.target.type, id: c.target.id }]);
        setCategory(c.categorySlug);
        setNote(c.criticalNote);
        setLoaded(true);
      })
      .catch((e) => setError(e.message));
  }, [editing, id]);

  const worksOf = (type) => (type === 'ARTWORK' ? arts : films);
  const [film, art] = slots.map((sl) => worksOf(sl.type).find((w) => w.id === sl.id));
  const setSlot = (i, patch) => setSlots((prev) => prev.map((sl, j) => (j === i ? { ...sl, ...patch } : sl)));

  const submit = async (e) => {
    e.preventDefault();
    if (!film || !art) return setError('Scegli le due opere da accostare.');
    if (film.id === art.id) return setError('Scegli due opere diverse.');
    if (!category) return setError('Scegli la categoria della connessione.');
    if (!note.trim()) return setError('Racconta perché queste due opere si parlano.');
    if (!personal) return setError('Conferma che si tratta di un’interpretazione personale.');
    setBusy(true);
    setError(null);
    try {
      const body = { sourceWorkId: film.id, targetWorkId: art.id, categorySlug: category, criticalNote: note.trim() };
      const saved = editing ? await connectionsApi.update(id, body) : await connectionsApi.create(body);
      navigate(`/connessioni/${saved.id}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
    return undefined;
  };

  const remove = async () => {
    if (!window.confirm('Eliminare questa critica? Spariscono anche i suoi mi piace e commenti.')) return;
    setBusy(true);
    try {
      await connectionsApi.remove(id);
      navigate('/profilo');
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <Shell rightText="L’archivio delle connessioni" caption="La connessione sarà pubblica. Puoi modificarla dal tuo profilo.">
      <form className="dialog dialog--wide critique" onSubmit={submit}>
        <div className="dialog__top">
          <span className="dialog__ticket">{editing ? 'Rivedi il tuo sguardo' : 'Un nuovo sguardo'}</span>
          <button type="button" className="dialog__back" onClick={() => navigate(-1)}><Icon name="arrowLeft" /> Indietro</button>
        </div>
        <h1>{editing ? 'Modifica critica' : 'Nuova critica'}</h1>
        <p className="dialog__intro">
          Accosta due opere e racconta la tua interpretazione. Non serve dimostrare un’influenza: serve uno sguardo.
        </p>

        <div className="critique__cols">
          {slots.map((sl, i) => {
            const picked = i === 0 ? film : art;
            const isArt = sl.type === 'ARTWORK';
            return (
              <div key={i}>
                <div className="kind-switch" role="radiogroup" aria-label={i === 0 ? 'Tipo della prima opera' : 'Tipo della seconda opera'}>
                  {[['FILM', 'Film'], ['ARTWORK', 'Opera d’arte']].map(([t, lbl]) => (
                    <button key={t} type="button" role="radio" aria-checked={sl.type === t} disabled={editing}
                      className={`chip ${sl.type === t ? 'chip--active' : ''}`}
                      onClick={() => sl.type !== t && setSlot(i, { type: t, id: null })}>
                      {lbl}
                    </button>
                  ))}
                </div>
                <WorkCombo label={`${i === 0 ? 'Prima' : 'Seconda'} opera: cerca ${isArt ? 'un’opera d’arte' : 'un film'}`}
                  works={worksOf(sl.type).filter((w) => w.id !== slots[1 - i].id || sl.type !== slots[1 - i].type)}
                  value={sl.id} onChange={(v) => setSlot(i, { id: v })} disabled={editing} />
                {picked && <p className="critique__sub">{picked.creator} · {isArt ? 'Pittura' : 'Cinema'}</p>}
              </div>
            );
          })}
        </div>

        <div className="critique__motif"><Motif film={film} art={art} size="md" /></div>

        <p className="critique__label">Categoria della connessione</p>
        <CategoryChips value={category} onChange={setCategory} includeAll={false} />

        <label className="field" style={{ marginTop: 20 }}>
          <span className="field__label">Perché queste due opere si parlano?</span>
          <textarea rows={4} value={note} maxLength={MAX} onChange={(e) => setNote(e.target.value)}
            placeholder="La prima frase diventa la citazione in evidenza; il resto il testo della lettura." />
        </label>
        <p className="critique__counter">{note.length} / {new Intl.NumberFormat('it-IT').format(MAX)} caratteri</p>

        <label className="check">
          <input type="checkbox" checked={personal} onChange={(e) => setPersonal(e.target.checked)} />
          Presento questa nota come interpretazione personale, non come influenza storica certa.
        </label>
        {error && <p className="form-error" style={{ marginTop: 16 }}>{error}</p>}
        <div>
          <div className="settings__row" style={{ marginTop: 26, justifyContent: 'space-between' }}>
            <button className="btn btn--primary" style={{ marginTop: 0 }} disabled={busy || !loaded}>
              <Icon name="arrowUpRight" /> {busy ? 'Salvo…' : editing ? 'Salva le modifiche' : 'Pubblica la connessione'}
            </button>
            {editing && (
              <button type="button" className="btn btn--ghost btn--sm btn--danger" onClick={remove} disabled={busy}>
                <Icon name="trash" /> Elimina la critica
              </button>
            )}
          </div>
        </div>
      </form>
    </Shell>
  );
}
