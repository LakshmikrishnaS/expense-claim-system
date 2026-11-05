import { useCallback, useEffect, useState } from "react";
import { managerApi } from "../api/services";
import { useAuth } from "../context/AuthContext";
import { ErrorBanner, Spinner } from "../components/Feedback";
import Pagination from "../components/Pagination";
import { errMsg, formatDate, label } from "../utils";

export default function Users() {
  const { user: me } = useAuth();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    managerApi.users({ page, size: 10 })
      .then(setData)
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false));
  }, [page]);
  useEffect(load, [load]);

  const toggle = async (u) => {
    const action = u.active ? "deactivate" : "activate";
    if (!window.confirm(`Do you want to ${action} ${u.name}?`)) return;
    setBusyId(u.id);
    setError("");
    try {
      await managerApi.setUserActive(u.id, !u.active);
      load();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      <h1>Users</h1>
      <ErrorBanner message={error} onRetry={load} />
      {loading && !data ? <Spinner /> : data && (
        <div className={loading ? "dim" : ""}>
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {data.content.map((u) => (
                  <tr key={u.id}>
                    <td>{u.name}</td>
                    <td>{u.email}</td>
                    <td>{label(u.role)}</td>
                    <td>{formatDate(u.createdAt)}</td>
                    <td><span className={`badge ${u.active ? "badge-approved" : "badge-rejected"}`}>{u.active ? "Active" : "Deactivated"}</span></td>
                    <td className="actions">
                      {u.id !== me.id && (
                        <button className={`btn btn-small ${u.active ? "btn-danger" : ""}`} disabled={busyId === u.id} onClick={() => toggle(u)}>
                          {u.active ? "Deactivate" : "Activate"}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />
        </div>
      )}
    </>
  );
}
