import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import './CadastrarPaciente.css';

const CadastrarPaciente = () => {
  const navigate = useNavigate();
  const { id } = useParams(); // Pega o ID da URL se existir (Modo Edição)
  const isEditMode = !!id; // Verdadeiro se tiver ID, falso se for cadastro novo

  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEditMode); // Tela de carregamento inicial para edição
  
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    cpf: '',
    telefone: '',
    dataNascimento: '', 
    sexo: '',
    profissao: '',
    queixaPrincipal: '',
    senha: '', // A senha geralmente vem vazia na edição para não sobrescrever se não for alterada
    dataInicioTratamento: ''
  });

  // --- Carregar dados do paciente (Modo Edição) ---
  useEffect(() => {
    if (isEditMode) {
      buscarPaciente();
    }
  }, [id]);

  const buscarPaciente = async () => {
    try {
      const token = localStorage.getItem('authToken') || sessionStorage.getItem('authToken');
      const response = await fetch(`https://maya-rpg-api-ckx5.onrender.com/api/admin/pacientes/${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        const data = await response.json();
        setFormData({
          name: data.name || '',
          email: data.email || '',
          cpf: data.cpf || '',
          telefone: data.telefone || '',
          dataNascimento: parseDateToInput(data.dataNascimento) || '',
          sexo: data.sexo || '',
          profissao: data.profissao || '',
          queixaPrincipal: data.queixaPrincipal || '',
          senha: '', // Mantém vazio, só preenche se quiser alterar
          dataInicioTratamento: parseDateToInput(data.dataInicioTratamento) || ''
        });
      } else {
        alert('Erro ao buscar dados do paciente.');
        navigate('/'); // Volta se o paciente não existir
      }
    } catch (error) {
      console.error(error);
      alert('Erro de conexão ao buscar paciente.');
    } finally {
      setFetching(false);
    }
  };

  // --- Máscaras e Formatações ---

  const formatTelefone = (value) => {
    const v = value.replace(/\D/g, '');
    if (v.length <= 2) return v.replace(/^(\d{0,2})/, '($1');
    if (v.length <= 7) return v.replace(/^(\d{2})(\d{0,5})/, '($1) $2');
    return v.replace(/^(\d{2})(\d{5})(\d{0,4}).*/, '($1) $2-$3');
  };

  const formatCPF = (value) => {
    const v = value.replace(/\D/g, '');
    if (v.length <= 3) return v;
    if (v.length <= 6) return v.replace(/(\d{3})(\d{0,3})/, '$1.$2');
    if (v.length <= 9) return v.replace(/(\d{3})(\d{3})(\d{0,3})/, '$1.$2.$3');
    return v.replace(/(\d{3})(\d{3})(\d{3})(\d{0,2}).*/, '$1.$2.$3-$4');
  };

  const formatData = (value) => {
    const v = value.replace(/\D/g, '');
    if (v.length <= 2) return v;
    if (v.length <= 4) return v.replace(/(\d{2})(\d{0,2})/, '$1/$2');
    return v.replace(/(\d{2})(\d{2})(\d{0,4}).*/, '$1/$2/$3');
  };

  // YYYY-MM-DD -> DD/MM/AAAA (Da API pro Input)
  const parseDateToInput = (dateString) => {
    if (!dateString) return '';
    const [year, month, day] = dateString.split('-');
    return `${day}/${month}/${year}`;
  };

  // DD/MM/AAAA -> YYYY-MM-DD (Do Input pra API)
  const parseDateToAPI = (dateString) => {
    if (!dateString || dateString.length < 10) return null;
    const [day, month, year] = dateString.split('/');
    return `${year}-${month}-${day}`;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    let formattedValue = value;

    if (name === 'telefone') formattedValue = formatTelefone(value);
    if (name === 'cpf') formattedValue = formatCPF(value);
    if (name === 'dataNascimento' || name === 'dataInicioTratamento') {
      formattedValue = formatData(value);
    }

    setFormData(prev => ({ ...prev, [name]: formattedValue }));
  };

  // --- Operações CRUD (Salvar e Deletar) ---

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const token = localStorage.getItem('authToken') || sessionStorage.getItem('authToken');
      
      const payload = {
        ...formData,
        dataNascimento: parseDateToAPI(formData.dataNascimento),
        dataInicioTratamento: parseDateToAPI(formData.dataInicioTratamento)
      };

      // Se a senha estiver vazia na edição, removemos do payload para a API não atualizar
      if (isEditMode && !payload.senha) {
        delete payload.senha;
      }

      // Se for edição usa PUT e a URL com ID, senão POST na URL base
      const url = isEditMode 
        ? `https://maya-rpg-api-ckx5.onrender.com/api/admin/pacientes/${id}`
        : 'https://maya-rpg-api-ckx5.onrender.com/api/admin/pacientes';
        
      const method = isEditMode ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method: method,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        alert(isEditMode ? 'Paciente atualizado com sucesso!' : 'Paciente cadastrado com sucesso!');
        navigate('/'); 
      } else {
        const errorData = await response.json();
        alert(`Erro: ${errorData.messagem || 'Falha na operação'}`);
      }
    } catch (error) {
      console.error('Erro de conexão:', error);
      alert('Erro de conexão com o servidor.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    const confirmar = window.confirm(`Tem certeza que deseja deletar o paciente ${formData.name}? Esta ação não pode ser desfeita.`);
    if (!confirmar) return;

    setLoading(true);
    try {
      const token = localStorage.getItem('authToken') || sessionStorage.getItem('authToken');
      const response = await fetch(`https://maya-rpg-api-ckx5.onrender.com/api/admin/pacientes/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        alert('Paciente deletado com sucesso.');
        navigate('/');
      } else {
        alert('Erro ao deletar paciente.');
      }
    } catch (error) {
      console.error(error);
      alert('Erro de conexão ao tentar deletar.');
    } finally {
      setLoading(false);
    }
  };

  if (fetching) {
    return <div className="page-content" style={{textAlign: 'center', marginTop: '50px'}}>Carregando dados do paciente...</div>;
  }

  return (
    <div className="page-content">
      <div className="form-wrapper">
        <div className="section-header">
          <h3>{isEditMode ? 'Editar Paciente' : 'Novo Paciente'}</h3>
          <p className="page-subtitle">
            {isEditMode ? 'Atualize os dados clínicos ou remova o paciente do sistema.' : 'Preencha os dados clínicos e de acesso do paciente.'}
          </p>
        </div>
        
        <form onSubmit={handleSubmit} className="paciente-form" autoComplete="off">
          <div className="form-grid">
            <div className="input-group">
              <label>Nome Completo *</label>
              <input className="input-shadow" type="text" name="name" value={formData.name} onChange={handleChange} required placeholder="Ex: Maria Souza" />
            </div>
            <div className="input-group">
              <label>E-mail *</label>
              <input className="input-shadow" type="email" name="email" value={formData.email} onChange={handleChange} required placeholder="maria@email.com" autoComplete="new-password" />
            </div>
          </div>

          <div className="form-grid">
            <div className="input-group">
              <label>CPF *</label>
              <input className="input-shadow" type="text" name="cpf" value={formData.cpf} onChange={handleChange} required placeholder="000.000.000-00" />
            </div>
            <div className="input-group">
              <label>Telefone</label>
              <input className="input-shadow" type="text" name="telefone" value={formData.telefone} onChange={handleChange} placeholder="(11) 99999-9999" />
            </div>
          </div>

          <div className="form-grid">
            <div className="input-group">
              <label>Data de Nascimento *</label>
              <input className="input-shadow" type="text" name="dataNascimento" value={formData.dataNascimento} onChange={handleChange} required placeholder="DD/MM/AAAA" maxLength="10" />
            </div>
            <div className="input-group">
              <label>Início do Tratamento</label>
              <input className="input-shadow" type="text" name="dataInicioTratamento" value={formData.dataInicioTratamento} onChange={handleChange} placeholder="DD/MM/AAAA" maxLength="10" />
            </div>
          </div>

          <div className="form-grid">
            <div className="input-group">
              <label>Sexo *</label>
              <select className="input-shadow" name="sexo" value={formData.sexo} onChange={handleChange} required>
                <option value="" disabled>Selecione uma opção</option>
                <option value="F">Feminino</option>
                <option value="M">Masculino</option>
                <option value="O">Outro</option>
              </select>
            </div>
            <div className="input-group">
              <label>Profissão</label>
              <input className="input-shadow" type="text" name="profissao" value={formData.profissao} onChange={handleChange} placeholder="Ex: Fisioterapeuta" />
            </div>
          </div>

          <div className="input-group full-width">
            <label>Queixa Principal</label>
            <textarea className="input-shadow" name="queixaPrincipal" value={formData.queixaPrincipal} onChange={handleChange} placeholder="Descreva o motivo da consulta, dores, etc..." rows="3"></textarea>
          </div>

          <div className="input-group full-width">
            <label>{isEditMode ? 'Nova Senha de Acesso (Deixe em branco para não alterar)' : 'Senha de Acesso ao App *'}</label>
            <input className="input-shadow" type="password" name="senha" value={formData.senha} onChange={handleChange} required={!isEditMode} minLength="6" placeholder="Defina uma senha provisória" autoComplete="new-password" />
          </div>

          <div className="form-actions" style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
            {isEditMode && (
              <button type="button" className="btn-cancel" onClick={handleDelete} style={{ backgroundColor: '#ff4d4d', color: 'white', marginRight: 'auto' }} disabled={loading}>
                Deletar Paciente
              </button>
            )}
            <button type="button" className="btn-cancel" onClick={() => navigate(-1)}>Cancelar</button>
            <button type="submit" className="btn-submit" disabled={loading}>
              {loading ? 'Processando...' : (isEditMode ? 'Atualizar Paciente' : 'Cadastrar Paciente')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CadastrarPaciente;