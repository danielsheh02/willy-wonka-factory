import React, { useState } from "react";
import {
  Box, Typography, TextField, Button, Card, CardContent, Grid,
  CircularProgress, Alert, Divider, Chip, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Paper, Stack
} from "@mui/material";
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import AssessmentIcon from '@mui/icons-material/Assessment';
import api, { API_URL } from "../api";
import { toUTCString, formatDate, parseUTCDate } from "../utils/dateUtils";

export default function ReportsPage() {
  const [startDate, setStartDate] = useState(getFirstDayOfMonth());
  const [endDate, setEndDate] = useState(getCurrentDate());
  const [loading, setLoading] = useState(false);
  const [reportData, setReportData] = useState(null);
  const [error, setError] = useState("");

  function getFirstDayOfMonth() {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 16);
  }

  function getCurrentDate() {
    return new Date().toISOString().slice(0, 16);
  }

  const handleGenerate = async () => {
    if (!startDate || !endDate) {
      setError("Укажите период отчета");
      return;
    }

    const start = new Date(startDate);
    const end = new Date(endDate);
    
    if (start >= end) {
      setError("Дата начала должна быть раньше даты окончания");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const { data } = await api.get(`${API_URL}/api/reports/statistics`, {
        params: {
          startDate: toUTCString(start),
          endDate: toUTCString(end)
        }
      });
      setReportData(data);
    } catch (err) {
      console.error("Ошибка генерации отчета:", err);
      setError("Ошибка при генерации отчета");
    }

    setLoading(false);
  };

  const handleExportPDF = () => {
    // Используем window.print() для экспорта в PDF
    // Пользователь сможет выбрать "Сохранить как PDF" в диалоге печати
    window.print();
  };

  const StatCard = ({ title, value, subtitle, color = "primary" }) => (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="h6" color="text.secondary" gutterBottom>
          {title}
        </Typography>
        <Typography variant="h3" color={`${color}.main`} sx={{ mb: 1 }}>
          {value !== null && value !== undefined ? value : 0}
        </Typography>
        {subtitle && (
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        )}
      </CardContent>
    </Card>
  );

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', pb: 4 }}>
      {/* Заголовок и фильтры - не печатаются */}
      <Box className="no-print" sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          📊 Отчеты и статистика
        </Typography>

        <Paper sx={{ p: 3, mb: 3 }}>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={4}>
              <TextField
                label="Дата начала"
                type="datetime-local"
                fullWidth
                InputLabelProps={{ shrink: true }}
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Дата окончания"
                type="datetime-local"
                fullWidth
                InputLabelProps={{ shrink: true }}
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <Stack direction="row" spacing={2}>
                <Button
                  variant="contained"
                  fullWidth
                  startIcon={<AssessmentIcon />}
                  onClick={handleGenerate}
                  disabled={loading}
                >
                  {loading ? <CircularProgress size={24} /> : "Сформировать отчет"}
                </Button>
                {reportData && (
                  <Button
                    variant="outlined"
                    startIcon={<PictureAsPdfIcon />}
                    onClick={handleExportPDF}
                  >
                    PDF
                  </Button>
                )}
              </Stack>
            </Grid>
          </Grid>

          {error && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {error}
            </Alert>
          )}
        </Paper>
      </Box>

      {/* Отчет - печатается */}
      {reportData && (
        <Box id="report-content">
          {/* Заголовок отчета - виден только при печати */}
          <Box className="print-only" sx={{ display: 'none', mb: 4 }}>
            <Typography variant="h4" align="center" gutterBottom>
              Отчет по деятельности фабрики
            </Typography>
            <Typography variant="body1" align="center" color="text.secondary">
              Период: {new Date(startDate).toLocaleString('ru-RU')} - {new Date(endDate).toLocaleString('ru-RU')}
            </Typography>
            <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 2 }}>
              Дата формирования: {new Date().toLocaleString('ru-RU')}
            </Typography>
            <Divider sx={{ my: 3 }} />
          </Box>

          {/* Статистика по задачам */}
          <Typography variant="h5" gutterBottom sx={{ mt: 2 }}>
            📋 Статистика по задачам
          </Typography>
          <Grid container spacing={2} sx={{ mb: 4 }}>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Всего задач"
                value={reportData.totalTasks}
                color="primary"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Выполнено"
                value={reportData.completedTasks}
                color="success"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="В работе"
                value={reportData.inProgressTasks}
                color="warning"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Процент выполнения"
                value={`${reportData.completionRate?.toFixed(1)}%`}
                color="info"
              />
            </Grid>
          </Grid>

          {/* Топ рабочих */}
          {reportData.topWorkers && reportData.topWorkers.length > 0 && (
            <Box sx={{ mb: 4 }}>
              <Typography variant="h6" gutterBottom>
                🏆 Топ-5 рабочих по выполненным задачам
              </Typography>
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell><strong>Рабочий</strong></TableCell>
                      <TableCell align="right"><strong>Выполнено</strong></TableCell>
                      <TableCell align="right"><strong>Всего</strong></TableCell>
                      <TableCell align="right"><strong>% выполнения</strong></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {reportData.topWorkers.map((worker, index) => (
                      <TableRow key={worker.userId}>
                        <TableCell>
                          {index === 0 && "🥇 "}
                          {index === 1 && "🥈 "}
                          {index === 2 && "🥉 "}
                          {worker.username}
                        </TableCell>
                        <TableCell align="right">{worker.completedTasks}</TableCell>
                        <TableCell align="right">{worker.totalTasks}</TableCell>
                        <TableCell align="right">
                          {((worker.completedTasks / worker.totalTasks) * 100).toFixed(1)}%
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}

          {/* Статистика по экскурсиям */}
          <Divider sx={{ my: 4 }} />
          <Typography variant="h5" gutterBottom>
            🎫 Статистика по экскурсиям
          </Typography>
          <Grid container spacing={2} sx={{ mb: 4 }}>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Всего экскурсий"
                value={reportData.totalExcursions}
                color="primary"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Завершено"
                value={reportData.completedExcursions}
                color="success"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Запланировано"
                value={reportData.upcomingExcursions}
                color="info"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Посетителей"
                value={reportData.totalParticipants}
                subtitle="Всего человек"
                color="secondary"
              />
            </Grid>
          </Grid>

          {/* Популярные цеха */}
          {reportData.popularWorkshops && reportData.popularWorkshops.length > 0 && (
            <Box sx={{ mb: 4 }}>
              <Typography variant="h6" gutterBottom>
                ⭐ Популярные цеха для экскурсий
              </Typography>
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell><strong>Цех</strong></TableCell>
                      <TableCell align="right"><strong>Количество посещений</strong></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {reportData.popularWorkshops.map((workshop) => (
                      <TableRow key={workshop.workshopId}>
                        <TableCell>{workshop.workshopName}</TableCell>
                        <TableCell align="right">{workshop.visitCount}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}

          {/* Статистика по оборудованию */}
          <Divider sx={{ my: 4 }} />
          <Typography variant="h5" gutterBottom>
            ⚙️ Статистика по оборудованию
          </Typography>
          <Grid container spacing={2} sx={{ mb: 4 }}>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Всего единиц"
                value={reportData.totalEquipment}
                color="primary"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Работает"
                value={reportData.workingEquipment}
                color="success"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="В ремонте"
                value={reportData.underRepairEquipment}
                color="warning"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Средн. здоровье"
                value={`${reportData.averageHealth?.toFixed(1)}%`}
                color="info"
              />
            </Grid>
          </Grid>

          {/* Статистика по золотым билетам */}
          <Divider sx={{ my: 4 }} />
          <Typography variant="h5" gutterBottom>
            🎟️ Статистика по золотым билетам
          </Typography>
          <Grid container spacing={2} sx={{ mb: 4 }}>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Всего билетов"
                value={reportData.totalTickets}
                color="primary"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Активные"
                value={reportData.activeTickets}
                color="success"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Забронировано"
                value={reportData.bookedTickets}
                color="warning"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                title="Использовано"
                value={reportData.usedTickets}
                color="info"
              />
            </Grid>
          </Grid>

          {/* Детальные таблицы */}
          {reportData.tasksData && reportData.tasksData.length > 0 && (
            <Box sx={{ mb: 4, pageBreakBefore: 'always' }}>
              <Typography variant="h6" gutterBottom>
                📝 Детальный список задач
              </Typography>
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell><strong>ID</strong></TableCell>
                      <TableCell><strong>Название</strong></TableCell>
                      <TableCell><strong>Статус</strong></TableCell>
                      <TableCell><strong>Рабочий</strong></TableCell>
                      <TableCell><strong>Создано</strong></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {reportData.tasksData.slice(0, 50).map((task) => (
                      <TableRow key={task.id}>
                        <TableCell>{task.id}</TableCell>
                        <TableCell>{task.name}</TableCell>
                        <TableCell>
                          <Chip
                            label={task.status}
                            size="small"
                            color={
                              task.status === 'COMPLETED' ? 'success' :
                              task.status === 'IN_PROGRESS' ? 'primary' : 'default'
                            }
                          />
                        </TableCell>
                        <TableCell>{task.username}</TableCell>
                        <TableCell>
                          {formatDate(task.createdAt)}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              {reportData.tasksData.length > 50 && (
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                  Показано 50 из {reportData.tasksData.length} задач
                </Typography>
              )}
            </Box>
          )}
        </Box>
      )}

      {/* Стили для печати */}
      <style>{`
        @media print {
          .no-print {
            display: none !important;
          }
          .print-only {
            display: block !important;
          }
          body {
            print-color-adjust: exact;
            -webkit-print-color-adjust: exact;
          }
          @page {
            margin: 2cm;
          }
        }
      `}</style>
    </Box>
  );
}

