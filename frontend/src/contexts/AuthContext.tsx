import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authApi, type AuthUser, setAuthToken } from '@/lib/api';

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * Wraps the app and provides auth state.
 * On mount it calls GET /auth/me to restore session from the httpOnly cookie
 * (so a page refresh keeps the user logged in as long as the cookie is valid).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser]       = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Try to restore session on mount
    const restoreSession = async () => {
      try {
        const userData = await authApi.me();
        console.log('[Auth] Session restored:', userData);
        setUser(userData);
      } catch (err) {
        // Expected if not logged in - just set user to null
        console.log('[Auth] No session found (expected on first load)');
        setUser(null);
      } finally {
        console.log('[Auth] Auth initialization complete');
        setIsLoading(false);
      }
    };

    restoreSession();
  }, []);

  const login = async (username: string, password: string) => {
    const userData = await authApi.login(username, password);
    setUser(userData);
  };

  const logout = async () => {
    await authApi.logout().catch(() => {});
    setAuthToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
