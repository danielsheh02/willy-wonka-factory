import React, { useEffect, useState, useMemo } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem, Select, FormControl, InputLabel, CircularProgress, Snackbar, Alert, ToggleButton, ToggleButtonGroup } from "@mui/material";
import PersonIcon from "@mui/icons-material/Person";
import PeopleIcon from "@mui/icons-material/People";
import api, { API_URL } from "../api";
import { useAuth } from "../auth/AuthProvider";

const columns = [
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
    width: 140
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
    width: 300,
    valueFormatter: (params) => {
      if (!params.value) return '-';
      const date = new Date(params.value);
      return date.toLocaleDateString('ru-RU');
    }
  },
];

export default function TasksPage() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [statuses, setStatuses] = useState([]);
  const [users, setUsers] = useState([]);
  const [notification, setNotification] = useState("");
  const [filterMode, setFilterMode] = useState("all"); // "all" или "my"

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
    // Валидация обязательных полей
    const userId = selectedTask.userId || selectedTask.user?.id;
    if (!selectedTask.name || !selectedTask.status || !userId) {
      setNotification("Заполните все обязательные поля!");
      return;
    }
    
    // Подготовка данных для отправки
    const dataToSend = {
      name: selectedTask.name,
      description: selectedTask.description,
      status: selectedTask.status,
      userId: userId
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
      setNotification("Ошибка при сохранении"); 
    }
  };
  const handleDelete = async (id) => {
    await api.delete(`${API_URL}/api/tasks/${id}`);
    fetchTasks();
    setNotification("Задача удалена");
  };

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
          <Button 
            variant="contained" 
            color="primary" 
            size="small"
            onClick={() => handleOpen()}
            sx={{ textTransform: 'none' }}
          >
            + Создать
          </Button>
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
          <FormControl margin="dense" fullWidth required>
            <InputLabel>Рабочий</InputLabel>
            <Select 
              value={selectedTask?.userId || selectedTask?.user?.id || ""} 
              onChange={e => setSelectedTask(t => ({ ...t, userId: e.target.value }))} 
              label="Рабочий"
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
                  {u.username} (ID: {u.id}) - {u.role === 'WORKER' ? 'Рабочий' : 'Начальник'}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
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
        </DialogContent>
        <DialogActions>
          {selectedTask?.id && <Button color="error" onClick={() => { handleDelete(selectedTask.id); handleClose(); }}>Удалить</Button>}
          <Button onClick={handleClose}>Отмена</Button>
          <Button onClick={handleSave} variant="contained">Сохранить</Button>
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
