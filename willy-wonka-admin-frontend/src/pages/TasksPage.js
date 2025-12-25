import React, { useEffect, useState } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem, Select, FormControl, InputLabel, CircularProgress, Snackbar, Alert } from "@mui/material";
import api, { API_URL } from "../api";

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
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [statuses, setStatuses] = useState([]);
  const [notification, setNotification] = useState("");

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

  useEffect(() => {
    fetchTasks();
    fetchStatuses();
  }, []);

  const handleOpen = (task) => {
    setSelectedTask(task || { name: "", description: "", status: statuses[0] || "", userId: "" });
    setOpen(true);
  };
  const handleClose = () => { setOpen(false); setSelectedTask(null); };

  const handleSave = async () => {
    // Валидация обязательных полей
    if (!selectedTask.name || !selectedTask.status || !selectedTask.userId) {
      setNotification("Заполните все обязательные поля!");
      return;
    }
    
    try {
      if (selectedTask.id) {
        await api.put(`${API_URL}/api/tasks/${selectedTask.id}`, selectedTask);
        setNotification("Задача обновлена!");
      } else {
        await api.post(`${API_URL}/api/tasks`, selectedTask);
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
      {loading ? <CircularProgress /> : (
        <Box sx={{ flexGrow: 1, minHeight: 0 }}>
          <DataGrid 
            rows={tasks} 
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
          <TextField 
            label="Рабочий (ID)" 
            margin="dense" 
            fullWidth 
            type="number" 
            required
            value={selectedTask?.userId || selectedTask?.user?.id || ""} 
            onChange={e => setSelectedTask(t => ({ ...t, userId: e.target.value }))} 
          />
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
