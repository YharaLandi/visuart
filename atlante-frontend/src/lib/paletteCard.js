// Genera e scarica un'immagine PNG 1080x1350 con la palette di un'opera,
// disegnata con i colori del tema attivo (formato adatto ai social).

const W = 1080;
const H = 1350;
const PAD = 96;

function wrap(ctx, text, maxWidth) {
  const lines = [];
  let line = '';
  text.split(' ').forEach((word) => {
    const next = line ? `${line} ${word}` : word;
    if (ctx.measureText(next).width > maxWidth && line) {
      lines.push(line);
      line = word;
    } else {
      line = next;
    }
  });
  if (line) lines.push(line);
  return lines.slice(0, 3);
}

function ring(ctx, x, y, r, color, width) {
  ctx.beginPath();
  ctx.arc(x, y, r, 0, Math.PI * 2);
  ctx.strokeStyle = color;
  ctx.lineWidth = width;
  ctx.stroke();
}

function slug(text) {
  return text.toLowerCase().normalize('NFD').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

export async function downloadPaletteCard({ colors, label, title, tone }) {
  const css = getComputedStyle(document.documentElement);
  const v = (name) => css.getPropertyValue(name).trim();
  const film = v('--film-ring');
  const art = v('--art-ring');
  const accent = v(tone === 'art' ? '--art' : '--film');

  // i caratteri devono essere pronti prima di disegnare sul canvas
  await Promise.all([
    document.fonts.load('italic 300 80px "Source Serif 4"'),
    document.fonts.load('400 72px "Source Serif 4"'),
    document.fonts.load('700 26px "Manrope"'),
  ]).catch(() => {});

  const canvas = document.createElement('canvas');
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d');

  ctx.fillStyle = v('--bg');
  ctx.fillRect(0, 0, W, H);
  ctx.fillStyle = v('--surface-1');
  ctx.beginPath();
  ctx.roundRect(48, 48, W - 96, H - 96, 32);
  ctx.fill();
  ctx.strokeStyle = v('--line-2');
  ctx.lineWidth = 2;
  ctx.stroke();

  // logo: due cerchi, linea sfumata, "VisuArt"
  ring(ctx, 114, 150, 14, film, 3);
  ring(ctx, 168, 150, 14, art, 3);
  const link = ctx.createLinearGradient(128, 0, 154, 0);
  link.addColorStop(0, film);
  link.addColorStop(1, art);
  ctx.fillStyle = link;
  ctx.fillRect(128, 148.5, 26, 3);
  ctx.fillStyle = v('--text-1');
  ctx.font = 'italic 400 44px "Source Serif 4", Georgia, serif';
  ctx.textBaseline = 'middle';
  ctx.fillText('VisuArt', 206, 152);
  ctx.textBaseline = 'alphabetic';

  ctx.fillStyle = accent;
  ctx.font = '700 26px "Manrope", sans-serif';
  if ('letterSpacing' in ctx) ctx.letterSpacing = '5px';
  ctx.fillText((label ?? 'Palette').toUpperCase(), PAD, 330);
  if ('letterSpacing' in ctx) ctx.letterSpacing = '0px';

  ctx.fillStyle = v('--text-1');
  ctx.font = 'italic 300 80px "Source Serif 4", Georgia, serif';
  wrap(ctx, title || 'La materia del colore', W - 2 * PAD).forEach((line, i) => ctx.fillText(line, PAD, 430 + i * 92));

  // fascia continua dei colori
  const band = (W - 2 * PAD) / colors.length;
  ctx.save();
  ctx.beginPath();
  ctx.roundRect(PAD, 600, W - 2 * PAD, 120, 16);
  ctx.clip();
  colors.forEach((c, i) => {
    ctx.fillStyle = c;
    ctx.fillRect(PAD + i * band, 600, band + 1, 120);
  });
  ctx.restore();

  // campioni rotondi con il codice esadecimale
  ctx.font = '600 24px ui-monospace, "SF Mono", Menlo, monospace';
  ctx.textAlign = 'center';
  colors.forEach((c, i) => {
    const x = 158 + i * 152.8;
    ctx.beginPath();
    ctx.arc(x, 860, 62, 0, Math.PI * 2);
    ctx.fillStyle = c;
    ctx.fill();
    ctx.strokeStyle = v('--line-2');
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.fillStyle = v('--text-muted');
    ctx.fillText(c.toUpperCase(), x, 970);
  });

  const rule = ctx.createLinearGradient(PAD, 0, W - PAD, 0);
  rule.addColorStop(0, film);
  rule.addColorStop(1, art);
  ctx.fillStyle = rule;
  ctx.fillRect(PAD, 1110, W - 2 * PAD, 3);
  ctx.textAlign = 'left';
  ctx.fillStyle = v('--text-muted');
  ctx.font = 'italic 400 30px "Source Serif 4", Georgia, serif';
  ctx.fillText('La materia del colore', PAD, 1190);

  const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'));
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `visuart-palette-${slug(title || label || 'palette')}.png`;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
