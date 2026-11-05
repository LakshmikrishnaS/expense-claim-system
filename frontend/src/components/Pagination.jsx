export default function Pagination({ page, totalPages, totalElements, onChange }) {
  if (!totalPages || totalPages <= 1) {
    return totalElements ? <div className="pager muted">{totalElements} total</div> : null;
  }
  return (
    <div className="pager">
      <button className="btn btn-small" disabled={page === 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <span>
        Page {page + 1} of {totalPages} ({totalElements} total)
      </span>
      <button className="btn btn-small" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Next
      </button>
    </div>
  );
}
