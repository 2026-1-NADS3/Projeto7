#!/bin/bash
# =============================================================================
# Script: gerenciar_servico.sh
# Projeto: MayaRPG Backend
# Descrição: Gerencia o ciclo de vida do MayaRPG Backend (Spring Boot/Maven).
#             Permite iniciar, parar, reiniciar, verificar status e ativar o
#             modo watchdog (restart automático em caso de falha).
# Uso:
#   bash gerenciar_servico.sh start       → inicia o backend
#   bash gerenciar_servico.sh stop        → para o backend
#   bash gerenciar_servico.sh restart     → reinicia o backend
#   bash gerenciar_servico.sh status      → exibe status detalhado
#   bash gerenciar_servico.sh build       → compila o projeto com Maven
#   bash gerenciar_servico.sh watchdog    → monitor com restart automático
#   bash gerenciar_servico.sh logs        → exibe logs em tempo real
# =============================================================================

# ─────────────────────────── VARIÁVEIS DE AMBIENTE ──────────────────────────
export MAYA_HOME="${HOME}/mayarpg"
export PROJETO_DIR="${HOME}/MayaRpgBackend/mayarpg"   # Raiz do módulo Maven
export JAR_DIR="${PROJETO_DIR}/target"
export JAR_NAME="mayarpg-0.0.1-SNAPSHOT.jar"
export JAR_PATH="${JAR_DIR}/${JAR_NAME}"
export APP_PORT="8080"
export APP_LOG="${MAYA_HOME}/logs/app_$(date +%Y%m%d).log"
export PID_FILE="${MAYA_HOME}/mayarpg.pid"
export LOG_DIR="${MAYA_HOME}/logs"

# Configurações JVM e Spring
export JAVA_OPTS="-Xms256m -Xmx512m"
export SPRING_PROFILES_ACTIVE="dev"
export JWT_SECRET="${JWT_SECRET:-maya_dev_secret_change_in_production}"

# Watchdog
export WATCHDOG_INTERVALO=15    # segundos entre verificações
export WATCHDOG_MAX_FALHAS=3    # falhas consecutivas antes de restart

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
    mkdir -p "${LOG_DIR}"
    # Redirecionamento >> para o log da aplicação
    echo "${ts} [${level}] ${msg}" >> "${APP_LOG}"
    case "$level" in
        INFO)  echo -e "${GREEN}[INFO]${NC}  ${msg}" ;;
        WARN)  echo -e "${YELLOW}[WARN]${NC}  ${msg}" ;;
        ERROR) echo -e "${RED}[ERROR]${NC} ${msg}" ;;
        STEP)  echo -e "${CYAN}[STEP]${NC}  ${msg}" ;;
    esac
}

# Retorna PID do processo MayaRPG, ou vazio se não estiver rodando
# Pipe: ps -> grep -> grep -v -> awk
obter_pid() {
    ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}' | head -1
}

# Verifica se a aplicação está respondendo na porta 8080
app_respondendo() {
    curl -s --max-time 5 "http://localhost:${APP_PORT}/swagger-ui/index.html" &>/dev/null
}

# ─────────────────────────── COMPILAR PROJETO ───────────────────────────────
build() {
    log "STEP" "Compilando MayaRPG Backend com Maven..."

    if [[ ! -d "${PROJETO_DIR}" ]]; then
        log "ERROR" "Diretório do projeto não encontrado: ${PROJETO_DIR}"
        log "ERROR" "Ajuste a variável PROJETO_DIR no script."
        exit 1
    fi

    if ! command -v mvn &>/dev/null; then
        log "ERROR" "Maven não encontrado. Execute setup_ambiente.sh primeiro."
        exit 1
    fi

    cd "${PROJETO_DIR}" || exit 1

    log "INFO" "Executando: mvn clean package -DskipTests"

    # Pipe: mvn -> tee (exibe e salva no log simultaneamente)
    mvn clean package -DskipTests 2>&1 | tee -a "${APP_LOG}"

    if [[ -f "${JAR_PATH}" ]]; then
        log "INFO" "Build concluído: ${JAR_PATH}"
        log "INFO" "Tamanho do JAR: $(du -sh "${JAR_PATH}" | awk '{print $1}')"
        return 0
    else
        log "ERROR" "Build falhou. JAR não encontrado em: ${JAR_PATH}"
        return 1
    fi
}

