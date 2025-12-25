import React, { useEffect, useState } from "react";
import { Box, Typography, Paper, CircularProgress, Chip, Divider, Grid } from "@mui/material";
import { useAuth } from "../auth/AuthProvider";
import api, { API_URL } from "../api";

export default function ProfilePage() {
  const { user } = useAuth();
  const [userData, setUserData] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUserData = async () => {
      if (!user?.username) return;
      try {
        setLoading(true);
        // Загружаем полные данные пользователя
        const { data: fullUser } = await api.get(`${API_URL}/api/users/by-username/${user.username}`);
        setUserData(fullUser);

        // Загружаем задачи пользователя (если он рабочий)
        if (fullUser.role === 'WORKER') {
          try {
            const { data: allTasks } = await api.get(`${API_URL}/api/tasks`);
            const userTasks = allTasks.filter(task => task.user?.id === fullUser.id);
            setTasks(userTasks);
          } catch (err) {
            console.error("Ошибка загрузки задач:", err);
          }
        }
      } catch (err) {
        console.error("Ошибка загрузки профиля:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, [user]);

  if (!user) return null;
  if (loading) return (
    <Box display="flex" justifyContent="center" mt={8}>
      <CircularProgress />
    </Box>
  );

  const roleLabel = userData?.role === 'WORKER' ? 'Рабочий' : 
                    userData?.role === 'FOREMAN' ? 'Начальник цеха' : 'Неизвестно';

  const formatDate = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const completedTasks = tasks.filter(t => t.status === 'COMPLETED').length;
  const inProgressTasks = tasks.filter(t => t.status === 'IN_PROGRESS').length;

  return (
    <Box maxWidth={720} mx="auto" mt={5}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h4" mb={3}>Личный кабинет</Typography>
        
        <Grid container spacing={2} mb={3}>
          <Grid item xs={12} sm={6}>
            <Typography color="text.secondary" variant="body2">ID пользователя</Typography>
            <Typography variant="h6">{userData?.id || '-'}</Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography color="text.secondary" variant="body2">Логин</Typography>
            <Typography variant="h6">{userData?.username || user.username}</Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography color="text.secondary" variant="body2">Роль</Typography>
            <Typography variant="h6">{roleLabel}</Typography>
          </Grid>
          <Grid item xs={12} sm={6}>
            <Typography color="text.secondary" variant="body2">Статус аккаунта</Typography>
            <Chip 
              label={userData?.isBanned ? "Заблокирован" : "Активен"} 
              color={userData?.isBanned ? "error" : "success"}
              size="small"
              sx={{ mt: 0.5 }}
            />
          </Grid>
          <Grid item xs={12}>
            <Typography color="text.secondary" variant="body2">Дата регистрации</Typography>
            <Typography variant="body1">{formatDate(userData?.createdAt)}</Typography>
          </Grid>
        </Grid>

        {userData?.workshops && userData.workshops.length > 0 && (
          <>
            <Divider sx={{ my: 2 }} />
            <Typography variant="h6" mb={2}>Мои цеха</Typography>
            <Box display="flex" gap={1} flexWrap="wrap">
              {userData.workshops.map(workshop => (
                <Chip 
                  key={workshop.id} 
                  label={workshop.name} 
                  color="primary" 
                  variant="outlined"
                />
              ))}
            </Box>
          </>
        )}

        {userData?.role === 'WORKER' && tasks.length > 0 && (
          <>
            <Divider sx={{ my: 3 }} />
            <Typography variant="h6" mb={2}>Статистика по задачам</Typography>
            <Grid container spacing={2}>
              <Grid item xs={4}>
                <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#f5f5f5' }}>
                  <Typography variant="h4" color="primary">{tasks.length}</Typography>
                  <Typography variant="body2" color="text.secondary">Всего задач</Typography>
                </Paper>
              </Grid>
              <Grid item xs={4}>
                <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#fff3e0' }}>
                  <Typography variant="h4" color="warning.main">{inProgressTasks}</Typography>
                  <Typography variant="body2" color="text.secondary">В работе</Typography>
                </Paper>
              </Grid>
              <Grid item xs={4}>
                <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#e8f5e9' }}>
                  <Typography variant="h4" color="success.main">{completedTasks}</Typography>
                  <Typography variant="body2" color="text.secondary">Завершено</Typography>
                </Paper>
              </Grid>
            </Grid>
          </>
        )}
      </Paper>
    </Box>
  );
}
