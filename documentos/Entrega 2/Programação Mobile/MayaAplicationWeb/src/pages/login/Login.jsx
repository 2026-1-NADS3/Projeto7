import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';

/* ============================================================
   SISTEMA DE TOAST NOTIFICATIONS
   ============================================================ */
const ToastItem = ({ id, message, type, onRemove }) => {
  const [isClosing, setIsClosing] = useState(false);
  const [isHovered, setIsHovered] = useState(false);

  const handleProgressEnd = () => {
    setIsClosing(true);
    setTimeout(() => onRemove(id), 300);
  };

  return (
    <div
      className={`maya-toast ${type} ${isClosing ? 'closing' : ''}`}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div className="maya-toast-content">{message}</div>
      <div
        className="maya-toast-progress"
        style={{ animationPlayState: isHovered ? 'paused' : 'running' }}
        onAnimationEnd={handleProgressEnd}
      />
    </div>
  );
};

/* ============================================================
   TELA DE LOGIN
   ============================================================ */
const Login = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [rememberMe, setRememberMe] = useState(false);
  const [loading, setLoading] = useState(false);
  
  // Estado do Toast
  const [toasts, setToasts] = useState([]);

  const showToast = (message, type = 'success') => {
    const newToast = { id: Date.now() + Math.random(), message, type };
    setToasts((prev) => [...prev, newToast]);
  };

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  };

  // 1. Verifica se há credenciais salvas assim que a tela abre
  useEffect(() => {
    const savedEmail = localStorage.getItem('adminEmail');
    const savedPassword = localStorage.getItem('adminPassword');

    if (savedEmail && savedPassword) {
      showToast('Restaurando sessão anterior...', 'success');
      performLogin(savedEmail, savedPassword, true);
    }
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  // 2. Lógica da API
  const performLogin = async (email, password, isAutoLogin = false) => {
    setLoading(true);

    try {
      const response = await fetch('https://maya-rpg-api-ckx5.onrender.com/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      if (!response.ok) {
        throw new Error('Email ou senha inválidos');
      }

      const data = await response.json();

      // Validar se o usuário é Admin
      if (data.role !== 'ADMIN') {
        showToast('Acesso negado. Apenas administradores podem acessar.', 'error');
        setLoading(false);
        if (isAutoLogin) clearSavedCredentials();
        return;
      }

      const token = data.token;

      // 3. Salva a sessão e as credenciais se necessário
      if (rememberMe || isAutoLogin) {
        localStorage.setItem('authToken', token);
        localStorage.setItem('userRole', data.role);
        localStorage.setItem('adminEmail', email);
        localStorage.setItem('adminPassword', password);
      } else {
        sessionStorage.setItem('authToken', token);
        sessionStorage.setItem('userRole', data.role);
        clearSavedCredentials();
      }

      showToast('Login efetuado com sucesso!', 'success');
      
      // Aguarda um pouco para o usuário ver a notificação antes de trocar de tela
      setTimeout(() => {
        navigate('/');
      }, 800);

    } catch (err) {
      showToast(isAutoLogin ? 'Sessão expirada. Faça login novamente.' : (err.message || 'Erro ao fazer login'), 'error');
      if (isAutoLogin) clearSavedCredentials();
      setLoading(false);
    }
  };

  const clearSavedCredentials = () => {
    localStorage.removeItem('adminEmail');
    localStorage.removeItem('adminPassword');
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    performLogin(formData.email, formData.password, false);
  };

  return (
    <div className="login-container">
      {/* Container de Notificações */}
      <div className="maya-toast-container">
        {toasts.map((toast) => (
          <ToastItem
            key={toast.id}
            id={toast.id}
            message={toast.message}
            type={toast.type}
            onRemove={removeToast}
          />
        ))}
      </div>

      <div className="login-card">
        {/* Logo / Ícone Placeholder - Opcional */}
        <div className="login-logo">
          <div className="logo-circle"></div>
        </div>

        <h1>Login Administrativo</h1>
        <p className="login-subtitle">Acesse o painel de controle da Clínica Maya.</p>
        
        <form onSubmit={handleSubmit}>
          <input
            className="input-pill"
            type="email"
            name="email"
            placeholder="E-mail"
            value={formData.email}
            onChange={handleChange}
            required
            disabled={loading}
          />
          <input
            className="input-pill"
            type="password"
            name="password"
            placeholder="Senha"
            value={formData.password}
            onChange={handleChange}
            required
            disabled={loading}
          />
          <div className="login-options">
            <label>
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                disabled={loading}
              />
              Lembrar-me
            </label>
          </div>
          <button type="submit" className="btn-login-pill" disabled={loading}>
            {loading ? 'Autenticando...' : 'Entrar no Sistema'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;