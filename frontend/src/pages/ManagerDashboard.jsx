import { useCallback, useEffect, useState } from "react";
import { dashboardApi } from "../api/services";
import { ErrorBanner, Spinner } from "../components/Feedback";
import { errMsg, formatMoney, label } from "../utils";

export default function ManagerDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    setError("");
    setData(null);
    dashboardApi.manager().then(setData).catch((e) => setError(errMsg(e)));
  }, []);
  useEffect(load, [load]);

  if (error) return <ErrorBanner message={error} onRetry={load} />;
  if (!data) return <Spinner />;

  const maxCat = Math.max(1, ...data.totalsByCategory.map((c) => Number(c.total)));

  return (
    <>
      <h1>Manager dashboard</h1>
      <div className="stat-grid">
        {Object.entries(data.countsByStatus).map(([status, count]) => (
          <div key={status} className={`stat stat-${status.toLowerCase()}`}>
            <span className="muted">{label(status)} claims</span>
            <strong>{count}</strong>
          </div>
        ))}
      </div>

      <div className="two-col">
        <section className="card">
          <h2>Total by category</h2>
          {data.totalsByCategory.length === 0 && <p className="muted">No claims yet.</p>}
          {data.totalsByCategory.map((c) => (
            <div key={c.category} className="bar-row">
              <span className="bar-label">{label(c.category)}</span>
              <div className="bar-track"><div className="bar-fill" style={{ width: `${(Number(c.total) / maxCat) * 100}%` }} /></div>
              <span className="bar-value">{formatMoney(c.total)}</span>
            </div>
          ))}
        </section>

        <section className="card">
          <h2>Monthly summary</h2>
          {data.monthlySummary.length === 0 ? (
            <p className="muted">No claims yet.</p>
          ) : (
            <table className="table">
              <thead><tr><th>Month</th><th className="num">Claims</th><th className="num">Total</th></tr></thead>
              <tbody>
                {data.monthlySummary.map((m) => (
                  <tr key={m.month}><td>{m.month}</td><td className="num">{m.count}</td><td className="num">{formatMoney(m.total)}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </>
  );
}
