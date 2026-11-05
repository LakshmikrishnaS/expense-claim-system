import { label } from "../utils";

export default function StatusBadge({ status }) {
  return <span className={`badge badge-${status?.toLowerCase()}`}>{label(status)}</span>;
}
