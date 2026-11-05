import { formatDateTime, label } from "../utils";

const VERBS = {
  CREATED: "created the claim",
  UPDATED: "edited the claim",
  DELETED: "deleted the claim",
  RECEIPT_UPLOADED: "attached a receipt",
  APPROVED: "approved the claim",
  REJECTED: "rejected the claim",
};

export default function Timeline({ logs }) {
  if (!logs.length) return <p className="muted">No activity yet.</p>;
  return (
    <ol className="timeline">
      {logs.map((l) => (
        <li key={l.id} className={`tl-item tl-${l.action.toLowerCase()}`}>
          <div className="tl-head">
            <strong>{l.actorName}</strong> <span className="muted">({label(l.actorRole)})</span>{" "}
            {VERBS[l.action] || label(l.action)}
          </div>
          {l.details && <div className="tl-details">{l.details}</div>}
          <time className="muted">{formatDateTime(l.createdAt)}</time>
        </li>
      ))}
    </ol>
  );
}
