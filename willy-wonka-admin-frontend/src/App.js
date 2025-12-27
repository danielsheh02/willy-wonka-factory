import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import TasksPage from "./pages/TasksPage";
import UsersPage from "./pages/UsersPage";
import EquipmentPage from "./pages/EquipmentPage";
import WorkshopsPage from "./pages/WorkshopsPage";
import ExcursionsPage from "./pages/ExcursionsPage";
import ProfilePage from "./pages/ProfilePage";
import MainLayout from "./layouts/MainLayout";
import AuthProvider, { useAuth } from "./auth/AuthProvider";

function PrivateRoute({ children }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/login" />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<MainLayout />}>
            <Route path="/tasks" element={<PrivateRoute><TasksPage /></PrivateRoute>} />
            <Route path="/users" element={<PrivateRoute><UsersPage /></PrivateRoute>} />
            <Route path="/equipment" element={<PrivateRoute><EquipmentPage /></PrivateRoute>} />
            <Route path="/workshops" element={<PrivateRoute><WorkshopsPage /></PrivateRoute>} />
            <Route path="/excursions" element={<PrivateRoute><ExcursionsPage /></PrivateRoute>} />
            <Route path="/profile" element={<PrivateRoute><ProfilePage /></PrivateRoute>} />
          </Route>
          <Route path="*" element={<Navigate to="/tasks" />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

