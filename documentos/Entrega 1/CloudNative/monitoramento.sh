#!/bin/bash
# =============================================================================
# Script: monitoramento.sh
# Projeto: MayaRPG Backend
# Descrição: Coleta métricas do sistema e da aplicação MayaRPG (CPU, memória,
#             disco, status do processo Java, acessos aos endpoints da API) e
#             gera logs estruturados. Pode rodar continuamente ou pontualmente.
# Uso:
#   bash monitoramento.sh              → coleta única e exibe relatório
#   bash monitoramento.sh --watch      → coleta a cada 30s continuamente
#   bash monitoramento.sh --relatorio  → gera relatório em arquivo .txt
# =============================================================================

# ─────────────────────────── VARIÁVEIS DE AMBIENTE ──────────────────────────
export MAYA_HOME="${HOME}/mayarpg"
export LOG_DIR="${MAYA_HOME}/logs"
export METRICS_LOG="${LOG_DIR}/metrics_$(date +%Y%m%d).log"
export ALERT_LOG="${LOG_DIR}/alertas_$(date +%Y%m%d).log"
export RELATORIO_DIR="${MAYA_HOME}/logs/relatorios"
export JAR_NAME="mayarpg-0.0.1-SNAPSHOT.jar"
export APP_PORT="8080"
export DB_PATH="${MAYA_HOME}/db/mayarpg.db"

# Limites para alertas
export CPU_LIMITE=80       # % de CPU
export MEM_LIMITE=85       # % de memória
export DISCO_LIMITE=90     # % de uso de disco
export INTERVALO=30        # segundos entre coletas no modo --watch

# ──────────────────────────── CORES PARA OUTPUT ─────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# ─────────────────────────── FUNÇÕES UTILITÁRIAS ────────────────────────────
inicializar_dirs() {
    mkdir -p "${LOG_DIR}" "${RELATORIO_DIR}"
}

timestamp() {
    date '+%Y-%m-%d %H:%M:%S'
}

# Registra linha no log de métricas (redirecionamento >>)
registrar_metrica() {
    local categoria="$1"
    local chave="$2"
    local valor="$3"
    echo "$(timestamp) | ${categoria} | ${chave} | ${valor}" >> "${METRICS_LOG}"
}

# Registra alertas em arquivo separado (pipe + redirecionamento)
registrar_alerta() {
    local msg="$1"
    echo "$(timestamp) [ALERTA] ${msg}" | tee -a "${ALERT_LOG}"
}

# ─────────────────────────── MÉTRICAS DE CPU ────────────────────────────────
coletar_cpu() {
    # Usa pipe: top -> grep -> awk para extrair uso de CPU
    local cpu_idle
    cpu_idle=$(top -bn1 | grep "Cpu(s)" | awk '{print $8}' | sed 's/%id,//' | tr -d ' ')

    # Caso top retorne formato diferente (compatibilidade)
    if [[ -z "$cpu_idle" ]]; then
        cpu_idle=$(top -bn1 | grep -i "cpu" | head -1 | awk -F',' '{print $4}' | awk '{print $1}')
    fi

    local cpu_uso
    cpu_uso=$(echo "100 - ${cpu_idle:-0}" | bc 2>/dev/null || echo "N/A")

    registrar_metrica "CPU" "uso_percentual" "${cpu_uso}%"

    echo -e "${CYAN}── CPU ──────────────────────────────────────────────${NC}"
    echo -e "  Uso atual:       ${cpu_uso}%"

    # Alerta por pipe: verifica limite e redireciona alerta
    if [[ "$cpu_uso" != "N/A" ]] && (( $(echo "$cpu_uso > $CPU_LIMITE" | bc -l 2>/dev/null || echo 0) )); then
        echo -e "  ${RED}⚠ ALERTA: CPU acima de ${CPU_LIMITE}%!${NC}"
        registrar_alerta "CPU em ${cpu_uso}% — acima do limite de ${CPU_LIMITE}%"
    fi

    # Carga do sistema (load average) via pipe
    local load_avg
    load_avg=$(uptime | awk -F'load average:' '{print $2}' | xargs)
    echo -e "  Load Average:    ${load_avg}"
    registrar_metrica "CPU" "load_average" "${load_avg}"

    # Top 3 processos por CPU (pipe encadeado)
    echo -e "  Top processos:"
    ps aux --sort=-%cpu | awk 'NR==2,NR==4 {printf "    %-25s %s%%\n", $11, $3}' 2>/dev/null
}

