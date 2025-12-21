import React, { useEffect, useState } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem, Select, FormControl, InputLabel, CircularProgress, Snackbar, Alert } from "@mui/material";
import api, { API_URL } from "../api";

const columns = [
  { field: "id", headerName: "ID", width: 60 },
  { field: "name", headerName: "Название", width: 240 },
  { field: "status", headerName: "Статус", width: 140 },
  { field: "workshopId", headerName: "ID Цеха", width: 100 },
  { field: "description", headerName: "Описание", width: 260 },
];

export default function EquipmentPage() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState(null);
  const [statuses, setStatuses] = useState([]);
  const [notification, setNotification] = useState("");

  const fetchData = async () => {
    setLoading(true);
    const { data } = await api.get(`${API_URL}/api/equipments`);
    setData(data);
    setLoading(false);
  };
  const fetchStatuses = async () => {
    const { data } = await api.get(`${API_URL}/api/equipments/statuses`);
    setStatuses(data);
  };
  useEffect(() => { fetchData(); fetchStatuses(); }, []);
  const handleOpen = (item) => { setSelected(item || { name: "", status: statuses[0] || "", description: "", workshopId: "" }); setOpen(true); };
  const handleClose = () => { setOpen(false); setSelected(null); };
  const handleSave = async () => {
    try {
      if (selected.id) {
        await api.put(`${API_URL}/api/equipments/${selected.id}`, selected);
        setNotification("Оборудование обновлено!");
      } else {
        await api.post(`${API_URL}/api/equipments`, selected);
        setNotification("Оборудование добавлено!");
      }
      fetchData();
      handleClose();
    } catch(e){ setNotification("Ошибка при сохранении"); }
  };
  const handleDelete = async (id) => { await api.delete(`${API_URL}/api/equipments/${id}`); fetchData(); setNotification("Оборудование удалено"); };

  return (
    <Box>
      <Typography variant="h4" mb={2}>Оборудование</Typography>
      <Button sx={{ mb: 2 }} variant="contained" color="primary" onClick={() => handleOpen()}>Добавить оборудование</Button>
      {loading ? <CircularProgress /> : (
        <DataGrid rows={data} columns={columns} autoHeight pageSize={10} onRowDoubleClick={({ row }) => handleOpen(row)} sx={{background:"#fff"}} />
      )}
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>{selected?.id ? "Редактировать оборудование" : "Добавить оборудование"}</DialogTitle>
        <DialogContent>
          <TextField label="Название" margin="dense" fullWidth value={selected?.name || ""} onChange={e => setSelected(t => ({ ...t, name: e.target.value }))} />
          <TextField label="Описание" margin="dense" fullWidth value={selected?.description || ""} onChange={e => setSelected(t => ({ ...t, description: e.target.value }))} />
          <TextField label="ID цеха" margin="dense" fullWidth value={selected?.workshopId || ""} onChange={e => setSelected(t => ({ ...t, workshopId: e.target.value }))} />
          <FormControl margin="dense" fullWidth>
            <InputLabel>Статус</InputLabel>
            <Select value={selected?.status || ""} onChange={e => setSelected(t => ({ ...t, status: e.target.value }))} label="Статус">
              {statuses.map(st => <MenuItem value={st} key={st}>{st}</MenuItem>)}
            </Select>
          </FormControl>
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
