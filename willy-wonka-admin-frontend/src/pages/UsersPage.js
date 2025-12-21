import React, { useEffect, useState } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Snackbar, Alert, MenuItem, FormControl, InputLabel, Select } from "@mui/material";
import api, { API_URL } from "../api";

const columns = [
  { field: "id", headerName: "ID", width: 80 },
  { field: "username", headerName: "Логин", width: 200 },
  { field: "role", headerName: "Роль", width: 160 },
];

const ROLES = [
  { value: "WORKER", label: "Рабочий" },
  { value: "FOREMAN", label: "Начальник цеха" }
];

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState(null);
  const [notification, setNotification] = useState("");

  const fetchUsers = async () => {
    const { data } = await api.get(`${API_URL}/api/users`);
    setUsers(data);
  };
  useEffect(() => { fetchUsers(); }, []);
  const handleOpen = (user) => { setSelected(user || { username: "", role: "WORKER", password: "" }); setOpen(true); };
  const handleClose = () => { setOpen(false); setSelected(null); };
  const handleSave = async () => {
    try {
      if (selected.id) {
        await api.put(`${API_URL}/api/users/${selected.id}`, selected);
        setNotification("Пользователь обновлён");
      } else {
        await api.post(`${API_URL}/api/users`, selected);
        setNotification("Пользователь создан");
      }
      fetchUsers();
      handleClose();
    } catch(err){ setNotification("Ошибка при сохранении"); }
  };
  const handleDelete = async (id) => { await api.delete(`${API_URL}/api/users/${id}`); fetchUsers(); setNotification("Пользователь удалён"); };

  return (
    <Box>
      <Typography variant="h4" mb={2}>Сотрудники</Typography>
      <Button sx={{ mb: 2 }} variant="contained" onClick={() => handleOpen()}>Создать</Button>
      <DataGrid rows={users} columns={columns} autoHeight pageSize={10} onRowDoubleClick={({ row }) => handleOpen(row)} style={{ background: "#fff" }} />
      <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
        <DialogTitle>{selected?.id ? "Редактировать" : "Создать"}</DialogTitle>
        <DialogContent>
          <TextField label="Логин" margin="dense" fullWidth value={selected?.username || ""} onChange={e => setSelected(u => ({ ...u, username: e.target.value }))} />
          <FormControl margin="dense" fullWidth>
            <InputLabel>Роль</InputLabel>
            <Select value={selected?.role || "WORKER"} onChange={e => setSelected(u => ({ ...u, role: e.target.value }))} label="Роль">
              {ROLES.map(r => <MenuItem value={r.value} key={r.value}>{r.label}</MenuItem>)}
            </Select>
          </FormControl>
          {!selected?.id && (
            <TextField label="Пароль" margin="dense" fullWidth type="password" value={selected?.password || ""} onChange={e => setSelected(u => ({ ...u, password: e.target.value }))} />
          )}
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
