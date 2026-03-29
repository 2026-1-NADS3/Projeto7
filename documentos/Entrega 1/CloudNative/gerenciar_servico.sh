#!/bin/bash

MAYA_HOME="${HOME}/mayarpg"
PROJETO_DIR="${HOME}/MayaRpgBackend/mayarpg"
JAR_DIR="${PROJETO_DIR}/target"
JAR_NAME="mayarpg-0.0.1-SNAPSHOT.jar"
JAR_PATH="${JAR_DIR}/${JAR_NAME}"
APP_PORT="8080"
APP_LOG="${MAYA_HOME}/logs/app_$(date +%Y%m%d).log"
PID_FILE="${MAYA_HOME}/mayarpg.pid"
LOG_DIR="${MAYA_HOME}/logs"

JAVA_OPTS="-Xms256m -Xmx512m"
SPRING_PROFILES_ACTIVE="dev"
JWT_SECRET="${JWT_SECRET:-maya_dev_secret_change_in_production}"

WATCHDOG_INTERVALO=15
WATCHDOG_MAX_FALHAS=3

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log() {
    local level="$1"
    local msg="$2"
    mkdir -p "${LOG_DIR}"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [${level}] ${msg}" >> "${APP_LOG}"
    case "$level" in
        INFO)  echo -e "${GREEN}[INFO]${NC}  ${msg}" ;;
        WARN)  echo -e "${YELLOW}[WARN]${NC}  ${msg}" ;;
        ERROR) echo -e "${RED}[ERROR]${NC} ${msg}" ;;
        STEP)  echo -e "${CYAN}[STEP]${NC}  ${msg}" ;;
    esac
}

obter_pid() {
    ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}' | head -1
}

app_respondendo() {
    curl -s --max-time 5 "http://localhost:${APP_PORT}/swagger-ui/index.html" &>/dev/null
}

build() {
    log "STEP" "Compilando com Maven..."

    if [[ ! -d "${PROJETO_DIR}" ]]; then
        log "ERROR" "Diretório do projeto não encontrado: ${PROJETO_DIR}"
        exit 1
    fi

    if ! command -v mvn &>/dev/null; then
        log "ERROR" "Maven não encontrado. Rode setup_ambiente.sh primeiro."
        exit 1
    fi

    cd "${PROJETO_DIR}" || exit 1
    mvn clean package -DskipTests 2>&1 | tee -a "${APP_LOG}"

    if [[ -f "${JAR_PATH}" ]]; then
        log "INFO" "Build concluído: ${JAR_PATH}"
        return 0
    else
        log "ERROR" "Build falhou. JAR não encontrado."
        return 1
    fi
}

start() {
    local pid
    pid=$(obter_pid)

    if [[ -n "${pid}" ]]; then
        log "WARN" "MayaRPG já está rodando (PID: ${pid})."
        status
        return 0
    fi

    if [[ ! -f "${JAR_PATH}" ]]; then
        log "WARN" "JAR não encontrado. Compilando antes de iniciar..."
        build || { log "ERROR" "Não foi possível compilar."; exit 1; }
    fi

    log "STEP" "Iniciando MayaRPG Backend..."
    mkdir -p "${LOG_DIR}"

    nohup java ${JAVA_OPTS} \
        -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
        -Dserver.port="${APP_PORT}" \
        -Djwt.secret="${JWT_SECRET}" \
        -jar "${JAR_PATH}" \
        >> "${APP_LOG}" 2>&1 &

    local new_pid=$!
    echo "${new_pid}" > "${PID_FILE}"
    log "INFO" "Processo iniciado (PID: ${new_pid}). Aguardando..."

    local tentativas=0
    while (( tentativas < 12 )); do
        sleep 5
        tentativas=$(( tentativas + 1 ))

        if ! kill -0 "${new_pid}" 2>/dev/null; then
            log "ERROR" "Processo encerrou inesperadamente. Veja: ${APP_LOG}"
            rm -f "${PID_FILE}"
            exit 1
        fi

        echo -ne "  Aguardando inicializacao (${tentativas}/12)...\r"

        if grep -q "Started MayarpgApplication" "${APP_LOG}" 2>/dev/null; then
            echo ""
            log "INFO" "MayaRPG Backend iniciado!"
            log "INFO" "Swagger: http://localhost:${APP_PORT}/swagger-ui/index.html"
            return 0
        fi
    done

    log "WARN" "Timeout. Verifique: tail -f ${APP_LOG}"
}

stop() {
    log "STEP" "Parando MayaRPG Backend..."

    local pid
    pid=$(obter_pid)

    if [[ -z "${pid}" ]]; then
        log "WARN" "MayaRPG não está em execução."
        rm -f "${PID_FILE}"
        return 0
    fi

    kill -TERM "${pid}" 2>/dev/null

    local contador=0
    while kill -0 "${pid}" 2>/dev/null && (( contador < 30 )); do
        sleep 1
        contador=$(( contador + 1 ))
        echo -ne "  Aguardando encerramento (${contador}s)...\r"
    done
    echo ""

    if kill -0 "${pid}" 2>/dev/null; then
        log "WARN" "Forçando encerramento (SIGKILL)..."
        kill -KILL "${pid}" 2>/dev/null
        sleep 2
    fi

    if ! kill -0 "${pid}" 2>/dev/null; then
        log "INFO" "Aplicação parada."
        rm -f "${PID_FILE}"
    else
        log "ERROR" "Não foi possível parar o processo ${pid}."
        exit 1
    fi
}

