#!/bin/bash
# =============================================================================
# Script: setup_ambiente.sh
# Projeto: MayaRPG Backend
# Descrição: Automatiza a instalação de dependências necessárias para o
#             ambiente de desenvolvimento do projeto MayaRPG Backend.
#             Instala Java 21, Maven, SQLite e configura variáveis de ambiente.
# Uso:
#   sudo bash setup_ambiente.sh
# =============================================================================

# ─────────────────────────── VARIÁVEIS DE AMBIENTE ──────────────────────────
export JAVA_VERSION="21"
export MAVEN_VERSION="3.9.6"
export MAVEN_URL="https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
export MAVEN_INSTALL_DIR="/opt/maven"
export MAYA_HOME="${HOME}/mayarpg"
export LOG_DIR="${MAYA_HOME}/logs"
export LOG_FILE="${LOG_DIR}/setup_$(date +%Y%m%d_%H%M%S).log"
export JAR_NAME="mayarpg-0.0.1-SNAPSHOT.jar"
export APP_PORT="8080"

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
    local timestamp
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "${timestamp} [${level}] ${msg}" >> "${LOG_FILE}" 2>/dev/null || true
    case "$level" in
        INFO)    echo -e "${GREEN}[INFO]${NC}  ${msg}" ;;
        WARN)    echo -e "${YELLOW}[WARN]${NC}  ${msg}" ;;
        ERROR)   echo -e "${RED}[ERROR]${NC} ${msg}" ;;
        STEP)    echo -e "${CYAN}[STEP]${NC}  ${msg}" ;;
    esac
}

check_root() {
    if [[ $EUID -ne 0 ]]; then
        echo -e "${RED}[ERROR]${NC} Este script precisa ser executado como root (use sudo)."
        exit 1
    fi
}

check_internet() {
    log "STEP" "Verificando conexão com a internet..."
    if ! ping -c 1 google.com &>/dev/null; then
        log "ERROR" "Sem conexão com a internet. Verifique sua rede."
        exit 1
    fi
    log "INFO" "Conexão com a internet OK."
}

criar_diretorios() {
    log "STEP" "Criando estrutura de diretórios do MayaRPG..."
    mkdir -p "${MAYA_HOME}"/{logs,backup,db,target}
    log "INFO" "Diretórios criados em ${MAYA_HOME}"
}

# ─────────────────────────── INSTALAÇÃO JAVA 21 ─────────────────────────────
instalar_java() {
    log "STEP" "Verificando instalação do Java..."

    if java -version 2>&1 | grep -q "21"; then
        log "INFO" "Java 21 já está instalado. Pulando instalação."
        java -version 2>&1 | tee -a "${LOG_FILE}"
        return 0
    fi

    log "STEP" "Instalando Java ${JAVA_VERSION}..."
    apt-get update -qq >> "${LOG_FILE}" 2>&1
    apt-get install -y "openjdk-${JAVA_VERSION}-jdk" >> "${LOG_FILE}" 2>&1

    if java -version 2>&1 | grep -q "21"; then
        log "INFO" "Java ${JAVA_VERSION} instalado com sucesso!"
    else
        log "ERROR" "Falha ao instalar Java ${JAVA_VERSION}. Verifique o log: ${LOG_FILE}"
        exit 1
    fi

    # Configurar JAVA_HOME via /etc/environment (pipe + redirecionamento)
    JAVA_HOME_PATH=$(dirname $(dirname $(readlink -f $(which java))))
    echo "export JAVA_HOME=${JAVA_HOME_PATH}" >> /etc/environment
    echo "export PATH=\$PATH:\$JAVA_HOME/bin" >> /etc/environment
    log "INFO" "JAVA_HOME configurado: ${JAVA_HOME_PATH}"
}

# ─────────────────────────── INSTALAÇÃO MAVEN ───────────────────────────────
instalar_maven() {
    log "STEP" "Verificando instalação do Maven..."

    if command -v mvn &>/dev/null && mvn --version 2>&1 | grep -q "3\.9"; then
        log "INFO" "Maven 3.9.x já está instalado. Pulando instalação."
        mvn --version | tee -a "${LOG_FILE}"
        return 0
    fi

    log "STEP" "Baixando Maven ${MAVEN_VERSION}..."
    local tmp_file="/tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

    # Usa pipe: wget -> stdout seria alternativa, aqui usamos redirecionamento de log
    wget -q "${MAVEN_URL}" -O "${tmp_file}" >> "${LOG_FILE}" 2>&1

    if [[ ! -f "${tmp_file}" ]]; then
        log "ERROR" "Falha ao baixar Maven. Verifique o log: ${LOG_FILE}"
        exit 1
    fi

    log "STEP" "Instalando Maven em ${MAVEN_INSTALL_DIR}..."
    mkdir -p "${MAVEN_INSTALL_DIR}"
    tar -xzf "${tmp_file}" -C "${MAVEN_INSTALL_DIR}" --strip-components=1 >> "${LOG_FILE}" 2>&1
    rm -f "${tmp_file}"

    # Symlink e PATH
    ln -sf "${MAVEN_INSTALL_DIR}/bin/mvn" /usr/local/bin/mvn
    echo "export M2_HOME=${MAVEN_INSTALL_DIR}" >> /etc/environment
    echo "export PATH=\$PATH:${MAVEN_INSTALL_DIR}/bin" >> /etc/environment

    if mvn --version &>/dev/null; then
        log "INFO" "Maven ${MAVEN_VERSION} instalado com sucesso!"
        mvn --version | tee -a "${LOG_FILE}"
    else
        log "ERROR" "Falha ao instalar Maven."
        exit 1
    fi
}

