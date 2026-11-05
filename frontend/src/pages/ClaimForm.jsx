import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { claimApi } from "../api/services";
import { ErrorBanner, Spinner } from "../components/Feedback";
import { CATEGORIES, errMsg, label, todayISO, validateReceipt } from "../utils";

const empty = { title: "", amount: "", category: "", expenseDate: todayISO(), description: "" };

export default function ClaimForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const [form, setForm] = useState(empty);
  const [file, setFile] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isEdit) return;
    claimApi.get(id)
      .then((c) => {
        if (c.status !== "PENDING") {
          navigate(`/claims/${id}`, { replace: true, state: { notice: "Only pending claims can be edited." } });
          return;
        }
        setForm({ title: c.title, amount: String(c.amount), category: c.category, expenseDate: c.expenseDate, description: c.description || "" });
      })
      .catch((e) => setError(errMsg(e)))
      .finally(() => setLoading(false));
  }, [id, isEdit, navigate]);

  const validate = () => {
    const e = {};
    if (!form.title.trim()) e.title = "Title is required";
    else if (form.title.length > 150) e.title = "Title must be at most 150 characters";
    const amt = Number(form.amount);
    if (form.amount === "") e.amount = "Amount is required";
    else if (!(amt > 0)) e.amount = "Amount must be greater than 0";
    else if (!/^\d+(\.\d{1,2})?$/.test(form.amount)) e.amount = "Use at most 2 decimals";
    if (!form.category) e.category = "Choose a category";
    if (!form.expenseDate) e.expenseDate = "Expense date is required";
    else if (form.expenseDate > todayISO()) e.expenseDate = "Expense date cannot be in the future";
    if (form.description.length > 1000) e.description = "Description must be at most 1000 characters";
    const fe = validateReceipt(file);
    if (fe) e.file = fe;
    return e;
  };

  const submit = async (ev) => {
    ev.preventDefault();
    const e = validate();
    setFieldErrors(e);
    if (Object.keys(e).length) return;
    setSaving(true);
    setError("");
    const payload = { ...form, title: form.title.trim(), amount: Number(form.amount) };
    try {
      const saved = isEdit ? await claimApi.update(id, payload) : await claimApi.create(payload);
      if (file) {
        try {
          await claimApi.uploadReceipt(saved.id, file);
        } catch (uploadErr) {
          navigate(`/claims/${saved.id}`, { state: { notice: `Claim saved, but the receipt was not uploaded: ${errMsg(uploadErr)}` } });
          return;
        }
      }
      navigate(`/claims/${saved.id}`);
    } catch (err) {
      setError(errMsg(err));
      setSaving(false);
    }
  };

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  if (loading) return <Spinner />;

  return (
    <form className="card form-card" onSubmit={submit} noValidate>
      <h1>{isEdit ? "Edit claim" : "New claim"}</h1>
      <ErrorBanner message={error} />

      <label>Title
        <input value={form.title} onChange={set("title")} maxLength={150} />
        {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
      </label>

      <div className="row">
        <label>Amount
          <input inputMode="decimal" value={form.amount} onChange={set("amount")} placeholder="0.00" />
          {fieldErrors.amount && <span className="field-error">{fieldErrors.amount}</span>}
        </label>
        <label>Category
          <select value={form.category} onChange={set("category")}>
            <option value="">Select...</option>
            {CATEGORIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}
          </select>
          {fieldErrors.category && <span className="field-error">{fieldErrors.category}</span>}
        </label>
        <label>Expense date
          <input type="date" max={todayISO()} value={form.expenseDate} onChange={set("expenseDate")} />
          {fieldErrors.expenseDate && <span className="field-error">{fieldErrors.expenseDate}</span>}
        </label>
      </div>

      <label>Description
        <textarea rows={4} value={form.description} onChange={set("description")} maxLength={1000} />
        {fieldErrors.description && <span className="field-error">{fieldErrors.description}</span>}
      </label>

      <label>Receipt (optional, JPEG, PNG or PDF, up to 5 MB)
        <input type="file" accept="image/jpeg,image/png,application/pdf" onChange={(e) => setFile(e.target.files[0] || null)} />
        {fieldErrors.file && <span className="field-error">{fieldErrors.file}</span>}
      </label>

      <div className="form-actions">
        <button className="btn btn-primary" disabled={saving}>{saving ? "Saving..." : isEdit ? "Save changes" : "Submit claim"}</button>
        <Link className="btn" to={isEdit ? `/claims/${id}` : "/claims"}>Cancel</Link>
      </div>
    </form>
  );
}
