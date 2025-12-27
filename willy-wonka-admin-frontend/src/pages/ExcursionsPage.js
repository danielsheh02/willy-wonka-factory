import React, { useEffect, useState, useCallback, useMemo } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { 
  Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, 
  TextField, Snackbar, Alert, MenuItem, FormControl, InputLabel, Select,
  Chip, Stack, IconButton, Stepper, Step, StepLabel, Card, CardContent,
  Switch, FormControlLabel, List, ListItem, ListItemText, Paper
} from "@mui/material";
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import api, { API_URL } from "../api";
import { useAuth } from "../auth/AuthProvider";

const statusColors = {
  'DRAFT': 'default',
  'CONFIRMED': 'success',
  'IN_PROGRESS': 'primary',
  'COMPLETED': 'info',
  'CANCELLED': 'error'
};

const statusLabels = {
  'DRAFT': 'Черновик',
  'CONFIRMED': 'Подтверждена',
  'IN_PROGRESS': 'В процессе',
  'COMPLETED': 'Завершена',
  'CANCELLED': 'Отменена'
};

const columns = [
  { 
    field: "id", 
    headerName: "ID", 
    width: 70
  },
  { 
    field: "name", 
    headerName: "Название", 
    flex: 1.5,
    minWidth: 180,
    renderCell: (params) => (
      <div style={{ whiteSpace: 'normal', wordWrap: 'break-word', lineHeight: '1.5' }}>
        {params.value}
      </div>
    )
  },
  { 
    field: "startTime", 
    headerName: "Начало", 
    width: 150,
    valueFormatter: (params) => {
      if (!params.value) return '-';
      const date = new Date(params.value);
      return date.toLocaleString('ru-RU', { 
        year: 'numeric', 
        month: '2-digit', 
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    }
  },
  { 
    field: "endTime", 
    headerName: "Окончание", 
    width: 150,
    valueFormatter: (params) => {
      if (!params.value) return '-';
      const date = new Date(params.value);
      return date.toLocaleString('ru-RU', { 
        year: 'numeric', 
        month: '2-digit', 
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    }
  },
  { 
    field: "participantsCount", 
    headerName: "Участников", 
    width: 110
  },
  { 
    field: "guideName", 
    headerName: "Экскурсовод", 
    flex: 1,
    minWidth: 150
  },
  { 
    field: "status", 
    headerName: "Статус", 
    width: 140,
    renderCell: (params) => (
      <Chip 
        label={statusLabels[params.value] || params.value} 
        color={statusColors[params.value] || 'default'}
        size="small"
      />
    )
  },
  {
    field: "routesCount",
    headerName: "Цехов в маршруте",
    width: 140,
    valueGetter: (params) => params.row.routes?.length || 0
  }
];

export default function ExcursionsPage() {
  const { user } = useAuth();
  const [excursions, setExcursions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [selectedExcursion, setSelectedExcursion] = useState(null);
  const [statuses, setStatuses] = useState([]);
  const [users, setUsers] = useState([]);
  const [workshops, setWorkshops] = useState([]);
  const [notification, setNotification] = useState("");
  const [activeStep, setActiveStep] = useState(0);
  const [autoGenerate, setAutoGenerate] = useState(true);
  const [manualRoutes, setManualRoutes] = useState([]);
  const [availabilityCheck, setAvailabilityCheck] = useState(null);

  const fetchExcursions = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get(`${API_URL}/api/excursions`);
      setExcursions(data);
    } catch (error) {
      console.error("Ошибка загрузки экскурсий:", error);
      setNotification("Ошибка загрузки экскурсий");
    }
    setLoading(false);
  }, []);

  const fetchStatuses = useCallback(async () => {
    try {
      const { data } = await api.get(`${API_URL}/api/excursions/statuses`);
      setStatuses(data);
    } catch (error) {
      console.error("Ошибка загрузки статусов:", error);
    }
  }, []);

  const fetchUsers = useCallback(async () => {
    try {
      const { data } = await api.get(`${API_URL}/api/users`);
      setUsers(data.filter(u => u.role === 'GUIDE'));
    } catch (error) {
      console.error("Ошибка загрузки экскурсоводов:", error);
    }
  }, []);

  const fetchWorkshops = useCallback(async () => {
    try {
      const { data } = await api.get(`${API_URL}/api/workshops`);
      setWorkshops(data);
    } catch (error) {
      console.error("Ошибка загрузки цехов:", error);
    }
  }, []);

  useEffect(() => {
    fetchExcursions();
    fetchStatuses();
    fetchUsers();
    fetchWorkshops();
  }, [fetchExcursions, fetchStatuses, fetchUsers, fetchWorkshops]);

  const handleOpen = (excursion) => {
    if (excursion) {
      setSelectedExcursion(excursion);
      setAutoGenerate(false);
      setManualRoutes(excursion.routes || []);
    } else {
      setSelectedExcursion({ 
        name: "", 
        startTime: "", 
        participantsCount: 10,
        guideId: user?.id || "",
        status: "DRAFT"
      });
      setAutoGenerate(true);
      setManualRoutes([]);
    }
    setActiveStep(0);
    setAvailabilityCheck(null);
    setOpen(true);
  };

  const handleClose = () => { 
    setOpen(false); 
    setSelectedExcursion(null); 
    setActiveStep(0);
    setManualRoutes([]);
    setAvailabilityCheck(null);
  };

  const handleNext = () => {
    setActiveStep((prevStep) => prevStep + 1);
  };

  const handleBack = () => {
    setActiveStep((prevStep) => prevStep - 1);
  };

  const handleAddRoutePoint = () => {
    setManualRoutes([...manualRoutes, {
      workshopId: "",
      orderNumber: manualRoutes.length + 1,
      durationMinutes: 15
    }]);
  };

  const handleRemoveRoutePoint = (index) => {
    const newRoutes = manualRoutes.filter((_, i) => i !== index);
    // Пересчитываем orderNumber
    newRoutes.forEach((route, i) => {
      route.orderNumber = i + 1;
    });
    setManualRoutes(newRoutes);
  };

  const handleRoutePointChange = (index, field, value) => {
    const newRoutes = [...manualRoutes];
    newRoutes[index][field] = value;
    setManualRoutes(newRoutes);
  };

  const checkAvailability = async () => {
    if (!selectedExcursion.startTime || !selectedExcursion.participantsCount) {
      setNotification("Заполните дату/время и количество участников");
      return;
    }

    if (manualRoutes.length === 0) {
      setNotification("Добавьте хотя бы один цех в маршрут");
      return;
    }

    const guideId = selectedExcursion.guideId || selectedExcursion.guide?.id;
    if (!guideId) {
      setNotification("Выберите экскурсовода");
      return;
    }

    try {
      const { data } = await api.post(`${API_URL}/api/excursions/check-availability`, {
        excursionId: selectedExcursion.id, // Для редактирования
        startTime: selectedExcursion.startTime,
        participantsCount: selectedExcursion.participantsCount,
        guideId: guideId,
        routes: manualRoutes
      });
      setAvailabilityCheck(data);
    } catch (error) {
      console.error("Ошибка проверки доступности:", error);
      setNotification("Ошибка проверки доступности");
    }
  };

  const handleSave = async () => {
    const guideId = selectedExcursion.guideId || selectedExcursion.guide?.id;
    
    if (!selectedExcursion.name || !selectedExcursion.startTime || 
        !selectedExcursion.participantsCount || !guideId) {
      setNotification("Заполните все обязательные поля!");
      return;
    }

    const dataToSend = {
      name: selectedExcursion.name,
      startTime: selectedExcursion.startTime,
      participantsCount: selectedExcursion.participantsCount,
      guideId: guideId,
      status: selectedExcursion.status,
      autoGenerateRoute: autoGenerate,
      routes: autoGenerate ? null : manualRoutes.filter(r => r.workshopId)
    };

    try {
      if (selectedExcursion.id) {
        await api.put(`${API_URL}/api/excursions/${selectedExcursion.id}`, dataToSend);
        setNotification("Экскурсия обновлена!");
      } else {
        await api.post(`${API_URL}/api/excursions`, dataToSend);
        setNotification("Экскурсия создана!");
      }
      fetchExcursions();
      handleClose();
    } catch (e) {
      console.error(e);
      // Извлекаем детальное сообщение об ошибке с сервера
      let errorMessage = "Ошибка при сохранении";
      
      if (e.response?.data) {
        if (typeof e.response.data === 'string') {
          errorMessage = e.response.data;
        } else if (e.response.data.error) {
          errorMessage = e.response.data.error;
        } else if (e.response.data.message) {
          errorMessage = e.response.data.message;
        }
      }
      
      setNotification(errorMessage);
      setAvailabilityCheck({
        available: false,
        message: errorMessage,
        conflicts: [errorMessage]
      });
    }
  };

  const handleDelete = async (id) => {
    try {
      await api.delete(`${API_URL}/api/excursions/${id}`);
      fetchExcursions();
      setNotification("Экскурсия удалена");
    } catch (error) {
      console.error("Ошибка удаления:", error);
      setNotification("Ошибка при удалении");
    }
  };

  const renderStepContent = () => {
    switch (activeStep) {
      case 0:
        return (
          <Box>
            <TextField
              label="Название экскурсии"
              margin="dense"
              fullWidth
              required
              value={selectedExcursion?.name || ""}
              onChange={e => setSelectedExcursion(t => ({ ...t, name: e.target.value }))}
            />
            <TextField
              label="Дата и время начала"
              margin="dense"
              fullWidth
              type="datetime-local"
              required
              InputLabelProps={{ shrink: true }}
              value={selectedExcursion?.startTime || ""}
              onChange={e => setSelectedExcursion(t => ({ ...t, startTime: e.target.value }))}
            />
            <TextField
              label="Количество участников"
              margin="dense"
              fullWidth
              type="number"
              required
              inputProps={{ min: 1 }}
              value={selectedExcursion?.participantsCount || 10}
              onChange={e => setSelectedExcursion(t => ({ ...t, participantsCount: parseInt(e.target.value) }))}
            />
            <FormControl margin="dense" fullWidth required>
              <InputLabel>Экскурсовод</InputLabel>
              <Select
                value={selectedExcursion?.guideId || selectedExcursion?.guide?.id || ""}
                onChange={e => setSelectedExcursion(t => ({ ...t, guideId: e.target.value }))}
                label="Экскурсовод"
                MenuProps={{
                  PaperProps: {
                    style: {
                      maxHeight: 300,
                    },
                  },
                }}
              >
                {users.map(u => (
                  <MenuItem key={u.id} value={u.id}>
                    {u.username} (ID: {u.id})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl margin="dense" fullWidth required>
              <InputLabel>Статус</InputLabel>
              <Select
                value={selectedExcursion?.status || "DRAFT"}
                onChange={e => setSelectedExcursion(t => ({ ...t, status: e.target.value }))}
                label="Статус"
                MenuProps={{
                  PaperProps: {
                    style: {
                      maxHeight: 300,
                    },
                  },
                }}
              >
                {statuses.map(st => (
                  <MenuItem value={st} key={st}>
                    <Chip 
                      label={statusLabels[st] || st} 
                      color={statusColors[st] || 'default'}
                      size="small"
                    />
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        );

      case 1:
        return (
          <Box>
            <FormControlLabel
              control={
                <Switch
                  checked={autoGenerate}
                  onChange={(e) => {
                    setAutoGenerate(e.target.checked);
                    if (e.target.checked) {
                      setManualRoutes([]);
                      setAvailabilityCheck(null);
                    }
                  }}
                />
              }
              label="Автоматическое построение маршрута"
            />
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 2 }}>
              {autoGenerate 
                ? "Система автоматически построит оптимальный маршрут с учетом занятости цехов"
                : "Вы можете вручную составить маршрут экскурсии"
              }
            </Typography>

            {!autoGenerate && (
              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6">Маршрут экскурсии</Typography>
                  <Button
                    startIcon={<AddIcon />}
                    size="small"
                    variant="outlined"
                    onClick={handleAddRoutePoint}
                  >
                    Добавить цех
                  </Button>
                </Box>

                {manualRoutes.length === 0 ? (
                  <Alert severity="info">Добавьте цеха в маршрут экскурсии</Alert>
                ) : (
                  <List>
                    {manualRoutes.map((route, index) => (
                      <Paper key={index} sx={{ mb: 2, p: 2 }}>
                        <Stack direction="row" spacing={2} alignItems="center">
                          <Typography variant="body1" sx={{ minWidth: 30 }}>
                            {index + 1}.
                          </Typography>
                          <FormControl fullWidth size="small">
                            <InputLabel>Цех</InputLabel>
                            <Select
                              value={route.workshopId || ""}
                              onChange={e => handleRoutePointChange(index, 'workshopId', e.target.value)}
                              label="Цех"
                              MenuProps={{
                                PaperProps: {
                                  style: {
                                    maxHeight: 300,
                                  },
                                },
                              }}
                            >
                              {workshops.map(ws => (
                                <MenuItem key={ws.id} value={ws.id}>
                                  {ws.name} 
                                  {ws.capacity && ` (вместимость: ${ws.capacity})`}
                                  {ws.visitDurationMinutes && ` [${ws.visitDurationMinutes} мин]`}
                                </MenuItem>
                              ))}
                            </Select>
                          </FormControl>
                          <TextField
                            label="Минут"
                            type="number"
                            size="small"
                            sx={{ width: 100 }}
                            value={route.durationMinutes || 15}
                            onChange={e => handleRoutePointChange(index, 'durationMinutes', parseInt(e.target.value))}
                            inputProps={{ min: 5, max: 120 }}
                          />
                          <IconButton 
                            color="error"
                            onClick={() => handleRemoveRoutePoint(index)}
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Stack>
                      </Paper>
                    ))}
                  </List>
                )}

                {manualRoutes.length > 0 && (
                  <Box sx={{ mt: 2 }}>
                    <Button
                      variant="outlined"
                      fullWidth
                      onClick={checkAvailability}
                      startIcon={<CheckCircleIcon />}
                    >
                      Проверить доступность маршрута
                    </Button>
                    {availabilityCheck && (
                      <Alert 
                        severity={availabilityCheck.available ? "success" : "error"}
                        sx={{ mt: 2 }}
                      >
                        <Typography variant="body2" fontWeight="bold">
                          {availabilityCheck.message}
                        </Typography>
                        {availabilityCheck.conflicts && availabilityCheck.conflicts.length > 0 && (
                          <ul style={{ margin: '8px 0 0 0', paddingLeft: '20px' }}>
                            {availabilityCheck.conflicts.map((conflict, i) => (
                              <li key={i}><Typography variant="body2">{conflict}</Typography></li>
                            ))}
                          </ul>
                        )}
                      </Alert>
                    )}
                  </Box>
                )}
              </Box>
            )}

            {selectedExcursion?.id && selectedExcursion?.routes && selectedExcursion.routes.length > 0 && (
              <Box sx={{ mt: 3 }}>
                <Typography variant="h6" gutterBottom>Текущий маршрут</Typography>
                <List>
                  {selectedExcursion.routes
                    .sort((a, b) => a.orderNumber - b.orderNumber)
                    .map((route, index) => (
                      <ListItem key={route.id} divider>
                        <ListItemText
                          primary={`${route.orderNumber}. ${route.workshopName}`}
                          secondary={`Длительность: ${route.durationMinutes} мин | Начало: ${new Date(route.startTime).toLocaleTimeString('ru-RU')}`}
                        />
                      </ListItem>
                    ))}
                </List>
              </Box>
            )}
          </Box>
        );

      default:
        return null;
    }
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Экскурсии</Typography>
        <Button
          variant="contained"
          color="primary"
          size="small"
          onClick={() => handleOpen()}
          sx={{ textTransform: 'none' }}
          startIcon={<AddIcon />}
        >
          Создать экскурсию
        </Button>
      </Box>
      {loading ? (
        <Typography>Загрузка...</Typography>
      ) : (
        <Box sx={{ flexGrow: 1, minHeight: 0 }}>
          <DataGrid
            rows={excursions}
            columns={columns}
            pageSizeOptions={[10, 25, 50, 100]}
            initialState={{
              pagination: {
                paginationModel: {
                  page: 0,
                  pageSize: 10,
                },
              },
            }}
            getRowHeight={() => 'auto'}
            onRowDoubleClick={({ row }) => handleOpen(row)}
            sx={{
              background: "#fff",
              '& .MuiDataGrid-cell': {
                py: 1.5,
              }
            }}
          />
        </Box>
      )}

      <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedExcursion?.id ? "Редактировать экскурсию" : "Создать экскурсию"}
        </DialogTitle>
        <DialogContent>
          <Stepper activeStep={activeStep} sx={{ my: 3 }}>
            <Step>
              <StepLabel>Основная информация</StepLabel>
            </Step>
            <Step>
              <StepLabel>Маршрут</StepLabel>
            </Step>
          </Stepper>
          
          {renderStepContent()}
        </DialogContent>
        <DialogActions>
          {selectedExcursion?.id && (
            <Button 
              color="error" 
              onClick={() => { 
                handleDelete(selectedExcursion.id); 
                handleClose(); 
              }}
            >
              Удалить
            </Button>
          )}
          <Box sx={{ flex: '1 1 auto' }} />
          {activeStep === 0 ? (
            <Button onClick={handleClose}>Отмена</Button>
          ) : (
            <Button onClick={handleBack}>Назад</Button>
          )}
          {activeStep === 0 ? (
            <Button onClick={handleNext} variant="contained">Далее</Button>
          ) : (
            <Button onClick={handleSave} variant="contained">
              {selectedExcursion?.id ? "Обновить" : "Создать"}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <Snackbar open={!!notification} autoHideDuration={3000} onClose={() => setNotification("")}>
        {notification ? (
          <Alert severity="info">
            {notification}
          </Alert>
        ) : null}
      </Snackbar>
    </Box>
  );
}

