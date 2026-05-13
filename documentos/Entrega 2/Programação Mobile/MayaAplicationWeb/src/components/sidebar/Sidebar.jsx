import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import './Sidebar.css';
import logoMaya from './logo_maya.png'; // <-- Altere a extensão aqui se necessário (.jpg, .svg)

const Sidebar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  
  // Estado para controlar a abertura da sidebar no mobile
  const [isOpen, setIsOpen] = useState(false);

  const isActive = (path) => location.pathname === path;

  // Funções para abrir e fechar a sidebar
  const toggleSidebar = () => setIsOpen(!isOpen);
  const closeSidebar = () => setIsOpen(false);

  const handleLogout = () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userRole');
    sessionStorage.removeItem('authToken');
    sessionStorage.removeItem('userRole');
    navigate('/login');
  };

  return (
    <>
      {/* Botão Hambúrguer - Visível apenas no Mobile */}
      <button className="mobile-toggle" onClick={toggleSidebar} aria-label="Abrir menu">
        ☰
      </button>

      {/* Overlay Escuro - Aparece atrás da sidebar no mobile */}
      <div 
        className={`sidebar-overlay ${isOpen ? 'visible' : ''}`} 
        onClick={closeSidebar}
      ></div>

      {/* Sidebar - Adiciona a classe 'open' se o estado for true */}
      <aside className={`sidebar ${isOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <img src={logoMaya} alt="Logo Maya" className="sidebar-logo" />
          {/* Botão de Fechar - Visível apenas no Mobile */}
          <button className="close-btn" onClick={closeSidebar} aria-label="Fechar menu">
            &times;
          </button>
        </div>

        <ul className="nav-menu">
          <li>
            <Link 
              to="/" 
              className={`nav-item ${isActive('/') ? 'active' : ''}`}
              onClick={closeSidebar} // Fecha ao clicar no mobile
            >
              Prescrições / Treinos
            </Link>
          </li>
          <li>
            <Link
              to="/banco-exercicios"
              className={`nav-item ${isActive('/banco-exercicios') ? 'active' : ''}`}
              onClick={closeSidebar}
            >
              Banco de Exercícios
            </Link>
          </li>
          <li>
            <Link
              to="/cadastrar-paciente"
              className={`nav-item ${isActive('/cadastrar-paciente') ? 'active' : ''}`}
              onClick={closeSidebar}
            >
              Cadastrar Paciente
            </Link>
          </li>
          <li>
            <Link
              to="/agendamentos"
              className={`nav-item ${isActive('/agendamentos') ? 'active' : ''}`}
              onClick={closeSidebar}
            >
              Agendamentos
            </Link>
          </li>
          <li>
            <Link 
              to="/chat" 
              className={`nav-item ${isActive('/chat') ? 'active' : ''}`}
              onClick={closeSidebar}
            >
              Atendimento / Chat
            </Link>
          </li>
        </ul>

        <div className="sidebar-footer">
          <button className="logout-btn" onClick={handleLogout}>
            Sair do Sistema
          </button>
        </div>
      </aside>
    </>
  );
};

export default Sidebar;