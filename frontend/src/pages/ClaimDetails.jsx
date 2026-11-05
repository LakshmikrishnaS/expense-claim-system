import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { claimApi, managerApi } from "../api/services";
import { useAuth } from "../context/AuthContext";
import { ErrorBanner, Notice, Spinner } from "../components/Feedback";
import StatusBadge from "../components/StatusBadge";
import Timeline from "../components/Timeline";
import { errMsg, formatDate, formatDateTime, formatMoney, label, validateReceipt } from "../utils";

export default function ClaimDetails() {
  const { id } = useParams();
  const { isManager } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [claim, setClaim] = useState(null);
  const [logs, setLogs] = useState([]);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(location.state?.notice || "");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [comment, setComment] = useState("");
  const [commentError, setCommentError] = useState("");
  const [preview, setPreview] = useState(null); // { url, type }
  const fileInput = useRef(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [c, l] = await Promise.all([claimApi.get(id), claimApi.auditLogs(id)]);
      setClaim(c);
      setLogs(l);
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setLoading(false);
    }
  }, [id]);
  useEffect(() => { load(); }, [load]);

  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview.url); }, [preview]);

  const run = async (fn) => {
    setBusy(true);
    setError("");
    try {
      await fn();
      await load();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  };

  const approve = () => run(async () => { await managerApi.approve(id, ""); setNotice("Claim approved."); });

  const reject = () => {
    if (!comment.trim()) { setCommentError("A comment is required when rejecting"); return; }
    setCommentError("");
    run(async () => { await managerApi.reject(id, comment.trim()); setRejecting(false); setComment(""); setNotice("Claim rejected."); });
  };

  const remove = async () => {
    if (!window.confirm("Delete this claim? This cannot be undone.")) return;
    setBusy(true);
    try {
      await claimApi.remove(id);
      navigate("/claims", { replace: true });
    } catch (e) {
      setError(errMsg(e));
      setBusy(false);
    }
  };

  const viewReceipt = async () => {
    setError("");
    try {
      const blob = await claimApi.receiptBlob(id);
      setPreview({ url: URL.createObjectURL(blob), type: blob.type });
    } catch (e) {
      setError("Could not load the receipt.");
    }
  };

  const downloadReceipt = async () => {
    setError("");
    try {
      const blob = await claimApi.receiptBlob(id, true);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = claim.receiptName || "receipt";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError("Could not download the receipt.");
    }
  };

  const onPickFile = (ev) => {
    const file = ev.target.files[0];
    ev.target.value = "";
    if (!file) return;
    const problem = validateReceipt(file);
    if (problem) { setError(problem); return; }
    run(async () => { await claimApi.uploadReceipt(id, file); setPreview(null); setNotice("Receipt uploaded."); });
  };

  if (loading && !claim) return <Spinner />;
  if (!claim) {
    return (
      <>
        <ErrorBanner message={error || "Claim not found"} onRetry={load} />
        <Link to={isManager ? "/manager/claims" : "/claims"}>Back to claims</Link>
      </>
    );
  }

  const pending = claim.status === "PENDING";
  const back = isManager ? "/manager/claims" : "/claims";

  return (
    <>
      <p><Link to={back}>&larr; Back to claims</Link></p>
      <ErrorBanner message={error} />
      <Notice message={notice} />

      <div className="two-col wide-left">
        <section className="card">
          <div className="page-head">
            <h1>{claim.title}</h1>
            <StatusBadge status={claim.status} />
          </div>

          <dl className="details">
            <dt>Amount</dt><dd><strong>{formatMoney(claim.amount)}</strong></dd>
            <dt>Category</dt><dd>{label(claim.category)}</dd>
            <dt>Expense date</dt><dd>{formatDate(claim.expenseDate)}</dd>
            <dt>Employee</dt><dd>{claim.employeeName}</dd>
            <dt>Submitted</dt><dd>{formatDateTime(claim.createdAt)}</dd>
            <dt>Description</dt><dd>{claim.description || <span className="muted">None</span>}</dd>
            {claim.reviewedByName && (
              <>
                <dt>Reviewed by</dt><dd>{claim.reviewedByName} on {formatDateTime(claim.reviewedAt)}</dd>
                <dt>Manager comment</dt><dd>{claim.reviewComment || <span className="muted">None</span>}</dd>
              </>
            )}
          </dl>

          <h2>Receipt</h2>
          {claim.hasReceipt ? (
            <div className="actions">
              <span>{claim.receiptName}</span>
              <button className="btn btn-small" onClick={viewReceipt}>View</button>
              <button className="btn btn-small" onClick={downloadReceipt}>Download</button>
            </div>
          ) : <p className="muted">No receipt attached.</p>}

          {preview && (
            <div className="preview">
              {preview.type === "application/pdf"
                ? <iframe title="Receipt" src={preview.url} />
                : <img alt="Receipt" src={preview.url} />}
              <button className="btn btn-small" onClick={() => setPreview(null)}>Close preview</button>
            </div>
          )}

          {!isManager && pending && (
            <div className="actions top-gap">
              <input ref={fileInput} type="file" hidden accept="image/jpeg,image/png,application/pdf" onChange={onPickFile} />
              <button className="btn btn-small" disabled={busy} onClick={() => fileInput.current.click()}>
                {claim.hasReceipt ? "Replace receipt" : "Attach receipt"}
              </button>
            </div>
          )}

          {!isManager && pending && (
            <div className="actions top-gap">
              <Link className="btn" to={`/claims/${claim.id}/edit`}>Edit claim</Link>
              <button className="btn btn-danger" disabled={busy} onClick={remove}>Delete claim</button>
            </div>
          )}
          {!isManager && !pending && <p className="muted top-gap">This claim has been reviewed and can no longer be changed.</p>}

          {isManager && pending && (
            <div className="review top-gap">
              <h2>Review</h2>
              {rejecting ? (
                <>
                  <label>Reason for rejection (required)
                    <textarea rows={3} value={comment} onChange={(e) => setComment(e.target.value)} />
                    {commentError && <span className="field-error">{commentError}</span>}
                  </label>
                  <div className="actions">
                    <button className="btn btn-danger" disabled={busy} onClick={reject}>{busy ? "Rejecting..." : "Confirm rejection"}</button>
                    <button className="btn" disabled={busy} onClick={() => { setRejecting(false); setCommentError(""); }}>Cancel</button>
                  </div>
                </>
              ) : (
                <div className="actions">
                  <button className="btn btn-primary" disabled={busy} onClick={approve}>{busy ? "Working..." : "Approve"}</button>
                  <button className="btn btn-danger" disabled={busy} onClick={() => setRejecting(true)}>Reject</button>
                </div>
              )}
            </div>
          )}
          {isManager && !pending && <p className="muted top-gap">This claim has been reviewed and is final.</p>}
        </section>

        <section className="card">
          <h2>Activity</h2>
          <Timeline logs={logs} />
        </section>
      </div>
    </>
  );
}
