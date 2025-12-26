import React, { useEffect, useState } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Snackbar, Alert, FormControl, InputLabel, Select, MenuItem, Chip } from "@mui/material";
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
    field: "foremans", 
    headerName: "Начальники", 
    flex: 1,
    minWidth: 150,
    valueGetter: (params) => params.row.foremans?.map(f => f.username).join(", ") || "-",
    renderCell: (params) => (
      <div style={{ whiteSpace: 'normal', wordWrap: 'break-word', lineHeight: '1.5' }}>
        {params.value}
      </div>
    )
  },
  { 
    field: "equipments", 
    headerName: "Оборудование", 
    width: 140,
    valueGetter: (params) => params.row.equipments?.length || 0 
  },
];

export default function WorkshopsPage() {
  const [list, setList] = useState([]);
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState(null);
  const [users, setUsers] = useState([]);
  const [notification, setNotification] = useState("");
  const fetchList = async () => {
    const { data } = await api.get(`${API_URL}/api/workshops`);
    setList(data);
  };
  
  const fetchUsers = async () => {
    try {
      const { data } = await api.get(`${API_URL}/api/users`);
      // Фильтруем только начальников
      const foremans = data.filter(u => u.role === 'FOREMAN');
      setUsers(foremans);
    } catch (error) {
      console.error("Ошибка загрузки пользователей:", error);
    }
  };
  
  useEffect(() => { fetchList(); fetchUsers(); }, []);
  const handleOpen = (item) => { 
    if (item) {
      // При редактировании конвертируем массив foremans в массив ID
      const foremanIds = item.foremans?.map(f => f.id) || [];
      setSelected({ ...item, foremanIds });
    } else {
      setSelected({ name: "", description: "", foremanIds: [] });
    }
    setOpen(true); 
  };
  const handleClose = () => { setOpen(false); setSelected(null); };
  const handleSave = async () => {
    // Валидация обязательных полей
    if (!selected.name) {
      setNotification("Заполните все обязательные поля!");
      return;
    }
    
    try {
      const payload = {
        name: selected.name,
        description: selected.description,
        foremanIds: selected.foremanIds || []
      };
      
      if (selected.id) {
        await api.put(`${API_URL}/api/workshops/${selected.id}`, payload);
        setNotification("Цех обновлён");
      } else {
        await api.post(`${API_URL}/api/workshops`, payload);
        setNotification("Цех добавлен");
      }
      fetchList();
      handleClose();
    } catch(e){ 
      console.error(e);
      setNotification("Ошибка при сохранении"); 
    }
  };
  const handleDelete = async (id) => { await api.delete(`${API_URL}/api/workshops/${id}`); fetchList(); setNotification("Цех удалён"); };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Цеха</Typography>
        <Button 
          variant="contained" 
          color="primary" 
          size="small"
          onClick={() => handleOpen()}
          sx={{ textTransform: 'none' }}
        >
          + Добавить
        </Button>
      </Box>
      <Box sx={{ flexGrow: 1, minHeight: 0 }}>
        <DataGrid 
          rows={list} 
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
            background:"#fff",
            '& .MuiDataGrid-cell': {
              py: 1.5,
            }
          }} 
        />
      </Box>
      <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
        <DialogTitle>{selected?.id ? "Редактировать цех" : "Добавить цех"}</DialogTitle>
        <DialogContent>
          <TextField 
            label="Название" 
            margin="dense" 
            fullWidth 
            required
            value={selected?.name || ""} 
            onChange={e => setSelected(t => ({ ...t, name: e.target.value }))} 
          />
          <TextField 
            label="Описание" 
            margin="dense" 
            fullWidth 
            multiline 
            rows={3}
            value={selected?.description || ""} 
            onChange={e => setSelected(t => ({ ...t, description: e.target.value }))} 
          />
          <FormControl margin="dense" fullWidth>
            <InputLabel>Начальники цеха</InputLabel>
            <Select 
              multiple
              value={selected?.foremanIds || []} 
              onChange={e => setSelected(t => ({ ...t, foremanIds: e.target.value }))}
              label="Начальники цеха"
              MenuProps={{
                PaperProps: {
                  style: {
                    maxHeight: 300,
                  },
                },
              }}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((id) => {
                    const user = users.find(u => u.id === id);
                    return <Chip key={id} label={user?.username || `ID: ${id}`} size="small" />;
                  })}
                </Box>
              )}
            >
              {users.map(u => (
                <MenuItem value={u.id} key={u.id}>
                  {u.username} (ID: {u.id})
                </MenuItem>
              ))}
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
      {notification ? (
        <Alert severity="info">
          {notification}
        </Alert>
      ) : null}
      </Snackbar>
    </Box>
  );
}