restart() {
    log "STEP" "Reiniciando MayaRPG Backend..."
    stop
    sleep 3
    start
}

status() {
    echo ""
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}   STATUS - MayaRPG Backend                    ${NC}"
    echo -e "${BLUE}================================================${NC}"

    local pid
    pid=$(obter_pid)

    if [[ -n "${pid}" ]]; then
        echo -e "  Status:     ${GREEN}RODANDO${NC}"
        echo -e "  PID:        ${pid}"
        echo -e "  Uptime:     $(ps -p "${pid}" -o etime= 2>/dev/null | xargs)"

        local cpu mem_kb
        cpu=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $3}' | head -1)
        mem_kb=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $6}' | head -1)
        echo -e "  CPU:        ${cpu}%"
        echo -e "  Memoria:    $(( ${mem_kb:-0} / 1024 )) MB"

        echo -ne "  Porta ${APP_PORT}:  "
        app_respondendo && echo -e "${GREEN}Respondendo${NC}" || echo -e "${YELLOW}Inicializando...${NC}"

        echo -e "  Conexoes:   $(ss -tn 2>/dev/null | grep ":${APP_PORT}" | wc -l)"
        echo ""
        echo -e "  Endpoints:"
        echo -e "  -> Swagger:   http://localhost:${APP_PORT}/swagger-ui/index.html"
        echo -e "  -> Login:     POST http://localhost:${APP_PORT}/api/auth/login"
        echo -e "  -> Register:  POST http://localhost:${APP_PORT}/api/auth/register"
        echo -e "  -> Pacientes: GET  http://localhost:${APP_PORT}/api/admin/pacientes"
        echo -e "  -> Ativos:    GET  http://localhost:${APP_PORT}/api/admin/pacientes/ativos"
    else
        echo -e "  Status:     ${RED}PARADO${NC}"
        [[ -f "${JAR_PATH}" ]] \
            && echo -e "  JAR:        ${GREEN}Encontrado${NC}" \
            || echo -e "  JAR:        ${RED}Nao encontrado (rode: bash gerenciar_servico.sh build)${NC}"
    fi

    echo ""
    echo -e "  Log: ${APP_LOG}"
    echo -e "${BLUE}================================================${NC}"
}

watchdog() {
    log "INFO" "Watchdog ativado. Verificando a cada ${WATCHDOG_INTERVALO}s (Ctrl+C para sair)"

    local falhas=0

    while true; do
        local pid
        pid=$(obter_pid)

        if [[ -z "${pid}" ]]; then
            falhas=$(( falhas + 1 ))
            log "WARN" "Aplicação parada! Falha ${falhas}/${WATCHDOG_MAX_FALHAS}"

            if (( falhas >= WATCHDOG_MAX_FALHAS )); then
                log "ERROR" "Limite atingido. Reiniciando automaticamente..."
                start
                falhas=0
            fi
        else
            falhas=0
            log "INFO" "OK (PID: ${pid})"
        fi

        sleep "${WATCHDOG_INTERVALO}"
    done
}

exibir_logs() {
    if [[ -f "${APP_LOG}" ]]; then
        echo -e "${YELLOW}Log: ${APP_LOG} (Ctrl+C para sair)${NC}"
        echo ""
        tail -f "${APP_LOG}" | while IFS= read -r linha; do
            if echo "$linha" | grep -q "ERROR"; then
                echo -e "${RED}${linha}${NC}"
            elif echo "$linha" | grep -q "WARN"; then
                echo -e "${YELLOW}${linha}${NC}"
            else
                echo "$linha"
            fi
        done
    else
        log "WARN" "Log não encontrado: ${APP_LOG}"
        log "INFO" "Inicie a aplicação primeiro: bash gerenciar_servico.sh start"
    fi
}

ajuda() {
    echo ""
    echo -e "${CYAN}MayaRPG Backend - Gerenciador de Servico${NC}"
    echo ""
    echo "Uso: bash gerenciar_servico.sh [comando]"
    echo ""
    echo "  start      Inicia o backend"
    echo "  stop       Para o backend"
    echo "  restart    Reinicia o backend"
    echo "  status     Mostra status e endpoints"
    echo "  build      Compila com Maven"
    echo "  watchdog   Monitoramento com restart automatico"
    echo "  logs       Logs em tempo real"
    echo "  help       Mostra esta ajuda"
    echo ""
}

main() {
    mkdir -p "${LOG_DIR}"

    case "${1:-help}" in
        start)    start ;;
        stop)     stop ;;
        restart)  restart ;;
        status)   status ;;
        build)    build ;;
        watchdog) watchdog ;;
        logs)     exibir_logs ;;
        help|--help|-h) ajuda ;;
        *)
            echo -e "${RED}[ERROR]${NC} Comando desconhecido: $1"
            ajuda
            exit 1
            ;;
    esac
}

main "$@"
