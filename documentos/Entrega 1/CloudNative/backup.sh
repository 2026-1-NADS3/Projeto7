#!/bin/bash
# =============================================================================
# Script: backup.sh
# Projeto: MayaRPG Backend
# Descrição: Automatiza o backup do código-fonte e do banco de dados SQLite
#             (mayarpg.db) do projeto MayaRPG. Gera backups compactados com
#             timestamp, mantém histórico configurável e registra tudo em log.
# Uso:
#   bash backup.sh                  → backup completo (código + banco)
#   bash backup.sh --codigo         → apenas código-fonte
#   bash backup.sh --banco          → apenas banco de dados
#   bash backup.sh --listar         → lista backups existentes
#   bash backup.sh --restaurar      → restaura o backup mais recente do banco
# =============================================================================

# ─────────────────────────── VARIÁVEIS DE AMBIENTE ──────────────────────────
export MAYA_HOME="${HOME}/mayarpg"
export PROJETO_DIR="${HOME}/MayaRpgBackend"      # Diretório do código-fonte
export BACKUP_DIR="${MAYA_HOME}/backup"
export LOG_DIR="${MAYA_HOME}/logs"
export BACKUP_LOG="${LOG_DIR}/backup_$(date +%Y%m%d).log"
export DB_PATH="${MAYA_HOME}/db/mayarpg.db"      # Banco SQLite do MayaRPG
export DB_NOME="mayarpg.db"
export JAR_NAME="mayarpg-0.0.1-SNAPSHOT.jar"
export MANTER_BACKUPS=7        # Número de backups a manter (rotação)
export TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# ──────────────────────────── CORES PARA OUTPUT ─────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ─────────────────────────── FUNÇÕES UTILITÁRIAS ────────────────────────────
log() {
    local level="$1"
    local msg="$2"
    local ts
    ts=$(date '+%Y-%m-%d %H:%M:%S')
    # Redirecionamento >> para gravar no log
    echo "${ts} [${level}] ${msg}" >> "${BACKUP_LOG}"
    case "$level" in
        INFO)  echo -e "${GREEN}[INFO]${NC}  ${msg}" ;;
        WARN)  echo -e "${YELLOW}[WARN]${NC}  ${msg}" ;;
        ERROR) echo -e "${RED}[ERROR]${NC} ${msg}" ;;
        STEP)  echo -e "${CYAN}[STEP]${NC}  ${msg}" ;;
        OK)    echo -e "${GREEN}[✓]${NC}     ${msg}" ;;
    esac
}

inicializar() {
    mkdir -p "${BACKUP_DIR}"/{codigo,banco,logs}
    mkdir -p "${LOG_DIR}"
    log "INFO" "Iniciando rotina de backup — MayaRPG Backend"
    log "INFO" "Timestamp: ${TIMESTAMP}"
}

# Calcula tamanho de arquivo/diretório (pipe: du -> awk)
tamanho() {
    du -sh "$1" 2>/dev/null | awk '{print $1}'
}

# Gera checksum MD5 para verificação de integridade (pipe: md5sum -> awk)
checksum() {
    md5sum "$1" 2>/dev/null | awk '{print $1}'
}

