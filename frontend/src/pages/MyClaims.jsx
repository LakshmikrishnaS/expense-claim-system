import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { claimApi } from "../api/services";
import { Empty, ErrorBanner, Spinner } from "../components/Feedback";
import Pagination from "../components/Pagination";
import StatusBadge from "../components/StatusBadge";
import { STATUSES, errMsg, formatDate, formatMoney, label } from "../utils";

export default function MyClaims() {
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // debounce the search box
  useEffect(() => {
    const t = setTimeout(() => { setQuery(search); setPage(0); }, 350);
    return () => clearTimeout(t);
  }, [search]);

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    claimApi.list({ page, size: 8, search: query, status })
      .then(setData)
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false));
  }, [page, query, status]);
  useEffect(load, [load]);

  const remove = async (c) => {
    if (!window.confirm(`Delete "${c.title}"? This cannot be undone.`)) return;
    try {
      await claimApi.remove(c.id);
      load();
    } catch (e) {
      setError(errMsg(e));
    }
  };

  return (
    <>
      <div className="page-head">
        <h1>My claims</h1>
        <Link className="btn btn-primary" to="/claims/new">New claim</Link>
      </div>

      <div className="filters">
        <input placeholder="Search title or description" value={search} onChange={(e) => setSearch(e.target.value)} />
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>

      <ErrorBanner message={error} onRetry={load} />
      {loading && !data ? <Spinner /> : (
        <div className={loading ? "dim" : ""}>
          {data && data.content.length === 0 ? (
            <Empty title="No claims found">
              {query || status ? "Try a different search or filter." : "Create your first claim to get started."}
            </Empty>
          ) : data && (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr><th>Title</th><th>Category</th><th>Date</th><th className="num">Amount</th><th>Status</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  {data.content.map((c) => (
                    <tr key={c.id}>
                      <td><Link to={`/claims/${c.id}`}>{c.title}</Link></td>
                      <td>{label(c.category)}</td>
                      <td>{formatDate(c.expenseDate)}</td>
                      <td className="num">{formatMoney(c.amount)}</td>
                      <td><StatusBadge status={c.status} /></td>
                      <td className="actions">
                        <Link className="btn btn-small" to={`/claims/${c.id}`}>View</Link>
                        {c.status === "PENDING" && (
                          <>
                            <Link className="btn btn-small" to={`/claims/${c.id}/edit`}>Edit</Link>
                            <button className="btn btn-small btn-danger" onClick={() => remove(c)}>Delete</button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {data && <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />}
        </div>
      )}
    </>
  );
}
