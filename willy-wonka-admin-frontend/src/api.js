import axios from "axios";

export const API_URL = process.env.REACT_APP_API_URL || "http://localhost:7999";

const api = axios.create({
  baseURL: API_URL
});

// Подключение Bearer Token ко всем запросам кроме /api/auth
api.interceptors.request.use((config) => {
  // Если это не /api/auth/signin или /api/auth/signup, добавим токен
  if (!config.url.includes("/api/auth/signin") && !config.url.includes("/api/auth/signup")) {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }
  }
  return config;
});

// Notifications API
export const notificationsAPI = {
  // Получить все уведомления текущего пользователя
  getMyNotifications: () => api.get("/api/notifications/my"),
  
  // Получить уведомления с пагинацией
  getMyNotificationsPaged: (page = 0, size = 10) => 
    api.get("/api/notifications/my/paged", { params: { page, size } }),
  
  // Получить непрочитанные уведомления
  getUnreadNotifications: () => api.get("/api/notifications/my/unread"),
  
  // Получить количество непрочитанных уведомлений
  getUnreadCount: () => api.get("/api/notifications/my/unread/count"),
  
  // Отметить уведомление как прочитанное
  markAsRead: (id) => api.put(`/api/notifications/${id}/read`),
  
  // Отметить все уведомления как прочитанные
  markAllAsRead: () => api.put("/api/notifications/my/read-all"),
  
  // Удалить уведомление
  deleteNotification: (id) => api.delete(`/api/notifications/${id}`),
  
  // Создать уведомление (для администраторов)
  createNotification: (data) => api.post("/api/notifications", data)
};

export default api;