# ─────────────────────────── INICIAR APLICAÇÃO ──────────────────────────────
start() {
    log "STEP" "Verificando se MayaRPG já está em execução..."

    local pid
    pid=$(obter_pid)

    if [[ -n "${pid}" ]]; then
        log "WARN" "MayaRPG já está rodando (PID: ${pid})."
        status
        return 0
    fi

    # Verificar se o JAR existe; se não, fazer build automaticamente
    if [[ ! -f "${JAR_PATH}" ]]; then
        log "WARN" "JAR não encontrado: ${JAR_PATH}"
        log "INFO" "Executando build antes de iniciar..."
        build || { log "ERROR" "Não foi possível compilar. Abortando."; exit 1; }
    fi

    log "STEP" "Iniciando MayaRPG Backend..."
    log "INFO" "JAR: ${JAR_PATH}"
    log "INFO" "Porta: ${APP_PORT} | Profile: ${SPRING_PROFILES_ACTIVE}"
    log "INFO" "JVM opts: ${JAVA_OPTS}"

    mkdir -p "${LOG_DIR}"

    # Inicia em background; redireciona stdout e stderr para o log da aplicação
    nohup java ${JAVA_OPTS} \
        -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
        -Dserver.port="${APP_PORT}" \
        -Djwt.secret="${JWT_SECRET}" \
        -jar "${JAR_PATH}" \
        >> "${APP_LOG}" 2>&1 &

    local new_pid=$!

    # Salva o PID em arquivo (redirecionamento >)
    echo "${new_pid}" > "${PID_FILE}"
    log "INFO" "Processo iniciado (PID: ${new_pid}). Aguardando inicialização..."

    # Aguarda a aplicação iniciar (máximo 60 segundos)
    local tentativas=0
    local max_tentativas=12  # 12 x 5s = 60s

    while (( tentativas < max_tentativas )); do
        sleep 5
        tentativas=$(( tentativas + 1 ))

        # Verifica se o processo ainda está vivo
        if ! kill -0 "${new_pid}" 2>/dev/null; then
            log "ERROR" "Processo encerrou inesperadamente. Verifique o log: ${APP_LOG}"
            rm -f "${PID_FILE}"
            exit 1
        fi

        echo -ne "  Aguardando (${tentativas}/${max_tentativas})... "

        # Verifica se a porta está respondendo (pipe: grep no log)
        if grep -q "Started MayarpgApplication" "${APP_LOG}" 2>/dev/null; then
            echo ""
            log "INFO" "MayaRPG Backend iniciado com sucesso!"
            log "INFO" "Swagger UI: http://localhost:${APP_PORT}/swagger-ui/index.html"
            log "INFO" "Auth API:   http://localhost:${APP_PORT}/api/auth"
            log "INFO" "Pacientes:  http://localhost:${APP_PORT}/api/admin/pacientes"
            return 0
        fi
        echo "ainda iniciando..."
    done

    log "WARN" "Timeout aguardando inicialização. Verifique: tail -f ${APP_LOG}"
}

