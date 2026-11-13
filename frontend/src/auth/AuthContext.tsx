import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { clearAuth, getAccessToken, subscribeAuth } from "../api/client";

type AuthContextValue = { authenticated: boolean; logout: () => void };
const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authenticated, setAuthenticated] = useState(Boolean(getAccessToken()));

  useEffect(
    () => {
      const unsubscribe = subscribeAuth(() => setAuthenticated(Boolean(getAccessToken())));
      return () => {
        unsubscribe();
      };
    },
    [],
  );

  const value = useMemo(
    () => ({ authenticated, logout: clearAuth }),
    [authenticated],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used within AuthProvider");
  return value;
}
