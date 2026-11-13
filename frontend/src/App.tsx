import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { AuthPage } from "./pages/AuthPage";
import { WorkspacePage } from "./pages/WorkspacePage";
import { WorkspacesPage } from "./pages/WorkspacesPage";

export function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<AuthPage mode="login" />} path="/login" />
        <Route element={<AuthPage mode="register" />} path="/register" />
        <Route element={<WorkspacesPage />} path="/workspaces" />
        <Route element={<WorkspacePage />} path="/workspaces/:workspaceId" />
        <Route element={<WorkspacePage />} path="/workspaces/:workspaceId/sessions/:sessionId" />
        <Route element={<Navigate replace to="/workspaces" />} path="*" />
      </Routes>
    </AuthProvider>
  );
}
