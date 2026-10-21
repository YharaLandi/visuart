import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import ReactFlow, { Handle, Position, ReactFlowProvider, useReactFlow } from 'reactflow';
import 'reactflow/dist/style.css';
import { graph as graphApi, imgUrl } from '../api/client';
import CategoryChips from '../components/CategoryChips';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { ErrorState, Loading } from '../components/ui/States';
import { PaletteBars } from '../components/ui/Visual';

function WorkNode({ data }) {
  const isArt = data.type === 'ARTWORK';
  const src = imgUrl(data.imageUrl);
  return (
    <div
      className={`gnode ${isArt ? 'gnode--art' : ''} ${data.selected ? 'gnode--selected' : ''} ${data.path ? 'gnode--path' : ''} ${data.dim ? 'gnode--dim' : ''}`}
      style={{ '--ring': data.ring }}
    >
      <Handle type="target" position={Position.Top} />
      <div className="gnode__c">{src && <img src={src} alt="" />}</div>
      <span className="gnode__type">{isArt ? 'OPERA D’ARTE' : 'FILM'}</span>
      <span className="gnode__title">{data.title}</span>
      <Handle type="source" position={Position.Bottom} />
    </div>
  );
}

const nodeTypes = { work: WorkNode };

const IS_MOBILE = typeof window !== 'undefined' && window.matchMedia('(max-width: 860px)').matches;
// su mobile gli anelli si allungano in verticale per stare nello schermo stretto
const STRETCH = IS_MOBILE ? [0.6, 1.25] : [1.35, 0.85];
const FIT = IS_MOBILE ? { padding: 0.08, minZoom: 0.85 } : { padding: 0.12, minZoom: 0.75 };

/** Percorso più breve (in archi) dal nodo centrale a quello sotto il mouse. */
function shortestPath(edges, from, to) {
  if (from == null || to == null) return null;
  const prev = new Map([[from, null]]);
  const queue = [from];
  while (queue.length) {
    const current = queue.shift();
    if (current === to) break;
    edges.forEach((e) => {
      const next = e.sourceId === current ? e.targetId : e.targetId === current ? e.sourceId : null;
      if (next != null && !prev.has(next)) {
        prev.set(next, { node: current, edge: e.id });
        queue.push(next);
      }
    });
  }
  if (!prev.has(to)) return null;
  const nodes = new Set([to]);
  const pathEdges = new Set();
  for (let step = prev.get(to); step; step = prev.get(step.node)) {
    nodes.add(step.node);
    pathEdges.add(step.edge);
  }
  return { nodes, edges: pathEdges };
}

/** Disposizione a anelli: il nodo scelto al centro, gli altri su cerchi concentrici sfalsati. */
function ringLayout(nodes, centerId) {
  const pos = {};
  const others = nodes.filter((n) => n.id !== centerId);
  if (centerId != null) pos[centerId] = { x: 0, y: 0, ring: 0 };
  let ring = 0, placed = 0;
  while (placed < others.length) {
    ring += 1;
    const capacity = 6 * ring;
    const radius = 230 * ring;
    const count = Math.min(capacity, others.length - placed);
    for (let i = 0; i < count; i++) {
      const angle = (2 * Math.PI * i) / count + ring * 0.6;
      pos[others[placed + i].id] = { x: Math.cos(angle) * radius * STRETCH[0], y: Math.sin(angle) * radius * STRETCH[1], ring };
    }
    placed += count;
  }
  return pos;
}

function Controls() {
  const { zoomIn, zoomOut, fitView } = useReactFlow();
  return (
    <div className="graph__controls">
      <button className="chip" onClick={() => zoomOut()} aria-label="Riduci"><Icon name="dash-lg" /></button>
      <button className="chip" onClick={() => zoomIn()} aria-label="Ingrandisci"><Icon name="plus" /></button>
      <button className="chip" onClick={() => fitView({ ...FIT, duration: 300 })}>Centra</button>
    </div>
  );
}

