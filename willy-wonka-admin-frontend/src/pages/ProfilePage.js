import React from "react";
import { Box, Typography, Paper } from "@mui/material";
import { useAuth } from "../auth/AuthProvider";
import { API_URL } from "../api";

export default function ProfilePage() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <Box maxWidth={520} mx="auto" mt={5}>
      <Paper sx={{p:4}}>
        <Typography variant="h5" mb={2}>Личный кабинет</Typography>
        <Typography>Логин: <b>{user.username}</b></Typography>
        <Typography>Роль: <b>{user.role === 'WORKER' ? 'Рабочий' : user.role === 'FOREMAN' ? 'Начальник цеха' : 'Неизвестно'}</b></Typography>
        <Typography>Ваш ID: <b>{user.id}</b></Typography>
        {/* Для расширения:
         Можно добавить кнопки или логику для дополнительных запросов от профиля: 
         например смены пароля через fetch(`${API_URL}/api/....`) и т.д. */}
      </Paper>
    </Box>
  );
}
