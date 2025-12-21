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

export default api;
