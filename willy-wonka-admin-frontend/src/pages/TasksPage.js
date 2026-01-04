import React, { useEffect, useState, useMemo, useCallback } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem, Select, FormControl, InputLabel, CircularProgress, Snackbar, Alert, ToggleButton, ToggleButtonGroup, IconButton, Chip, FormControlLabel, Checkbox } from "@mui/material";
import PersonIcon from "@mui/icons-material/Person";
import PeopleIcon from "@mui/icons-material/People";
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AssignmentIndIcon from '@mui/icons-material/AssignmentInd';
import AssignmentReturnIcon from '@mui/icons-material/AssignmentReturn';
import api, { API_URL } from "../api";
import { useAuth } from "../auth/AuthProvider";
import { usePermissions } from "../hooks/usePermissions";
import { formatDate } from "../utils/dateUtils";

export default function TasksPage() {
  const { user } = useAuth();
  const permissions = usePermissions();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [statuses, setStatuses] = useState([]);
  const [users, setUsers] = useState([]);
  const [notification, setNotification] = useState("");
  const [filterMode, setFilterMode] = useState("all"); // "all" или "my"
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [taskToDelete, setTaskToDelete] = useState(null);

  const getRoleLabel = (role) => {
    switch(role) {
      case 'WORKER': return 'Рабочий';
      case 'FOREMAN': return 'Начальник';
      case 'ADMIN': return 'Администратор';
      case 'MASTER': return 'Мастер';
      case 'GUIDE': return 'Экскурсовод';
      default: return role;
    }
  };

  const fetchTasks = async () => {
    setLoading(true);
    const { data } = await api.get(`${API_URL}/api/tasks`);
    setTasks(data);
    setLoading(false);
  };
  const fetchStatuses = async () => {
    const { data } = await api.get(`${API_URL}/api/tasks/statuses`);
    setStatuses(data);
  };
  
  const fetchUsers = async () => {
    try {
      const { data } = await api.get(`${API_URL}/api/users`);
      setUsers(data);
    } catch (error) {
      console.error("Ошибка загрузки пользователей:", error);
    }
  };

  useEffect(() => {
    fetchTasks();
    fetchStatuses();
    fetchUsers();
  }, []);

  // Фильтрация задач
  const filteredTasks = useMemo(() => {
    if (filterMode === "my" && user?.id) {
      return tasks.filter(task => task.user?.id === user.id);
    }
    return tasks;
  }, [tasks, filterMode, user]);

  const handleOpen = (task) => {
    setSelectedTask(task || { name: "", description: "", status: statuses[0] || "", userId: "" });
    setOpen(true);
  };
  const handleClose = () => { setOpen(false); setSelectedTask(null); };

  const handleSave = async () => {
    // Валидация обязательных полей (userId теперь необязателен)
    if (!selectedTask.name || !selectedTask.status) {
      setNotification("Заполните все обязательные поля!");
      return;
    }
    
    const userId = selectedTask.userId || selectedTask.user?.id;
    
    // Подготовка данных для отправки
    const dataToSend = {
      name: selectedTask.name,
      description: selectedTask.description,
      status: selectedTask.status,
      userId: userId,
      force: selectedTask.force || false
    };
    
    try {
      if (selectedTask.id) {
        await api.put(`${API_URL}/api/tasks/${selectedTask.id}`, dataToSend);
        setNotification("Задача обновлена!");
      } else {
        await api.post(`${API_URL}/api/tasks`, dataToSend);
        setNotification("Задача создана!");
      }
      fetchTasks();
      handleClose();
    } catch (e) { 
      console.error(e);
      // Проверяем статус ошибки
      if (e.response?.status === 409) {
        setNotification("У выбранного рабочего превышен лимит активных задач. Используйте принудительное назначение или выберите другого рабочего.");
      } else {
        setNotification(e.response?.data || "Ошибка при сохранении");
      }
    }
  };
  const handleDelete = async (id) => {
    await api.delete(`${API_URL}/api/tasks/${id}`);
    fetchTasks();
    setNotification("Задача удалена");
  };

  const handleTakeTask = async (taskId) => {
    try {
      await api.post(`${API_URL}/api/tasks/${taskId}/assign-to-me?userId=${user.id}`);
      fetchTasks();
      setNotification("Задача взята в работу");
    } catch (error) {
      console.error("Ошибка при взятии задачи:", error);
      setNotification(error.response?.data || "Ошибка при взятии задачи");
    }
  };

  const handleUnassignTask = async (taskId) => {
    try {
      await api.post(`${API_URL}/api/tasks/${taskId}/unassign?userId=${user.id}`);
      fetchTasks();
      setNotification("Вы отказались от задачи");
    } catch (error) {
      console.error("Ошибка при отказе от задачи:", error);
      setNotification(error.response?.data || "Ошибка при отказе от задачи");
    }
  };

  const handleEditCallback = useCallback((task) => {
    setSelectedTask(task);
    setOpen(true);
  }, []);

  const handleDeleteCallback = useCallback((task) => {
    setTaskToDelete(task);
    setDeleteDialogOpen(true);
  }, []);

  const confirmDelete = async () => {
    if (taskToDelete) {
      try {
        await api.delete(`${API_URL}/api/tasks/${taskToDelete.id}`);
        fetchTasks();
        setNotification("Задача удалена");
      } catch (error) {
        console.error("Ошибка удаления:", error);
        setNotification("Ошибка при удалении");
      }
    }
    setDeleteDialogOpen(false);
    setTaskToDelete(null);
  };

  const cancelDelete = () => {
    setDeleteDialogOpen(false);
    setTaskToDelete(null);
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
      flex: 1,
      minWidth: 150,
      renderCell: (params) => (
        <div style={{ whiteSpace: 'normal', wordWrap: 'break-word', lineHeight: '1.5' }}>
          {params.value}
        </div>
      )
    },
    { 
      field: "description", 
      headerName: "Описание", 
      flex: 1.5,
      minWidth: 200,
      renderCell: (params) => (
        <div style={{ whiteSpace: 'normal', wordWrap: 'break-word', lineHeight: '1.5' }}>
          {params.value || '-'}
        </div>
      )
    },
    { 
      field: "status", 
      headerName: "Статус", 
      width: 160,
      renderCell: (params) => {
        const statusColors = {
          'NOT_ASSIGNED': 'default',
          'IN_PROGRESS': 'primary',
          'COMPLETED': 'success'
        };
        
        const statusLabels = {
          'NOT_ASSIGNED': 'Не назначена',
          'IN_PROGRESS': 'В работе',
          'COMPLETED': 'Завершена'
        };
        
        return (
          <Chip 
            label={statusLabels[params.value] || params.value}
            color={statusColors[params.value] || 'default'}
            size="small"
          />
        );
      }
    },
    { 
      field: "userId", 
      headerName: "ID Рабочего", 
      width: 110,
      valueGetter: (params) => params.row.user?.id 
    },
    { 
      field: "username", 
      headerName: "Рабочий", 
      flex: 0.8,
      minWidth: 120,
      valueGetter: (params) => params.row.user?.username 
    },
    { 
      field: "createdAt", 
      headerName: "Создано", 
      width: 180,
      valueFormatter: (params) => formatDate(params.value)
    },
    {
      field: "actions",
      headerName: "Действия",
      width: 180,
      sortable: false,
      renderCell: (params) => {
        const canEdit = permissions.canEditTask(params.row.user?.id);
        const canDelete = permissions.canDeleteTask;
        const isUnassigned = !params.row.user;
        const isMyTask = params.row.user?.id === user?.id;
        
        return (
          <Box sx={{ display: 'flex', gap: 0.5 }}>
            {isUnassigned && (
              <IconButton 
                size="small" 
                color="success"
                onClick={(e) => {
                  e.stopPropagation();
                  handleTakeTask(params.row.id);
                }}
                title="Взять задачу"
              >
                <AssignmentIndIcon fontSize="small" />
              </IconButton>
            )}
            {isMyTask && (
              <IconButton 
                size="small" 
                color="warning"
                onClick={(e) => {
                  e.stopPropagation();
                  handleUnassignTask(params.row.id);
                }}
                title="Отказаться от задачи"
              >
                <AssignmentReturnIcon fontSize="small" />
              </IconButton>
            )}
            {canEdit && (
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
            {canDelete && (
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
        );
      }
    }
  ], [handleEditCallback, handleDeleteCallback, permissions]);

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Задачи</Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <ToggleButtonGroup
            value={filterMode}
            exclusive
            onChange={(e, newValue) => newValue && setFilterMode(newValue)}
            size="small"
          >
            <ToggleButton value="all" aria-label="все задачи">
              <PeopleIcon sx={{ mr: 0.5, fontSize: 18 }} />
              Все
            </ToggleButton>
            <ToggleButton value="my" aria-label="мои задачи">
              <PersonIcon sx={{ mr: 0.5, fontSize: 18 }} />
              Мои
            </ToggleButton>
          </ToggleButtonGroup>
          {permissions.canCreateTask && (
            <Button 
              variant="contained" 
              color="primary" 
              size="small"
              onClick={() => handleOpen()}
              sx={{ textTransform: 'none' }}
            >
              + Создать
            </Button>
          )}
        </Box>
      </Box>
      {loading ? <CircularProgress /> : (
        <Box sx={{ flexGrow: 1, minHeight: 0 }}>
          <DataGrid 
            pageSizeOptions={[10, 25, 50, 100]}
            initialState={{
              pagination: {
                paginationModel: {
                  page: 0,
                  pageSize: 10,
                },
              },
            }}
            rows={filteredTasks} 
            columns={columns} 
            pageSize={10}
            rowsPerPageOptions={[10, 25, 50]}
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
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedTask?.id ? "Редактировать задачу" : "Создать задачу"}</DialogTitle>
        <DialogContent>
          <TextField 
            label="Название" 
            margin="dense" 
            fullWidth 
            required
            value={selectedTask?.name || ""} 
            onChange={e => setSelectedTask(t => ({ ...t, name: e.target.value }))} 
          />
          <TextField 
            label="Описание" 
            margin="dense" 
            fullWidth 
            multiline 
            rows={3}
            value={selectedTask?.description || ""} 
            onChange={e => setSelectedTask(t => ({ ...t, description: e.target.value }))} 
          />
          <Box sx={{ mt: 2, mb: 1 }}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              Рабочий (необязательно)
            </Typography>
            {selectedTask?.userId || selectedTask?.user?.id ? (
              <Chip
                label={users.find(u => u.id === (selectedTask?.userId || selectedTask?.user?.id))?.username || "Неизвестен"}
                onDelete={() => setSelectedTask(t => ({ ...t, userId: null, user: null }))}
                color="primary"
                sx={{ mt: 1 }}
              />
            ) : (
              <FormControl fullWidth size="small">
                <InputLabel>Выбрать рабочего</InputLabel>
                <Select 
                  value="" 
                  onChange={e => setSelectedTask(t => ({ ...t, userId: e.target.value }))} 
                  label="Выбрать рабочего"
                  MenuProps={{
                    PaperProps: {
                      style: {
                        maxHeight: 300,
                      },
                    },
                  }}
                >
                  {users.map(u => (
                    <MenuItem value={u.id} key={u.id}>
                      {u.username} (ID: {u.id}) - {getRoleLabel(u.role)}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
          </Box>
          <FormControl margin="dense" fullWidth required>
            <InputLabel>Статус</InputLabel>
            <Select 
              value={selectedTask?.status || ""} 
              onChange={e => setSelectedTask(t => ({ ...t, status: e.target.value }))} 
              label="Статус"
            >
              {statuses.map(st => <MenuItem value={st} key={st}>{st}</MenuItem>)}
            </Select>
          </FormControl>
          
          {selectedTask?.userId && (
            <FormControlLabel
              control={
                <Checkbox
                  checked={selectedTask?.force || false}
                  onChange={(e) => setSelectedTask(t => ({ ...t, force: e.target.checked }))}
                  color="warning"
                />
              }
              label={
                <Typography variant="body2">
                  Принудительное назначение
                  <Typography variant="caption" display="block" color="text.secondary">
                    (игнорировать лимит активных задач)
                  </Typography>
                </Typography>
              }
              sx={{ mt: 2 }}
            />
          )}
        </DialogContent>
        <DialogActions>
          {selectedTask?.id && <Button color="error" onClick={() => { handleDelete(selectedTask.id); handleClose(); }}>Удалить</Button>}
          <Button onClick={handleClose}>Отмена</Button>
          <Button onClick={handleSave} variant="contained">Сохранить</Button>
        </DialogActions>
      </Dialog>
      {/* Диалог подтверждения удаления */}
      <Dialog open={deleteDialogOpen} onClose={cancelDelete}>
        <DialogTitle>Подтверждение удаления</DialogTitle>
        <DialogContent>
          <Typography>
            Вы уверены, что хотите удалить задачу <strong>{taskToDelete?.name}</strong>?
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
