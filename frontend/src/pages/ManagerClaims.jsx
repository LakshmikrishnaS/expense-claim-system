import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { managerApi } from "../api/services";
import { Empty, ErrorBanner, Spinner } from "../components/Feedback";
import Pagination from "../components/Pagination";
import StatusBadge from "../components/StatusBadge";
import { STATUSES, errMsg, formatDate, formatMoney, label } from "../utils";

export default function ManagerClaims() {
  const [filters, setFilters] = useState({ status: "", employeeId: "", from: "", to: "" });
  const [employees, setEmployees] = useState([]);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    managerApi.users({ page: 0, size: 200 })
      .then((r) => setEmployees(r.content.filter((u) => u.role === "EMPLOYEE")))
      .catch(() => {});
  }, []);

  const load = useCallback(() => {
    setLoading(true);
    setError("");
    managerApi.claims({ ...filters, page, size: 10 })
      .then(setData)
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false));
  }, [filters, page]);
  useEffect(load, [load]);

  const change = (k) => (e) => { setFilters({ ...filters, [k]: e.target.value }); setPage(0); };
  const dateProblem = filters.from && filters.to && filters.from > filters.to ? "'From' date must be before 'To' date" : "";

  const exportCsv = async () => {
    setExporting(true);
    setError("");
    try {
      const blob = await managerApi.exportCsv(filters);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "claims.csv";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError("Could not export claims.");
    } finally {
      setExporting(false);
    }
  };

  return (
    <>
      <div className="page-head">
        <h1>All claims</h1>
        <button className="btn" onClick={exportCsv} disabled={exporting}>{exporting ? "Exporting..." : "Export CSV"}</button>
      </div>

      <div className="filters">
        <select value={filters.status} onChange={change("status")}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        <select value={filters.employeeId} onChange={change("employeeId")}>
          <option value="">All employees</option>
          {employees.map((u) => <option key={u.id} value={u.id}>{u.name}</option>)}
        </select>
        <label className="inline">From <input type="date" value={filters.from} onChange={change("from")} /></label>
        <label className="inline">To <input type="date" value={filters.to} onChange={change("to")} /></label>
        <button className="btn btn-small" onClick={() => { setFilters({ status: "", employeeId: "", from: "", to: "" }); setPage(0); }}>Clear</button>
      </div>
      {dateProblem && <div className="field-error">{dateProblem}</div>}

      <ErrorBanner message={error} onRetry={load} />
      {loading && !data ? <Spinner /> : (
        <div className={loading ? "dim" : ""}>
          {data && data.content.length === 0 ? (
            <Empty title="No claims match these filters" />
          ) : data && (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr><th>Employee</th><th>Title</th><th>Category</th><th>Date</th><th className="num">Amount</th><th>Status</th><th></th></tr>
                </thead>
                <tbody>
                  {data.content.map((c) => (
                    <tr key={c.id}>
                      <td>{c.employeeName}</td>
                      <td><Link to={`/claims/${c.id}`}>{c.title}</Link></td>
                      <td>{label(c.category)}</td>
                      <td>{formatDate(c.expenseDate)}</td>
                      <td className="num">{formatMoney(c.amount)}</td>
                      <td><StatusBadge status={c.status} /></td>
                      <td className="actions"><Link className="btn btn-small" to={`/claims/${c.id}`}>{c.status === "PENDING" ? "Review" : "View"}</Link></td>
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