# ─────────────────────────── PARAR APLICAÇÃO ────────────────────────────────
stop() {
    log "STEP" "Parando MayaRPG Backend..."

    local pid
    pid=$(obter_pid)

    if [[ -z "${pid}" ]]; then
        log "WARN" "MayaRPG não está em execução."
        rm -f "${PID_FILE}"
        return 0
    fi

    log "INFO" "Enviando SIGTERM para PID ${pid}..."
    kill -TERM "${pid}" 2>/dev/null

    # Aguarda encerramento gracioso (até 30 segundos)
    local contador=0
    while kill -0 "${pid}" 2>/dev/null && (( contador < 30 )); do
        sleep 1
        contador=$(( contador + 1 ))
        echo -ne "  Aguardando encerramento (${contador}s)...\r"
    done
    echo ""

    if kill -0 "${pid}" 2>/dev/null; then
        log "WARN" "Processo não encerrou. Enviando SIGKILL..."
        kill -KILL "${pid}" 2>/dev/null
        sleep 2
    fi

    if ! kill -0 "${pid}" 2>/dev/null; then
        log "INFO" "MayaRPG Backend parado com sucesso."
        rm -f "${PID_FILE}"
    else
        log "ERROR" "Não foi possível parar o processo ${pid}."
        exit 1
    fi
}

# ─────────────────────────── REINICIAR APLICAÇÃO ────────────────────────────
restart() {
    log "STEP" "Reiniciando MayaRPG Backend..."
    stop
    sleep 3
    start
}

# ─────────────────────────── STATUS DA APLICAÇÃO ────────────────────────────
status() {
    echo ""
    echo -e "${BLUE}════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}   STATUS — MayaRPG Backend                     ${NC}"
    echo -e "${BLUE}════════════════════════════════════════════════${NC}"

    local pid
    pid=$(obter_pid)

    if [[ -n "${pid}" ]]; then
        echo -e "  Status:      ${GREEN}● RODANDO${NC}"
        echo -e "  PID:         ${pid}"

        # Tempo de execução (pipe: ps -> awk)
        local uptime_app
        uptime_app=$(ps -p "${pid}" -o etime= 2>/dev/null | xargs)
        echo -e "  Uptime:      ${uptime_app}"

        # Recursos consumidos (pipe: ps -> awk)
        local cpu mem_kb mem_mb
        cpu=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $3}' | head -1)
        mem_kb=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $6}' | head -1)
        mem_mb=$(( ${mem_kb:-0} / 1024 ))
        echo -e "  CPU:         ${cpu}%"
        echo -e "  Memória:     ${mem_mb} MB"

        # Verificar health do endpoint
        echo -ne "  Porta ${APP_PORT}:    "
        if app_respondendo; then
            echo -e "${GREEN}Respondendo ✓${NC}"
        else
            echo -e "${YELLOW}Inicializando ou sem resposta${NC}"
        fi

        # Conexões ativas (pipe: ss -> grep -> wc)
        local conexoes
        conexoes=$(ss -tn 2>/dev/null | grep ":${APP_PORT}" | wc -l)
        echo -e "  Conexões:    ${conexoes} ativa(s)"

        echo ""
        echo -e "  Endpoints disponíveis:"
        echo -e "  → Swagger:   http://localhost:${APP_PORT}/swagger-ui/index.html"
        echo -e "  → Login:     POST http://localhost:${APP_PORT}/api/auth/login"
        echo -e "  → Register:  POST http://localhost:${APP_PORT}/api/auth/register"
        echo -e "  → Pacientes: GET  http://localhost:${APP_PORT}/api/admin/pacientes"
        echo -e "  → Ativos:    GET  http://localhost:${APP_PORT}/api/admin/pacientes/ativos"

    else
        echo -e "  Status:      ${RED}● PARADO${NC}"
        echo -e "  JAR:         ${JAR_PATH}"
        [[ -f "${JAR_PATH}" ]] && \
            echo -e "  JAR existe:  ${GREEN}Sim${NC}" || \
            echo -e "  JAR existe:  ${RED}Não (rode: bash gerenciar_servico.sh build)${NC}"
    fi

    echo ""
    echo -e "  Log ativo:   ${APP_LOG}"
    echo -e "  PID file:    ${PID_FILE}"
    echo -e "${BLUE}════════════════════════════════════════════════${NC}"
}

