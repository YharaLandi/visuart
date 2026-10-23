import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { chat as chatApi, favorites as favoritesApi } from '../api/client';
import { useAuth } from './AuthContext';

/** Stato condiviso tra le pagine: opere preferite e messaggi non letti. */
const AppContext = createContext(null);

export function AppProvider({ children }) {
  const { user } = useAuth();
  // si riparte solo quando cambia la persona, non quando il profilo viene riletto dal server
  const username = user?.username ?? null;
  const [favIds, setFavIds] = useState(new Set());
  const [chatUnread, setChatUnread] = useState(0);

  const refreshFavorites = useCallback(() => {
    if (!username) {
      setFavIds(new Set());
      return Promise.resolve([]);
    }
    return favoritesApi.list()
      .then((works) => {
        setFavIds(new Set(works.map((w) => w.id)));
        return works;
      })
      .catch(() => []);
  }, [username]);

  const refreshChatUnread = useCallback(() => {
    if (!username) {
      setChatUnread(0);
      return;
    }
    chatApi.unreadCount().then((r) => setChatUnread(r.unread)).catch(() => {});
  }, [username]);

  useEffect(() => {
    refreshFavorites();
    refreshChatUnread();
  }, [refreshFavorites, refreshChatUnread]);

  const toggleFavorite = useCallback(async (workId) => {
    const saved = favIds.has(workId);
    if (saved) await favoritesApi.remove(workId);
    else await favoritesApi.add(workId);
    setFavIds((prev) => {
      const next = new Set(prev);
      if (saved) next.delete(workId);
      else next.add(workId);
      return next;
    });
  }, [favIds]);

  return (
    <AppContext.Provider value={{ favIds, toggleFavorite, refreshFavorites, chatUnread, refreshChatUnread }}>
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp deve essere usato dentro AppProvider');
  return ctx;
}
