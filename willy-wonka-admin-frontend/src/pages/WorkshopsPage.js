import React, { useEffect, useState, useMemo, useCallback } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Snackbar, Alert, FormControl, InputLabel, Select, MenuItem, Chip, IconButton } from "@mui/material";
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { API_URL } from "../api";
import { usePermissions } from "../hooks/usePermissions";

export default function WorkshopsPage() {
  const permissions = usePermissions();
  const [list, setList] = useState([]);
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState(null);
  const [users, setUsers] = useState([]);
  const [notification, setNotification] = useState("");
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [workshopToDelete, setWorkshopToDelete] = useState(null);
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
      setSelected({ 
        name: "", 
        description: "", 
        capacity: null, 
        visitDurationMinutes: 15, 
        foremanIds: [] 
      });
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
        capacity: selected.capacity,
        visitDurationMinutes: selected.visitDurationMinutes,
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

  const handleEditCallback = useCallback((item) => {
    if (item) {
      const foremanIds = item.foremans?.map(f => f.id) || [];
      setSelected({ ...item, foremanIds });
    }
    setOpen(true);
  }, []);

  const handleDeleteCallback = useCallback((workshop) => {
    setWorkshopToDelete(workshop);
    setDeleteDialogOpen(true);
  }, []);

  const confirmDelete = async () => {
    if (workshopToDelete) {
      try {
        await api.delete(`${API_URL}/api/workshops/${workshopToDelete.id}`);
        fetchList();
        setNotification("Цех удалён");
      } catch (error) {
        console.error("Ошибка удаления:", error);
        setNotification("Ошибка при удалении");
      }
    }
    setDeleteDialogOpen(false);
    setWorkshopToDelete(null);
  };

  const cancelDelete = () => {
    setDeleteDialogOpen(false);
    setWorkshopToDelete(null);
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
      field: "capacity", 
      headerName: "Вместимость", 
      width: 120,
      valueGetter: (params) => params.row.capacity || '-'
    },
    { 
      field: "visitDurationMinutes", 
      headerName: "Длительность (мин)", 
      width: 150,
      valueGetter: (params) => params.row.visitDurationMinutes || '-'
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
      width: 120,
      valueGetter: (params) => params.row.equipments?.length || 0 
    },
    {
      field: "actions",
      headerName: "Действия",
      width: 120,
      sortable: false,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          {permissions.canEditWorkshop && (
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
          {permissions.canDeleteWorkshop && (
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
      )
    }
  ], [handleEditCallback, handleDeleteCallback, permissions]);

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Цеха</Typography>
        {permissions.canCreateWorkshop && (
          <Button 
            variant="contained" 
            color="primary" 
            size="small"
            onClick={() => handleOpen()}
            sx={{ textTransform: 'none' }}
          >
            + Добавить
          </Button>
        )}
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
            label="Вместимость (количество человек)" 
            margin="dense" 
            fullWidth 
            type="number"
            inputProps={{ min: 1 }}
            value={selected?.capacity || ""} 
            onChange={e => setSelected(t => ({ ...t, capacity: e.target.value ? parseInt(e.target.value) : null }))} 
          />
          <TextField 
            label="Длительность посещения (минуты)" 
            margin="dense" 
            fullWidth 
            type="number"
            inputProps={{ min: 5, max: 120 }}
            value={selected?.visitDurationMinutes || ""} 
            onChange={e => setSelected(t => ({ ...t, visitDurationMinutes: e.target.value ? parseInt(e.target.value) : null }))} 
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
      {/* Диалог подтверждения удаления */}
      <Dialog open={deleteDialogOpen} onClose={cancelDelete}>
        <DialogTitle>Подтверждение удаления</DialogTitle>
        <DialogContent>
          <Typography>
            Вы уверены, что хотите удалить цех <strong>{workshopToDelete?.name}</strong>?
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
