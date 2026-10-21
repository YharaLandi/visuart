import { useEffect, useState } from 'react';
import Icon from './ui/Icon';

const HEX = /^#[0-9a-f]{6}$/i;
const MIN = 3;
const MAX = 8;

export function parsePalette(p) {
  return (p ?? '').split(',').map((c) => c.trim()).filter((c) => HEX.test(c));
}

/** "ff3366", "#FF3366", " #ff3366 " → "#ff3366"; null se non è un colore esadecimale a 6 cifre. */
function normalizeHex(text) {
  const t = text.trim().replace(/^#?/, '#').toLowerCase();
  return HEX.test(t) ? t : null;
}

/**
 * Palette scelta da chi scrive per un'opera della connessione. Si parte dalla palette degli
 * archivi (o da quella dell'immagine caricata); i colori si scrivono come codice esadecimale.
 * value = null significa "usa la palette dell'opera".
 */
export default function PaletteEditor({ value, base, imagePalette, onChange, label }) {
  const colors = parsePalette(value ?? base);
  const [selected, setSelected] = useState(0);
  const [draft, setDraft] = useState('');
  const [newColor, setNewColor] = useState('');
  const custom = value != null;
  const current = colors[selected];

  // il campo mostra sempre il codice del colore selezionato
  useEffect(() => { setDraft(current ?? ''); }, [current]);

  const commit = (list) => onChange(list.join(','));
  const editDraft = (text) => {
    setDraft(text);
    const hex = normalizeHex(text);
    if (hex && hex !== current) commit(colors.map((c, i) => (i === selected ? hex : c)));
  };
  const add = (e) => {
    e?.preventDefault();
    const hex = normalizeHex(newColor);
    if (!hex || colors.length >= MAX) return;
    commit([...colors, hex]);
    setSelected(colors.length);
    setNewColor('');
  };
  const remove = (i) => {
    if (colors.length <= MIN) return;
    commit(colors.filter((_, j) => j !== i));
    setSelected(Math.max(0, Math.min(selected, colors.length - 2)));
  };

  const draftValid = !draft || normalizeHex(draft);
  const newValid = !newColor || normalizeHex(newColor);
  const id = label.replace(/\W+/g, '-').toLowerCase();

  return (
    <div className="palette-editor">
      <p className="palette-editor__label">
        {label} · {custom ? 'palette scelta da te' : 'palette degli archivi'}
      </p>
      <div className="palette-editor__swatches" role="group" aria-label={`Colori della palette ${label}`}>
        {colors.map((c, i) => (
          <span key={i} className={`palette-editor__swatch ${i === selected ? 'is-selected' : ''}`}>
            <button type="button" style={{ background: c }} onClick={() => setSelected(i)}
              aria-pressed={i === selected} aria-label={`Colore ${i + 1}: ${c}`} />
            {colors.length > MIN && (
              <button type="button" className="palette-editor__remove" onClick={() => remove(i)} aria-label={`Togli ${c}`}>
                <Icon name="close" />
              </button>
            )}
          </span>
        ))}
      </div>

      <div className="palette-editor__fields">
        {current && (
          <label className="field palette-editor__hex">
            <span className="field__label">Colore {selected + 1}</span>
            <input id={`${id}-hex`} value={draft} onChange={(e) => editDraft(e.target.value)}
              maxLength={7} spellCheck={false} autoComplete="off" aria-invalid={!draftValid}
              aria-describedby={`${id}-hint`} />
          </label>
        )}
        {colors.length < MAX && (
          <div className="palette-editor__new">
            <label className="field palette-editor__hex">
              <span className="field__label">Nuovo colore</span>
              <input value={newColor} onChange={(e) => setNewColor(e.target.value)} placeholder="#c2410c"
                onKeyDown={(e) => { if (e.key === 'Enter') add(e); }}
                maxLength={7} spellCheck={false} autoComplete="off" aria-invalid={!newValid}
                aria-describedby={`${id}-hint`} />
            </label>
            <button type="button" className="btn btn--ghost btn--sm" onClick={add} disabled={!newColor || !newValid}>
              <Icon name="plus" /> Aggiungi
            </button>
          </div>
        )}
      </div>
      <p id={`${id}-hint`} className={`palette-editor__hint ${draftValid && newValid ? '' : 'is-error'}`}>
        {draftValid && newValid ? 'Codice esadecimale a 6 cifre, per esempio #c2410c. Da 3 a 8 colori.' : 'Il codice deve avere 6 cifre esadecimali, per esempio #c2410c.'}
      </p>

      <div className="palette-editor__actions">
        {imagePalette && imagePalette !== value && (
          <button type="button" className="link-muted" onClick={() => onChange(imagePalette)}>Usa i colori della tua immagine</button>
        )}
        {custom && <button type="button" className="link-muted" onClick={() => onChange(null)}>Torna alla palette degli archivi</button>}
      </div>
    </div>
  );
}
