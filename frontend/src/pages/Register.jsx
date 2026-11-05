import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { ErrorBanner } from "../components/Feedback";
import { errMsg } from "../utils";

export default function Register() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const validate = () => {
    const e = {};
    if (!form.name.trim()) e.name = "Name is required";
    if (!form.email.trim()) e.email = "Email is required";
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) e.email = "Enter a valid email";
    if (form.password.length < 6) e.password = "Password must be at least 6 characters";
    if (form.confirm !== form.password) e.confirm = "Passwords do not match";
    return e;
  };

  const submit = async (ev) => {
    ev.preventDefault();
    const e = validate();
    setFieldErrors(e);
    if (Object.keys(e).length) return;
    setLoading(true);
    setError("");
    try {
      await register(form.name.trim(), form.email.trim(), form.password);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(errMsg(err, "Registration failed"));
    } finally {
      setLoading(false);
    }
  };

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  return (
    <div className="auth-wrap">
      <form className="card narrow" onSubmit={submit} noValidate>
        <h1>Create account</h1>
        <p className="muted">New accounts are employee accounts.</p>
        <ErrorBanner message={error} />
        <label>Full name
          <input value={form.name} onChange={set("name")} />
          {fieldErrors.name && <span className="field-error">{fieldErrors.name}</span>}
        </label>
        <label>Email
          <input type="email" value={form.email} onChange={set("email")} autoComplete="username" />
          {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
        </label>
        <label>Password
          <input type="password" value={form.password} onChange={set("password")} autoComplete="new-password" />
          {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
        </label>
        <label>Confirm password
          <input type="password" value={form.confirm} onChange={set("confirm")} autoComplete="new-password" />
          {fieldErrors.confirm && <span className="field-error">{fieldErrors.confirm}</span>}
        </label>
        <button className="btn btn-primary" disabled={loading}>{loading ? "Creating account..." : "Create account"}</button>
        <p className="muted small">Already registered? <Link to="/login">Log in</Link></p>
      </form>
    </div>
  );
}
