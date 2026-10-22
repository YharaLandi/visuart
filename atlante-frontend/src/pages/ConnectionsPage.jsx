import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { connections as connectionsApi } from '../api/client';
import CategoryChips from '../components/CategoryChips';
import ConnectionCard from '../components/ConnectionCard';
import Icon from '../components/ui/Icon';
import Layout from '../components/ui/Layout';
import { ErrorState, Loading } from '../components/ui/States';
import { useAuth } from '../context/AuthContext';
import { PageSign } from '../components/Marquee';

const PAGE_SIZE = 9;
// "In primo piano" usa il punteggio di popolarità del server: mi piace, commenti e viste, con un leggero peso alla novità
const SORTS = { popular: 'In primo piano', views: 'Più viste', recent: 'Più recenti' };
const NEXT_SORT = { popular: 'views', views: 'recent', recent: 'popular' };

export default function ConnectionsPage() {
  const { user } = useAuth();
  const [category, setCategory] = useState(null);
  const [sort, setSort] = useState('popular');
  const [list, setList] = useState([]);
  const [info, setInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 });
  const [status, setStatus] = useState('loading');
  const [loadingMore, setLoadingMore] = useState(false);

  const load = useCallback(() => {
    setStatus('loading');
    connectionsApi.search({ category, sort, page: 0, size: PAGE_SIZE })
      .then((data) => {
        setList(data.content);
        setInfo({ page: 0, totalPages: data.totalPages, totalElements: data.totalElements });
        setStatus('ok');
      })
      .catch(() => setStatus('error'));
  }, [category, sort]);

  // ricarica anche al login/logout: "mi piace" dipende da chi guarda
  useEffect(() => { load(); }, [load, user]);

  const more = () => {
    const next = info.page + 1;
    setLoadingMore(true);
    connectionsApi.search({ category, sort, page: next, size: PAGE_SIZE })
      .then((data) => {
        setList((prev) => [...prev, ...data.content.filter((c) => !prev.some((p) => p.id === c.id))]);
        setInfo({ page: next, totalPages: data.totalPages, totalElements: data.totalElements });
      })
      .finally(() => setLoadingMore(false));
  };

  const hasMore = info.page + 1 < info.totalPages;

  return (
    <Layout>
      <div className="container">
        <header className="page-head">
          <PageSign text="Le connessioni più viste" />
          <p className="lead">
            Accostamenti da osservare, interpretazioni da discutere. Lasciati guidare da una luce, una forma, un colore.
          </p>
          <div className="legend"><span className="t-film">Film</span><span className="t-art">Opera d’arte</span></div>
        </header>

        <div className="toolbar">
          <CategoryChips value={category} onChange={setCategory} />
          <button
            className="toolbar__count"
            onClick={() => setSort(NEXT_SORT[sort])}
            title="Cambia ordinamento"
          >
            {info.totalElements} connessioni · {SORTS[sort]} <Icon name="arrowDown" />
          </button>
        </div>

        {status === 'loading' && <Loading />}
        {status === 'error' && <ErrorState onRetry={load} />}
        {status === 'ok' && list.length === 0 && (
          <div className="state">
            <h3>Nessun risultato, per ora.</h3>
            <p>Nessuna connessione in questa categoria. Proponi tu il primo accostamento.</p>
          </div>
        )}
        {status === 'ok' && list.length > 0 && (
          <section aria-labelledby="conn-list">
            <h2 id="conn-list" className="sr-only">Elenco delle connessioni</h2>
            <div className="cards-3">
              {list.map((c) => <ConnectionCard key={c.id} conn={c} />)}
            </div>
          </section>
        )}

        {status === 'ok' && (
          <div className="end-note">
            {hasMore ? (
              <button className="btn btn--ghost" onClick={more} disabled={loadingMore}>
                {loadingMore ? 'Carico…' : `Mostra altre ${info.totalElements - list.length} connessioni`}
              </button>
            ) : (
              <>
                <p>Hai esplorato tutte le connessioni in evidenza.</p>
                <Link to="/nuova-critica" className="btn btn--ghost"><Icon name="plus" /> Proponi il tuo sguardo</Link>
              </>
            )}
          </div>
        )}
      </div>
    </Layout>
  );
}
