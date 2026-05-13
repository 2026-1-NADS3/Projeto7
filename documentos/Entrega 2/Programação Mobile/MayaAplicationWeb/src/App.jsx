import React from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import Login from './pages/login/Login.jsx';
import Register from './pages/register/Register.jsx';
import NotFound from './pages/NotFound';
import Sidebar from './components/sidebar/Sidebar.jsx';
import Prescricoes from './pages/prescricoes/Prescricoes.jsx';
import ChatAdmin from './pages/chat/ChatAdmin.jsx';
import BancoExercicios from './pages/banco-exercicios/BancoExercicios.jsx';
import CadastrarPaciente from './pages/cadastrar-paciente/CadastrarPaciente.jsx';
import Agendamentos from './pages/agendamentos/Agendamentos.jsx'; 
import './styles/App.css';

const Topbar = () => {
  const token =
    localStorage.getItem('authToken') ||
    sessionStorage.getItem('authToken') ||
    '';
  return (
    <header className="topbar">
      <h2>Painel de Controle</h2>
      <div style={{ display: 'flex', gap: '15px', alignItems: 'center' }}>
        <div className="token-area">
          <span style={{ fontSize: '0.8rem', fontWeight: 'bold', color: '#888' }}>JWT:</span>
          <input type="text" value={token} readOnly placeholder="Sessão Ativa..." />
        </div>
        <span style={{ fontSize: '0.9rem', color: '#555' }}>Administrador</span>
      </div>
    </header>
  );
};

const App = () => {
  const location = useLocation();
  const hideSidebar = ['/login', '/register'].includes(location.pathname);

  return (
    <div className="app-layout">
      {!hideSidebar && <Sidebar />}
      <main className={`main-content ${hideSidebar ? 'full-width' : ''}`}>
        {!hideSidebar && <Topbar />}
        <div className="content-area">
          <Routes>
            <Route path="/login"              element={<Login />} />
            <Route path="/register"           element={<Register />} />
            <Route path="/"                   element={<Prescricoes />} />
            <Route path="/chat"               element={<ChatAdmin />} />
            <Route path="/banco-exercicios"   element={<BancoExercicios />} />
            
            {/* Rotas de Cadastro e Edição de Paciente */}
            <Route path="/cadastrar-paciente"      element={<CadastrarPaciente />} />
            <Route path="/cadastrar-paciente/:id"  element={<CadastrarPaciente />} />
            
            <Route path="/agendamentos"       element={<Agendamentos />} /> {/* ← NOVO */}
            <Route path="*"                   element={<NotFound />} />
          </Routes>
        </div>
      </main>
    </div>
  );
};

export default App;