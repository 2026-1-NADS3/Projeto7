import React, { useState, useEffect, useRef } from 'react';
import './BancoExercicios.css';
import API_BASE_URL from '../../config/api';

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

const BancoExercicios = () => {
  const [exercicios, setExercicios]     = useState([]);
  const [loadingTable, setLoadingTable] = useState(true);
  const [salvando, setSalvando]         = useState(false);
  const [erroForm, setErroForm]         = useState('');

  // ESTADO DOS TOASTS
  const [toasts, setToasts] = useState([]);

  // ESTADOS DE EDIÇÃO
  const [editId, setEditId] = useState(null);
  const detailsRef = useRef(null); 

  const [formData, setFormData] = useState({
    titulo: '',
    categoria: 'FORCA',
    descricao: '',
    videoUrl: '',
  });

  const [imagemFile, setImagemFile]       = useState(null);
  const [imagemPreview, setImagemPreview] = useState('');
  const fileInputRef = useRef(null);

  // === CONFIGURAÇÕES DO CLOUDINARY ===
  const CLOUDINARY_CLOUD_NAME = "dwadux2yn"; 
  const CLOUDINARY_UPLOAD_PRESET = "maya_exercicios"; 

  useEffect(() => { carregarExercicios(); }, []);

  // --- Funções de Toast ---
  const showToast = (message, type = 'success') => {
    const newToast = { id: Date.now() + Math.random(), message, type };
    setToasts((prev) => [...prev, newToast]);
  };

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  };

  const carregarExercicios = async () => {
    setLoadingTable(true);
    try {
      const res = await fetch(`${API_BASE_URL}/api/admin/exercicios`, {
        headers: getAuthHeaders(),
      });
      if (res.ok) setExercicios(await res.json());
    } catch (e) {
      console.warn('Erro ao carregar exercícios:', e.message);
      showToast('Erro ao carregar catálogo de exercícios.', 'error');
    } finally {
      setLoadingTable(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleImagemChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setErroForm('Selecione um arquivo de imagem (JPG, PNG, WEBP...).');
      return;
    }

    if (file.size > 1.5 * 1024 * 1024) {
      setErroForm('Recomendamos imagens de até 1,5 MB para não demorar o upload.');
    } else {
      setErroForm('');
    }

    setImagemFile(file);
    setImagemPreview(URL.createObjectURL(file));
  };

  const removerImagem = () => {
    setImagemFile(null);
    setImagemPreview('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const uploadImagemCloudinary = async (file) => {
    const formDataCloudinary = new FormData();
    formDataCloudinary.append("file", file);
    formDataCloudinary.append("upload_preset", CLOUDINARY_UPLOAD_PRESET);

    const res = await fetch(`https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`, {
      method: "POST",
      body: formDataCloudinary,
    });

    if (!res.ok) throw new Error('Falha ao fazer upload da imagem na nuvem.');

    const data = await res.json();
    return data.secure_url;
  };

  // --- AÇÕES CRUD ---

  const salvarExercicio = async (e) => {
    e.preventDefault();
    setErroForm('');
    setSalvando(true);

    try {
      let urlDaFotoSalva = null;

      if (imagemFile) {
        urlDaFotoSalva = await uploadImagemCloudinary(imagemFile);
      } 
      else if (imagemPreview && imagemPreview.startsWith('http')) {
        urlDaFotoSalva = imagemPreview;
      }

      const payload = {
        titulo:    formData.titulo,
        categoria: formData.categoria,
        descricao: formData.descricao,
        videoUrl:  formData.videoUrl || null,
        fotoUrl:   urlDaFotoSalva,
      };

      const url = editId 
        ? `${API_BASE_URL}/api/admin/exercicios/${editId}` 
        : `${API_BASE_URL}/api/admin/exercicios`;
      
      const method = editId ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method: method,
        headers: getAuthHeaders(),
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.messagem || `Erro HTTP ${res.status}`);
      }

      showToast(`Exercício "${formData.titulo}" ${editId ? 'atualizado' : 'salvo'} com sucesso!`, 'success');
      cancelarEdicao(); 
      carregarExercicios(); 
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setSalvando(false);
    }
  };

  const handleEditar = (ex) => {
    setEditId(ex.id);
    setFormData({
      titulo: ex.titulo || '',
      categoria: ex.categoria || 'FORCA',
      descricao: ex.descricao || '',
      videoUrl: ex.videoUrl || '',
    });
    setImagemPreview(ex.fotoUrl || '');
    setImagemFile(null);
    
    if (detailsRef.current) detailsRef.current.open = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDeletar = async (id, titulo) => {
    if (!window.confirm(`Tem certeza que deseja DELETAR permanentemente o exercício "${titulo}"?`)) return;

    try {
      const res = await fetch(`${API_BASE_URL}/api/admin/exercicios/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });

      if (res.ok) {
        showToast('Exercício deletado com sucesso!', 'success');
        if (editId === id) cancelarEdicao();
        carregarExercicios();
      } else {
        showToast('Erro ao deletar exercício.', 'error');
      }
    } catch (err) {
      console.error(err);
      showToast('Erro de conexão ao tentar deletar.', 'error');
    }
  };

  const cancelarEdicao = () => {
    setEditId(null);
    setFormData({ titulo: '', categoria: 'FORCA', descricao: '', videoUrl: '' });
    removerImagem();
  };

  return (
    <div className="section page-fade">
      {/* RENDERIZA TOASTS */}
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

      <div className="section-header">
        <div>
          <h3>Gerenciador de Exercícios</h3>
          <p>Cadastre, edite ou remova exercícios do banco de dados da clínica.</p>
        </div>
      </div>

      <details className="gaveta shadow-container" open ref={detailsRef}>
        <summary>
          {editId ? `✎ Editando Exercício #${editId}` : '+ Cadastrar Novo Exercício Base'}
        </summary>
        <div className="gaveta-content">
          <form onSubmit={salvarExercicio}>

            <div className="form-grid">
              <div className="form-group">
                <label>Título do Exercício *</label>
                <input
                  type="text"
                  name="titulo"
                  className="input-shadow"
                  required
                  placeholder="Ex: Agachamento Livre"
                  value={formData.titulo}
                  onChange={handleChange}
                />
              </div>
              <div className="form-group">
                <label>Categoria *</label>
                <select name="categoria" className="input-shadow" required value={formData.categoria} onChange={handleChange}>
                  <option value="FORCA">Força</option>
                  <option value="MOBILIDADE">Mobilidade</option>
                  <option value="CARDIO">Cardio</option>
                  <option value="ALONGAMENTO">Alongamento</option>
                  <option value="EQUILIBRIO">Equilíbrio</option>
                  <option value="RESPIRACAO">Respiração</option>
                  <option value="OUTRO">Outro</option>
                </select>
              </div>
            </div>

            <div className="form-group">
              <label>Descrição Completa / Instruções</label>
              <textarea
                name="descricao"
                className="input-shadow"
                rows="3"
                placeholder="Exercício para fortalecimento de membros inferiores..."
                value={formData.descricao}
                onChange={handleChange}
              />
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label>URL do Vídeo (Opcional)</label>
                <input
                  type="url"
                  name="videoUrl"
                  className="input-shadow"
                  placeholder="https://youtube.com/watch?v=..."
                  value={formData.videoUrl}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label>Foto do Exercício (Opcional)</label>

                {!imagemPreview ? (
                  <div className="upload-area" onClick={() => fileInputRef.current?.click()}>
                    <span className="upload-icon">📁</span>
                    <span>Clique para selecionar uma imagem</span>
                    <small>JPG, PNG, WEBP — recomendado até 1,5 MB</small>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/*"
                      style={{ display: 'none' }}
                      onChange={handleImagemChange}
                    />
                  </div>
                ) : (
                  <div className="upload-preview">
                    <img src={imagemPreview} alt="Preview" className="preview-img" />
                    <div className="preview-info">
                      <span className="preview-name">{imagemFile ? imagemFile.name : 'Imagem atual'}</span>
                      <button type="button" className="btn-remover-img" onClick={removerImagem}>
                        ✕ Remover
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {erroForm && <p className="form-error">{erroForm}</p>}

            <div className="form-actions-row">
              {editId && (
                <button type="button" className="btn-cancelar-edicao" onClick={cancelarEdicao} disabled={salvando}>
                  Cancelar Edição
                </button>
              )}
              <button type="submit" className="btn" disabled={salvando}>
                {salvando ? 'Processando...' : (editId ? 'Atualizar Exercício' : 'Salvar no Catálogo')}
              </button>
            </div>
          </form>
        </div>
      </details>

      <div className="section-header" style={{ marginTop: '30px' }}>
        <h4>Exercícios Cadastrados ({exercicios.length})</h4>
      </div>

      <div className="table-container shadow-container">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Foto</th>
              <th>Título</th>
              <th>Categoria</th>
              <th style={{ textAlign: 'right' }}>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loadingTable && (
              <tr><td colSpan="5" style={{ textAlign: 'center', color: '#666', padding: '20px' }}>Carregando catálogo...</td></tr>
            )}
            {!loadingTable && exercicios.length === 0 && (
              <tr><td colSpan="5" style={{ textAlign: 'center', color: '#666', padding: '20px' }}>Nenhum exercício cadastrado ainda.</td></tr>
            )}
            {exercicios.map((ex) => (
              <tr key={ex.id} className={editId === ex.id ? 'row-editing' : ''}>
                <td>{ex.id}</td>
                <td>
                  {ex.fotoUrl ? (
                    <img
                      src={ex.fotoUrl}
                      alt={ex.titulo}
                      style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 6 }}
                    />
                  ) : (
                    <span style={{ color: '#bbb', fontSize: '0.8rem' }}>—</span>
                  )}
                </td>
                <td><strong>{ex.titulo}</strong></td>
                <td><span className={`badge-categoria badge-${ex.categoria.toLowerCase()}`}>{ex.categoria}</span></td>
                
                {/* CÉLULA DE AÇÕES */}
                <td style={{ textAlign: 'right' }}>
                  <button className="btn-acao btn-editar" onClick={() => handleEditar(ex)} title="Editar">
                    ✏️
                  </button>
                  <button className="btn-acao btn-deletar" onClick={() => handleDeletar(ex.id, ex.titulo)} title="Deletar">
                    🗑️
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

export default BancoExercicios;
