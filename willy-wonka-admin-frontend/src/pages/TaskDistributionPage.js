import React, { useState, useEffect } from "react";
import { DataGrid } from "@mui/x-data-grid";
import {
  Button, Box, Typography, Snackbar, Alert, FormControlLabel,
  Checkbox, Card, CardContent, CircularProgress, Chip
} from "@mui/material";
import AssignmentIcon from '@mui/icons-material/Assignment';
import api, { API_URL } from "../api";
import { formatDate } from "../utils/dateUtils";

export default function TaskDistributionPage() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedTaskIds, setSelectedTaskIds] = useState([]);
  const [force, setForce] = useState(false);
  const [distributing, setDistributing] = useState(false);
  const [notification, setNotification] = useState("");
  const [notificationSeverity, setNotificationSeverity] = useState("info");
  const [distributionResult, setDistributionResult] = useState(null);

  const statusColors = {
    'NOT_ASSIGNED': 'default',
    'IN_PROGRESS': 'primary',
    'COMPLETED': 'success'
  };

  const statusLabels = {
    'NOT_ASSIGNED': 'Не назначена',
    'IN_PROGRESS': 'В работе',
    'COMPLETED': 'Завершена'
  };

  const columns = [
    {
      field: "id",
      headerName: "ID",
      width: 70
    },
    {
      field: "name",
      headerName: "Название",
      flex: 1.5,
      minWidth: 200,
      renderCell: (params) => (
        <div style={{ whiteSpace: 'normal', wordWrap: 'break-word', lineHeight: '1.5' }}>
          {params.value}
        </div>
      )
    },
    {
      field: "description",
      headerName: "Описание",
      flex: 2,
      minWidth: 250,
      renderCell: (params) => (
        <div style={{ whiteSpace: 'normal', wordWrap: 'break-word', lineHeight: '1.5' }}>
          {params.value || '-'}
        </div>
      )
    },
    {
      field: "status",
      headerName: "Статус",
      width: 160,
      renderCell: (params) => (
        <Chip
          label={statusLabels[params.value] || params.value}
          color={statusColors[params.value] || 'default'}
          size="small"
        />
      )
    },
    {
      field: "createdAt",
      headerName: "Создано",
      width: 125,
      valueFormatter: (params) => formatDate(params.value)
    }
  ];

  const fetchUnassignedTasks = async () => {
    setLoading(true);
    try {
      const { data } = await api.get(`${API_URL}/api/tasks/unassigned`);
      setTasks(data);
    } catch (error) {
      console.error("Ошибка загрузки задач:", error);
      setNotification("Ошибка загрузки задач");
      setNotificationSeverity("error");
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchUnassignedTasks();
  }, []);

  const handleDistribute = async () => {
    if (selectedTaskIds.length === 0) {
      setNotification("Выберите хотя бы одну задачу для распределения");
      setNotificationSeverity("warning");
      return;
    }

    setDistributing(true);
    setDistributionResult(null);

    try {
      const { data } = await api.post(`${API_URL}/api/tasks/distribute`, {
        taskIds: selectedTaskIds,
        force: force
      });

      setDistributionResult(data);

      if (data.success) {
        setNotification(data.message);
        setNotificationSeverity("success");
        setSelectedTaskIds([]);
        // Обновляем список задач
        await fetchUnassignedTasks();
      } else {
        setNotification(data.message);
        setNotificationSeverity("warning");
      }
    } catch (error) {
      console.error("Ошибка при распределении задач:", error);
      setNotification(error.response?.data?.error || "Ошибка при распределении задач");
      setNotificationSeverity("error");
    }

    setDistributing(false);
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', p: 3 }}>
      <Typography variant="h4" gutterBottom>
        📊 Автоматическое распределение задач
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Нераспределенные задачи: {tasks.length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Выбрано: {selectedTaskIds.length}
              </Typography>
            </Box>

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'flex-end' }}>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={force}
                    onChange={(e) => setForce(e.target.checked)}
                    color="warning"
                  />
                }
                label={
                  <Typography variant="body2">
                    Принудительное распределение
                    <Typography variant="caption" display="block" color="text.secondary">
                      (игнорировать лимит задач)
                    </Typography>
                  </Typography>
                }
              />

              <Button
                variant="contained"
                color="primary"
                size="large"
                startIcon={<AssignmentIcon />}
                onClick={handleDistribute}
                disabled={distributing || selectedTaskIds.length === 0}
              >
                {distributing ? <CircularProgress size={24} /> : "Распределить задачи"}
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Card>

      {distributionResult && (
        <Card sx={{ mb: 3, bgcolor: distributionResult.success ? 'success.light' : 'warning.light' }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Результат распределения:
            </Typography>
            <Typography variant="body1">
              ✅ Распределено: {distributionResult.distributedCount} из {distributionResult.totalTasks}
            </Typography>
            {distributionResult.skippedCount > 0 && (
              <Typography variant="body1" color="warning.dark">
                ⏭️ Пропущено: {distributionResult.skippedCount}
              </Typography>
            )}
            {distributionResult.errors && distributionResult.errors.length > 0 && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" fontWeight="bold">Ошибки:</Typography>
                {distributionResult.errors.map((error, index) => (
                  <Typography key={index} variant="body2" color="error">
                    • {error}
                  </Typography>
                ))}
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Box sx={{ flexGrow: 1, minHeight: 0 }}>
          <DataGrid
            rows={tasks}
            columns={columns}
            checkboxSelection
            onRowSelectionModelChange={(newSelection) => {
              setSelectedTaskIds(newSelection);
            }}
            rowSelectionModel={selectedTaskIds}
            pageSizeOptions={[10, 25, 50, 100]}
            initialState={{
              pagination: {
                paginationModel: {
                  page: 0,
                  pageSize: 25,
                },
              },
            }}
            getRowHeight={() => 'auto'}
            sx={{
              background: "#fff",
              '& .MuiDataGrid-cell': {
                py: 1.5,
              }
            }}
          />
        </Box>
      )}

      <Snackbar
        open={!!notification}
        autoHideDuration={6000}
        onClose={() => setNotification("")}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={notificationSeverity} onClose={() => setNotification("")}>
          {notification}
        </Alert>
      </Snackbar>
    </Box>
  );
}

