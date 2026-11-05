import axios from "axios";

export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

export const tokens = {
  access: () => localStorage.getItem("accessToken"),
  refresh: () => localStorage.getItem("refreshToken"),
  save: ({ accessToken, refreshToken, user }) => {
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
    if (user) localStorage.setItem("user", JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
  },
};

const api = axios.create({ baseURL: API_URL });

// 1) Attach the access token to every request
api.interceptors.request.use((config) => {
  const token = tokens.access();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 2) On 401: refresh once (shared by parallel requests), retry; if refresh fails, log out
let refreshPromise = null;

function refreshTokens() {
  if (!refreshPromise) {
    // plain axios (not `api`) so this call can never loop through the interceptor
    refreshPromise = axios
      .post(`${API_URL}/auth/refresh`, { refreshToken: tokens.refresh() })
      .then((res) => {
        tokens.save(res.data);
        return res.data.accessToken;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const isAuthCall = original?.url?.includes("/auth/");
    if (error.response?.status === 401 && original && !original._retry && !isAuthCall && tokens.refresh()) {
      original._retry = true;
      try {
        const newToken = await refreshTokens();
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      } catch (refreshError) {
        tokens.clear();
        window.dispatchEvent(new Event("auth:logout"));
        return Promise.reject(refreshError);
      }
    }
    if (error.response?.status === 401 && !isAuthCall && !tokens.refresh()) {
      window.dispatchEvent(new Event("auth:logout"));
    }
    return Promise.reject(error);
  }
);

export default api;
