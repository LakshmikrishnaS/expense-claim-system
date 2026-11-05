import { Link } from "react-router-dom";

export default function AccessDenied() {
  return (
    <div className="card narrow center">
      <h1>Access denied</h1>
      <p className="muted">Your role does not have permission to open this page.</p>
      <Link className="btn btn-primary" to="/">
        Back to my dashboard
      </Link>
    </div>
  );
}
