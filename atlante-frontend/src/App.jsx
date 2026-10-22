import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AppProvider } from './context/AppContext';
import { AuthProvider, useAuth } from './context/AuthContext';
import AdminPage from './pages/AdminPage';
import { ForgotPasswordPage, LoginPage, RegisterPage, ResetPasswordPage } from './pages/AuthPages';
import ChatPage from './pages/ChatPage';
import CollectionPage from './pages/CollectionPage';
import ConnectionDetailPage from './pages/ConnectionDetailPage';
import ConnectionsPage from './pages/ConnectionsPage';
import GraphPage from './pages/GraphPage';
import HomePage from './pages/HomePage';
import NewCritiquePage from './pages/NewCritiquePage';
import ProfilePage from './pages/ProfilePage';
import SearchPage from './pages/SearchPage';
import SettingsPage from './pages/SettingsPage';
import UserProfilePage from './pages/UserProfilePage';
import PhantomPage from './pages/PhantomPage';
import './styles/visuart.css';
import './styles/cinema.css';
import './styles/halloween.css';

/** Le pagine personali chiedono l'accesso e poi riportano dove si voleva andare. */
function RequireAuth({ children, admin = false }) {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) {
    return <Navigate to={`/accedi?next=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  }
  if (admin && user.role !== 'ADMIN') return <Navigate to="/profilo" replace />;
  return children;
}

/** I link delle vecchie mail di recupero erano /?reset=TOKEN. */
function Home() {
  const { search } = useLocation();
  const legacy = new URLSearchParams(search).get('reset');
  return legacy ? <Navigate to={`/reimposta-password?token=${legacy}`} replace /> : <HomePage />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppProvider>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/connessioni" element={<ConnectionsPage />} />
            <Route path="/connessioni/:id" element={<ConnectionDetailPage />} />
            <Route path="/grafo" element={<GraphPage />} />
            <Route path="/cerca" element={<SearchPage />} />
            <Route path="/collezione" element={<RequireAuth><CollectionPage /></RequireAuth>} />
            <Route path="/chat" element={<RequireAuth><ChatPage /></RequireAuth>} />
            <Route path="/chat/:username" element={<RequireAuth><ChatPage /></RequireAuth>} />
            <Route path="/profilo" element={<RequireAuth><ProfilePage /></RequireAuth>} />
            <Route path="/utenti/:username" element={<UserProfilePage />} />
            <Route path="/reparto-fantasma" element={<PhantomPage />} />
            <Route path="/impostazioni" element={<RequireAuth><SettingsPage /></RequireAuth>} />
            <Route path="/admin" element={<RequireAuth admin><AdminPage /></RequireAuth>} />
            <Route path="/nuova-critica" element={<RequireAuth><NewCritiquePage /></RequireAuth>} />
            <Route path="/connessioni/:id/modifica" element={<RequireAuth><NewCritiquePage /></RequireAuth>} />
            <Route path="/accedi" element={<LoginPage />} />
            <Route path="/registrati" element={<RegisterPage />} />
            <Route path="/recupero-password" element={<ForgotPasswordPage />} />
            <Route path="/reimposta-password" element={<ResetPasswordPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AppProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
