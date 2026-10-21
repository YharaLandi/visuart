import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { connections as connectionsApi, imgUrl, uploads as uploadsApi, works as worksApi } from '../api/client';
import CategoryChips from '../components/CategoryChips';
import PaletteEditor from '../components/PaletteEditor';
import Icon from '../components/ui/Icon';
import { Shell } from '../components/ui/Layout';
import { Motif } from '../components/ui/Visual';

const MAX = 3000;

/** Select con ricerca: "Cerca e seleziona un Film / un'Opera". */
function WorkCombo({ label, works, value, onChange, onAddNew, disabled = false }) {
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
          {list.length === 0 && <p className="small" style={{ padding: '10px 14px' }}>Nessuna opera con questo nome negli archivi.</p>}
          {onAddNew && (
            <button type="button" className="picker__add" onClick={() => { setOpen(false); onAddNew(q.trim()); }}>
              <Icon name="plus" /> Non la trovi? Aggiungi {q.trim() ? `“${q.trim()}”` : 'un’opera'}
            </button>
          )}
        </div>
      )}
    </div>
  );
}

/** Carica un'immagine e restituisce l'URL salvato (formato e peso si controllano anche sul server). */
async function uploadFile(file) {
  if (file.size > 5 * 1024 * 1024) throw new Error('L’immagine supera i 5 MB: scegline una più leggera.');
  return uploadsApi.image(file);
}

/**
 * Immagine scelta per questa connessione: un fotogramma preciso del film o un dettaglio del
 * quadro. L'opera resta quella degli archivi; senza immagine si usa quella dell'opera.
 */
function PairImagePicker({ isArt, value, fallback, onChange }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const pick = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const up = await uploadFile(file);
      onChange(up.url, up.palette);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };
  const shown = imgUrl(value ?? fallback);
  return (
    <div className="pair-image">
      <span className="pair-image__thumb">{shown && <img src={shown} alt="" />}</span>
      <div className="pair-image__body">
        <p className="pair-image__label">
          {value ? (isArt ? 'Dettaglio scelto da te' : 'Fotogramma scelto da te') : 'Immagine degli archivi'}
        </p>
        <div className="pair-image__actions">
          <label className="btn btn--ghost btn--sm">
            <Icon name="upload" /> {busy ? 'Carico…' : value ? (isArt ? 'Cambia dettaglio' : 'Cambia fotogramma') : isArt ? 'Carica un dettaglio del quadro' : 'Carica un fotogramma'}
            <input type="file" accept="image/jpeg,image/png,image/webp" className="sr-only" onChange={pick} disabled={busy} />
          </label>
          {value && <button type="button" className="link-muted" onClick={() => onChange(null, null)}>Usa l’immagine degli archivi</button>}
        </div>
        {error && <p className="form-error" role="alert">{error}</p>}
      </div>
    </div>
  );
}

