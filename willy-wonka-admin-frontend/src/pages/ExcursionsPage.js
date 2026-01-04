import React, { useEffect, useState, useCallback, useMemo } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { 
  Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, 
  TextField, Snackbar, Alert, MenuItem, FormControl, InputLabel, Select,
  Chip, Stack, IconButton, Stepper, Step, StepLabel, Card, CardContent,
  Switch, FormControlLabel, List, ListItem, ListItemText, Paper, Divider
} from "@mui/material";
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon from '@mui/icons-material/Edit';
import api, { API_URL } from "../api";
import { useAuth } from "../auth/AuthProvider";
import { usePermissions } from "../hooks/usePermissions";
import { utcToLocalInputValue, formatDateTime, formatDateTimeShort, formatTime, toUTCString, parseUTCDate } from "../utils/dateUtils";

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

// Колонки будут определены внутри компонента, чтобы иметь доступ к обработчикам

export default function ExcursionsPage() {
  const { user } = useAuth();
  const permissions = usePermissions();
  const [excursions, setExcursions] = useState([]);
  const [excursionsWithBookings, setExcursionsWithBookings] = useState([]); // Экскурсии с информацией о бронированиях
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false); // Диалог просмотра
  const [selectedExcursion, setSelectedExcursion] = useState(null);
  const [viewExcursion, setViewExcursion] = useState(null); // Экскурсия для просмотра
  const [statuses, setStatuses] = useState([]);
  const [users, setUsers] = useState([]);
  const [workshops, setWorkshops] = useState([]);
  const [notification, setNotification] = useState("");
  const [activeStep, setActiveStep] = useState(0);
  const [autoGenerate, setAutoGenerate] = useState(true);
  const [manualRoutes, setManualRoutes] = useState([]);
  const [availabilityCheck, setAvailabilityCheck] = useState(null);
  const [minRequiredWorkshops, setMinRequiredWorkshops] = useState(0); // 0 = максимально возможное
  const [formError, setFormError] = useState(""); // Ошибки формы
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [excursionToDelete, setExcursionToDelete] = useState(null);

  const fetchExcursions = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get(`${API_URL}/api/excursions`);
      setExcursions(data);
      
      // Получаем информацию о бронированиях для каждой экскурсии
      const ticketsResponse = await api.get(`${API_URL}/api/tickets`);
      const tickets = ticketsResponse.data;
      
      const excursionsWithBookingInfo = data.map(excursion => {
        const bookedTickets = tickets.filter(
          ticket => ticket.excursionId === excursion.id && ticket.status === 'BOOKED'
        );
        return {
          ...excursion,
          bookedCount: bookedTickets.length
        };
      });
      
      setExcursionsWithBookings(excursionsWithBookingInfo);
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

  const handleView = (excursion) => {
    setViewExcursion(excursion);
    setViewDialogOpen(true);
  };

  const handleCloseView = () => {
    setViewDialogOpen(false);
    setViewExcursion(null);
  };

  const handleEdit = (excursion) => {
    // Конвертируем UTC время в локальное для input type="datetime-local"
    const localStartTime = excursion.startTime 
      ? parseUTCDate(excursion.startTime).toISOString().slice(0, 16) 
      : "";
    
    setSelectedExcursion({
      ...excursion,
      startTime: localStartTime
    });
    setAutoGenerate(false);
    setManualRoutes(excursion.routes || []);
    setMinRequiredWorkshops(0);
    setActiveStep(0);
    setAvailabilityCheck(null);
    setFormError("");
    setOpen(true);
  };

  const handleOpen = (excursion) => {
    if (excursion) {
      handleEdit(excursion);
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
      setMinRequiredWorkshops(0);
      setActiveStep(0);
      setAvailabilityCheck(null);
      setFormError("");
      setOpen(true);
    }
  };

  const handleClose = () => { 
    setOpen(false); 
    setSelectedExcursion(null); 
    setActiveStep(0);
    setManualRoutes([]);
    setAvailabilityCheck(null);
    setFormError("");
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
    setFormError("");
  };

  const handleRemoveRoutePoint = (index) => {
    const newRoutes = manualRoutes.filter((_, i) => i !== index);
    // Пересчитываем orderNumber
    newRoutes.forEach((route, i) => {
      route.orderNumber = i + 1;
    });
    setManualRoutes(newRoutes);
    setFormError("");
  };

  const handleRoutePointChange = (index, field, value) => {
    const newRoutes = [...manualRoutes];
    newRoutes[index][field] = value;
    setManualRoutes(newRoutes);
    setFormError("");
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

    // Конвертируем локальное время в UTC перед отправкой на сервер
    const startTimeUTC = toUTCString(selectedExcursion.startTime);

    const dataToSend = {
      name: selectedExcursion.name,
      startTime: startTimeUTC,
      participantsCount: selectedExcursion.participantsCount,
      guideId: guideId,
      status: selectedExcursion.status,
      autoGenerateRoute: autoGenerate,
      routes: autoGenerate ? null : manualRoutes.filter(r => r.workshopId),
      minRequiredWorkshops: autoGenerate ? (minRequiredWorkshops > 0 ? minRequiredWorkshops : null) : null
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
      
      // Отображаем ошибку в форме вместо notification
      setFormError(errorMessage);
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

  // Используем useCallback для стабильности функций
  const handleViewCallback = useCallback((excursion) => {
    setViewExcursion(excursion);
    setViewDialogOpen(true);
  }, []);

  const handleEditCallback = useCallback((excursion) => {
    setSelectedExcursion({
      ...excursion,
      startTime: utcToLocalInputValue(excursion.startTime)
    });

    setAutoGenerate(false);
    setManualRoutes(excursion.routes || []);
    setMinRequiredWorkshops(0);
    setActiveStep(0);
    setAvailabilityCheck(null);
    setFormError("");
    setOpen(true);
  }, []);

  const handleDeleteCallback = useCallback((excursion) => {
    setExcursionToDelete(excursion);
    setDeleteDialogOpen(true);
  }, []);

  const confirmDelete = async () => {
    if (excursionToDelete) {
      try {
        await api.delete(`${API_URL}/api/excursions/${excursionToDelete.id}`);
        fetchExcursions();
        setNotification("Экскурсия удалена");
      } catch (error) {
        console.error("Ошибка удаления:", error);
        setNotification("Ошибка при удалении");
      }
    }
    setDeleteDialogOpen(false);
    setExcursionToDelete(null);
  };

  const cancelDelete = () => {
    setDeleteDialogOpen(false);
    setExcursionToDelete(null);
  };

  const columns = useMemo(() => [
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
      valueFormatter: (params) => formatDateTimeShort(params.value)
    },
    { 
      field: "endTime", 
      headerName: "Окончание", 
      width: 150,
      valueFormatter: (params) => formatDateTimeShort(params.value)
    },
    { 
      field: "participantsCount", 
      headerName: "Места", 
      width: 130,
      renderCell: (params) => {
        const booked = params.row.bookedCount || 0;
        const total = params.row.participantsCount || 0;
        const color = booked >= total ? 'error' : booked > total * 0.7 ? 'warning' : 'success';
        return (
          <Chip 
            label={`${booked}/${total}`}
            color={color}
            size="small"
            variant="outlined"
          />
        );
      }
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
      width: 180,
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
      headerName: "Цехов",
      width: 90,
      valueGetter: (params) => params.row.routes?.length || 0
    },
    {
      field: "actions",
      headerName: "Действия",
      width: 160,
      sortable: false,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <IconButton 
            size="small" 
            color="info"
            onClick={(e) => {
              e.stopPropagation();
              handleViewCallback(params.row);
            }}
            title="Просмотр"
          >
            <VisibilityIcon fontSize="small" />
          </IconButton>
          {permissions.canEditExcursion && (
            <IconButton 
              size="small" 
              color="primary"
              onClick={(e) => {
                e.stopPropagation();
                handleEditCallback(params.row);
              }}
              title="Редактировать"
            >
              <EditIcon fontSize="small" />
            </IconButton>
          )}
          {permissions.canDeleteExcursion && (
            <IconButton 
              size="small" 
              color="error"
              onClick={(e) => {
                e.stopPropagation();
                handleDeleteCallback(params.row);
              }}
              title="Удалить"
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          )}
        </Box>
      )
    }
  ], [handleViewCallback, handleEditCallback, handleDeleteCallback]);

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
              onChange={e => {
                setSelectedExcursion(t => ({ ...t, name: e.target.value }));
                setFormError("");
              }}
            />
            <TextField
              label="Дата и время начала"
              margin="dense"
              fullWidth
              type="datetime-local"
              required
              InputLabelProps={{ shrink: true }}
              value={selectedExcursion?.startTime || ""}
              onChange={e => {
                setSelectedExcursion(t => ({ ...t, startTime: e.target.value }));
                setFormError("");
              }}
            />
            <TextField
              label="Количество участников"
              margin="dense"
              fullWidth
              type="number"
              required
              inputProps={{ min: 1 }}
              value={selectedExcursion?.participantsCount || 10}
              onChange={e => {
                setSelectedExcursion(t => ({ ...t, participantsCount: parseInt(e.target.value) }));
                setFormError("");
              }}
            />
            <FormControl margin="dense" fullWidth required>
              <InputLabel>Экскурсовод</InputLabel>
              <Select
                value={selectedExcursion?.guideId || selectedExcursion?.guide?.id || ""}
                onChange={e => {
                  setSelectedExcursion(t => ({ ...t, guideId: e.target.value }));
                  setFormError("");
                }}
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
                onChange={e => {
                  setSelectedExcursion(t => ({ ...t, status: e.target.value }));
                  setFormError("");
                }}
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
                    // Очищаем все ошибки при переключении режима
                    setFormError("");
                    setAvailabilityCheck(null);
                    setNotification("");
                    if (e.target.checked) {
                      setManualRoutes([]);
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

            {/* Отображение ошибок формы */}
            {formError && (
              <Alert severity="error" sx={{ mb: 2 }} onClose={() => setFormError("")}>
                {formError}
              </Alert>
            )}

            {autoGenerate && (
              <Box sx={{ mb: 3 }}>
                <TextField
                  label="Минимальное количество цехов"
                  type="number"
                  fullWidth
                  margin="dense"
                  value={minRequiredWorkshops}
                  onChange={e => {
                    setMinRequiredWorkshops(parseInt(e.target.value) || 0);
                    setFormError(""); // Сбрасываем ошибки при изменении
                  }}
                  helperText={
                    minRequiredWorkshops > 0 
                      ? `Экскурсия должна пройти минимум через ${minRequiredWorkshops} цехов` 
                      : "0 = максимально возможное количество цехов"
                  }
                  inputProps={{ min: 0 }}
                />
                <Alert severity="info" sx={{ mt: 2 }}>
                  {minRequiredWorkshops > 0 
                    ? `Будет создан маршрут минимум через ${minRequiredWorkshops} цехов. Если указанное количество недоступно - вы получите ошибку.`
                    : "Будет создан маршрут через максимально возможное количество доступных цехов."
                  }
                </Alert>
              </Box>
            )}

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
                          secondary={`Длительность: ${route.durationMinutes} мин | Начало: ${formatTime(route.startTime)}`}
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
        {permissions.canCreateExcursion && (
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
        )}
      </Box>
      {loading ? (
        <Typography>Загрузка...</Typography>
      ) : (
        <Box sx={{ flexGrow: 1, minHeight: 0 }}>
          <DataGrid
            rows={excursionsWithBookings}
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

      {/* Диалог подтверждения удаления */}
      <Dialog open={deleteDialogOpen} onClose={cancelDelete}>
        <DialogTitle>Подтверждение удаления</DialogTitle>
        <DialogContent>
          <Typography>
            Вы уверены, что хотите удалить экскурсию <strong>{excursionToDelete?.name}</strong>?
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Это действие нельзя будет отменить.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={cancelDelete}>Отмена</Button>
          <Button onClick={confirmDelete} color="error" variant="contained">
            Удалить
          </Button>
        </DialogActions>
      </Dialog>

      {/* Диалог просмотра экскурсии */}
      <Dialog open={viewDialogOpen} onClose={handleCloseView} maxWidth="md" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Детали экскурсии</Typography>
            <Chip 
              label={statusLabels[viewExcursion?.status] || viewExcursion?.status} 
              color={statusColors[viewExcursion?.status] || 'default'}
            />
          </Box>
        </DialogTitle>
        <DialogContent dividers>
          {viewExcursion && (
            <Box>
              {/* Основная информация */}
              <Card sx={{ mb: 3 }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom color="primary">
                    {viewExcursion.name}
                  </Typography>
                  
                  <Stack spacing={1.5} sx={{ mt: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">ID экскурсии:</Typography>
                      <Typography variant="body2" fontWeight="bold">#{viewExcursion.id}</Typography>
                    </Box>
                    
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Экскурсовод:</Typography>
                      <Typography variant="body2" fontWeight="bold">{viewExcursion.guideUsername}</Typography>
                    </Box>
                    
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Начало:</Typography>
                      <Typography variant="body2" fontWeight="bold">
                        {formatDateTime(viewExcursion.startTime)}
                      </Typography>
                    </Box>
                    
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Окончание:</Typography>
                      <Typography variant="body2" fontWeight="bold">
                        {formatDateTime(viewExcursion.endTime)}
                      </Typography>
                    </Box>
                    
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Занято мест:</Typography>
                      <Chip 
                        label={`${viewExcursion.bookedCount || 0} / ${viewExcursion.participantsCount}`}
                        color={
                          (viewExcursion.bookedCount || 0) >= viewExcursion.participantsCount 
                            ? 'error' 
                            : (viewExcursion.bookedCount || 0) > viewExcursion.participantsCount * 0.7 
                            ? 'warning' 
                            : 'success'
                        }
                        size="small"
                      />
                    </Box>
                    
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2" color="text.secondary">Создано:</Typography>
                      <Typography variant="body2">
                        {formatDateTime(viewExcursion.createdAt)}
                      </Typography>
                    </Box>
                  </Stack>
                </CardContent>
              </Card>

              {/* Маршрут экскурсии */}
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom color="primary">
                    📍 Маршрут экскурсии
                  </Typography>
                  
                  {viewExcursion.routes && viewExcursion.routes.length > 0 ? (
                    <List>
                      {viewExcursion.routes
                        .sort((a, b) => a.orderNumber - b.orderNumber)
                        .map((route, index) => (
                          <React.Fragment key={route.id}>
                            {index > 0 && <Divider />}
                            <ListItem>
                              <ListItemText
                                primary={
                                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                    <Chip 
                                      label={route.orderNumber} 
                                      size="small" 
                                      color="primary"
                                      sx={{ minWidth: 40 }}
                                    />
                                    <Typography variant="body1" fontWeight="bold">
                                      {route.workshopName}
                                    </Typography>
                                  </Box>
                                }
                                secondary={
                                  <Box sx={{ ml: 6, mt: 1 }}>
                                    <Typography variant="body2" color="text.secondary">
                                      ⏱️ Длительность: <strong>{route.durationMinutes} минут</strong>
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                      🕐 Время начала: <strong>{formatTime(route.startTime)}</strong>
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                      🕐 Время окончания: <strong>
                                        {formatTime(
                                          new Date(parseUTCDate(route.startTime).getTime() + route.durationMinutes * 60000).toISOString()
                                        )}
                                      </strong>
                                    </Typography>
                                  </Box>
                                }
                              />
                            </ListItem>
                          </React.Fragment>
                        ))}
                    </List>
                  ) : (
                    <Alert severity="info">Маршрут еще не составлен</Alert>
                  )}
                  
                  {viewExcursion.routes && viewExcursion.routes.length > 0 && (
                    <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.100', borderRadius: 1 }}>
                      <Typography variant="body2" color="text.secondary">
                        <strong>Общая длительность маршрута:</strong>{' '}
                        {viewExcursion.routes.reduce((sum, r) => sum + r.durationMinutes, 0)} минут
                        {' '}({Math.floor(viewExcursion.routes.reduce((sum, r) => sum + r.durationMinutes, 0) / 60)} ч {viewExcursion.routes.reduce((sum, r) => sum + r.durationMinutes, 0) % 60} мин)
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        <strong>Количество цехов:</strong> {viewExcursion.routes.length}
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseView}>Закрыть</Button>
          {permissions.canEditExcursion && (
            <Button 
              variant="contained" 
              startIcon={<EditIcon />}
              onClick={() => {
                handleCloseView();
                handleEdit(viewExcursion);
              }}
            >
              Редактировать
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

