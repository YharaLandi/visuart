import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { account as accountApi, auth as authApi, currentToken, isRemembered, setToken } from '../api/client';

const AuthContext = createContext(null);
const USER_KEY = 'visuart_user';

function toUser(data) {
  return {
    username: data.username,
    email: data.email,
    displayName: data.displayName,
    role: data.role,
    bio: data.bio ?? null,
    createdAt: data.createdAt ?? null,
    hasAvatar: data.hasAvatar ?? false,
    avatarVersion: data.avatarVersion ?? null,
  };
}

function storage() {
  return isRemembered() ? localStorage : sessionStorage;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem(USER_KEY) ?? sessionStorage.getItem(USER_KEY);
    return saved && currentToken() ? JSON.parse(saved) : null;
  });

  const saveUser = useCallback((u) => {
    storage().setItem(USER_KEY, JSON.stringify(u));
    setUser(u);
  }, []);

  const persist = useCallback((response, remember = isRemembered()) => {
    setToken(response.token, remember);
    localStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(USER_KEY);
    saveUser(toUser(response));
  }, [saveUser]);

  const clear = useCallback(() => {
    setToken(null);
    localStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  // All'avvio verifica che la sessione salvata sia ancora valida (può essere stata revocata
  // da un logout altrove, da un cambio password o da un admin) e riallinea i dati del profilo.
  useEffect(() => {
    if (!currentToken()) {
      clear();
      return;
    }
    accountApi.me()
      .then((me) => saveUser(toUser(me)))
      .catch((e) => {
        // solo una sessione rifiutata fa uscire; un server spento no
        if (e.status === 401 || e.status === 403) clear();
      });
  }, [clear, saveUser]);

  const login = useCallback(async (email, password, remember) => {
    persist(await authApi.login(email, password), remember);
  }, [persist]);

  const register = useCallback(async (email, password, displayName) => {
    persist(await authApi.register(email, password, displayName), true);
  }, [persist]);

  /** Revoca il token sul server; anche se la chiamata fallisce, l'utente esce in locale. */
  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // token già scaduto o server irraggiungibile
    }
    clear();
  }, [clear]);

  return (
    <AuthContext.Provider value={{ user, login, register, logout, persist, saveUser, clear }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth deve essere usato dentro AuthProvider');
  }
  return ctx;
}
