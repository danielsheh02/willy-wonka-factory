import React, { useMemo } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import { AppBar, Toolbar, Button, Typography, Box } from "@mui/material";
import { useAuth } from "../auth/AuthProvider";
import { usePermissions } from "../hooks/usePermissions";
import NotificationBell from "../components/NotificationBell";

export default function MainLayout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const permissions = usePermissions();

  const filteredNavItems = useMemo(() => {
    const allNavItems = [
      { path: "/tasks", label: "Задачи", visible: permissions.canViewTasks },
      { path: "/task-distribution", label: "Распределение задач", visible: permissions.canViewTaskDistribution },
      { path: "/users", label: "Сотрудники", visible: permissions.canViewUsers },
      { path: "/equipment", label: "Оборудование", visible: permissions.canViewEquipment },
      { path: "/workshops", label: "Цеха", visible: permissions.canViewWorkshops },
      { path: "/excursions", label: "Экскурсии", visible: permissions.canViewExcursions },
      { path: "/tickets", label: "Золотые билеты", visible: permissions.canViewTickets },
      { path: "/reports", label: "Отчеты", visible: permissions.canViewReports },
      { path: "/profile", label: "Профиль", visible: true }
    ];

    return allNavItems.filter(item => item.visible);
  }, [permissions]);

  return (
    <>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Willy Wonka Factory
          </Typography>
          {filteredNavItems.map((item) => (
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

