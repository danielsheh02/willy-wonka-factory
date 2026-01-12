import React, { useState } from "react";
import { TextField, Button, Box, Typography, Paper, Alert, IconButton, InputAdornment } from "@mui/material";
import { Visibility, VisibilityOff } from "@mui/icons-material";
import api, { API_URL } from "../api";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";

export default function LoginPage() {
  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const { login: loginUser } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setError(null);
      const response = await api.post(`${API_URL}/api/auth/signin`, {
        username: login,
        password: password
      });
      const { token, id, username, role } = response.data;
      const user = { id, username, role };
      loginUser(user, token);
      console.log(response.data);
      console.log(user);
      // Перенаправление по роли:
      if(role === "FOREMAN") {
        navigate("/users"); // для начальника цеха
      } else if(role === "WORKER") {
        navigate("/tasks"); // для простого работника
      } else {
        navigate("/profile"); // по умолчанию
      }
    } catch (err) {
      setError("Неверный логин или пароль");
    }
  };

  return (
    <Box sx={{ maxWidth: 400, mx: "auto", mt: 8 }}>
      <Paper sx={{ p: 4 }} elevation={3}>
        <Typography variant="h5" mb={2}>Вход в систему</Typography>
        {error && <Alert severity="error">{error}</Alert>}
        <form onSubmit={handleSubmit}>
          <TextField
            label="Логин"
            value={login}
            onChange={e => setLogin(e.target.value)}
            fullWidth
            required
            margin="normal"
          />
          <TextField
            label="Пароль"
            type={showPassword ? "text" : "password"}
            value={password}
            onChange={e => setPassword(e.target.value)}
            fullWidth
            required
            margin="normal"
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    onClick={() => setShowPassword(!showPassword)}
                    edge="end"
                    aria-label="toggle password visibility"
                  >
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          <Button type="submit" variant="contained" color="primary" fullWidth sx={{ mt: 2 }}>
            Войти
          </Button>
          {/* <Button color="secondary" fullWidth sx={{ mt: 1 }} onClick={() => navigate("/register")}>Зарегистрироваться</Button> */}
          <Button color="info" variant="outlined" fullWidth sx={{ mt: 1 }} onClick={() => navigate("/booking")}>
            🎫 Записаться на экскурсию
          </Button>
        </form>
      </Paper>
    </Box>
  );
}
