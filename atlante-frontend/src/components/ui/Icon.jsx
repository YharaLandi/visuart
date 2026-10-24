import 'bootstrap-icons/font/bootstrap-icons.css';

// Nomi usati nell'app -> Bootstrap Icons (https://icons.getbootstrap.com)
const BI = {
  search: 'search',
  plus: 'plus-lg',
  arrowUpRight: 'arrow-up-right',
  arrowRight: 'arrow-right',
  arrowLeft: 'arrow-left',
  arrowUp: 'arrow-up',
  arrowDown: 'arrow-down',
  arrowsLR: 'arrow-left-right',
  close: 'x-lg',
  menu: 'list',
  chevronDown: 'chevron-down',
  chevronLeft: 'chevron-left',
  chevronRight: 'chevron-right',
  thumb: 'hand-thumbs-up',
  thumbFill: 'hand-thumbs-up-fill',
  heart: 'heart',
  heartFill: 'heart-fill',
  bookmark: 'bookmark',
  bookmarkFill: 'bookmark-fill',
  compass: 'compass',
  graph: 'diagram-3',
  chat: 'chat',
  sparkles: 'stars',
  link: 'link-45deg',
  clip: 'paperclip',
  logout: 'box-arrow-right',
  refresh: 'arrow-clockwise',
  check: 'check-lg',
  checkAll: 'check-all',
  user: 'person',
  bell: 'bell',
  gear: 'gear',
  shield: 'shield-lock',
  envelope: 'envelope',
  trash: 'trash3',
  pencil: 'pencil',
  people: 'people',
  dot: 'circle-fill',
  circle: 'circle',
};

/** Icona Bootstrap: eredita colore dal testo; size in px (opzionale). */
export default function Icon({ name, size, className = '', style, ...rest }) {
  return (
    <i
      className={`bi bi-${BI[name] ?? name} ${className}`}
      style={size ? { fontSize: size, ...style } : style}
      aria-hidden="true"
      {...rest}
    />
  );
}
