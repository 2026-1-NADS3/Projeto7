#!/bin/bash

JAVA_VERSION="21"
MAVEN_VERSION="3.9.6"
MAVEN_URL="https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_INSTALL_DIR="/opt/maven"
MAYA_HOME="${HOME}/mayarpg"
LOG_DIR="${MAYA_HOME}/logs"
LOG_FILE="${LOG_DIR}/setup_$(date +%Y%m%d_%H%M%S).log"
JAR_NAME="mayarpg-0.0.1-SNAPSHOT.jar"
APP_PORT="8080"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log() {
    local level="$1"
    local msg="$2"
    local timestamp
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "${timestamp} [${level}] ${msg}" >> "${LOG_FILE}" 2>/dev/null || true
    case "$level" in
        INFO)  echo -e "${GREEN}[INFO]${NC}  ${msg}" ;;
        WARN)  echo -e "${YELLOW}[WARN]${NC}  ${msg}" ;;
        ERROR) echo -e "${RED}[ERROR]${NC} ${msg}" ;;
        STEP)  echo -e "${CYAN}[STEP]${NC}  ${msg}" ;;
    esac
}

check_root() {
    if [[ $EUID -ne 0 ]]; then
        echo -e "${RED}[ERROR]${NC} Execute com sudo."
        exit 1
    fi
}

check_internet() {
    log "STEP" "Verificando conexão com a internet..."
    if ! ping -c 1 google.com &>/dev/null; then
        log "ERROR" "Sem conexão com a internet."
        exit 1
    fi
    log "INFO" "Conexão OK."
}

criar_diretorios() {
    log "STEP" "Criando diretórios do projeto..."
    mkdir -p "${MAYA_HOME}"/{logs,backup,db,target}
    log "INFO" "Diretórios criados em ${MAYA_HOME}"
}

instalar_java() {
    log "STEP" "Verificando Java..."

    if java -version 2>&1 | grep -q "21"; then
        log "INFO" "Java 21 já instalado."
        return 0
    fi

    log "STEP" "Instalando Java ${JAVA_VERSION}..."
    apt-get update -qq >> "${LOG_FILE}" 2>&1
    apt-get install -y "openjdk-${JAVA_VERSION}-jdk" >> "${LOG_FILE}" 2>&1

    if java -version 2>&1 | grep -q "21"; then
        log "INFO" "Java ${JAVA_VERSION} instalado com sucesso!"
    else
        log "ERROR" "Falha ao instalar Java. Veja o log: ${LOG_FILE}"
        exit 1
    fi

    JAVA_HOME_PATH=$(dirname $(dirname $(readlink -f $(which java))))
    echo "export JAVA_HOME=${JAVA_HOME_PATH}" >> /etc/environment
    echo "export PATH=\$PATH:\$JAVA_HOME/bin" >> /etc/environment
    log "INFO" "JAVA_HOME configurado: ${JAVA_HOME_PATH}"
}

instalar_maven() {
    log "STEP" "Verificando Maven..."

    if command -v mvn &>/dev/null && mvn --version 2>&1 | grep -q "3\.9"; then
        log "INFO" "Maven 3.9.x já instalado."
        return 0
    fi

    local tmp_file="/tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    wget -q "${MAVEN_URL}" -O "${tmp_file}" >> "${LOG_FILE}" 2>&1

    if [[ ! -f "${tmp_file}" ]]; then
        log "ERROR" "Falha ao baixar Maven."
        exit 1
    fi

    mkdir -p "${MAVEN_INSTALL_DIR}"
    tar -xzf "${tmp_file}" -C "${MAVEN_INSTALL_DIR}" --strip-components=1 >> "${LOG_FILE}" 2>&1
    rm -f "${tmp_file}"

    ln -sf "${MAVEN_INSTALL_DIR}/bin/mvn" /usr/local/bin/mvn
    echo "export M2_HOME=${MAVEN_INSTALL_DIR}" >> /etc/environment
    echo "export PATH=\$PATH:${MAVEN_INSTALL_DIR}/bin" >> /etc/environment

    if mvn --version &>/dev/null; then
        log "INFO" "Maven ${MAVEN_VERSION} instalado!"
    else
        log "ERROR" "Falha ao instalar Maven."
        exit 1
    fi
}

instalar_sqlite() {
    log "STEP" "Verificando SQLite..."

    if command -v sqlite3 &>/dev/null; then
        log "INFO" "SQLite já instalado: $(sqlite3 --version)"
        return 0
    fi

    apt-get install -y sqlite3 >> "${LOG_FILE}" 2>&1

    if command -v sqlite3 &>/dev/null; then
        log "INFO" "SQLite3 instalado: $(sqlite3 --version)"
    else
        log "ERROR" "Falha ao instalar SQLite3."
        exit 1
    fi
}

instalar_android_sdk_tools() {
    log "STEP" "Instalando dependências para Android SDK..."

    apt-get install -y wget unzip curl git lib32stdc++6 lib32z1 >> "${LOG_FILE}" 2>&1

    log "INFO" "Dependências instaladas."
    log "WARN" "Para o Android SDK completo acesse: https://developer.android.com/studio"
}

configurar_env() {
    log "STEP" "Criando arquivo .env do projeto..."

    local env_file="${MAYA_HOME}/.env"

    cat > "${env_file}" <<EOF
APP_PORT=${APP_PORT}
JAR_NAME=${JAR_NAME}
MAYA_HOME=${MAYA_HOME}
JWT_SECRET=maya_dev_secret_change_in_production
JWT_EXPIRATION=86400000
DB_PATH=${MAYA_HOME}/db/mayarpg.db
LOG_DIR=${LOG_DIR}
API_BASE_URL=http://localhost:${APP_PORT}/api
SWAGGER_URL=http://localhost:${APP_PORT}/swagger-ui/index.html
EOF

    chmod 600 "${env_file}"
    log "INFO" ".env criado em ${env_file}"
    log "WARN" "Troque o JWT_SECRET antes de ir pra produção!"
}

verificar_instalacao() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}        RESUMO - MayaRPG Backend           ${NC}"
    echo -e "${BLUE}============================================${NC}"

    java -version &>/dev/null    && echo -e "  Java 21:   ${GREEN}OK${NC}"    || echo -e "  Java 21:   ${RED}FALHOU${NC}"
    mvn --version &>/dev/null    && echo -e "  Maven:     ${GREEN}OK${NC}"    || echo -e "  Maven:     ${RED}FALHOU${NC}"
    sqlite3 --version &>/dev/null && echo -e "  SQLite3:   ${GREEN}OK${NC}"  || echo -e "  SQLite3:   ${RED}FALHOU${NC}"

    echo ""
    echo -e "  Pasta do projeto: ${MAYA_HOME}"
    echo -e "  Log desta execução: ${LOG_FILE}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
    log "INFO" "Setup finalizado."
}

main() {
    mkdir -p "${LOG_DIR}" 2>/dev/null || {
        LOG_DIR="/tmp/maya-logs"
        LOG_FILE="${LOG_DIR}/setup_$(date +%Y%m%d_%H%M%S).log"
        mkdir -p "${LOG_DIR}"
    }

    echo ""
    echo -e "${CYAN}  MayaRPG Backend - Setup do Ambiente${NC}"
    echo ""

    check_root
    check_internet
    criar_diretorios
    instalar_java
    instalar_maven
    instalar_sqlite
    instalar_android_sdk_tools
    configurar_env
    verificar_instalacao
}

main "$@"
