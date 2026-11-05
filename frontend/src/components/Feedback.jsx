export function Spinner({ text = "Loading..." }) {
  return (
    <div className="center-note" role="status">
      <span className="spinner" aria-hidden="true" /> {text}
    </div>
  );
}

export function ErrorBanner({ message, onRetry }) {
  if (!message) return null;
  return (
    <div className="alert alert-error" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button className="btn btn-small" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function Notice({ message }) {
  if (!message) return null;
  return <div className="alert alert-info">{message}</div>;
}

export function Empty({ title, children }) {
  return (
    <div className="center-note">
      <strong>{title}</strong>
      {children && <div className="muted">{children}</div>}
    </div>
  );
}