# ─────────────────────────── INSTALAÇÃO SQLITE ──────────────────────────────
instalar_sqlite() {
    log "STEP" "Verificando SQLite (banco de dados do MayaRPG)..."

    if command -v sqlite3 &>/dev/null; then
        log "INFO" "SQLite já está instalado: $(sqlite3 --version)"
        return 0
    fi

    log "STEP" "Instalando SQLite3..."
    apt-get install -y sqlite3 >> "${LOG_FILE}" 2>&1

    if command -v sqlite3 &>/dev/null; then
        log "INFO" "SQLite3 instalado: $(sqlite3 --version)"
    else
        log "ERROR" "Falha ao instalar SQLite3."
        exit 1
    fi
}

# ─────────────────────────── ANDROID SDK TOOLS ──────────────────────────────
instalar_android_sdk_tools() {
    log "STEP" "Instalando ferramentas base para build Android..."

    # Dependências base necessárias para o Android SDK
    apt-get install -y \
        wget unzip curl git \
        lib32stdc++6 lib32z1 \
        >> "${LOG_FILE}" 2>&1

    log "INFO" "Ferramentas base para Android SDK instaladas."
    log "WARN" "Para o Android SDK completo, acesse: https://developer.android.com/studio"
}

# ──────────────────────── CONFIGURAR VARIÁVEIS DO PROJETO ───────────────────
configurar_env_projeto() {
    log "STEP" "Configurando arquivo .env do projeto MayaRPG..."

    local env_file="${MAYA_HOME}/.env"

    # Redirecionamento (>) para criar o arquivo de configuração
    cat > "${env_file}" <<EOF
# ── Variáveis de Ambiente MayaRPG Backend ──
# Gerado automaticamente por setup_ambiente.sh em $(date)

# Aplicação
APP_PORT=${APP_PORT}
JAR_NAME=${JAR_NAME}
MAYA_HOME=${MAYA_HOME}

# JWT (TROCAR EM PRODUÇÃO!)
JWT_SECRET=maya_dev_secret_change_in_production
JWT_EXPIRATION=86400000

# Banco de dados SQLite
DB_PATH=${MAYA_HOME}/db/mayarpg.db

# Logs
LOG_DIR=${LOG_DIR}
LOG_LEVEL=INFO

# Endpoints do backend
API_BASE_URL=http://localhost:${APP_PORT}/api
SWAGGER_URL=http://localhost:${APP_PORT}/swagger-ui/index.html

# Endpoints MayaRPG
ENDPOINT_LOGIN=/api/auth/login
ENDPOINT_REGISTER=/api/auth/register
ENDPOINT_PACIENTES=/api/admin/pacientes
EOF

    # Permissão restrita ao arquivo .env (conceito de permissões Linux)
    chmod 600 "${env_file}"
    log "INFO" "Arquivo .env criado em ${env_file} com permissão 600"
    log "WARN" "IMPORTANTE: Altere JWT_SECRET antes de ir para produção!"
}

# ──────────────────────────── VERIFICAÇÃO FINAL ─────────────────────────────
verificar_instalacao() {
    log "STEP" "Executando verificação final da instalação..."
    echo ""
    echo -e "${BLUE}════════════════════════════════════════════${NC}"
    echo -e "${BLUE}   RESUMO DA INSTALAÇÃO — MayaRPG Backend   ${NC}"
    echo -e "${BLUE}════════════════════════════════════════════${NC}"

    local status_java status_maven status_sqlite
    java -version &>/dev/null   && status_java="${GREEN}✓ OK${NC}"   || status_java="${RED}✗ FALHOU${NC}"
    mvn --version &>/dev/null   && status_maven="${GREEN}✓ OK${NC}"  || status_maven="${RED}✗ FALHOU${NC}"
    sqlite3 --version &>/dev/null && status_sqlite="${GREEN}✓ OK${NC}" || status_sqlite="${RED}✗ FALHOU${NC}"

    echo -e "  Java 21:    $(eval echo -e "${status_java}")"
    echo -e "  Maven 3.9:  $(eval echo -e "${status_maven}")"
    echo -e "  SQLite3:    $(eval echo -e "${status_sqlite}")"
    echo ""
    echo -e "  MAYA_HOME:  ${MAYA_HOME}"
    echo -e "  Logs:       ${LOG_FILE}"
    echo -e "  .env:       ${MAYA_HOME}/.env"
    echo ""
    echo -e "  Próximo passo:"
    echo -e "  → Entre no diretório do projeto e rode: ./gerenciar_servico.sh start"
    echo -e "${BLUE}════════════════════════════════════════════${NC}"
    echo ""
    log "INFO" "Setup finalizado com sucesso."
}

# ─────────────────────────────── MAIN ───────────────────────────────────────
main() {
    # Criar diretório de log antes de qualquer coisa
    mkdir -p "${LOG_DIR}" 2>/dev/null || {
        LOG_DIR="/tmp/maya-logs"
        LOG_FILE="${LOG_DIR}/setup_$(date +%Y%m%d_%H%M%S).log"
        mkdir -p "${LOG_DIR}"
    }

    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║   MayaRPG Backend — Setup do Ambiente      ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════╝${NC}"
    echo ""

    check_root
    check_internet
    criar_diretorios
    instalar_java
    instalar_maven
    instalar_sqlite
    instalar_android_sdk_tools
    configurar_env_projeto
    verificar_instalacao
}

main "$@"
