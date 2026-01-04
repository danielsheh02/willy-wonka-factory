import React, { useEffect, useState, useMemo, useCallback } from "react";
import { DataGrid } from "@mui/x-data-grid";
import { Button, Box, Typography, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem, Select, FormControl, InputLabel, CircularProgress, Snackbar, Alert, IconButton, Chip } from "@mui/material";
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { API_URL } from "../api";
import { usePermissions } from "../hooks/usePermissions";

export default function EquipmentPage() {
  const permissions = usePermissions();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState(null);
  const [statuses, setStatuses] = useState([]);
  const [workshops, setWorkshops] = useState([]);
  const [notification, setNotification] = useState("");
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState(null);

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
  
  const fetchWorkshops = async () => {
    try {
      const { data } = await api.get(`${API_URL}/api/workshops`);
      setWorkshops(data);
    } catch (error) {
      console.error("Ошибка загрузки цехов:", error);
    }
  };
  
  useEffect(() => { fetchData(); fetchStatuses(); fetchWorkshops(); }, []);
  const handleOpen = (item) => { setSelected(item || { name: "", model: "", status: statuses[0] || "", description: "", health: 100, temperature: 0, workshopId: "" }); setOpen(true); };
  const handleClose = () => { setOpen(false); setSelected(null); };
  const handleSave = async () => {
    // Валидация обязательных полей
    const workshopId = selected.workshopId || selected.workshop?.id;
    if (!selected.name || !selected.status || selected.health === undefined || !workshopId) {
      setNotification("Заполните все обязательные поля!");
      return;
    }
    
    // Подготовка данных для отправки
    const dataToSend = {
      ...selected,
      workshopId: workshopId
    };
    
    try {
      if (selected.id) {
        await api.put(`${API_URL}/api/equipments/${selected.id}`, dataToSend);
        setNotification("Оборудование обновлено!");
      } else {
        await api.post(`${API_URL}/api/equipments`, dataToSend);
        setNotification("Оборудование добавлено!");
      }
      fetchData();
      handleClose();
    } catch(e){ 
      console.error(e);
      setNotification("Ошибка при сохранении"); 
    }
  };
  const handleDelete = async (id) => { await api.delete(`${API_URL}/api/equipments/${id}`); fetchData(); setNotification("Оборудование удалено"); };

  const handleEditCallback = useCallback((item) => {
    setSelected(item);
    setOpen(true);
  }, []);

  const handleDeleteCallback = useCallback((item) => {
    setItemToDelete(item);
    setDeleteDialogOpen(true);
  }, []);

  const confirmDelete = async () => {
    if (itemToDelete) {
      try {
        await api.delete(`${API_URL}/api/equipments/${itemToDelete.id}`);
        fetchData();
        setNotification("Оборудование удалено");
      } catch (error) {
        console.error("Ошибка удаления:", error);
        setNotification("Ошибка при удалении");
      }
    }
    setDeleteDialogOpen(false);
    setItemToDelete(null);
  };

  const cancelDelete = () => {
    setDeleteDialogOpen(false);
    setItemToDelete(null);
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
      field: "model", 
      headerName: "Модель", 
      flex: 0.8,
      minWidth: 120,
      renderCell: (params) => (
        <div style={{ whiteSpace: 'normal', wordWrap: 'break-word', lineHeight: '1.5' }}>
          {params.value || '-'}
        </div>
      )
    },
    { 
      field: "status", 
      headerName: "Статус", 
      width: 150,
      renderCell: (params) => {
        const statusColors = {
          'WORKING': 'success',
          'UNDER_REPAIR': 'warning',
          'BROKEN': 'error'
        };
        
        const statusLabels = {
          'WORKING': 'Работает',
          'UNDER_REPAIR': 'В ремонте',
          'BROKEN': 'Сломано'
        };
        
        return (
          <Chip 
            label={statusLabels[params.value] || params.value}
            color={statusColors[params.value] || 'default'}
            size="small"
          />
        );
      }
    },
    { 
      field: "health", 
      headerName: "Состояние", 
      width: 110
    },
    { 
      field: "temperature", 
      headerName: "Температура", 
      width: 120
    },
    { 
      field: "workshopId", 
      headerName: "ID Цеха", 
      width: 100,
      valueGetter: (params) => params.row.workshop?.id 
    },
    { 
      field: "workshopName", 
      headerName: "Цех", 
      flex: 0.8,
      minWidth: 120,
      valueGetter: (params) => params.row.workshop?.name,
      renderCell: (params) => (
        <div style={{ whiteSpace: 'normal', wordWrap: 'break-word', lineHeight: '1.5' }}>
          {params.value || '-'}
        </div>
      )
    },
    {
      field: "actions",
      headerName: "Действия",
      width: 120,
      sortable: false,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          {permissions.canEditEquipment && (
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
          {permissions.canDeleteEquipment && (
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
        <Typography variant="h4">Оборудование</Typography>
        {permissions.canCreateEquipment && (
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
      {loading ? <CircularProgress /> : (
        <Box sx={{ flexGrow: 1, minHeight: 0 }}>
          <DataGrid 
            rows={data} 
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
      )}
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>{selected?.id ? "Редактировать оборудование" : "Добавить оборудование"}</DialogTitle>
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
            label="Модель" 
            margin="dense" 
            fullWidth 
            value={selected?.model || ""} 
            onChange={e => setSelected(t => ({ ...t, model: e.target.value }))} 
          />
          <TextField 
            label="Описание" 
            margin="dense" 
            fullWidth 
            value={selected?.description || ""} 
            onChange={e => setSelected(t => ({ ...t, description: e.target.value }))} 
          />
          <TextField 
            label="Состояние (0-100)" 
            margin="dense" 
            fullWidth 
            type="number" 
            required
            inputProps={{ min: 0, max: 100 }}
            value={selected?.health || 0} 
            onChange={e => setSelected(t => ({ ...t, health: parseInt(e.target.value) }))} 
          />
          <TextField 
            label="Температура" 
            margin="dense" 
            fullWidth 
            type="number" 
            value={selected?.temperature || 0} 
            onChange={e => setSelected(t => ({ ...t, temperature: parseFloat(e.target.value) }))} 
          />
          <FormControl margin="dense" fullWidth required>
            <InputLabel>Цех</InputLabel>
            <Select 
              value={selected?.workshopId || selected?.workshop?.id || ""} 
              onChange={e => setSelected(t => ({ ...t, workshopId: e.target.value }))} 
              label="Цех"
              MenuProps={{
                PaperProps: {
                  style: {
                    maxHeight: 300,
                  },
                },
              }}
            >
              {workshops.map(w => (
                <MenuItem value={w.id} key={w.id}>
                  {w.name} (ID: {w.id})
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl margin="dense" fullWidth required>
            <InputLabel>Статус</InputLabel>
            <Select 
              value={selected?.status || ""} 
              onChange={e => setSelected(t => ({ ...t, status: e.target.value }))} 
              label="Статус"
            >
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
      {/* Диалог подтверждения удаления */}
      <Dialog open={deleteDialogOpen} onClose={cancelDelete}>
        <DialogTitle>Подтверждение удаления</DialogTitle>
        <DialogContent>
          <Typography>
            Вы уверены, что хотите удалить оборудование <strong>{itemToDelete?.name}</strong>?
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