function Panel({ work, edges, nodes, onExpand }) {
  const [pick, setPick] = useState(0);
  useEffect(() => setPick(0), [work?.id]);
  if (!work) {
    return (
      <aside className="graph-panel">
        <p className="eyebrow eyebrow--light">Nessun nodo selezionato</p>
        <p className="graph-panel__meta">Seleziona un’opera nel grafo per leggerne le connessioni.</p>
      </aside>
    );
  }
  const isArt = work.type === 'ARTWORK';
  const byId = new Map(nodes.map((n) => [n.id, n]));
  const mine = edges.filter((e) => e.sourceId === work.id || e.targetId === work.id);
  const current = mine[pick];
  const other = current ? byId.get(current.sourceId === work.id ? current.targetId : current.sourceId) : null;
  const src = imgUrl(work.imageUrl);

  return (
    <aside className="graph-panel">
      <div className="graph-panel__top">
        <span className={`tag ${isArt ? 'tag--art' : 'tag--film'}`}>{isArt ? 'Opera d’arte' : 'Film'}</span>
        <span className="tiny">NODO SELEZIONATO</span>
      </div>
      <div className="graph-panel__img">{src && <img src={src} alt={work.title} />}</div>
      <h2 className={isArt ? 't-art' : 't-film'}>{work.title}</h2>
      <p className="graph-panel__meta">{work.creator}{work.year ? ` · ${work.year}` : ''}</p>
      <PaletteBars palette={work.palette} label={isArt ? 'Opera d’arte' : 'Film'} tone={isArt ? 'art' : 'film'} />
      <div className="graph-panel__sep" />
      <h3>{mine.length} {mine.length === 1 ? 'connessione' : 'connessioni'} da esplorare</h3>

      {mine.length > 1 && (
        <div className="graph-panel__list">
          {mine.map((e, i) => {
            const o = byId.get(e.sourceId === work.id ? e.targetId : e.sourceId);
            return (
              <button key={e.id} className={i === pick ? 'on' : ''} onClick={() => setPick(i)}>
                <Icon name={i === pick ? 'dot' : 'circle'} style={{ fontSize: 8, marginRight: 8 }} />{o?.title}
              </button>
            );
          })}
        </div>
      )}

      {current && other && (
        <>
          <p className={`graph-panel__conn ${other.type === 'ARTWORK' ? 't-art' : 't-film'}`}>
            <Icon name="arrowsLR" style={{ marginRight: 6 }} />{other.title} · {other.creator}
          </p>
          <p className="graph-panel__conn-meta">{current.category} · Connessione #{String(current.id).padStart(3, '0')}</p>
          <p className="graph-panel__quote">«{current.criticalNote.split(/(?<=[.!?…])\s/)[0]}»</p>
          <div className="graph-panel__actions">
            <Link to={`/connessioni/${current.id}`} className="btn btn--primary"><Icon name="arrowUpRight" /> Leggi la connessione</Link>
            <button className="btn btn--ghost" onClick={() => onExpand(work.id)}><Icon name="graph" /> Espandi il nodo</button>
          </div>
        </>
      )}
    </aside>
  );
}

