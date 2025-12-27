import React, { useEffect, useState } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Snackbar, Alert, MenuItem, FormControl, InputLabel, Select } from "@mui/material";
import api, { API_URL } from "../api";

const columns = [
  { 
    field: "id", 
    headerName: "ID", 
    width: 80
  },
  { 
    field: "username", 
    headerName: "Логин", 
    flex: 1,
    minWidth: 150
  },
  { 
    field: "role", 
    headerName: "Роль", 
    width: 160
  },
  { 
    field: "isBanned", 
    headerName: "Заблокирован", 
    width: 130,
    valueGetter: (params) => params.row.isBanned ? "Да" : "Нет" 
  },
  { 
    field: "workshops", 
    headerName: "Цеха", 
    flex: 1.5,
    minWidth: 200,
    valueGetter: (params) => params.row.workshops?.map(w => w.name).join(", ") || "-",
    renderCell: (params) => (
      <div style={{ whiteSpace: 'normal', wordWrap: 'break-word', lineHeight: '1.5' }}>
        {params.value}
      </div>
    )
  },
];

const ROLES = [
  { value: "WORKER", label: "Рабочий" },
  { value: "FOREMAN", label: "Начальник цеха" },
  { value: "ADMIN", label: "Администратор" },
  { value: "MASTER", label: "Мастер" },
  { value: "GUIDE", label: "Экскурсовод" }
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
    // Валидация обязательных полей
    if (!selected.username || !selected.role) {
      setNotification("Заполните все обязательные поля!");
      return;
    }
    
    // При создании пароль обязателен
    if (!selected.id && !selected.password) {
      setNotification("Укажите пароль для нового пользователя!");
      return;
    }
    
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
    } catch(err){ 
      console.error(err);
      setNotification("Ошибка при сохранении"); 
    }
  };
  const handleDelete = async (id) => { await api.delete(`${API_URL}/api/users/${id}`); fetchUsers(); setNotification("Пользователь удалён"); };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Сотрудники</Typography>
        <Button 
          variant="contained" 
          size="small"
          onClick={() => handleOpen()}
          sx={{ textTransform: 'none' }}
        >
          + Создать
        </Button>
      </Box>
      <Box sx={{ flexGrow: 1, minHeight: 0 }}>
        <DataGrid 
          rows={users} 
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
      <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
        <DialogTitle>{selected?.id ? "Редактировать" : "Создать"}</DialogTitle>
        <DialogContent>
          <TextField 
            label="Логин" 
            margin="dense" 
            fullWidth 
            required
            value={selected?.username || ""} 
            onChange={e => setSelected(u => ({ ...u, username: e.target.value }))} 
          />
          <FormControl margin="dense" fullWidth required>
            <InputLabel>Роль</InputLabel>
            <Select 
              value={selected?.role || "WORKER"} 
              onChange={e => setSelected(u => ({ ...u, role: e.target.value }))} 
              label="Роль"
            >
              {ROLES.map(r => <MenuItem value={r.value} key={r.value}>{r.label}</MenuItem>)}
            </Select>
          </FormControl>
          {!selected?.id && (
            <TextField 
              label="Пароль" 
              margin="dense" 
              fullWidth 
              type="password" 
              required
              value={selected?.password || ""} 
              onChange={e => setSelected(u => ({ ...u, password: e.target.value }))} 
            />
          )}
        </DialogContent>
        <DialogActions>
          {selected?.id && <Button color="error" onClick={() => { handleDelete(selected.id); handleClose(); }}>Удалить</Button>}
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
