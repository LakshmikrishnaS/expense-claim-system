import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import { useAuth } from "./context/AuthContext";
import Login from "./pages/Login";
import Register from "./pages/Register";
import AccessDenied from "./pages/AccessDenied";
import NotFound from "./pages/NotFound";
import EmployeeDashboard from "./pages/EmployeeDashboard";
import ManagerDashboard from "./pages/ManagerDashboard";
import MyClaims from "./pages/MyClaims";
import ClaimForm from "./pages/ClaimForm";
import ClaimDetails from "./pages/ClaimDetails";
import ManagerClaims from "./pages/ManagerClaims";
import Users from "./pages/Users";

function Home() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={user.role === "MANAGER" ? "/manager/dashboard" : "/dashboard"} replace />;
}

const EMPLOYEE = ["EMPLOYEE"];
const MANAGER = ["MANAGER"];
const ANY = ["EMPLOYEE", "MANAGER"];

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/access-denied" element={<AccessDenied />} />

        <Route path="/dashboard" element={<ProtectedRoute roles={EMPLOYEE}><EmployeeDashboard /></ProtectedRoute>} />
        <Route path="/claims" element={<ProtectedRoute roles={EMPLOYEE}><MyClaims /></ProtectedRoute>} />
        <Route path="/claims/new" element={<ProtectedRoute roles={EMPLOYEE}><ClaimForm /></ProtectedRoute>} />
        <Route path="/claims/:id/edit" element={<ProtectedRoute roles={EMPLOYEE}><ClaimForm /></ProtectedRoute>} />
        <Route path="/claims/:id" element={<ProtectedRoute roles={ANY}><ClaimDetails /></ProtectedRoute>} />

        <Route path="/manager/dashboard" element={<ProtectedRoute roles={MANAGER}><ManagerDashboard /></ProtectedRoute>} />
        <Route path="/manager/claims" element={<ProtectedRoute roles={MANAGER}><ManagerClaims /></ProtectedRoute>} />
        <Route path="/manager/users" element={<ProtectedRoute roles={MANAGER}><Users /></ProtectedRoute>} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
