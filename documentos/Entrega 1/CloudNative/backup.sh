#!/bin/bash

MAYA_HOME="${HOME}/mayarpg"
PROJETO_DIR="${HOME}/MayaRpgBackend"
BACKUP_DIR="${MAYA_HOME}/backup"
LOG_DIR="${MAYA_HOME}/logs"
BACKUP_LOG="${LOG_DIR}/backup_$(date +%Y%m%d).log"
DB_PATH="${MAYA_HOME}/db/mayarpg.db"
DB_NOME="mayarpg.db"
MANTER_BACKUPS=7
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log() {
    local level="$1"
    local msg="$2"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [${level}] ${msg}" >> "${BACKUP_LOG}"
    case "$level" in
        INFO)  echo -e "${GREEN}[INFO]${NC}  ${msg}" ;;
        WARN)  echo -e "${YELLOW}[WARN]${NC}  ${msg}" ;;
        ERROR) echo -e "${RED}[ERROR]${NC} ${msg}" ;;
        STEP)  echo -e "${CYAN}[STEP]${NC}  ${msg}" ;;
        OK)    echo -e "${GREEN}[OK]${NC}    ${msg}" ;;
    esac
}

inicializar() {
    mkdir -p "${BACKUP_DIR}"/{codigo,banco} "${LOG_DIR}"
    log "INFO" "Iniciando backup do MayaRPG - ${TIMESTAMP}"
}

tamanho() {
    du -sh "$1" 2>/dev/null | awk '{print $1}'
}

checksum() {
    md5sum "$1" 2>/dev/null | awk '{print $1}'
}

backup_codigo() {
    log "STEP" "Fazendo backup do código-fonte..."

    if [[ ! -d "${PROJETO_DIR}" ]]; then
        local encontrado
        encontrado=$(find "${HOME}" -name "pom.xml" -path "*/mayarpg*" 2>/dev/null | head -1 | xargs dirname 2>/dev/null)

        if [[ -n "${encontrado}" ]]; then
            PROJETO_DIR="${encontrado}"
            log "INFO" "Projeto encontrado em: ${PROJETO_DIR}"
        else
            log "WARN" "Diretório do projeto não encontrado. Ajuste a variável PROJETO_DIR."
            return 1
        fi
    fi

    local arquivo_backup="${BACKUP_DIR}/codigo/mayarpg_codigo_${TIMESTAMP}.tar.gz"
    log "INFO" "Compactando: ${PROJETO_DIR} ($(tamanho "${PROJETO_DIR}"))"

    tar -czf "${arquivo_backup}" \
        --exclude="*/target/*" \
        --exclude="*/.git/*" \
        --exclude="*/.idea/*" \
        --exclude="*.class" \
        --exclude="*.jar" \
        -C "$(dirname "${PROJETO_DIR}")" \
        "$(basename "${PROJETO_DIR}")" \
        2>> "${BACKUP_LOG}"

    if [[ $? -eq 0 && -f "${arquivo_backup}" ]]; then
        local cs
        cs=$(checksum "${arquivo_backup}")
        log "OK" "Backup criado: $(basename "${arquivo_backup}") | MD5: ${cs}"

        cat > "${BACKUP_DIR}/codigo/$(basename "${arquivo_backup}" .tar.gz).manifest" <<EOF
data=$(date)
arquivo=$(basename "${arquivo_backup}")
origem=${PROJETO_DIR}
tamanho=$(tamanho "${arquivo_backup}")
checksum_md5=${cs}
EOF
        return 0
    else
        log "ERROR" "Falha ao criar backup do código."
        return 1
    fi
}

backup_banco() {
    log "STEP" "Fazendo backup do banco de dados (${DB_NOME})..."

    if [[ ! -f "${DB_PATH}" ]]; then
        local db_encontrado
        db_encontrado=$(find "${HOME}" -name "${DB_NOME}" 2>/dev/null | head -1)

        if [[ -n "${db_encontrado}" ]]; then
            DB_PATH="${db_encontrado}"
            log "INFO" "Banco encontrado em: ${DB_PATH}"
        else
            log "WARN" "Banco ${DB_NOME} não encontrado."
            return 1
        fi
    fi

    local arquivo_backup="${BACKUP_DIR}/banco/mayarpg_banco_${TIMESTAMP}.db.gz"
    log "INFO" "Banco: ${DB_PATH} ($(tamanho "${DB_PATH}"))"

    if command -v sqlite3 &>/dev/null; then
        sqlite3 "${DB_PATH}" .dump | gzip > "${arquivo_backup}" 2>> "${BACKUP_LOG}"
    else
        log "WARN" "sqlite3 não disponível. Usando cópia direta..."
        gzip -c "${DB_PATH}" > "${arquivo_backup}" 2>> "${BACKUP_LOG}"
    fi

    if [[ -f "${arquivo_backup}" ]]; then
        local cs
        cs=$(checksum "${arquivo_backup}")
        log "OK" "Banco salvo: $(basename "${arquivo_backup}") | MD5: ${cs}"

        cat > "${BACKUP_DIR}/banco/$(basename "${arquivo_backup}" .db.gz).manifest" <<EOF
data=$(date)
arquivo=$(basename "${arquivo_backup}")
banco_origem=${DB_PATH}
tamanho=$(tamanho "${arquivo_backup}")
checksum_md5=${cs}
EOF
        return 0
    else
        log "ERROR" "Falha ao criar backup do banco."
        return 1
    fi
}