/** Un'opera che non è negli archivi: la aggiunge l'utente, con la sua immagine. */
function NewWorkForm({ type, initialTitle, onCreated, onCancel }) {
  const isArt = type === 'ARTWORK';
  const [form, setForm] = useState({ title: initialTitle ?? '', creator: '', year: '', description: '' });
  const [image, setImage] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const pick = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setError(null);
    try { setImage((await uploadFile(file)).url); } catch (err) { setError(err.message); }
  };
  const save = async () => {
    if (!form.title.trim()) return setError('Il titolo è obbligatorio.');
    if (form.year && !/^\d{3,4}$/.test(form.year)) return setError('L’anno deve essere un numero, per esempio 1975.');
    if (!image) return setError(`Carica un’immagine ${isArt ? 'dell’opera' : 'del film'}: serve per la palette e per la scheda.`);
    setBusy(true);
    setError(null);
    try {
      const created = await worksApi.create({
        type,
        title: form.title.trim(),
        creator: form.creator.trim() || null,
        year: form.year ? Number(form.year) : null,
        description: form.description.trim() || null,
        imageUrl: image,
      });
      onCreated(created);
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
    return undefined;
  };
  return (
    <div className="new-work" role="group" aria-label={isArt ? 'Nuova opera d’arte' : 'Nuovo film'}>
      <p className="new-work__title">{isArt ? 'Aggiungi un’opera d’arte' : 'Aggiungi un film'}</p>
      <p className="new-work__intro">Per le opere che non sono negli archivi. Resterà disponibile anche per le critiche degli altri.</p>
      <label className="field"><span className="field__label">Titolo</span><input value={form.title} onChange={set('title')} maxLength={200} /></label>
      <div className="new-work__row">
        <label className="field"><span className="field__label">{isArt ? 'Artista' : 'Regia'}</span><input value={form.creator} onChange={set('creator')} maxLength={160} /></label>
        <label className="field"><span className="field__label">Anno</span><input value={form.year} onChange={set('year')} inputMode="numeric" maxLength={4} /></label>
      </div>
      <label className="field"><span className="field__label">Descrizione breve</span><input value={form.description} onChange={set('description')} maxLength={300} /></label>
      <div className="pair-image">
        <span className="pair-image__thumb">{image && <img src={imgUrl(image)} alt="" />}</span>
        <label className="btn btn--ghost btn--sm">
          <Icon name="upload" /> {image ? 'Cambia immagine' : 'Carica l’immagine'}
          <input type="file" accept="image/jpeg,image/png,image/webp" className="sr-only" onChange={pick} />
        </label>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="new-work__actions">
        <button type="button" className="btn btn--primary btn--sm" onClick={save} disabled={busy}>{busy ? 'Salvo…' : 'Aggiungi e scegli'}</button>
        <button type="button" className="link-muted" onClick={onCancel}>Annulla</button>
      </div>
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
  const [slots, setSlots] = useState([
    { type: 'FILM', id: null, image: null, imagePalette: null, palette: null },
    { type: 'ARTWORK', id: null, image: null, imagePalette: null, palette: null },
  ]);
  const [adding, setAdding] = useState(null);
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
        setSlots([
          { type: c.source.type, id: c.source.id, image: c.sourceImageUrl ?? null, imagePalette: null, palette: c.sourcePalette ?? null },
          { type: c.target.type, id: c.target.id, image: c.targetImageUrl ?? null, imagePalette: null, palette: c.targetPalette ?? null },
        ]);
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
      const body = {
        sourceWorkId: film.id, targetWorkId: art.id, categorySlug: category, criticalNote: note.trim(),
        sourceImageUrl: slots[0].image, targetImageUrl: slots[1].image,
        sourcePalette: slots[0].palette, targetPalette: slots[1].palette,
      };
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
                      onClick={() => sl.type !== t && setSlot(i, { type: t, id: null, image: null, imagePalette: null, palette: null })}>
                      {lbl}
                    </button>
                  ))}
                </div>
                <WorkCombo label={`${i === 0 ? 'Prima' : 'Seconda'} opera: cerca ${isArt ? 'un’opera d’arte' : 'un film'}`}
                  works={worksOf(sl.type).filter((w) => w.id !== slots[1 - i].id || sl.type !== slots[1 - i].type)}
                  value={sl.id} onChange={(v) => setSlot(i, { id: v, palette: null })} disabled={editing}
                  onAddNew={editing ? undefined : (title) => setAdding({ slot: i, type: sl.type, title })} />
                {adding?.slot === i && (
                  <NewWorkForm type={adding.type} initialTitle={adding.title} onCancel={() => setAdding(null)}
                    onCreated={(w) => {
                      if (w.type === 'ARTWORK') setArts((prev) => [w, ...prev]); else setFilms((prev) => [w, ...prev]);
                      setSlot(i, { id: w.id });
                      setAdding(null);
                    }} />
                )}
                {picked && <p className="critique__sub">{picked.creator ? `${picked.creator} · ` : ''}{isArt ? 'Pittura' : 'Cinema'}</p>}
                {picked && (
                  <PairImagePicker isArt={isArt} value={sl.image} fallback={picked.imageUrl}
                    onChange={(url, pal) => setSlot(i, {
                      image: url,
                      imagePalette: pal ?? null,
                      // con un'immagine nuova si propongono i suoi colori, se la palette non era già stata toccata
                      palette: sl.palette ?? pal ?? null,
                    })} />
                )}
                {picked && (
                  <PaletteEditor label={isArt ? 'Opera d’arte' : 'Film'} value={sl.palette} base={picked.palette}
                    imagePalette={sl.imagePalette} onChange={(p) => setSlot(i, { palette: p })} />
                )}
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