# ─────────────────────────── BACKUP DO CÓDIGO-FONTE ─────────────────────────
backup_codigo() {
    log "STEP" "Iniciando backup do código-fonte..."

    # Verificar se o diretório do projeto existe
    if [[ ! -d "${PROJETO_DIR}" ]]; then
        # Tenta localizar automaticamente (pipe: find -> head)
        local encontrado
        encontrado=$(find "${HOME}" -name "pom.xml" -path "*/mayarpg*" 2>/dev/null | head -1 | xargs dirname 2>/dev/null)

        if [[ -n "${encontrado}" ]]; then
            PROJETO_DIR="${encontrado}"
            log "INFO" "Projeto encontrado em: ${PROJETO_DIR}"
        else
            log "WARN" "Diretório do projeto não encontrado. Defina PROJETO_DIR no script."
            log "WARN" "Pulando backup de código-fonte."
            return 1
        fi
    fi

    local arquivo_backup="${BACKUP_DIR}/codigo/mayarpg_codigo_${TIMESTAMP}.tar.gz"
    local tamanho_original
    tamanho_original=$(tamanho "${PROJETO_DIR}")

    log "INFO" "Compactando: ${PROJETO_DIR} (${tamanho_original})"

    # Compacta excluindo arquivos desnecessários (pipe de exclusões)
    # Redireciona stderr para o log
    tar -czf "${arquivo_backup}" \
        --exclude="*/target/*" \
        --exclude="*/.git/*" \
        --exclude="*/.idea/*" \
        --exclude="*/node_modules/*" \
        --exclude="*.class" \
        --exclude="*.jar" \
        -C "$(dirname "${PROJETO_DIR}")" \
        "$(basename "${PROJETO_DIR}")" \
        2>> "${BACKUP_LOG}"

    if [[ $? -eq 0 && -f "${arquivo_backup}" ]]; then
        local tamanho_backup checksum_backup
        tamanho_backup=$(tamanho "${arquivo_backup}")
        checksum_backup=$(checksum "${arquivo_backup}")

        log "OK"   "Backup de código criado: $(basename ${arquivo_backup})"
        log "INFO" "Tamanho: ${tamanho_backup} | MD5: ${checksum_backup}"

        # Salva manifest com metadados (redirecionamento >)
        cat > "${BACKUP_DIR}/codigo/$(basename ${arquivo_backup} .tar.gz).manifest" <<EOF
# Manifest — MayaRPG Backend Backup de Código
data=$(date)
arquivo=$(basename ${arquivo_backup})
origem=${PROJETO_DIR}
tamanho_original=${tamanho_original}
tamanho_backup=${tamanho_backup}
checksum_md5=${checksum_backup}
excluidos=target,.git,.idea,*.class,*.jar
EOF
        return 0
    else
        log "ERROR" "Falha ao criar backup do código-fonte."
        return 1
    fi
}

# ─────────────────────────── BACKUP DO BANCO DE DADOS ───────────────────────
backup_banco() {
    log "STEP" "Iniciando backup do banco de dados SQLite (${DB_NOME})..."

    # Tentar localizar o banco se não estiver no caminho padrão
    if [[ ! -f "${DB_PATH}" ]]; then
        local db_encontrado
        db_encontrado=$(find "${HOME}" -name "${DB_NOME}" 2>/dev/null | head -1)

        if [[ -n "${db_encontrado}" ]]; then
            DB_PATH="${db_encontrado}"
            log "INFO" "Banco encontrado em: ${DB_PATH}"
        else
            log "WARN" "Banco ${DB_NOME} não encontrado. Pulando backup do banco."
            return 1
        fi
    fi

    local arquivo_backup="${BACKUP_DIR}/banco/mayarpg_banco_${TIMESTAMP}.db.gz"
    local tamanho_original
    tamanho_original=$(tamanho "${DB_PATH}")

    log "INFO" "Banco atual: ${DB_PATH} (${tamanho_original})"

    # Verifica se o SQLite está disponível para dump seguro
    if command -v sqlite3 &>/dev/null; then
        log "INFO" "Usando sqlite3 para dump seguro (garante consistência)..."

        local dump_file="/tmp/mayarpg_dump_${TIMESTAMP}.sql"

        # Pipe: sqlite3 dump -> gzip -> arquivo compactado
        sqlite3 "${DB_PATH}" .dump | gzip > "${arquivo_backup}" 2>> "${BACKUP_LOG}"

        if [[ $? -eq 0 ]]; then
            log "OK" "Dump SQL criado e compactado com sucesso."
        else
            log "WARN" "Falha no dump SQL. Tentando cópia direta..."
            # Fallback: cópia direta compactada (pipe: gzip)
            gzip -c "${DB_PATH}" > "${arquivo_backup}" 2>> "${BACKUP_LOG}"
        fi
    else
        log "WARN" "sqlite3 não disponível. Usando cópia direta..."
        # Pipe: gzip direto no arquivo do banco
        gzip -c "${DB_PATH}" > "${arquivo_backup}" 2>> "${BACKUP_LOG}"
    fi

    if [[ -f "${arquivo_backup}" ]]; then
        local tamanho_backup checksum_backup
        tamanho_backup=$(tamanho "${arquivo_backup}")
        checksum_backup=$(checksum "${arquivo_backup}")

        log "OK"   "Backup do banco criado: $(basename ${arquivo_backup})"
        log "INFO" "Tamanho: ${tamanho_backup} | MD5: ${checksum_backup}"

        # Manifest do banco (redirecionamento >)
        cat > "${BACKUP_DIR}/banco/$(basename ${arquivo_backup} .db.gz).manifest" <<EOF
# Manifest — MayaRPG Backend Backup de Banco
data=$(date)
arquivo=$(basename ${arquivo_backup})
banco_origem=${DB_PATH}
tamanho_original=${tamanho_original}
tamanho_backup=${tamanho_backup}
checksum_md5=${checksum_backup}
metodo=$(command -v sqlite3 &>/dev/null && echo "sqlite3_dump" || echo "copia_direta")
EOF
        return 0
    else
        log "ERROR" "Falha ao criar backup do banco de dados."
        return 1
    fi
}