# ─────────────────────────── MÉTRICAS DE MEMÓRIA ────────────────────────────
coletar_memoria() {
    echo -e "${CYAN}── MEMÓRIA ──────────────────────────────────────────${NC}"

    # Pipe: free -> grep -> awk para extrair valores
    local mem_total mem_usada mem_livre mem_percentual
    mem_total=$(free -m | grep "^Mem:" | awk '{print $2}')
    mem_usada=$(free -m | grep "^Mem:" | awk '{print $3}')
    mem_livre=$(free -m | grep "^Mem:" | awk '{print $4}')
    mem_percentual=$(awk "BEGIN {printf \"%.1f\", ($mem_usada / $mem_total) * 100}" 2>/dev/null)

    echo -e "  Total:           ${mem_total} MB"
    echo -e "  Em uso:          ${mem_usada} MB (${mem_percentual}%)"
    echo -e "  Disponível:      ${mem_livre} MB"

    registrar_metrica "MEM" "total_mb"   "${mem_total}"
    registrar_metrica "MEM" "usada_mb"   "${mem_usada}"
    registrar_metrica "MEM" "percentual" "${mem_percentual}%"

    # Alerta de memória
    if (( $(echo "${mem_percentual} > ${MEM_LIMITE}" | bc -l 2>/dev/null || echo 0) )); then
        echo -e "  ${RED}⚠ ALERTA: Memória acima de ${MEM_LIMITE}%!${NC}"
        registrar_alerta "Memória em ${mem_percentual}% — acima do limite de ${MEM_LIMITE}%"
    fi

    # Swap
    local swap_total swap_usada
    swap_total=$(free -m | grep "^Swap:" | awk '{print $2}')
    swap_usada=$(free -m | grep "^Swap:" | awk '{print $3}')
    echo -e "  Swap:            ${swap_usada}/${swap_total} MB"
    registrar_metrica "SWAP" "usada_mb" "${swap_usada}"
}

# ─────────────────────────── MÉTRICAS DE DISCO ──────────────────────────────
coletar_disco() {
    echo -e "${CYAN}── DISCO ────────────────────────────────────────────${NC}"

    # Pipe: df -> grep -> awk para cada partição relevante
    df -h | grep -vE "^(tmpfs|devtmpfs|udev)" | awk 'NR>1 {
        gsub(/%/, "", $5)
        printf "  %-25s %6s / %6s  (%s%%)\n", $6, $3, $2, $5
    }'

    # Checar disco raiz especificamente para alertas
    local disco_raiz_uso
    disco_raiz_uso=$(df -h / | awk 'NR==2 {gsub(/%/,"",$5); print $5}')
    registrar_metrica "DISCO" "raiz_percentual" "${disco_raiz_uso}%"

    if (( disco_raiz_uso > DISCO_LIMITE )); then
        echo -e "  ${RED}⚠ ALERTA: Disco raiz acima de ${DISCO_LIMITE}%!${NC}"
        registrar_alerta "Disco raiz em ${disco_raiz_uso}% — acima do limite de ${DISCO_LIMITE}%"
    fi

    # Tamanho do banco SQLite do MayaRPG
    if [[ -f "${DB_PATH}" ]]; then
        local db_size
        db_size=$(du -sh "${DB_PATH}" | awk '{print $1}')
        echo -e "  Banco mayarpg.db: ${db_size}"
        registrar_metrica "DISCO" "db_size" "${db_size}"
    else
        echo -e "  Banco mayarpg.db: não encontrado em ${DB_PATH}"
    fi

    # Tamanho total dos logs
    if [[ -d "${LOG_DIR}" ]]; then
        local logs_size
        logs_size=$(du -sh "${LOG_DIR}" 2>/dev/null | awk '{print $1}')
        echo -e "  Tamanho dos logs: ${logs_size}"
        registrar_metrica "DISCO" "logs_size" "${logs_size}"
    fi
}

