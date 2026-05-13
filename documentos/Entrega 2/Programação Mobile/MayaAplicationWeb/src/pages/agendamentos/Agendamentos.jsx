import React, { useState, useEffect } from 'react';
import './Agendamentos.css';

const API_BASE_URL = 'https://maya-rpg-api-ckx5.onrender.com/api';

const getAuthHeaders = () => {
  const token = localStorage.getItem('authToken') || sessionStorage.getItem('authToken');
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

const STATUS_LABELS = {
  AGENDADO:  { label: 'Agendado',  className: 'badge-agendado'  },
  CONCLUIDO: { label: 'Concluído', className: 'badge-concluido' },
};

const formatarDataHora = (iso) => {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
};

const formatData = (value) => {
  const v = value.replace(/\D/g, '');
  if (v.length <= 2) return v;
  if (v.length <= 4) return v.replace(/(\d{2})(\d{0,2})/, '$1/$2');
  return v.replace(/(\d{2})(\d{2})(\d{0,4}).*/, '$1/$2/$3');
};

const formatHora = (value) => {
  const v = value.replace(/\D/g, '');
  if (v.length <= 2) return v;
  return v.replace(/(\d{2})(\d{0,2}).*/, '$1:$2');
};

const montarISO = (data, hora) => {
  if (!data || data.length < 10 || !hora || hora.length < 5) return null;
  const [day, month, year] = data.split('/');
  return `${year}-${month}-${day}T${hora}:00`;
};

const ListaPacientes = ({ onGerenciar }) => {
  const [pacientes, setPacientes] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [busca, setBusca]         = useState('');
  const [error, setError]         = useState('');

  useEffect(() => { carregarPacientes(); }, []);

  const carregarPacientes = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE_URL}/admin/pacientes`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error(`Erro HTTP: ${res.status}`);
      setPacientes(await res.json());
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const filtrados = pacientes.filter((p) =>
    p.name?.toLowerCase().includes(busca.toLowerCase()) ||
    p.email?.toLowerCase().includes(busca.toLowerCase())
  );

  return (
    <div className="page-fade">
      <div className="section-header">
        <div>
          <h3>Gerenciar Agendamentos</h3>
          <p>Selecione um paciente para visualizar, criar ou editar seus agendamentos.</p>
        </div>
      </div>

      <div className="busca-wrapper">
        <input
          type="text"
          placeholder="Filtrar por nome ou e-mail..."
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          className="busca-input"
        />
      </div>

      <div className="table-responsive">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Paciente</th>
              <th>E-mail</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr><td colSpan="4" className="td-center td-muted">Buscando pacientes...</td></tr>
            )}
            {error && (
              <tr><td colSpan="4" className="td-center td-error"><strong>Falha:</strong> {error}</td></tr>
            )}
            {!loading && !error && filtrados.length === 0 && (
              <tr><td colSpan="4" className="td-center">Nenhum paciente encontrado.</td></tr>
            )}
            {filtrados.map((p) => (
              <tr key={p.id}>
                <td data-label="ID">{p.id}</td>
                <td data-label="Paciente"><strong>{p.name}</strong></td>
                <td data-label="E-mail">{p.email}</td>
                <td data-label="Ações">
                  <button className="btn" onClick={() => onGerenciar(p.id, p.name)}>
                    Ver Agendamentos
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

const PainelAgendamentos = ({ paciente, onVoltar }) => {
  const [agendamentos, setAgendamentos] = useState([]);
  const [loading, setLoading]           = useState(true);

  const [form, setForm]           = useState({ data: '', hora: '', observacao: '' });
  const [salvando, setSalvando]   = useState(false);
  const [erroForm, setErroForm]   = useState('');

  const [modalAberto, setModalAberto]                 = useState(false);
  const [agendamentoEditando, setAgendamentoEditando] = useState(null);
  const [novoStatus, setNovoStatus]                   = useState('');
  const [salvandoStatus, setSalvandoStatus]           = useState(false);

  useEffect(() => { carregarAgendamentos(); }, [paciente.id]);

  const carregarAgendamentos = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_URL}/admin/agendamentos`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Erro ao buscar agendamentos');
      const todos = await res.json();
      const doPaciente = todos.filter((a) => a.pacienteId === paciente.id);
      doPaciente.sort((a, b) => new Date(b.dataHora) - new Date(a.dataHora));
      setAgendamentos(doPaciente);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    let v = value;
    if (name === 'data') v = formatData(value);
    if (name === 'hora') v = formatHora(value);
    setForm((prev) => ({ ...prev, [name]: v }));
  };

  const criarAgendamento = async (e) => {
    e.preventDefault();
    setErroForm('');

    const dataHoraISO = montarISO(form.data, form.hora);
    if (!dataHoraISO) {
      setErroForm('Preencha a data (DD/MM/AAAA) e o horário (HH:MM) corretamente.');
      return;
    }

    setSalvando(true);
    try {
      const res = await fetch(`${API_BASE_URL}/admin/agendamentos`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          pacienteId: paciente.id,
          dataHora: dataHoraISO,
          observacao: form.observacao || '',
        }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.messagem || `Erro HTTP ${res.status}`);
      }
      setForm({ data: '', hora: '', observacao: '' });
      carregarAgendamentos();
    } catch (e) {
      setErroForm(e.message);
    } finally {
      setSalvando(false);
    }
  };

  const abrirEdicao = (agendamento) => {
    setAgendamentoEditando(agendamento);
    setNovoStatus(agendamento.status);
    setModalAberto(true);
  };

  const salvarStatus = async () => {
    if (!agendamentoEditando) return;
    setSalvandoStatus(true);
    try {
      const res = await fetch(
        `${API_BASE_URL}/admin/agendamentos/${agendamentoEditando.id}/status?novoStatus=${novoStatus}`,
        { method: 'PATCH', headers: getAuthHeaders() }
      );
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.messagem || `Erro HTTP ${res.status}`);
      }
      setModalAberto(false);
      carregarAgendamentos();
    } catch (e) {
      alert('Erro ao atualizar status: ' + e.message);
    } finally {
      setSalvandoStatus(false);
    }
  };

  const cancelarAgendamento = async (ag) => {
    if (!window.confirm(`Cancelar agendamento de ${formatarDataHora(ag.dataHora)}?`)) return;
    try {
      const res = await fetch(
        `${API_BASE_URL}/admin/agendamentos/${ag.id}/status?novoStatus=CANCELADO`,
        { method: 'PATCH', headers: getAuthHeaders() }
      );
      if (!res.ok) throw new Error(`Erro HTTP ${res.status}`);
      carregarAgendamentos();
    } catch (e) {
      alert('Erro ao cancelar: ' + e.message);
    }
  };

  const agendamentosAtivos   = agendamentos.filter((a) => a.status === 'AGENDADO');
  const agendamentosPassados = agendamentos.filter((a) => a.status !== 'AGENDADO');

  return (
    <div className="page-fade">
      <div className="section-header">
        <div>
          <button className="btn btn-outline" onClick={onVoltar} style={{ marginBottom: '10px' }}>
            ← Voltar à Lista
          </button>
          <h3>Agendamentos de: {paciente.nome}</h3>
        </div>
      </div>

      <details className="gaveta" open>
        <summary>+ Criar Novo Agendamento</summary>
        <div className="gaveta-content">
          <form onSubmit={criarAgendamento}>
            <div className="linha-agendamento">
              <div className="form-group">
                <label>Data *</label>
                <input
                  type="text"
                  name="data"
                  value={form.data}
                  onChange={handleFormChange}
                  placeholder="DD/MM/AAAA"
                  maxLength="10"
                  required
                />
              </div>

              <div className="form-group">
                <label>Horário *</label>
                <input
                  type="text"
                  name="hora"
                  value={form.hora}
                  onChange={handleFormChange}
                  placeholder="HH:MM"
                  maxLength="5"
                  required
                />
              </div>

              <div className="form-group obs-group">
                <label>Observações</label>
                <input
                  type="text"
                  name="observacao"
                  placeholder="Ex: Foco na cervical, trazer exames..."
                  value={form.observacao}
                  onChange={handleFormChange}
                />
              </div>
            </div>

            {erroForm && <p className="form-error">{erroForm}</p>}

            <div style={{ textAlign: 'right', marginTop: '10px' }}>
              <button type="submit" className="btn" disabled={salvando}>
                {salvando ? 'Salvando...' : 'Confirmar Agendamento'}
              </button>
            </div>
          </form>
        </div>
      </details>

      <div className="section-header" style={{ marginTop: '30px' }}>
        <h4>Consultas Agendadas ({agendamentosAtivos.length})</h4>
      </div>

      {loading ? (
        <p className="td-muted">Carregando agendamentos...</p>
      ) : agendamentosAtivos.length === 0 ? (
        <p className="td-muted">Nenhuma consulta agendada.</p>
      ) : (
        agendamentosAtivos.map((ag) => (
          <AgendamentoCard
            key={ag.id}
            ag={ag}
            onEditar={() => abrirEdicao(ag)}
            onCancelar={() => cancelarAgendamento(ag)}
          />
        ))
      )}

      {agendamentosPassados.length > 0 && (
        <details className="gaveta" style={{ marginTop: '20px' }}>
          <summary>Histórico ({agendamentosPassados.length} registros)</summary>
          <div className="gaveta-content">
            {agendamentosPassados.map((ag) => (
              <AgendamentoCard
                key={ag.id}
                ag={ag}
                onEditar={() => abrirEdicao(ag)}
                somenteLeitura={ag.status === 'CANCELADO'}
              />
            ))}
          </div>
        </details>
      )}

      {modalAberto && agendamentoEditando && (
        <div className="modal-overlay" onClick={() => setModalAberto(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <h4>Editar Status do Agendamento</h4>
            <p className="modal-info">
              <strong>Data:</strong> {formatarDataHora(agendamentoEditando.dataHora)}<br />
              <strong>Obs:</strong> {agendamentoEditando.observacao || '—'}
            </p>

            <div className="form-group">
              <label>Novo Status *</label>
              <select value={novoStatus} onChange={(e) => setNovoStatus(e.target.value)}>
                <option value="AGENDADO">Agendado</option>
                <option value="CONCLUIDO">Concluído</option>
              </select>
            </div>

            <div className="modal-actions">
              <button
                className="btn btn-outline"
                onClick={() => setModalAberto(false)}
                disabled={salvandoStatus}
              >
                Cancelar
              </button>
              <button className="btn" onClick={salvarStatus} disabled={salvandoStatus}>
                {salvandoStatus ? 'Salvando...' : 'Salvar Status'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const AgendamentoCard = ({ ag, onEditar, onCancelar, somenteLeitura = false }) => {
  const statusInfo = STATUS_LABELS[ag.status] || { label: ag.status, className: '' };

  return (
    <div className="agendamento-card">
      <div className="agendamento-info">
        <div className="agendamento-data">{formatarDataHora(ag.dataHora)}</div>
        {ag.observacao && <div className="agendamento-obs">{ag.observacao}</div>}
      </div>
      <div className="agendamento-acoes">
        <span className={`badge ${statusInfo.className}`}>{statusInfo.label}</span>
        {!somenteLeitura && (
          <>
            <button className="btn btn-sm btn-outline" onClick={onEditar}>
              Editar Status
            </button>
            {ag.status === 'AGENDADO' && onCancelar && (
              <button className="btn btn-sm btn-danger" onClick={onCancelar}>
                Cancelar
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
};

const Agendamentos = () => {
  const [view, setView]                   = useState('lista');
  const [pacienteAtivo, setPacienteAtivo] = useState(null);

  const abrirPaciente = (id, nome) => {
    setPacienteAtivo({ id, nome });
    setView('agendamentos');
  };

  return (
    <div className="section">
      {view === 'lista' && (
        <ListaPacientes onGerenciar={abrirPaciente} />
      )}
      {view === 'agendamentos' && pacienteAtivo && (
        <PainelAgendamentos
          paciente={pacienteAtivo}
          onVoltar={() => setView('lista')}
        />
      )}
    </div>
  );
};

export default Agendamentos;