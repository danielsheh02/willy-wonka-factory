import React, { useState, useEffect } from "react";
import {
  Box, Typography, TextField, Button, Card, CardContent,
  Stepper, Step, StepLabel, Alert, CircularProgress,
  List, ListItem, ListItemButton, ListItemText, Divider, Chip,
  Snackbar
} from "@mui/material";
import api, { API_URL } from "../api";
import { formatDateTime, parseUTCDate } from "../utils/dateUtils";

const steps = ["Введите номер билета", "Выберите экскурсию", "Введите данные", "Подтверждение"];

const statusLabels = {
  DRAFT: "Черновик",
  CONFIRMED: "Подтверждена",
  IN_PROGRESS: "В процессе",
  COMPLETED: "Завершена",
  CANCELLED: "Отменена"
};

const statusColors = {
  DRAFT: "default",
  CONFIRMED: "success",
  IN_PROGRESS: "info",
  COMPLETED: "default",
  CANCELLED: "error"
};

export default function PublicBookingPage() {
  const [activeStep, setActiveStep] = useState(0);
  const [ticketNumber, setTicketNumber] = useState("");
  const [ticketValid, setTicketValid] = useState(null);
  const [validationError, setValidationError] = useState("");
  const [loading, setLoading] = useState(false);

  const [excursions, setExcursions] = useState([]);
  const [selectedExcursion, setSelectedExcursion] = useState(null);

  const [holderName, setHolderName] = useState("");
  const [holderEmail, setHolderEmail] = useState("");

  const [bookingResult, setBookingResult] = useState(null);
  const [error, setError] = useState("");
  const [notification, setNotification] = useState("");

  useEffect(() => {
    if (activeStep === 1) {
      fetchExcursions();
    }
  }, [activeStep]);

  const fetchExcursions = async () => {
    setLoading(true);
    try {
      const { data } = await api.get(`${API_URL}/api/excursions`);
      // Фильтруем только будущие подтвержденные экскурсии
      const availableExcursions = data.filter(
        (exc) => exc.status === "CONFIRMED" && parseUTCDate(exc.startTime) > new Date()
      );
      setExcursions(availableExcursions);
    } catch (err) {
      console.error("Ошибка загрузки экскурсий:", err);
      setError("Не удалось загрузить список экскурсий");
    }
    setLoading(false);
  };

  const handleValidateTicket = async () => {
    if (!ticketNumber || ticketNumber.trim().length < 5) {
      setValidationError("Введите корректный номер билета");
      return;
    }

    setLoading(true);
    setValidationError("");

    try {
      const { data } = await api.get(`${API_URL}/api/tickets/validate/${ticketNumber.trim().toUpperCase()}`);

      if (data.valid) {
        setTicketValid(data.ticket);
        
        // Проверяем статус билета
        if (data.ticket.status === 'BOOKED') {
          // Билет уже забронирован - показываем шаг управления бронированием
          setActiveStep(4); // Новый шаг для управления существующим бронированием
        } else if (data.ticket.status === 'ACTIVE') {
          // Билет активен - можно записаться
          setActiveStep(1);
        } else {
          // USED, EXPIRED, CANCELLED
          setValidationError("Этот билет больше не действителен. Статус: " + data.ticket.status);
        }
      } else {
        // Билет не найден или невалиден
        if (!data.ticket) {
          setValidationError("Билет с таким номером не найден");
        } else {
          // Билет найден, но невалиден (USED, EXPIRED, CANCELLED или истек срок)
          const status = data.ticket.status;
          if (status === 'USED') {
            setValidationError("Этот билет уже был использован");
          } else if (status === 'EXPIRED') {
            setValidationError("Срок действия этого билета истек");
          } else if (status === 'CANCELLED') {
            setValidationError("Этот билет был отменен");
          } else if (data.ticket.expiresAt && parseUTCDate(data.ticket.expiresAt) < new Date()) {
            setValidationError("Срок действия этого билета истек");
          } else {
            setValidationError("Билет недействителен. Статус: " + status);
          }
        }
      }
    } catch (err) {
      console.error("Ошибка проверки билета:", err);
      setValidationError(err.response?.data?.error || "Ошибка при проверке билета");
    }

    setLoading(false);
  };

  const handleSelectExcursion = (excursion) => {
    setSelectedExcursion(excursion);
    setActiveStep(2);
  };

  const handleSubmitBooking = async () => {
    if (!holderName || !holderEmail) {
      setError("Заполните все обязательные поля");
      return;
    }

    if (!holderEmail.includes("@")) {
      setError("Введите корректный email");
      return;
    }

    setLoading(true);
    setError("");

    try {
      // Если билет был BOOKED, сначала отменяем старое бронирование
      if (ticketValid && ticketValid.status === 'BOOKED') {
        await api.delete(`${API_URL}/api/tickets/${ticketNumber.trim().toUpperCase()}/cancel`);
      }

      // Создаем новое бронирование
      const { data } = await api.post(`${API_URL}/api/tickets/book`, {
        ticketNumber: ticketNumber.trim().toUpperCase(),
        excursionId: selectedExcursion.id,
        holderName: holderName.trim(),
        holderEmail: holderEmail.trim()
      });

      setBookingResult(data);
      setActiveStep(3);
    } catch (err) {
      console.error("Ошибка бронирования:", err);
      setError(err.response?.data?.error || "Ошибка при бронировании");
    }

    setLoading(false);
  };

  const handleCancelBooking = async () => {
    if (!ticketNumber) {
      setError("Номер билета не найден");
      return;
    }

    setLoading(true);
    setError("");

    try {
      await api.delete(`${API_URL}/api/tickets/${ticketNumber.trim().toUpperCase()}/cancel`);
      
      // Показываем уведомление
      setNotification("Бронирование успешно отменено");
      
      // Сбрасываем форму, но не очищаем notification
      setActiveStep(0);
      setTicketNumber("");
      setTicketValid(null);
      setValidationError("");
      setSelectedExcursion(null);
      setHolderName("");
      setHolderEmail("");
      setBookingResult(null);
      setError("");
      // notification остается и закроется сам через 4 секунды
    } catch (err) {
      console.error("Ошибка отмены бронирования:", err);
      setError(err.response?.data?.error || "Ошибка при отмене бронирования");
    }

    setLoading(false);
  };

  const handleRebookToAnotherExcursion = () => {
    // Заполняем данные владельца из текущего билета, если они есть
    if (ticketValid) {
      if (ticketValid.holderName && !holderName) {
        setHolderName(ticketValid.holderName);
      }
      if (ticketValid.holderEmail && !holderEmail) {
        setHolderEmail(ticketValid.holderEmail);
      }
    }
    // Переходим к выбору новой экскурсии (отмена произойдет при новом бронировании)
    setActiveStep(1);
  };

  const handleReset = () => {
    setActiveStep(0);
    setTicketNumber("");
    setTicketValid(null);
    setValidationError("");
    setSelectedExcursion(null);
    setHolderName("");
    setHolderEmail("");
    setBookingResult(null);
    setError("");
    setNotification("");
  };

  return (
    <Box sx={{ maxWidth: 800, mx: "auto", mt: 4, p: 3 }}>
      <Typography variant="h4" gutterBottom align="center" sx={{ mb: 4 }}>
        🎫 Запись на экскурсию по золотому билету
      </Typography>

      <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {/* Шаг 1: Ввод номера билета */}
      {activeStep === 0 && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Введите номер вашего золотого билета
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Номер билета состоит из 8 символов (буквы и цифры)
            </Typography>

            <TextField
              label="Номер билета"
              fullWidth
              value={ticketNumber}
              onChange={(e) => {
                setTicketNumber(e.target.value.toUpperCase());
                setValidationError("");
              }}
              placeholder="Например: GW4A7K2M"
              inputProps={{ maxLength: 10, style: { textTransform: "uppercase" } }}
              error={!!validationError}
              helperText={validationError}
              sx={{ mb: 3 }}
            />

            {validationError && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {validationError}
              </Alert>
            )}

            <Button
              variant="contained"
              fullWidth
              size="large"
              onClick={handleValidateTicket}
              disabled={loading || !ticketNumber}
            >
              {loading ? <CircularProgress size={24} /> : "Проверить билет"}
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Шаг 2: Выбор экскурсии */}
      {activeStep === 1 && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Выберите экскурсию
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Доступные экскурсии:
            </Typography>

            {loading ? (
              <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
                <CircularProgress />
              </Box>
            ) : excursions.length === 0 ? (
              <Alert severity="info">На данный момент нет доступных экскурсий</Alert>
            ) : (
              <List>
                {excursions.map((excursion, index) => (
                  <React.Fragment key={excursion.id}>
                    {index > 0 && <Divider />}
                    <ListItem disablePadding>
                      <ListItemButton onClick={() => handleSelectExcursion(excursion)}>
                        <ListItemText
                          primary={excursion.name}
                          secondary={
                            <>
                              <Typography component="span" variant="body2">
                                📅 {formatDateTime(excursion.startTime)}
                              </Typography>
                              <br />
                              <Typography component="span" variant="body2">
                                👥 {excursion.participantsCount} мест
                              </Typography>
                              {" • "}
                              <Chip
                                label={statusLabels[excursion.status] || excursion.status}
                                color={statusColors[excursion.status] || "default"}
                                size="small"
                              />
                            </>
                          }
                        />
                      </ListItemButton>
                    </ListItem>
                  </React.Fragment>
                ))}
              </List>
            )}

            <Button onClick={() => setActiveStep(0)} sx={{ mt: 2 }}>
              Назад
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Шаг 3: Ввод данных */}
      {activeStep === 2 && selectedExcursion && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Введите ваши данные
            </Typography>

            <Alert severity="info" sx={{ mb: 3 }}>
              Экскурсия: <strong>{selectedExcursion.name}</strong>
              <br />
              Дата: {formatDateTime(selectedExcursion.startTime)}
            </Alert>

            <TextField
              label="Фамилия Имя"
              fullWidth
              required
              value={holderName}
              onChange={(e) => {
                setHolderName(e.target.value);
                setError("");
              }}
              placeholder="Иванов Иван"
              sx={{ mb: 2 }}
            />

            <TextField
              label="Email"
              fullWidth
              required
              type="email"
              value={holderEmail}
              onChange={(e) => {
                setHolderEmail(e.target.value);
                setError("");
              }}
              placeholder="ivan@example.com"
              sx={{ mb: 3 }}
            />

            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <Box sx={{ display: "flex", gap: 2 }}>
              <Button onClick={() => setActiveStep(1)}>Назад</Button>
              <Button
                variant="contained"
                fullWidth
                onClick={handleSubmitBooking}
                disabled={loading}
              >
                {loading ? <CircularProgress size={24} /> : "Забронировать"}
              </Button>
            </Box>
          </CardContent>
        </Card>
      )}

      {/* Шаг 4: Подтверждение */}
      {activeStep === 3 && bookingResult && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom align="center" color="success.main">
              ✅ Бронирование успешно!
            </Typography>

            <Alert severity="success" sx={{ mb: 3 }}>
              Ваш билет успешно забронирован на экскурсию
            </Alert>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Номер билета:
              </Typography>
              <Typography variant="h6" sx={{ fontFamily: "monospace" }}>
                {bookingResult.ticketNumber}
              </Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Экскурсия:
              </Typography>
              <Typography variant="body1">{bookingResult.excursionName}</Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Дата и время:
              </Typography>
              <Typography variant="body1">
                {formatDateTime(bookingResult.excursionStartTime)}
              </Typography>
            </Box>

            <Box sx={{ mb: 3 }}>
              <Typography variant="body2" color="text.secondary">
                Владелец:
              </Typography>
              <Typography variant="body1">{bookingResult.holderName}</Typography>
              <Typography variant="body2">{bookingResult.holderEmail}</Typography>
            </Box>

            <Button variant="outlined" fullWidth onClick={handleReset}>
              Забронировать еще один билет
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Управление существующим бронированием */}
      {activeStep === 4 && ticketValid && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom align="center" color="warning.main">
              ⚠️ У вас уже есть бронирование
            </Typography>

            <Alert severity="warning" sx={{ mb: 3 }}>
              Этот билет уже забронирован на экскурсию
            </Alert>

            {ticketValid.excursionName && (
              <>
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    Текущая экскурсия:
                  </Typography>
                  <Typography variant="body1" fontWeight="bold">
                    {ticketValid.excursionName}
                  </Typography>
                </Box>

                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    Дата и время:
                  </Typography>
                  <Typography variant="body1">
                    {formatDateTime(ticketValid.excursionStartTime)}
                  </Typography>
                </Box>

                {ticketValid.holderName && (
                  <Box sx={{ mb: 3 }}>
                    <Typography variant="body2" color="text.secondary">
                      Владелец:
                    </Typography>
                    <Typography variant="body1">{ticketValid.holderName}</Typography>
                    {ticketValid.holderEmail && (
                      <Typography variant="body2">{ticketValid.holderEmail}</Typography>
                    )}
                  </Box>
                )}

                {/* Проверка, не прошла ли экскурсия */}
                {ticketValid.excursionStartTime && 
                 parseUTCDate(ticketValid.excursionStartTime) <= new Date() ? (
                  <Alert severity="error" sx={{ mb: 3 }}>
                    Экскурсия уже прошла. Перезапись невозможна.
                  </Alert>
                ) : (
                  <>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      Что вы хотите сделать?
                    </Typography>

                    {error && (
                      <Alert severity="error" sx={{ mb: 2 }}>
                        {error}
                      </Alert>
                    )}

                    <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                      <Button
                        variant="outlined"
                        color="primary"
                        fullWidth
                        onClick={handleRebookToAnotherExcursion}
                        disabled={loading}
                      >
                        Перезаписаться на другую экскурсию
                      </Button>

                      <Button
                        variant="outlined"
                        color="error"
                        fullWidth
                        onClick={handleCancelBooking}
                        disabled={loading}
                      >
                        {loading ? <CircularProgress size={24} /> : "Отменить бронирование"}
                      </Button>

                      <Button
                        variant="text"
                        fullWidth
                        onClick={handleReset}
                      >
                        Назад к вводу билета
                      </Button>
                    </Box>
                  </>
                )}
              </>
            )}
          </CardContent>
        </Card>
      )}

      {/* Уведомления */}
      <Snackbar 
        open={!!notification} 
        autoHideDuration={4000} 
        onClose={() => setNotification("")}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" onClose={() => setNotification("")}>
          {notification}
        </Alert>
      </Snackbar>
    </Box>
  );
}