rotacionar_backups() {
    log "STEP" "Removendo backups antigos (mantendo ${MANTER_BACKUPS})..."

    for tipo in codigo banco; do
        local dir="${BACKUP_DIR}/${tipo}"
        local total
        total=$(ls -1 "${dir}"/*.gz 2>/dev/null | wc -l)

        if (( total > MANTER_BACKUPS )); then
            local remover=$(( total - MANTER_BACKUPS ))
            log "INFO" "[${tipo}] Removendo ${remover} backup(s) antigo(s)..."
            ls -1t "${dir}"/*.gz 2>/dev/null | tail -n "${remover}" | while IFS= read -r arquivo; do
                rm -f "${arquivo}" "${arquivo%.*}.manifest" 2>/dev/null
                log "INFO" "Removido: $(basename "${arquivo}")"
            done
        fi
    done
}

listar_backups() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}     BACKUPS - MayaRPG Backend         ${NC}"
    echo -e "${BLUE}========================================${NC}"

    echo -e "\n${CYAN}Codigo-fonte:${NC}"
    if ls "${BACKUP_DIR}/codigo/"*.gz &>/dev/null 2>&1; then
        ls -lh "${BACKUP_DIR}/codigo/"*.gz | awk '{printf "  %-45s  %s\n", $NF, $5}'
    else
        echo -e "  Nenhum backup encontrado."
    fi

    echo -e "\n${CYAN}Banco de dados:${NC}"
    if ls "${BACKUP_DIR}/banco/"*.gz &>/dev/null 2>&1; then
        ls -lh "${BACKUP_DIR}/banco/"*.gz | awk '{printf "  %-45s  %s\n", $NF, $5}'
    else
        echo -e "  Nenhum backup encontrado."
    fi

    echo -e "\n  Espaco total: $(tamanho "${BACKUP_DIR}")"
    echo -e "${BLUE}========================================${NC}"
}

restaurar_banco() {
    log "STEP" "Buscando backup mais recente do banco..."

    local backup_recente
    backup_recente=$(ls -1t "${BACKUP_DIR}/banco/"*.db.gz 2>/dev/null | head -1)

    if [[ -z "${backup_recente}" ]]; then
        log "ERROR" "Nenhum backup encontrado em ${BACKUP_DIR}/banco/"
        exit 1
    fi

    log "INFO" "Backup: $(basename "${backup_recente}")"
    echo -ne "${YELLOW}Confirma restauração? (s/N): ${NC}"
    read -r confirmacao

    if [[ "${confirmacao,,}" != "s" ]]; then
        log "INFO" "Restauração cancelada."
        exit 0
    fi

    if [[ -f "${DB_PATH}" ]]; then
        cp "${DB_PATH}" "${DB_PATH}.bak_$(date +%Y%m%d_%H%M%S)"
        log "INFO" "Banco atual salvo como .bak"
    fi

    if zcat "${backup_recente}" 2>/dev/null | head -1 | grep -q "PRAGMA\|BEGIN TRANSACTION"; then
        zcat "${backup_recente}" | sqlite3 "${DB_PATH}" 2>> "${BACKUP_LOG}"
    else
        gunzip -c "${backup_recente}" > "${DB_PATH}" 2>> "${BACKUP_LOG}"
    fi

    if [[ $? -eq 0 ]]; then
        log "OK" "Banco restaurado: ${DB_PATH}"
    else
        log "ERROR" "Falha na restauração."
        exit 1
    fi
}

main() {
    echo ""
    echo -e "${CYAN}  MayaRPG Backend - Backup${NC}"
    echo ""

    inicializar

    case "${1:-}" in
        --codigo)    backup_codigo ;;
        --banco)     backup_banco ;;
        --listar)    listar_backups; exit 0 ;;
        --restaurar) restaurar_banco; exit 0 ;;
        *)
            backup_codigo
            echo ""
            backup_banco
            echo ""
            rotacionar_backups
            echo ""
            echo -e "${GREEN}Backup concluido! Arquivos em: ${BACKUP_DIR}${NC}"
            echo -e "Log: ${BACKUP_LOG}"
            ;;
    esac
}

main "$@"