# ─────────────────────────── STATUS DA APLICAÇÃO ────────────────────────────
coletar_status_app() {
    echo -e "${CYAN}── MAYARPG BACKEND ──────────────────────────────────${NC}"

    # Verifica se o processo Java do MayaRPG está rodando
    # Pipe: ps -> grep -> grep -v -> wc
    local pid_app
    pid_app=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}' | head -1)

    if [[ -n "${pid_app}" ]]; then
        echo -e "  Status:          ${GREEN}● RODANDO${NC} (PID: ${pid_app})"
        registrar_metrica "APP" "status" "RODANDO"
        registrar_metrica "APP" "pid"    "${pid_app}"

        # Memória consumida pelo processo Java (pipe: ps -> awk)
        local app_mem app_cpu
        app_mem=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $6}' | head -1)
        app_cpu=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $3}' | head -1)
        local app_mem_mb=$(( ${app_mem:-0} / 1024 ))

        echo -e "  CPU (processo):  ${app_cpu}%"
        echo -e "  Memória:         ${app_mem_mb} MB"
        registrar_metrica "APP" "cpu_percentual" "${app_cpu}%"
        registrar_metrica "APP" "memoria_mb"     "${app_mem_mb}"

        # Tempo de execução (pipe: ps -> awk)
        local uptime_app
        uptime_app=$(ps -p "${pid_app}" -o etime= 2>/dev/null | xargs)
        echo -e "  Uptime:          ${uptime_app}"
        registrar_metrica "APP" "uptime" "${uptime_app}"

    else
        echo -e "  Status:          ${RED}● PARADO${NC}"
        registrar_metrica "APP" "status" "PARADO"
        registrar_alerta "MayaRPG Backend não está em execução!"
    fi

    # Verificar se a porta 8080 está respondendo (pipe: curl -> grep)
    echo -ne "  Porta ${APP_PORT}:        "
    if curl -s --max-time 3 "http://localhost:${APP_PORT}/actuator/health" 2>/dev/null | grep -q "UP"; then
        echo -e "${GREEN}Respondendo (health: UP)${NC}"
        registrar_metrica "APP" "health" "UP"
    elif curl -s --max-time 3 "http://localhost:${APP_PORT}/swagger-ui/index.html" &>/dev/null; then
        echo -e "${GREEN}Respondendo (Swagger OK)${NC}"
        registrar_metrica "APP" "health" "RESPONDENDO"
    else
        echo -e "${YELLOW}Não responde (app pode estar iniciando ou parado)${NC}"
        registrar_metrica "APP" "health" "SEM_RESPOSTA"
    fi

    # Contar conexões ativas na porta 8080 (pipe: ss -> grep -> wc)
    local conexoes
    conexoes=$(ss -tn 2>/dev/null | grep ":${APP_PORT}" | wc -l)
    echo -e "  Conexões ativas: ${conexoes}"
    registrar_metrica "APP" "conexoes_ativas" "${conexoes}"
}

# ─────────────────────────── ANÁLISE DE LOGS DA APLICAÇÃO ───────────────────
analisar_logs_app() {
    echo -e "${CYAN}── ANÁLISE DE LOGS DA APLICAÇÃO ─────────────────────${NC}"

    # Localizar o log do Spring Boot (pode estar em locais diferentes)
    local app_log=""
    for caminho in \
        "${MAYA_HOME}/logs/spring.log" \
        "${MAYA_HOME}/mayarpg.log" \
        "./logs/spring.log" \
        "/tmp/mayarpg.log"; do
        if [[ -f "$caminho" ]]; then
            app_log="$caminho"
            break
        fi
    done

    if [[ -z "$app_log" ]]; then
        echo -e "  Log do Spring Boot não encontrado."
        echo -e "  Dica: Configure logging.file.name no application.properties"
        return
    fi

    echo -e "  Arquivo de log: ${app_log}"

    # Contar erros nas últimas 100 linhas (pipe encadeado: tail -> grep -> wc)
    local erros warnings logins cadastros
    erros=$(tail -n 100 "${app_log}" | grep -i "ERROR" | wc -l)
    warnings=$(tail -n 100 "${app_log}" | grep -i "WARN" | wc -l)

    # Acessos aos endpoints do MayaRPG nas últimas 100 linhas
    logins=$(tail -n 100 "${app_log}" | grep -i "POST /api/auth/login" | wc -l)
    cadastros=$(tail -n 100 "${app_log}" | grep -i "POST /api/auth/register" | wc -l)

    echo -e "  Últimas 100 linhas:"
    echo -e "    ERRORs:             ${erros}"
    echo -e "    WARNINGs:           ${warnings}"
    echo -e "    Logins (/auth/login):    ${logins}"
    echo -e "    Cadastros (/register):   ${cadastros}"

    registrar_metrica "LOG" "erros_100lin"    "${erros}"
    registrar_metrica "LOG" "warnings_100lin" "${warnings}"
    registrar_metrica "LOG" "logins"          "${logins}"
    registrar_metrica "LOG" "cadastros"       "${cadastros}"

    # Mostrar últimos 5 erros (pipe: grep -> tail)
    if (( erros > 0 )); then
        echo -e "  ${RED}Últimos erros:${NC}"
        grep -i "ERROR" "${app_log}" | tail -n 5 | while IFS= read -r linha; do
            echo -e "    ${RED}→${NC} ${linha:0:100}"
        done
    fi
}

