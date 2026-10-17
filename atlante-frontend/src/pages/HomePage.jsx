import { Link } from 'react-router-dom';
import Layout from '../components/ui/Layout';
import Icon from '../components/ui/Icon';
import heroFilm from '../assets/visuart/hero-film.jpg';
import heroArt from '../assets/visuart/hero-art.jpg';
import explore from '../assets/visuart/home-explore.jpg';
import graph from '../assets/visuart/home-graph.jpg';
import collection from '../assets/visuart/home-collection.jpg';

const WAYS = [
  { to: '/connessioni', tag: 'Esplora', num: '01', title: 'Più viste', text: 'Le connessioni che accendono lo sguardo', img: explore },
  { to: '/grafo', tag: 'Orientati', num: '02', title: 'Grafo', text: 'Segui il filo tra un’opera e l’altra', img: graph },
  { to: '/collezione', tag: 'Conserva', num: '03', title: 'La mia collezione', text: 'Un atlante personale di affinità', img: collection },
];

export default function HomePage() {
  return (
    <Layout minimalNav>
      <div className="container">
        <section className="hero">
          <div>
            <p className="eyebrow eyebrow--light">Cinema e arti visive · Un archivio di relazioni</p>
            <h1 className="hero__title">Le immagini<br />si parlano.</h1>
            <p className="hero__text">
              Un’inquadratura, un dipinto, una somiglianza inattesa. VisuArt raccoglie gli sguardi
              che mettono in dialogo il cinema e le arti visive.
            </p>
            <Link to="/connessioni" className="btn btn--primary hero__cta">
              <Icon name="arrowUpRight" /> Esplora le connessioni più viste
            </Link>
            <p className="hero__aside">Non cerchiamo certezze. Apriamo nuove letture.</p>
          </div>

          <div className="hero__pair">
            <div className="hero__circle">
              <span className="motif__c"><img src={heroFilm} alt="In the Mood for Love" /></span>
              <span className="hero__link" />
              <div className="hero__cap">
                <p className="eyebrow eyebrow--light t-film">Film · 2000</p>
                <h3>In the Mood for Love</h3>
                <p>Wong Kar-wai</p>
              </div>
            </div>
            <div className="hero__circle">
              <span className="motif__c motif__c--art"><img src={heroArt} alt="Nighthawks" /></span>
              <div className="hero__cap">
                <p className="eyebrow eyebrow--light t-art">Opera d’arte · 1942</p>
                <h3>Nighthawks</h3>
                <p>Edward Hopper</p>
              </div>
            </div>
          </div>
        </section>

        <div className="ways__head">
          <h2 className="h-section">Tre modi di guardare</h2>
          <p className="eyebrow eyebrow--light">L’archivio, da prospettive diverse</p>
        </div>
        <div className="ways">
          {WAYS.map((w) => (
            <Link key={w.to} to={w.to} className="way">
              <img src={w.img} alt="" />
              <span className="way__tag">{w.tag.toUpperCase()}</span>
              <span className="way__num">{w.num}</span>
              <div className="way__cap">
                <h3>{w.title}</h3>
                <p>{w.text}</p>
                <span className="way__arrow"><Icon name="arrowUpRight" /></span>
              </div>
            </Link>
          ))}
        </div>

        <section className="manifesto">
          <h2>Una relazione è un invito a riguardare.</h2>
          <p>
            Ogni connessione nasce da una nota critica personale. Le affinità proposte non attestano
            un’influenza storica: raccontano un modo di vedere.
          </p>
        </section>
      </div>
    </Layout>
  );
}
