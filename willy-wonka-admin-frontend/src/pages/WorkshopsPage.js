import React, { useEffect, useState } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Snackbar, Alert } from "@mui/material";
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
  const [notification, setNotification] = useState("");
  const fetchList = async () => {
    const { data } = await api.get(`${API_URL}/api/workshops`);
    setList(data);
  };
  useEffect(() => { fetchList(); }, []);
  const handleOpen = (item) => { 
    if (item) {
      // При редактировании конвертируем массив foremans в строку ID через запятую
      const foremanIds = item.foremans?.map(f => f.id).join(",") || "";
      setSelected({ ...item, foremanIds });
    } else {
      setSelected({ name: "", description: "", foremanIds: "" });
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
      // Преобразуем строку foremanIds в массив чисел
      const foremanIdsArray = selected.foremanIds 
        ? selected.foremanIds.split(",").map(id => parseInt(id.trim())).filter(id => !isNaN(id))
        : [];
      
      const payload = {
        name: selected.name,
        description: selected.description,
        foremanIds: foremanIdsArray
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
          pageSize={10}
          rowsPerPageOptions={[10, 25, 50]}
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
          <TextField 
            label="ID начальников (через запятую)" 
            margin="dense" 
            fullWidth 
            value={selected?.foremanIds || ""} 
            onChange={e => setSelected(t => ({ ...t, foremanIds: e.target.value }))}
            helperText="Например: 1,2,3"
          />
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
