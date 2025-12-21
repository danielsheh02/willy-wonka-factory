import React, { useEffect, useState } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Snackbar, Alert } from "@mui/material";
import api, { API_URL } from "../api";

const columns = [
  { field: "id", headerName: "ID", width: 70 },
  { field: "name", headerName: "Название", width: 220 },
  { field: "foremanId", headerName: "ID начальника", width: 130 },
];

export default function WorkshopsPage() {
  const [list, setList] = useState([]);
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState(null);
  const [notification, setNotification] = useState("");
  const fetchList = async () => {
    const { data } = await api.get(`${API_URL}/api/workshops`);
    setList(data);
  };
  useEffect(() => { fetchList(); }, []);
  const handleOpen = (item) => { setSelected(item || { name: "", foremanId: "" }); setOpen(true); };
  const handleClose = () => { setOpen(false); setSelected(null); };
  const handleSave = async () => {
    try {
      if (selected.id) {
        await api.put(`${API_URL}/api/workshops/${selected.id}`, selected);
        setNotification("Цех обновлён");
      } else {
        await api.post(`${API_URL}/api/workshops`, selected);
        setNotification("Цех добавлен");
      }
      fetchList();
      handleClose();
    } catch(e){ setNotification("Ошибка при сохранении"); }
  };
  const handleDelete = async (id) => { await api.delete(`${API_URL}/api/workshops/${id}`); fetchList(); setNotification("Цех удалён"); };

  return (
    <Box>
      <Typography variant="h4" mb={2}>Цеха</Typography>
      <Button sx={{ mb: 2 }} variant="contained" color="primary" onClick={() => handleOpen()}>Добавить цех</Button>
      <DataGrid rows={list} columns={columns} autoHeight pageSize={10} onRowDoubleClick={({ row }) => handleOpen(row)} sx={{background:"#fff"}} />
      <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
        <DialogTitle>{selected?.id ? "Редактировать цех" : "Добавить цех"}</DialogTitle>
        <DialogContent>
          <TextField label="Название" margin="dense" fullWidth value={selected?.name || ""} onChange={e => setSelected(t => ({ ...t, name: e.target.value }))} />
          <TextField label="ID начальника" margin="dense" fullWidth value={selected?.foremanId || ""} onChange={e => setSelected(t => ({ ...t, foremanId: e.target.value }))} />
        </DialogContent>
        <DialogActions>
          {selected?.id && <Button color="error" onClick={() => { handleDelete(selected.id); handleClose(); }}>Удалить</Button>}
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
