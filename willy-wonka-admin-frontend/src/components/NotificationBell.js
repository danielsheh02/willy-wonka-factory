import React, { useState, useEffect, useCallback } from "react";
import {
  IconButton,
  Badge,
  Menu,
  MenuItem,
  Typography,
  Box,
  Divider,
  Button,
  ListItemText,
  Chip,
  Stack
} from "@mui/material";
import NotificationsIcon from "@mui/icons-material/Notifications";
import NotificationsNoneIcon from "@mui/icons-material/NotificationsNone";
import DeleteIcon from "@mui/icons-material/Delete";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import { notificationsAPI } from "../api";

const NotificationBell = () => {
  const [anchorEl, setAnchorEl] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const open = Boolean(anchorEl);

  // Загрузить уведомления
  const loadNotifications = useCallback(async () => {
    try {
      const response = await notificationsAPI.getMyNotifications();
      setNotifications(response.data);
      
      // Подсчитываем непрочитанные
      const unread = response.data.filter(n => !n.isRead).length;
      setUnreadCount(unread);
    } catch (error) {
      console.error("Ошибка загрузки уведомлений:", error);
    }
  }, []);

  // Загружаем уведомления при монтировании
  useEffect(() => {
    loadNotifications();
    // Автоматическое обновление каждые 30 секунд
    const interval = setInterval(loadNotifications, 5000);
    return () => clearInterval(interval);
  }, [loadNotifications]);

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  // Отметить уведомление как прочитанное
  const handleMarkAsRead = async (notificationId) => {
    try {
      await notificationsAPI.markAsRead(notificationId);
      await loadNotifications();
    } catch (error) {
      console.error("Ошибка отметки уведомления:", error);
    }
  };

  // Отметить все как прочитанные
  const handleMarkAllAsRead = async () => {
    try {
      await notificationsAPI.markAllAsRead();
      await loadNotifications();
    } catch (error) {
      console.error("Ошибка отметки всех уведомлений:", error);
    }
  };

  // Удалить уведомление
  const handleDelete = async (notificationId, event) => {
    event.stopPropagation();
    try {
      await notificationsAPI.deleteNotification(notificationId);
      await loadNotifications();
    } catch (error) {
      console.error("Ошибка удаления уведомления:", error);
    }
  };

  // Получить цвет для типа уведомления
  const getNotificationColor = (type) => {
    switch (type) {
      case "TASK_ASSIGNED":
        return "primary";
      case "TASK_UPDATED":
        return "info";
      case "TASK_COMPLETED":
        return "success";
      case "EQUIPMENT_ALERT":
        return "error";
      case "SYSTEM_MESSAGE":
        return "warning";
      default:
        return "default";
    }
  };

  // Форматирование даты
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return "только что";
    if (minutes < 60) return `${minutes} мин назад`;
    if (hours < 24) return `${hours} ч назад`;
    if (days < 7) return `${days} дн назад`;
    return date.toLocaleDateString("ru-RU");
  };

  return (
    <>
      <IconButton
        color="inherit"
        onClick={handleClick}
        aria-label="уведомления"
      >
        <Badge badgeContent={unreadCount} color="error">
          {unreadCount > 0 ? <NotificationsIcon /> : <NotificationsNoneIcon />}
        </Badge>
      </IconButton>

      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        PaperProps={{
          sx: {
            width: 400,
            maxHeight: 500,
            overflow: "auto"
          }
        }}
      >
        <Box sx={{ p: 2, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Typography variant="h6">Уведомления</Typography>
          {unreadCount > 0 && (
            <Button
              size="small"
              startIcon={<DoneAllIcon />}
              onClick={handleMarkAllAsRead}
            >
              Прочитать все
            </Button>
          )}
        </Box>
        
        <Divider />

        {notifications.length === 0 ? (
          <Box sx={{ p: 3, textAlign: "center" }}>
            <Typography color="text.secondary">
              Нет уведомлений
            </Typography>
          </Box>
        ) : (
          notifications.map((notification) => (
            <MenuItem
              key={notification.id}
              onClick={() => !notification.isRead && handleMarkAsRead(notification.id)}
              sx={{
                backgroundColor: notification.isRead ? "transparent" : "action.hover",
                borderLeft: notification.isRead ? "none" : "4px solid",
                borderColor: `${getNotificationColor(notification.type)}.main`,
                flexDirection: "column",
                alignItems: "flex-start",
                py: 2,
                "&:hover": {
                  backgroundColor: notification.isRead ? "action.hover" : "action.selected"
                }
              }}
            >
              <Box sx={{ width: "100%", display: "flex", gap: 1, alignItems: "flex-start" }}>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" sx={{ mb: 0.5 }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: notification.isRead ? 400 : 600 }}>
                      {notification.title}
                    </Typography>
                    <Chip 
                      label={notification.type.replace("_", " ")} 
                      size="small" 
                      color={getNotificationColor(notification.type)}
                      sx={{ height: 20, fontSize: "0.7rem" }}
                    />
                  </Stack>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    {notification.message}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: "block" }}>
                    {formatDate(notification.createdAt)}
                  </Typography>
                </Box>
                <IconButton
                  size="small"
                  onClick={(e) => handleDelete(notification.id, e)}
                  sx={{ flexShrink: 0 }}
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Box>
            </MenuItem>
          ))
        )}
      </Menu>
    </>
  );
};

export default NotificationBell;