function GraphCanvas() {
  const [params, setParams] = useSearchParams();
  const centerParam = params.get('opera') ? Number(params.get('opera')) : null;
  const [category, setCategory] = useState(null);
  const [depth, setDepth] = useState(2);
  const [data, setData] = useState({ nodes: [], edges: [] });
  const [selectedId, setSelectedId] = useState(centerParam);
  const [status, setStatus] = useState('loading');
  const [hoverId, setHoverId] = useState(null);

  const load = useCallback(() => {
    setStatus('loading');
    graphApi.get({ workId: centerParam ?? undefined, depth, category })
      .then((d) => {
        setData(d);
        setSelectedId((prev) => (prev && d.nodes.some((n) => n.id === prev) ? prev : d.nodes[0]?.id ?? null));
        setStatus('ok');
      })
      .catch(() => setStatus('error'));
  }, [centerParam, depth, category]);

  useEffect(() => { load(); }, [load]);

  const positions = useMemo(() => ringLayout(data.nodes, centerParam ?? data.nodes[0]?.id), [data.nodes, centerParam]);

  const centerId = centerParam ?? data.nodes[0]?.id;
  const path = useMemo(() => shortestPath(data.edges, centerId, hoverId), [data.edges, centerId, hoverId]);

  const nodes = useMemo(() => data.nodes.map((n) => ({
    id: String(n.id),
    type: 'work',
    ariaLabel: `${n.type === 'ARTWORK' ? 'Opera d’arte' : 'Film'} · ${n.title}`,
    position: positions[n.id] ?? { x: 0, y: 0 },
    data: { ...n, selected: n.id === selectedId, ring: positions[n.id]?.ring ?? 0, path: path?.nodes.has(n.id), dim: path && !path.nodes.has(n.id) },
  })), [data.nodes, positions, selectedId, path]);

  const edges = useMemo(() => data.edges.map((e) => {
    const active = e.sourceId === selectedId || e.targetId === selectedId;
    const onPath = path?.edges.has(e.id);
    return {
      id: String(e.id),
      source: String(e.sourceId),
      target: String(e.targetId),
      type: 'default',
      className: onPath ? 'edge--path' : path ? 'edge--dim' : '',
      style: onPath
        ? { stroke: 'var(--film-ring)', strokeWidth: 2 }
        : { stroke: active ? 'var(--edge-active)' : 'var(--edge)', strokeWidth: active ? 1.5 : 1 },
    };
  }), [data.edges, selectedId, path]);

  const selected = data.nodes.find((n) => n.id === selectedId);
  const expand = (workId) => {
    setParams({ opera: String(workId) });
    setDepth((d) => Math.min(3, d + 1));
  };

  return (
    <>
      <header className="container graph-head">
        <div className="page-head__row">
          <div>
            <h1 className="h-page">Atlante delle affinità</h1>
            <p className="lead">Ogni opera è un punto di partenza. Segui le relazioni, cambia prospettiva.</p>
          </div>
          <div className="legend" style={{ marginTop: 14 }}><span className="t-film">Film</span><span className="t-art">Opera d’arte</span></div>
        </div>
        <div className="toolbar" style={{ borderBottom: 0, paddingBottom: 0, marginTop: 24 }}>
          <CategoryChips value={category} onChange={setCategory} />
          <div className="depth">
            Profondità
            {[1, 2, 3].map((d) => (
              <button key={d} className={`chip ${depth === d ? 'chip--active' : ''}`} onClick={() => setDepth(d)} aria-pressed={depth === d}>{d}</button>
            ))}
          </div>
        </div>
      </header>

      <div className="graph">
        <div className="graph__canvas">
          <p className="graph__hint">Trascina per esplorare · Seleziona un nodo per leggere le connessioni</p>
          {status === 'loading' && <Loading title="Disegniamo il grafo…" />}
          {status === 'error' && <ErrorState onRetry={load} />}
          {status === 'ok' && (
            <ReactFlow
              nodes={nodes}
              edges={edges}
              nodeTypes={nodeTypes}
              onNodeClick={(_, node) => setSelectedId(Number(node.id))}
              onNodeMouseEnter={(_, node) => setHoverId(Number(node.id))}
              onNodeMouseLeave={() => setHoverId(null)}
              fitView
              fitViewOptions={FIT}
              minZoom={0.3}
              maxZoom={2}
              nodesConnectable={false}
              proOptions={{ hideAttribution: true }}
            >
              <Controls />
            </ReactFlow>
          )}
        </div>
        <Panel work={selected} edges={data.edges} nodes={data.nodes} onExpand={expand} />
      </div>
    </>
  );
}

export default function GraphPage() {
  return (
    <Layout>
      <ReactFlowProvider>
        <GraphCanvas />
      </ReactFlowProvider>
    </Layout>
  );
}
