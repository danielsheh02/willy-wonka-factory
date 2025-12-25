import React from "react";
import { Outlet, useNavigate } from "react-router-dom";
import { AppBar, Toolbar, Button, Typography, Box } from "@mui/material";
import { useAuth } from "../auth/AuthProvider";
import NotificationBell from "../components/NotificationBell";

const navItems = [
  { path: "/tasks", label: "Задачи" },
  { path: "/users", label: "Сотрудники" },
  { path: "/equipment", label: "Оборудование" },
  { path: "/workshops", label: "Цеха" },
  { path: "/profile", label: "Профиль" }
];

export default function MainLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  return (
    <>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Willy Wonka Admin
          </Typography>
          {navItems.map((item) => (
            <Button
              key={item.path}
              color="inherit"
              onClick={() => navigate(item.path)}
            >
              {item.label}
            </Button>
          ))}
          {user && <NotificationBell />}
          {user && (
            <Button color="inherit" onClick={logout}>
              Выйти
            </Button>
          )}
        </Toolbar>
      </AppBar>
      <Box sx={{ p: 3 }}>
        <Outlet />
      </Box>
    </>
  );
}

