import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Register.css';

const Register = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
      name: '',
      email: '',
      password: '',
      cpf: '',
      telefone: '',
      dataNascimento: ''
    });
  
    const handleChange = (e) => {
      const { name, value } = e.target;
      setFormData(prev => ({
        ...prev,
        [name]: value
      }));
    };
  
    const handleSubmit = (e) => {
      e.preventDefault();
      console.log('Dados do cadastro:', formData);
      navigate('/');
    };
  
    return (
      <div className="register-container">
        <div className="register-card">
          <h1>Cadastro</h1>
          <form onSubmit={handleSubmit}>
            <label>Nome: <input type="text" name="name" placeholder="Digite seu nome" value={formData.name} onChange={handleChange} required /></label>
            <label>Email: <input type="email" name="email" placeholder="Digite seu email" value={formData.email} onChange={handleChange} required /></label>
            <label>Senha: <input type="password" name="password" placeholder="Digite sua senha" value={formData.password} onChange={handleChange} required /></label>
            <label>CPF: <input type="text" name="cpf" placeholder="Digite seu CPF" value={formData.cpf} onChange={handleChange} required /></label>
            <label>Telefone: <input type="tel" name="telefone" placeholder="Digite seu telefone" value={formData.telefone} onChange={handleChange} required /></label>
            <label>Data de Nascimento: <input type="date" name="dataNascimento" value={formData.dataNascimento} onChange={handleChange} required /></label>
            <button type="submit">Cadastrar</button>
          </form>
        </div>
      </div>
    );
  };
  
  export default Register;