# ─────────────────────────── GERAR RELATÓRIO ────────────────────────────────
gerar_relatorio() {
    local relatorio="${RELATORIO_DIR}/relatorio_$(date +%Y%m%d_%H%M%S).txt"

    # Redireciona toda a saída para o arquivo de relatório (>)
    {
        echo "=========================================================="
        echo "  RELATÓRIO DE MONITORAMENTO — MAYARPG BACKEND"
        echo "  Gerado em: $(timestamp)"
        echo "  Hostname:  $(hostname)"
        echo "=========================================================="
        echo ""
        echo "── SISTEMA ──"
        uname -a
        echo "Uptime: $(uptime)"
        echo ""

        echo "── CPU ──"
        top -bn1 | head -5
        echo ""

        echo "── MEMÓRIA ──"
        free -h
        echo ""

        echo "── DISCO ──"
        df -h
        echo ""

        echo "── PROCESSO MAYARPG ──"
        ps aux | grep "${JAR_NAME}" | grep -v grep || echo "Aplicação não está em execução"
        echo ""

        echo "── CONEXÕES DE REDE ──"
        ss -tn | grep ":${APP_PORT}" || echo "Nenhuma conexão ativa na porta ${APP_PORT}"
        echo ""

        echo "── ÚLTIMAS MÉTRICAS ──"
        tail -n 30 "${METRICS_LOG}" 2>/dev/null || echo "Sem métricas registradas ainda"
        echo ""

        echo "── ALERTAS RECENTES ──"
        tail -n 20 "${ALERT_LOG}" 2>/dev/null || echo "Sem alertas registrados"
        echo ""
        echo "=========================================================="
        echo "  FIM DO RELATÓRIO"
        echo "=========================================================="
    } > "${relatorio}"

    echo -e "${GREEN}[INFO]${NC} Relatório salvo em: ${relatorio}"
}

# ──────────────────────────── EXIBIR CABEÇALHO ──────────────────────────────
exibir_cabecalho() {
    clear
    echo -e "${MAGENTA}╔══════════════════════════════════════════════════════╗${NC}"
    echo -e "${MAGENTA}║       MayaRPG Backend — Monitor do Sistema           ║${NC}"
    echo -e "${MAGENTA}║       $(timestamp)                    ║${NC}"
    echo -e "${MAGENTA}╚══════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# ─────────────────────────────── MAIN ───────────────────────────────────────
main() {
    inicializar_dirs

    case "${1:-}" in
        --watch)
            echo -e "${GREEN}[INFO]${NC} Modo contínuo ativado. Atualizando a cada ${INTERVALO}s. (Ctrl+C para sair)"
            while true; do
                exibir_cabecalho
                coletar_cpu
                echo ""
                coletar_memoria
                echo ""
                coletar_disco
                echo ""
                coletar_status_app
                echo ""
                analisar_logs_app
                echo ""
                echo -e "${YELLOW}[Próxima atualização em ${INTERVALO}s — Ctrl+C para sair]${NC}"
                sleep "${INTERVALO}"
            done
            ;;
        --relatorio)
            exibir_cabecalho
            coletar_cpu
            coletar_memoria
            coletar_disco
            coletar_status_app
            analisar_logs_app
            gerar_relatorio
            ;;
        *)
            exibir_cabecalho
            coletar_cpu
            echo ""
            coletar_memoria
            echo ""
            coletar_disco
            echo ""
            coletar_status_app
            echo ""
            analisar_logs_app
            echo ""
            echo -e "${BLUE}Métricas salvas em: ${METRICS_LOG}${NC}"
            ;;
    esac
}

main "$@"
