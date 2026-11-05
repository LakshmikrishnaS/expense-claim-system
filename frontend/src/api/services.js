import api from "./axios";

const clean = (params) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== "" && v !== null && v !== undefined));

export const authApi = {
  login: (data) => api.post("/auth/login", data).then((r) => r.data),
  register: (data) => api.post("/auth/register", data).then((r) => r.data),
  logout: (refreshToken) => api.post("/auth/logout", { refreshToken }),
};

export const claimApi = {
  list: (params) => api.get("/claims", { params: clean(params) }).then((r) => r.data),
  get: (id) => api.get(`/claims/${id}`).then((r) => r.data),
  create: (data) => api.post("/claims", data).then((r) => r.data),
  update: (id, data) => api.put(`/claims/${id}`, data).then((r) => r.data),
  remove: (id) => api.delete(`/claims/${id}`),
  auditLogs: (id) => api.get(`/claims/${id}/audit-logs`).then((r) => r.data),
  uploadReceipt: (id, file) => {
    const form = new FormData();
    form.append("file", file);
    return api.post(`/claims/${id}/receipt`, form).then((r) => r.data);
  },
  receiptBlob: (id, download = false) =>
    api.get(`/claims/${id}/receipt`, { params: { download }, responseType: "blob" }).then((r) => r.data),
};

export const managerApi = {
  claims: (params) => api.get("/manager/claims", { params: clean(params) }).then((r) => r.data),
  approve: (id, comment) => api.patch(`/manager/claims/${id}/approve`, { comment }).then((r) => r.data),
  reject: (id, comment) => api.patch(`/manager/claims/${id}/reject`, { comment }).then((r) => r.data),
  exportCsv: (params) =>
    api.get("/manager/claims/export", { params: clean(params), responseType: "blob" }).then((r) => r.data),
  users: (params) => api.get("/manager/users", { params }).then((r) => r.data),
  setUserActive: (id, active) => api.patch(`/manager/users/${id}/status`, { active }).then((r) => r.data),
};

export const dashboardApi = {
  employee: () => api.get("/dashboard/employee").then((r) => r.data),
  manager: () => api.get("/dashboard/manager").then((r) => r.data),
};
