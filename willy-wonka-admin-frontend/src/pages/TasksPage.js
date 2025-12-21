import React, { useEffect, useState } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem, Select, FormControl, InputLabel, CircularProgress, Snackbar, Alert } from "@mui/material";
import api, { API_URL } from "../api";

const columns = [
  { field: "id", headerName: "ID", width: 60 },
  { field: "title", headerName: "Название", width: 220 },
  { field: "description", headerName: "Описание", width: 280 },
  { field: "priority", headerName: "Приоритет", width: 120 },
  { field: "status", headerName: "Статус", width: 140 },
  { field: "workerId", headerName: "ID Рабочего", width: 100 },
  { field: "deadline", headerName: "Дедлайн", width: 110 },
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
    setSelectedTask(task || { title: "", description: "", priority: "", status: statuses[0] || "", deadline: "", workerId: "" });
    setOpen(true);
  };
  const handleClose = () => { setOpen(false); setSelectedTask(null); };

  const handleSave = async () => {
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
    } catch (e) { setNotification("Ошибка при сохранении"); }
  };
  const handleDelete = async (id) => {
    await api.delete(`${API_URL}/api/tasks/${id}`);
    fetchTasks();
    setNotification("Задача удалена");
  };

  return (
    <Box>
      <Typography variant="h4" mb={2}>Задачи</Typography>
      <Button sx={{ mb: 2 }} variant="contained" color="primary" onClick={() => handleOpen()}>Создать задачу</Button>
      {loading ? <CircularProgress /> : (
        <DataGrid rows={tasks} columns={columns} autoHeight pageSize={10}
          onRowDoubleClick={({ row }) => handleOpen(row)}
          sx={{ background: "#fff" }}
        />
      )}
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedTask?.id ? "Редактировать задачу" : "Создать задачу"}</DialogTitle>
        <DialogContent>
          <TextField label="Название" margin="dense" fullWidth value={selectedTask?.title || ""} onChange={e => setSelectedTask(t => ({ ...t, title: e.target.value }))} />
          <TextField label="Описание" margin="dense" fullWidth multiline value={selectedTask?.description || ""} onChange={e => setSelectedTask(t => ({ ...t, description: e.target.value }))} />
          <TextField label="Приоритет" margin="dense" fullWidth value={selectedTask?.priority || ""} onChange={e => setSelectedTask(t => ({ ...t, priority: e.target.value }))} />
          <TextField label="Дедлайн" margin="dense" fullWidth type="date" value={selectedTask?.deadline || ""} onChange={e => setSelectedTask(t => ({ ...t, deadline: e.target.value }))} InputLabelProps={{shrink:true}} />
          <TextField label="Рабочий (ID)" margin="dense" fullWidth type="number" value={selectedTask?.workerId || ""} onChange={e => setSelectedTask(t => ({ ...t, workerId: e.target.value }))} />
          <FormControl margin="dense" fullWidth>
            <InputLabel>Статус</InputLabel>
            <Select value={selectedTask?.status || ""} onChange={e => setSelectedTask(t => ({ ...t, status: e.target.value }))} label="Статус">
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
          {notification && <Alert severity="info">{notification}</Alert>}
      </Snackbar>
    </Box>
  );
}
