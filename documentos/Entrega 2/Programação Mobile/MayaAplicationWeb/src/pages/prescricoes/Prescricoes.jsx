import React, { useState, useEffect, useRef } from 'react';
import './Prescricoes.css';

const API_BASE_URL = 'https://maya-rpg-api-ckx5.onrender.com/api';

const getAuthHeaders = () => {
  const token =
    localStorage.getItem('authToken') ||
    sessionStorage.getItem('authToken');
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

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
   VIEW 1 — Lista de Pacientes (COM PESQUISA)
   ============================================================ */
const ListaPacientes = ({ onGerenciar, showToast }) => {
  const [pacientes, setPacientes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

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
      showToast('Erro ao carregar lista de pacientes.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const pacientesFiltrados = pacientes.filter(p => 
    p.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    p.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="page-fade">
      <div className="section-header">
        <div>
          <h3>Prescrição de Exercícios</h3>
          <p>Selecione um paciente para gerenciar seus treinos e acompanhar a evolução.</p>
        </div>
      </div>

      <div style={{ marginBottom: '20px' }}>
        <input 
          type="text" 
          className="input-shadow" 
          placeholder="🔍 Buscar paciente por nome ou e-mail..." 
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      <div className="table-container shadow-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Paciente</th>
              <th>Email</th>
              <th style={{ textAlign: 'right' }}>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan="4" style={{ textAlign: 'center', color: '#666', padding: '20px' }}>
                  Buscando pacientes no servidor...
                </td>
              </tr>
            )}
            {error && (
              <tr>
                <td colSpan="4" style={{ textAlign: 'center', color: 'red', padding: '20px' }}>
                  <strong>Falha:</strong> {error}
                </td>
              </tr>
            )}
            {!loading && !error && pacientesFiltrados.length === 0 && (
              <tr>
                <td colSpan="4" style={{ textAlign: 'center', padding: '20px' }}>
                  Nenhum paciente encontrado com esse nome.
                </td>
              </tr>
            )}
            {pacientesFiltrados.map((p) => (
              <tr key={p.id}>
                <td>{p.id}</td>
                <td><strong>{p.name}</strong></td>
                <td>{p.email}</td>
                <td style={{ textAlign: 'right' }}>
                  <button className="btn" onClick={() => onGerenciar(p.id, p.name)}>
                    Gerenciar Paciente
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

/* ============================================================
   VIEW 2 — Painel do Paciente (Prontuários e Gráfico de Dor)
   ============================================================ */
const PainelPaciente = ({ paciente, onVoltar, onAbrirProntuario, showToast }) => {
  const [prontuarios, setProntuarios] = useState([]);
  const [evolucao, setEvolucao] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Estados para CRUD de Prontuário
  const [editProntuarioId, setEditProntuarioId] = useState(null);
  const detailsRef = useRef(null);

  const [formData, setFormData] = useState({
    titulo: '',
    observacao: '',
    nivelDor: 0,
  });

  useEffect(() => { 
    carregarProntuarios(); 
    carregarEvolucao();
  }, [paciente.id]);

  const carregarProntuarios = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_URL}/admin/pacientes/${paciente.id}/prontuarios`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Erro ao buscar prontuários');
      setProntuarios(await res.json());
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const carregarEvolucao = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/admin/pacientes/${paciente.id}/checkins/evolucao`, { headers: getAuthHeaders() });
      if (res.ok) {
        setEvolucao(await res.json());
      }
    } catch (e) {
      console.error("Falha ao carregar gráfico de evolução", e);
    }
  };

  // --- Ações de Prontuário ---
  
  const salvarProntuario = async (e) => {
    e.preventDefault();
    try {
      const url = editProntuarioId
        ? `${API_BASE_URL}/admin/prontuarios/${editProntuarioId}`
        : `${API_BASE_URL}/admin/prontuarios`;
      
      const method = editProntuarioId ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method: method,
        headers: getAuthHeaders(),
        body: JSON.stringify({
          pacienteId: parseInt(paciente.id),
          titulo: formData.titulo,
          observacao: formData.observacao,
          nivelDor: parseInt(formData.nivelDor),
        }),
      });

      if (!res.ok) throw new Error('Falha ao salvar o Prontuário.');
      
      showToast(`Prontuário ${editProntuarioId ? 'atualizado' : 'criado'} com sucesso!`, 'success');
      cancelarEdicao();
      carregarProntuarios();
    } catch (e) {
      showToast(e.message, 'error');
    }
  };

  const handleEditarProntuario = (p) => {
    setEditProntuarioId(p.id);
    setFormData({
      titulo: p.titulo || '',
      observacao: p.observacao || '',
      nivelDor: p.nivelDor || 0,
    });

    if (detailsRef.current) detailsRef.current.open = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDeletarProntuario = async (id, titulo) => {
    if (!window.confirm(`ATENÇÃO! Deseja APAGAR o prontuário "${titulo}" e todas as rotinas atreladas a ele?`)) return;
    try {
      const res = await fetch(`${API_BASE_URL}/admin/prontuarios/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        showToast('Prontuário apagado com sucesso!', 'success');
        if (editProntuarioId === id) cancelarEdicao();
        carregarProntuarios();
      } else {
        showToast('Erro ao deletar prontuário.', 'error');
      }
    } catch (e) {
      showToast('Erro de conexão ao tentar deletar.', 'error');
    }
  };

  const cancelarEdicao = () => {
    setEditProntuarioId(null);
    setFormData({ titulo: '', observacao: '', nivelDor: 0 });
  };

  // Funções Visuais do Gráfico
  const formatarDataGrafico = (dataStr) => {
    if (!dataStr) return '';
    const partes = dataStr.split('-');
    return `${partes[2]}/${partes[1]}`;
  };

  const getCorDor = (media) => {
    if (media <= 3) return '#10b981'; 
    if (media <= 6) return '#f59e0b'; 
    return '#ef4444'; 
  };

  return (
    <div className="page-fade">
      <div className="section-header">
        <div>
          <button className="btn btn-outline" onClick={onVoltar} style={{ marginBottom: '10px' }}>
            ← Voltar à Lista
          </button>
          <h3>Painel do Paciente: {paciente.nome}</h3>
        </div>
      </div>

      <div className="section-header" style={{ marginTop: '20px' }}>
        <h4>Evolução do Nível de Dor (Pós-Exercícios)</h4>
      </div>
      <div className="evolucao-chart-container shadow-container">
        {evolucao.length === 0 ? (
          <p style={{ textAlign: 'center', color: '#64748b', padding: '30px' }}>O paciente ainda não possui check-ins registrados.</p>
        ) : (
          <div className="evolucao-chart">
            {evolucao.map((dia, idx) => (
              <div key={idx} className="chart-col">
                <div 
                  className="chart-bar" 
                  style={{ 
                    height: `${(dia.mediaNivelDor / 10) * 100}%`, 
                    backgroundColor: getCorDor(dia.mediaNivelDor)
                  }}
                  data-tooltip={`Média de Dor: ${dia.mediaNivelDor.toFixed(1)}/10\nExercícios: ${dia.totalExerciciosConcluidos}`}
                ></div>
                <span className="chart-label">{formatarDataGrafico(dia.data)}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <details className="gaveta shadow-container" open ref={detailsRef}>
        <summary>{editProntuarioId ? `✎ Editando Prontuário: ${formData.titulo}` : '+ Abrir Novo Prontuário (Queixa)'}</summary>
        <div className="gaveta-content">
          <form onSubmit={salvarProntuario}>
            <div className="form-group">
              <label>Título / Queixa Principal *</label>
              <input
                type="text"
                className="input-shadow"
                required
                placeholder="Ex: Dor Lombar Crônica"
                value={formData.titulo}
                onChange={(e) => setFormData((p) => ({ ...p, titulo: e.target.value }))}
              />
            </div>
            <div className="form-group">
              <label>Observações</label>
              <textarea
                className="input-shadow"
                rows="2"
                placeholder="Detalhes do estado inicial..."
                value={formData.observacao}
                onChange={(e) => setFormData((p) => ({ ...p, observacao: e.target.value }))}
              />
            </div>
            <div className="form-group">
              <label>Nível de Dor Inicial (0 a 10) *</label>
              <input
                type="number"
                className="input-shadow"
                required
                min="0"
                max="10"
                value={formData.nivelDor}
                onChange={(e) => setFormData((p) => ({ ...p, nivelDor: e.target.value }))}
              />
            </div>
            
            <div className="form-actions-row">
              {editProntuarioId && (
                <button type="button" className="btn-cancelar-edicao" onClick={cancelarEdicao}>
                  Cancelar Edição
                </button>
              )}
              <button type="submit" className="btn">
                {editProntuarioId ? 'Atualizar Prontuário' : 'Salvar Prontuário'}
              </button>
            </div>
          </form>
        </div>
      </details>

      <div className="section-header" style={{ marginTop: '30px' }}>
        <h4>Prontuários e Prescrições</h4>
      </div>

      <div>
        {loading && <p style={{ color: '#666' }}>Buscando prontuários...</p>}
        {!loading && prontuarios.length === 0 && (
          <p style={{ color: '#666' }}>Nenhum prontuário encontrado. Crie um acima.</p>
        )}
        {prontuarios.map((p) => (
          <div key={p.id} className={`prontuario-card shadow-container ${editProntuarioId === p.id ? 'row-editing' : ''}`} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px', marginBottom: '12px' }}>
            <div>
              <strong style={{ color: 'var(--primary)', fontSize: '1.05rem' }}>{p.titulo}</strong>
              <br />
              <small style={{ color: '#666' }}>
                Dor Inicial: {p.nivelDor}/10 | {p.observacao || 'Sem observações'}
              </small>
            </div>
            
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <button className="btn" style={{ marginRight: '10px' }} onClick={() => onAbrirProntuario(p.id, p.titulo)}>
                Prescrever Exercícios
              </button>
              <button className="btn-acao btn-editar" onClick={() => handleEditarProntuario(p)} title="Editar Prontuário">✏️</button>
              <button className="btn-acao btn-deletar" onClick={() => handleDeletarProntuario(p.id, p.titulo)} title="Apagar Prontuário">🗑️</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

/* ============================================================
   VIEW 3 — Painel do Prontuário (Rotinas)
   ============================================================ */
const PainelProntuario = ({ prontuario, onVoltar, catalogoExercicios, showToast }) => {
  const [rotinas, setRotinas] = useState([]);
  const [formData, setFormData] = useState({ nome: '', observacoes: '' });
  const [linhasExercicio, setLinhasExercicio] = useState([
    { id: Date.now(), exercicioId: '', series: 3, repeticoes: 10, tempo: 0 },
  ]);

  const [editRotinaId, setEditRotinaId] = useState(null);
  const [exerciciosSalvos, setExerciciosSalvos] = useState([]);
  const detailsRef = useRef(null);

  useEffect(() => { carregarRotinas(); }, [prontuario.id]);

  const carregarRotinas = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/admin/prontuarios/${prontuario.id}/rotinas`, { headers: getAuthHeaders() });
      if (!res.ok) throw new Error('Erro ao buscar rotinas');
      setRotinas(await res.json());
    } catch (e) {
      console.error(e);
    }
  };

  const carregarExerciciosDaRotina = async (rotinaId) => {
    try {
      const res = await fetch(`${API_BASE_URL}/admin/rotinas/${rotinaId}/exercicios`, { headers: getAuthHeaders() });
      if (res.ok) setExerciciosSalvos(await res.json());
    } catch (e) {
      console.error('Erro ao buscar exercícios da rotina', e);
    }
  };

  const handleEditarRotina = (rotina) => {
    setEditRotinaId(rotina.id);
    setFormData({ nome: rotina.nome || '', observacoes: rotina.observacoes || '' });
    setLinhasExercicio([]); 
    setExerciciosSalvos([]); 
    
    carregarExerciciosDaRotina(rotina.id);

    if (detailsRef.current) detailsRef.current.open = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDeletarRotina = async (id, nome) => {
    if (!window.confirm(`Tem certeza que deseja DELETAR permanentemente a rotina "${nome}"?`)) return;
    try {
      const res = await fetch(`${API_BASE_URL}/admin/rotinas/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        showToast('Rotina apagada com sucesso!', 'success');
        if (editRotinaId === id) cancelarEdicao();
        carregarRotinas();
      } else {
        showToast('Erro ao deletar rotina.', 'error');
      }
    } catch (e) {
      showToast('Erro de conexão ao tentar deletar.', 'error');
    }
  };

  const handleDeletarExercicioSalvo = async (id, titulo) => {
    if (!window.confirm(`Remover o exercício "${titulo}" desta rotina?`)) return;
    try {
      const res = await fetch(`${API_BASE_URL}/admin/exercicios-prescritos/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        setExerciciosSalvos((prev) => prev.filter((ex) => ex.id !== id));
        showToast(`Exercício removido com sucesso.`, 'success');
      } else {
        showToast('Erro ao remover exercício.', 'error');
      }
    } catch (e) {
      showToast('Erro de conexão.', 'error');
    }
  };

  const cancelarEdicao = () => {
    setEditRotinaId(null);
    setFormData({ nome: '', observacoes: '' });
    setExerciciosSalvos([]);
    setLinhasExercicio([{ id: Date.now(), exercicioId: '', series: 3, repeticoes: 10, tempo: 0 }]);
  };

  const salvarGrupo = async (e) => {
    e.preventDefault();

    if (!editRotinaId && linhasExercicio.length === 0) {
      showToast('Adicione pelo menos um exercício à rotina!', 'error');
      return;
    }

    try {
      let rotinaId = editRotinaId;
      const url = editRotinaId ? `${API_BASE_URL}/admin/rotinas/${editRotinaId}` : `${API_BASE_URL}/admin/rotinas`;
      const method = editRotinaId ? 'PUT' : 'POST';

      const rotinaRes = await fetch(url, {
        method: method,
        headers: getAuthHeaders(),
        body: JSON.stringify({
          prontuarioId: parseInt(prontuario.id),
          nome: formData.nome,
          observacoes: formData.observacoes,
        }),
      });

      if (!rotinaRes.ok) throw new Error('Falha ao salvar a Rotina.');
      const rotina = await rotinaRes.json();
      
      if (!editRotinaId) rotinaId = rotina.id;

      for (let i = 0; i < linhasExercicio.length; i++) {
        const linha = linhasExercicio[i];
        if (!linha.exercicioId) continue;

        const prescRes = await fetch(`${API_BASE_URL}/admin/rotinas/${rotinaId}/exercicios`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({
              exercicioId: parseInt(linha.exercicioId),
              ordemNaRotina: exerciciosSalvos.length + i + 1,
              series: parseInt(linha.series),
              repeticoes: parseInt(linha.repeticoes),
              tempoSegundos: parseInt(linha.tempo),
              observacoes: '',
            }),
          }
        );
        if (!prescRes.ok) throw new Error(`Falha ao prescrever exercício (Linha ${i + 1}).`);
      }

      showToast(`Rotina "${formData.nome}" ${editRotinaId ? 'atualizada' : 'salva'} com sucesso!`, 'success');
      cancelarEdicao();
      carregarRotinas();
    } catch (e) {
      showToast(e.message, 'error');
    }
  };

  const adicionarLinha = () => {
    setLinhasExercicio((prev) => [
      ...prev,
      { id: Date.now(), exercicioId: '', series: 3, repeticoes: 10, tempo: 0 },
    ]);
  };

  const removerLinha = (id) => {
    setLinhasExercicio((prev) => prev.filter((l) => l.id !== id));
  };

  const atualizarLinha = (id, campo, valor) => {
    setLinhasExercicio((prev) =>
      prev.map((l) => (l.id === id ? { ...l, [campo]: valor } : l))
    );
  };

  return (
    <div className="page-fade">
      <div className="section-header">
        <div>
          <button className="btn btn-outline" onClick={onVoltar} style={{ marginBottom: '10px' }}>
            ← Voltar aos Prontuários
          </button>
          <h3>Rotinas do Prontuário: {prontuario.titulo}</h3>
        </div>
      </div>

      <details className="gaveta shadow-container" open>
        <summary>Rotinas Criadas (Neste Prontuário)</summary>
        <div className="gaveta-content">
          {rotinas.length === 0 ? (
            <p style={{ color: '#666' }}>Nenhuma rotina criada neste prontuário.</p>
          ) : (
            rotinas.map((r) => (
              <div key={r.id} className={`rotina-card ${editRotinaId === r.id ? 'row-editing' : ''}`}>
                <div>
                  <strong style={{ color: 'var(--primary)', fontSize: '1.05rem' }}>{r.nome}</strong>
                  <br />
                  <small style={{ color: '#666' }}>{r.observacoes || 'Sem observações'}</small>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <span className="badge-salva">ID: {r.id}</span>
                  <button className="btn-acao btn-editar" onClick={() => handleEditarRotina(r)} title="Editar Rotina e Exercícios">✏️</button>
                  <button className="btn-acao btn-deletar" onClick={() => handleDeletarRotina(r.id, r.nome)} title="Apagar Rotina">🗑️</button>
                </div>
              </div>
            ))
          )}
        </div>
      </details>

      <details className="gaveta shadow-container" open ref={detailsRef}>
        <summary>{editRotinaId ? `✎ Editando Rotina: ${formData.nome}` : '+ Criar Nova Rotina de Exercícios'}</summary>
        <div className="gaveta-content">
          <form onSubmit={salvarGrupo}>
            <div className="form-group" style={{ maxWidth: '500px' }}>
              <label>Nome da Rotina (ex: Semana 1 - Foco Lombar) *</label>
              <input
                type="text"
                className="input-shadow"
                required
                value={formData.nome}
                onChange={(e) => setFormData((p) => ({ ...p, nome: e.target.value }))}
              />
            </div>
            <div className="form-group" style={{ maxWidth: '500px' }}>
              <label>Observações da Rotina</label>
              <input
                type="text"
                className="input-shadow"
                placeholder="Instruções gerais..."
                value={formData.observacoes}
                onChange={(e) => setFormData((p) => ({ ...p, observacoes: e.target.value }))}
              />
            </div>

            <hr style={{ border: 0, borderTop: '1px solid #e2e8f0', margin: '20px 0' }} />

            {editRotinaId && (
              <div className="exercicios-salvos-container shadow-container">
                <h4 style={{ margin: '0 0 12px 0', color: '#334155' }}>Exercícios Já Prescritos:</h4>
                {exerciciosSalvos.length === 0 ? (
                  <p style={{ fontSize: '0.85rem', color: '#64748b', margin: 0 }}>Nenhum exercício nesta rotina.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {exerciciosSalvos.map((ex) => (
                      <div key={ex.id} className="exercicio-salvo-item">
                        <span style={{ fontSize: '0.9rem', color: '#1e293b', fontWeight: '500' }}>
                          {ex.exercicioTitulo} <span style={{ color: '#64748b', fontWeight: 'normal' }}>
                            (Séries: {ex.series} | Reps: {ex.repeticoes}{ex.tempoSegundos ? ` | Tempo: ${ex.tempoSegundos}s` : ''})
                          </span>
                        </span>
                        <button type="button" className="btn-acao btn-deletar" onClick={() => handleDeletarExercicioSalvo(ex.id, ex.exercicioTitulo)}>
                          ✕ Remover
                        </button>
                      </div>
                    ))}
                  </div>
                )}
                <hr style={{ border: 0, borderTop: '1px solid #e2e8f0', margin: '20px 0' }} />
              </div>
            )}

            <h4 style={{ marginBottom: '15px', color: 'var(--primary)' }}>
              {editRotinaId ? '+ Adicionar Novos Exercícios (Opcional)' : 'Selecione os Exercícios do Catálogo'}
            </h4>

            {linhasExercicio.map((linha) => (
              <div key={linha.id} className="linha-exercicio shadow-container" style={{ padding: '15px', background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', marginBottom: '15px' }}>
                <div className="form-group">
                  <label>Exercício *</label>
                  <select
                    className="input-shadow"
                    required
                    value={linha.exercicioId}
                    onChange={(e) => atualizarLinha(linha.id, 'exercicioId', e.target.value)}
                  >
                    <option value="" disabled>-- Escolha do Catálogo --</option>
                    {catalogoExercicios.length === 0 ? (
                      <option disabled>(Cadastre exercícios no Banco primeiro)</option>
                    ) : (
                      catalogoExercicios.map((ex) => (
                        <option key={ex.id} value={ex.id}>{ex.titulo} ({ex.categoria})</option>
                      ))
                    )}
                  </select>
                </div>
                <div className="form-group">
                  <label>Séries *</label>
                  <input type="number" className="input-shadow" min="1" required value={linha.series} onChange={(e) => atualizarLinha(linha.id, 'series', e.target.value)} />
                </div>
                <div className="form-group">
                  <label>Repetições *</label>
                  <input type="number" className="input-shadow" min="1" required value={linha.repeticoes} onChange={(e) => atualizarLinha(linha.id, 'repeticoes', e.target.value)} />
                </div>
                <div className="form-group">
                  <label>Tempo (seg)</label>
                  <input type="number" className="input-shadow" min="0" value={linha.tempo} onChange={(e) => atualizarLinha(linha.id, 'tempo', e.target.value)} />
                </div>
                <div className="form-group" style={{ display: 'flex', alignItems: 'flex-end' }}>
                  <button type="button" className="btn btn-danger" onClick={() => removerLinha(linha.id)} style={{ width: '100%' }}>X</button>
                </div>
              </div>
            ))}

            <div style={{ marginTop: '15px' }}>
              <button type="button" className="btn btn-outline" onClick={adicionarLinha}>+ Adicionar Outro Exercício</button>
            </div>

            <hr style={{ border: 0, borderTop: '1px solid #e2e8f0', margin: '20px 0' }} />
            
            <div className="form-actions-row">
              {editRotinaId && (
                <button type="button" className="btn-cancelar-edicao" onClick={cancelarEdicao}>Cancelar Edição</button>
              )}
              <button type="submit" className="btn" style={{ fontSize: '1.05rem', padding: '12px 25px' }}>
                {editRotinaId ? 'Atualizar Rotina' : 'Finalizar Rotina e Salvar'}
              </button>
            </div>
          </form>
        </div>
      </details>
    </div>
  );
};

/* ============================================================
   COMPONENTE PRINCIPAL
   ============================================================ */
const Prescricoes = () => {
  const [view, setView] = useState('lista');
  const [pacienteAtivo, setPacienteAtivo] = useState(null);
  const [prontuarioAtivo, setProntuarioAtivo] = useState(null);
  const [catalogoExercicios, setCatalogoExercicios] = useState([]);
  
  const [toasts, setToasts] = useState([]);

  useEffect(() => { carregarCatalogo(); }, []);

  const carregarCatalogo = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/admin/exercicios`, {
        headers: getAuthHeaders(),
      });
      if (res.ok) setCatalogoExercicios(await res.json());
    } catch (e) {
      console.warn('Catálogo não carregado:', e.message);
    }
  };

  const showToast = (message, type = 'success') => {
    const newToast = { id: Date.now() + Math.random(), message, type };
    setToasts((prev) => [...prev, newToast]);
  };

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  };

  const abrirPaciente = (id, nome) => {
    setPacienteAtivo({ id, nome });
    setView('paciente');
  };

  const abrirProntuario = (id, titulo) => {
    setProntuarioAtivo({ id, titulo });
    setView('prontuario');
  };

  return (
    <div className="section">
      <div className="maya-toast-container">
        {toasts.map((toast) => (
          <ToastItem key={toast.id} id={toast.id} message={toast.message} type={toast.type} onRemove={removeToast} />
        ))}
      </div>

      {view === 'lista' && <ListaPacientes onGerenciar={abrirPaciente} showToast={showToast} />}

      {view === 'paciente' && pacienteAtivo && (
        <PainelPaciente paciente={pacienteAtivo} onVoltar={() => setView('lista')} onAbrirProntuario={abrirProntuario} showToast={showToast} />
      )}

      {view === 'prontuario' && prontuarioAtivo && (
        <PainelProntuario prontuario={prontuarioAtivo} onVoltar={() => setView('paciente')} catalogoExercicios={catalogoExercicios} showToast={showToast} />
      )}
    </div>
  );
};

export default Prescricoes;
