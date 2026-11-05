import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { dashboardApi } from "../api/services";
import { ErrorBanner, Spinner } from "../components/Feedback";
import { errMsg, formatMoney } from "../utils";

export default function EmployeeDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    setError("");
    setData(null);
    dashboardApi.employee().then(setData).catch((e) => setError(errMsg(e)));
  }, []);
  useEffect(load, [load]);

  if (error) return <ErrorBanner message={error} onRetry={load} />;
  if (!data) return <Spinner />;

  const cards = [
    ["Total claimed", data.totalClaimed, "total"],
    ["Approved", data.totalApproved, "approved"],
    ["Pending", data.totalPending, "pending"],
    ["Rejected", data.totalRejected, "rejected"],
  ];
  return (
    <>
      <div className="page-head">
        <h1>My dashboard</h1>
        <Link className="btn btn-primary" to="/claims/new">New claim</Link>
      </div>
      <div className="stat-grid">
        {cards.map(([title, value, tone]) => (
          <div key={title} className={`stat stat-${tone}`}>
            <span className="muted">{title}</span>
            <strong>{formatMoney(value)}</strong>
          </div>
        ))}
      </div>
      <p className="muted">{data.claimCount} claim{data.claimCount === 1 ? "" : "s"} in total.</p>
    </>
  );
}