# ─────────────────────────── WATCHDOG (RESTART AUTOMÁTICO) ──────────────────
watchdog() {
    log "INFO" "Watchdog iniciado. Monitorando MayaRPG a cada ${WATCHDOG_INTERVALO}s..."
    log "INFO" "Pressione Ctrl+C para sair."

    local falhas_consecutivas=0

    while true; do
        local pid
        pid=$(obter_pid)

        if [[ -z "${pid}" ]]; then
            falhas_consecutivas=$(( falhas_consecutivas + 1 ))
            log "WARN" "MayaRPG não está rodando! Falha consecutiva: ${falhas_consecutivas}/${WATCHDOG_MAX_FALHAS}"

            if (( falhas_consecutivas >= WATCHDOG_MAX_FALHAS )); then
                log "ERROR" "Limite de falhas atingido. Executando restart automático..."
                start
                falhas_consecutivas=0
                log "INFO" "Restart automático concluído. Monitoramento continua..."
            fi
        else
            falhas_consecutivas=0

            # Verificar se está respondendo na porta (pipe: curl -> grep)
            if ! app_respondendo; then
                log "WARN" "Processo existe (PID: ${pid}) mas porta ${APP_PORT} não responde."
            else
                log "INFO" "MayaRPG OK (PID: ${pid}) — porta ${APP_PORT} respondendo."
            fi
        fi

        sleep "${WATCHDOG_INTERVALO}"
    done
}

# ─────────────────────────── EXIBIR LOGS ────────────────────────────────────
exibir_logs() {
    if [[ -f "${APP_LOG}" ]]; then
        log "INFO" "Exibindo logs em tempo real (Ctrl+C para sair):"
        echo -e "${YELLOW}Arquivo: ${APP_LOG}${NC}"
        echo ""
        # tail com pipe para colorização de níveis
        tail -f "${APP_LOG}" | while IFS= read -r linha; do
            if echo "$linha" | grep -q "ERROR"; then
                echo -e "${RED}${linha}${NC}"
            elif echo "$linha" | grep -q "WARN"; then
                echo -e "${YELLOW}${linha}${NC}"
            elif echo "$linha" | grep -q "INFO"; then
                echo -e "${GREEN}${linha}${NC}"
            else
                echo "$linha"
            fi
        done
    else
        log "WARN" "Arquivo de log não encontrado: ${APP_LOG}"
        log "INFO" "Inicie a aplicação primeiro: bash gerenciar_servico.sh start"
    fi
}

# ─────────────────────────── AJUDA ──────────────────────────────────────────
ajuda() {
    echo ""
    echo -e "${CYAN}MayaRPG Backend — Gerenciador de Serviço${NC}"
    echo ""
    echo "Uso: bash gerenciar_servico.sh [comando]"
    echo ""
    echo "Comandos:"
    echo "  start      Inicia o backend (compila se necessário)"
    echo "  stop       Para o backend graciosamente"
    echo "  restart    Para e reinicia o backend"
    echo "  status     Exibe status detalhado do processo e endpoints"
    echo "  build      Compila o projeto com Maven (mvn clean package)"
    echo "  watchdog   Ativa monitoramento com restart automático"
    echo "  logs       Exibe logs em tempo real (com colorização)"
    echo "  help       Exibe esta mensagem"
    echo ""
    echo "Variáveis de ambiente configuráveis:"
    echo "  PROJETO_DIR           Diretório raiz do projeto Maven"
    echo "  APP_PORT              Porta da aplicação (padrão: 8080)"
    echo "  SPRING_PROFILES_ACTIVE  Profile ativo (padrão: dev)"
    echo "  JWT_SECRET            Chave JWT (SEMPRE alterar em produção)"
    echo ""
}

# ─────────────────────────────── MAIN ───────────────────────────────────────
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
