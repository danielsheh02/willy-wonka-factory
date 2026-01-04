import React, { useState, useEffect, useCallback } from "react";
import { DataGrid } from "@mui/x-data-grid";
import {
  Button, Box, Typography, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Snackbar, Alert, Chip
} from "@mui/material";
import api, { API_URL } from "../api";
import { usePermissions } from "../hooks/usePermissions";
import { formatDate } from "../utils/dateUtils";

const statusLabels = {
  ACTIVE: "Активный",
  BOOKED: "Забронирован",
  USED: "Использован",
  EXPIRED: "Истек",
  CANCELLED: "Отменен"
};

const statusColors = {
  ACTIVE: "success",
  BOOKED: "warning",
  USED: "default",
  EXPIRED: "error",
  CANCELLED: "error"
};

const columns = [
  { field: "id", headerName: "ID", width: 70 },
  { field: "ticketNumber", headerName: "Номер билета", width: 130, flex: 0.5 },
  {
    field: "status",
    headerName: "Статус",
    width: 140,
    renderCell: (params) => (
      <Chip
        label={statusLabels[params.value] || params.value}
        color={statusColors[params.value] || "default"}
        size="small"
      />
    )
  },
  {
    field: "excursionName",
    headerName: "Экскурсия",
    flex: 1,
    minWidth: 150,
    valueGetter: (params) => params.row.excursionName || "-"
  },
  {
    field: "holderName",
    headerName: "Владелец",
    flex: 0.7,
    minWidth: 150,
    valueGetter: (params) => params.row.holderName || "-"
  },
  {
    field: "generatedAt",
    headerName: "Создан",
    width: 150,
    valueFormatter: (params) => formatDate(params.value)
  },
  {
    field: "expiresAt",
    headerName: "Истекает",
    width: 150,
    valueFormatter: (params) => {
      if (!params.value) return "Бессрочно";
      return formatDate(params.value);
    }
  }
];

export default function GoldenTicketsPage() {
  const permissions = usePermissions();
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [count, setCount] = useState(10);
  const [expiresInDays, setExpiresInDays] = useState("");
  const [notification, setNotification] = useState("");

  const fetchTickets = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get(`${API_URL}/api/tickets`);
      setTickets(data);
    } catch (error) {
      console.error("Ошибка загрузки билетов:", error);
      setNotification("Ошибка загрузки билетов");
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  const handleOpen = () => {
    setOpen(true);
    setCount(10);
    setExpiresInDays("");
  };

  const handleClose = () => {
    setOpen(false);
  };

  const handleGenerate = async () => {
    if (!count || count <= 0) {
      setNotification("Укажите корректное количество билетов");
      return;
    }

    try {
      const payload = { count: parseInt(count) };
      if (expiresInDays && parseInt(expiresInDays) > 0) {
        payload.expiresInDays = parseInt(expiresInDays);
      }

      const { data } = await api.post(`${API_URL}/api/tickets/generate`, payload);
      setNotification(data.message || `Создано ${data.totalGenerated} билетов`);
      fetchTickets();
      handleClose();
    } catch (err) {
      console.error(err);
      const errorMessage = err.response?.data?.error || "Ошибка при генерации билетов";
      setNotification(errorMessage);
    }
  };

  return (
    <Box sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 2 }}>
        <Typography variant="h4">Золотые билеты</Typography>
        {permissions.canGenerateTickets && (
          <Button
            variant="contained"
            size="small"
            onClick={handleOpen}
            sx={{ textTransform: "none" }}
          >
            + Сгенерировать билеты
          </Button>
        )}
      </Box>

      <Box sx={{ flexGrow: 1, minHeight: 0 }}>
        <DataGrid
          rows={tickets}
          columns={columns}
          loading={loading}
          pageSizeOptions={[10, 25, 50, 100]}
          initialState={{
            pagination: {
              paginationModel: {
                page: 0,
                pageSize: 25
              }
            }
          }}
          sx={{ background: "#fff" }}
        />
      </Box>

      {/* Диалог генерации */}
      <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
        <DialogTitle>Генерация золотых билетов</DialogTitle>
        <DialogContent>
          <TextField
            label="Количество билетов"
            margin="dense"
            fullWidth
            type="number"
            required
            inputProps={{ min: 1, max: 1000 }}
            value={count}
            onChange={(e) => setCount(e.target.value)}
            helperText="От 1 до 1000 билетов"
          />
          <TextField
            label="Истекает через (дней)"
            margin="dense"
            fullWidth
            type="number"
            inputProps={{ min: 1 }}
            value={expiresInDays}
            onChange={(e) => setExpiresInDays(e.target.value)}
            helperText="Оставьте пустым для бессрочных билетов"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose}>Отмена</Button>
          <Button onClick={handleGenerate} variant="contained">
            Сгенерировать
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!notification} autoHideDuration={4000} onClose={() => setNotification("")}>
        {notification ? <Alert severity="info">{notification}</Alert> : null}
      </Snackbar>
    </Box>
  );
}