# ─────────────────────────── ROTAÇÃO DE BACKUPS ─────────────────────────────
rotacionar_backups() {
    log "STEP" "Executando rotação de backups (mantendo ${MANTER_BACKUPS} mais recentes)..."

    for tipo in codigo banco; do
        local dir="${BACKUP_DIR}/${tipo}"

        # Conta quantos backups existem (pipe: ls -> wc)
        local total
        total=$(ls -1 "${dir}"/*.gz 2>/dev/null | wc -l)

        if (( total > MANTER_BACKUPS )); then
            local remover=$(( total - MANTER_BACKUPS ))
            log "INFO" "[${tipo}] ${total} backups encontrados, removendo ${remover} mais antigos..."

            # Pipe: ls (ordenado por data) -> head -> xargs -> rm
            ls -1t "${dir}"/*.gz 2>/dev/null | tail -n "${remover}" | while IFS= read -r arquivo; do
                rm -f "${arquivo}" "${arquivo%.*}.manifest" 2>/dev/null
                log "INFO" "Removido: $(basename ${arquivo})"
            done
        else
            log "INFO" "[${tipo}] ${total}/${MANTER_BACKUPS} backups. Rotação não necessária."
        fi
    done
}

# ─────────────────────────── LISTAR BACKUPS ─────────────────────────────────
listar_backups() {
    echo ""
    echo -e "${BLUE}════════════════════════════════════════════${NC}"
    echo -e "${BLUE}        BACKUPS DO MAYARPG BACKEND          ${NC}"
    echo -e "${BLUE}════════════════════════════════════════════${NC}"

    echo -e "\n${CYAN}── Código-fonte:${NC}"
    # Pipe: ls -> awk para formatar saída
    if ls "${BACKUP_DIR}/codigo/"*.gz &>/dev/null 2>&1; then
        ls -lh "${BACKUP_DIR}/codigo/"*.gz | awk '{printf "  %-50s %s\n", $9, $5}' | xargs -I{} basename {}
        ls -lh "${BACKUP_DIR}/codigo/"*.gz | awk '{printf "  %-45s  %s\n", $NF, $5}'
    else
        echo -e "  Nenhum backup de código encontrado."
    fi

    echo -e "\n${CYAN}── Banco de dados:${NC}"
    if ls "${BACKUP_DIR}/banco/"*.gz &>/dev/null 2>&1; then
        ls -lh "${BACKUP_DIR}/banco/"*.gz | awk '{printf "  %-45s  %s\n", $NF, $5}'
    else
        echo -e "  Nenhum backup de banco encontrado."
    fi

    echo -e "\n${CYAN}── Espaço total de backups:${NC}"
    echo -e "  $(tamanho "${BACKUP_DIR}")"
    echo -e "${BLUE}════════════════════════════════════════════${NC}"
}

# ─────────────────────────── RESTAURAR BANCO ────────────────────────────────
restaurar_banco() {
    log "STEP" "Localizando backup mais recente do banco..."

    # Pipe: ls (ordenado) -> head para pegar o mais recente
    local backup_recente
    backup_recente=$(ls -1t "${BACKUP_DIR}/banco/"*.db.gz 2>/dev/null | head -1)

    if [[ -z "${backup_recente}" ]]; then
        log "ERROR" "Nenhum backup de banco encontrado em ${BACKUP_DIR}/banco/"
        exit 1
    fi

    log "INFO" "Backup selecionado: $(basename ${backup_recente})"
    echo -ne "${YELLOW}Deseja restaurar este backup? (s/N): ${NC}"
    read -r confirmacao

    if [[ "${confirmacao,,}" != "s" ]]; then
        log "INFO" "Restauração cancelada pelo usuário."
        exit 0
    fi

    # Fazer backup do banco atual antes de restaurar
    if [[ -f "${DB_PATH}" ]]; then
        local backup_atual="${DB_PATH}.bak_$(date +%Y%m%d_%H%M%S)"
        cp "${DB_PATH}" "${backup_atual}"
        log "INFO" "Banco atual salvo em: ${backup_atual}"
    fi

    log "STEP" "Restaurando banco de dados..."

    # Verifica se é dump SQL ou cópia direta (pipe: zcat -> head)
    if zcat "${backup_recente}" 2>/dev/null | head -1 | grep -q "SQLite format\|PRAGMA\|BEGIN TRANSACTION"; then
        log "INFO" "Restaurando via dump SQL..."
        # Pipe: zcat -> sqlite3
        zcat "${backup_recente}" | sqlite3 "${DB_PATH}" 2>> "${BACKUP_LOG}"
    else
        log "INFO" "Restaurando via cópia direta..."
        # Redireciona saída do gunzip para o arquivo do banco
        gunzip -c "${backup_recente}" > "${DB_PATH}" 2>> "${BACKUP_LOG}"
    fi

    if [[ $? -eq 0 ]]; then
        log "OK" "Banco restaurado com sucesso: ${DB_PATH}"
    else
        log "ERROR" "Falha na restauração. Banco anterior preservado em: ${backup_atual}"
        exit 1
    fi
}

# ─────────────────────────── RESUMO DO BACKUP ───────────────────────────────
resumo() {
    echo ""
    echo -e "${GREEN}════════════════════════════════════════════${NC}"
    echo -e "${GREEN}   BACKUP CONCLUÍDO — MAYARPG BACKEND       ${NC}"
    echo -e "${GREEN}════════════════════════════════════════════${NC}"
    echo -e "  Data:    $(date)"
    echo -e "  Destino: ${BACKUP_DIR}"
    echo -e "  Log:     ${BACKUP_LOG}"
    echo ""
    echo -e "  Backups disponíveis:"
    echo -e "  Código: $(ls "${BACKUP_DIR}/codigo/"*.gz 2>/dev/null | wc -l) arquivo(s)"
    echo -e "  Banco:  $(ls "${BACKUP_DIR}/banco/"*.gz 2>/dev/null | wc -l) arquivo(s)"
    echo -e "${GREEN}════════════════════════════════════════════${NC}"
    echo ""
    echo -e "  Para listar:   bash backup.sh --listar"
    echo -e "  Para restaurar: bash backup.sh --restaurar"
}

# ─────────────────────────────── MAIN ───────────────────────────────────────
main() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║   MayaRPG Backend — Sistema de Backup    ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
    echo ""

    inicializar

    case "${1:-}" in
        --codigo)
            backup_codigo
            ;;
        --banco)
            backup_banco
            ;;
        --listar)
            listar_backups
            exit 0
            ;;
        --restaurar)
            restaurar_banco
            exit 0
            ;;
        *)
            # Backup completo (padrão)
            backup_codigo
            echo ""
            backup_banco
            echo ""
            rotacionar_backups
            resumo
            ;;
    esac
}

main "$@"
