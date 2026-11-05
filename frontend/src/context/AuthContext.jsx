import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { authApi } from "../api/services";
import { tokens } from "../api/axios";

const AuthContext = createContext(null);

function readUser() {
  try {
    return tokens.access() ? JSON.parse(localStorage.getItem("user")) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);

  const login = useCallback(async (email, password) => {
    const data = await authApi.login({ email, password });
    tokens.save(data);
    setUser(data.user);
    return data.user;
  }, []);

  const register = useCallback(async (name, email, password) => {
    const data = await authApi.register({ name, email, password });
    tokens.save(data);
    setUser(data.user);
    return data.user;
  }, []);

  const logout = useCallback(async () => {
    const refresh = tokens.refresh();
    tokens.clear();
    setUser(null);
    if (refresh) {
      try {
        await authApi.logout(refresh);
      } catch {
        /* the token is already removed locally */
      }
    }
  }, []);

  // Fired by the Axios interceptor when a token refresh fails
  useEffect(() => {
    const onForcedLogout = () => setUser(null);
    window.addEventListener("auth:logout", onForcedLogout);
    return () => window.removeEventListener("auth:logout", onForcedLogout);
  }, []);

  const value = useMemo(
    () => ({ user, isManager: user?.role === "MANAGER", login, register, logout }),
    [user, login, register, logout]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
