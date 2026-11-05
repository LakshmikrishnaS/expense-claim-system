import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Layout() {
  const { user, isManager, logout } = useAuth();
  const navigate = useNavigate();

  const links = isManager
    ? [
        ["/manager/dashboard", "Dashboard"],
        ["/manager/claims", "All claims"],
        ["/manager/users", "Users"],
      ]
    : [
        ["/dashboard", "Dashboard"],
        ["/claims", "My claims"],
        ["/claims/new", "New claim"],
      ];

  const onLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <span className="brand">Expense Claims</span>
          <nav className="nav">
            {links.map(([to, text]) => (
              <NavLink key={to} to={to} end className={({ isActive }) => (isActive ? "active" : "")}>
                {text}
              </NavLink>
            ))}
          </nav>
          <div className="who">
            <span>
              {user.name} <small className="role-tag">{isManager ? "Manager" : "Employee"}</small>
            </span>
            <button className="btn btn-ghost" onClick={onLogout}>
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  );
}
