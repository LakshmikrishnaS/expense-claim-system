import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { ErrorBanner } from "../components/Feedback";
import { errMsg } from "../utils";

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const validate = () => {
    const e = {};
    if (!form.email.trim()) e.email = "Email is required";
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) e.email = "Enter a valid email";
    if (!form.password) e.password = "Password is required";
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
      const u = await login(form.email.trim(), form.password);
      const from = location.state?.from;
      navigate(from || (u.role === "MANAGER" ? "/manager/dashboard" : "/dashboard"), { replace: true });
    } catch (err) {
      setError(errMsg(err, "Login failed"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrap">
      <form className="card narrow" onSubmit={submit} noValidate>
        <h1>Log in</h1>
        <p className="muted">Expense Claim Management</p>
        <ErrorBanner message={error} />
        <label>
          Email
          <input type="email" value={form.email} autoComplete="username"
                 onChange={(e) => setForm({ ...form, email: e.target.value })} />
          {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
        </label>
        <label>
          Password
          <input type="password" value={form.password} autoComplete="current-password"
                 onChange={(e) => setForm({ ...form, password: e.target.value })} />
          {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
        </label>
        <button className="btn btn-primary" disabled={loading}>{loading ? "Logging in..." : "Log in"}</button>
        <p className="muted small">
          New here? <Link to="/register">Create an employee account</Link>
        </p>
      </form>
    </div>
  );
}
