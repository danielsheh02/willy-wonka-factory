import React, { useState } from "react";
import { TextField, Button, Box, Typography, Paper, Alert } from "@mui/material";
import api, { API_URL } from "../api";
import { useNavigate } from "react-router-dom";

export default function RegisterPage() {
  const [login, setLogin] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);
    try {
      await api.post(`${API_URL}/api/auth/signup`, {
        username: login,
        password
      });
      setSuccess(true);
      setTimeout(() => navigate("/login"), 1000);
    } catch (err) {
      setError("Ошибка регистрации, попробуйте другой логин");
    }
  };

  return (
    <Box sx={{ maxWidth: 400, mx: "auto", mt: 8 }}>
      <Paper sx={{ p: 4 }} elevation={3}>
        <Typography variant="h5" mb={2}>Регистрация</Typography>
        {error && <Alert severity="error">{error}</Alert>}
        {success && <Alert severity="success">Успешная регистрация!</Alert>}
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
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            fullWidth
            required
            margin="normal"
          />
          <Button type="submit" variant="contained" color="primary" fullWidth sx={{ mt: 2 }}>
            Зарегистрироваться
          </Button>
          <Button color="secondary" fullWidth sx={{ mt: 1 }} onClick={() => navigate("/login")}>Назад ко входу</Button>
        </form>
      </Paper>
    </Box>
  );
}
