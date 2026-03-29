#!/bin/bash

MAYA_HOME="${HOME}/mayarpg"
LOG_DIR="${MAYA_HOME}/logs"
METRICS_LOG="${LOG_DIR}/metrics_$(date +%Y%m%d).log"
ALERT_LOG="${LOG_DIR}/alertas_$(date +%Y%m%d).log"
RELATORIO_DIR="${LOG_DIR}/relatorios"
JAR_NAME="mayarpg-0.0.1-SNAPSHOT.jar"
APP_PORT="8080"
DB_PATH="${MAYA_HOME}/db/mayarpg.db"

CPU_LIMITE=80
MEM_LIMITE=85
DISCO_LIMITE=90
INTERVALO=30

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

inicializar_dirs() {
    mkdir -p "${LOG_DIR}" "${RELATORIO_DIR}"
}

timestamp() {
    date '+%Y-%m-%d %H:%M:%S'
}

registrar_metrica() {
    echo "$(timestamp) | $1 | $2 | $3" >> "${METRICS_LOG}"
}

registrar_alerta() {
    echo "$(timestamp) [ALERTA] $1" | tee -a "${ALERT_LOG}"
}

coletar_cpu() {
    local cpu_idle
    cpu_idle=$(top -bn1 | grep "Cpu(s)" | awk '{print $8}' | sed 's/%id,//' | tr -d ' ')

    if [[ -z "$cpu_idle" ]]; then
        cpu_idle=$(top -bn1 | grep -i "cpu" | head -1 | awk -F',' '{print $4}' | awk '{print $1}')
    fi

    local cpu_uso
    cpu_uso=$(echo "100 - ${cpu_idle:-0}" | bc 2>/dev/null || echo "N/A")

    registrar_metrica "CPU" "uso_percentual" "${cpu_uso}%"

    echo -e "${CYAN}-- CPU --${NC}"
    echo -e "  Uso atual:    ${cpu_uso}%"

    if [[ "$cpu_uso" != "N/A" ]] && (( $(echo "$cpu_uso > $CPU_LIMITE" | bc -l 2>/dev/null || echo 0) )); then
        echo -e "  ${RED}ALERTA: CPU acima de ${CPU_LIMITE}%${NC}"
        registrar_alerta "CPU em ${cpu_uso}% - acima de ${CPU_LIMITE}%"
    fi

    local load_avg
    load_avg=$(uptime | awk -F'load average:' '{print $2}' | xargs)
    echo -e "  Load Average: ${load_avg}"
    registrar_metrica "CPU" "load_average" "${load_avg}"

    echo -e "  Top processos:"
    ps aux --sort=-%cpu | awk 'NR==2,NR==4 {printf "    %-25s %s%%\n", $11, $3}' 2>/dev/null
}

coletar_memoria() {
    echo -e "${CYAN}-- MEMORIA --${NC}"

    local mem_total mem_usada mem_livre mem_percentual
    mem_total=$(free -m | grep "^Mem:" | awk '{print $2}')
    mem_usada=$(free -m | grep "^Mem:" | awk '{print $3}')
    mem_livre=$(free -m | grep "^Mem:" | awk '{print $4}')
    mem_percentual=$(awk "BEGIN {printf \"%.1f\", ($mem_usada / $mem_total) * 100}" 2>/dev/null)

    echo -e "  Total:        ${mem_total} MB"
    echo -e "  Em uso:       ${mem_usada} MB (${mem_percentual}%)"
    echo -e "  Disponivel:   ${mem_livre} MB"

    registrar_metrica "MEM" "total_mb"   "${mem_total}"
    registrar_metrica "MEM" "usada_mb"   "${mem_usada}"
    registrar_metrica "MEM" "percentual" "${mem_percentual}%"

    if (( $(echo "${mem_percentual} > ${MEM_LIMITE}" | bc -l 2>/dev/null || echo 0) )); then
        echo -e "  ${RED}ALERTA: Memória acima de ${MEM_LIMITE}%${NC}"
        registrar_alerta "Memória em ${mem_percentual}% - acima de ${MEM_LIMITE}%"
    fi

    local swap_total swap_usada
    swap_total=$(free -m | grep "^Swap:" | awk '{print $2}')
    swap_usada=$(free -m | grep "^Swap:" | awk '{print $3}')
    echo -e "  Swap:         ${swap_usada}/${swap_total} MB"
}

coletar_disco() {
    echo -e "${CYAN}-- DISCO --${NC}"

    df -h | grep -vE "^(tmpfs|devtmpfs|udev)" | awk 'NR>1 {
        gsub(/%/, "", $5)
        printf "  %-25s %6s / %6s  (%s%%)\n", $6, $3, $2, $5
    }'

    local disco_raiz_uso
    disco_raiz_uso=$(df -h / | awk 'NR==2 {gsub(/%/,"",$5); print $5}')
    registrar_metrica "DISCO" "raiz_percentual" "${disco_raiz_uso}%"

    if (( disco_raiz_uso > DISCO_LIMITE )); then
        echo -e "  ${RED}ALERTA: Disco raiz acima de ${DISCO_LIMITE}%${NC}"
        registrar_alerta "Disco raiz em ${disco_raiz_uso}% - acima de ${DISCO_LIMITE}%"
    fi

    if [[ -f "${DB_PATH}" ]]; then
        echo -e "  mayarpg.db:   $(du -sh "${DB_PATH}" | awk '{print $1}')"
    fi

    if [[ -d "${LOG_DIR}" ]]; then
        echo -e "  Logs:         $(du -sh "${LOG_DIR}" 2>/dev/null | awk '{print $1}')"
    fi
}

coletar_status_app() {
    echo -e "${CYAN}-- MAYARPG BACKEND --${NC}"

    local pid_app
    pid_app=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}' | head -1)

    if [[ -n "${pid_app}" ]]; then
        echo -e "  Status:       ${GREEN}RODANDO${NC} (PID: ${pid_app})"
        registrar_metrica "APP" "status" "RODANDO"
        registrar_metrica "APP" "pid"    "${pid_app}"

        local app_mem app_cpu
        app_mem=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $6}' | head -1)
        app_cpu=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $3}' | head -1)
        local app_mem_mb=$(( ${app_mem:-0} / 1024 ))

        echo -e "  CPU:          ${app_cpu}%"
        echo -e "  Memória:      ${app_mem_mb} MB"

        local uptime_app
        uptime_app=$(ps -p "${pid_app}" -o etime= 2>/dev/null | xargs)
        echo -e "  Uptime:       ${uptime_app}"

        registrar_metrica "APP" "cpu_percentual" "${app_cpu}%"
        registrar_metrica "APP" "memoria_mb"     "${app_mem_mb}"
        registrar_metrica "APP" "uptime"         "${uptime_app}"
    else
        echo -e "  Status:       ${RED}PARADO${NC}"
        registrar_metrica "APP" "status" "PARADO"
        registrar_alerta "MayaRPG Backend nao esta em execucao"
    fi

    echo -ne "  Porta ${APP_PORT}:       "
    if curl -s --max-time 3 "http://localhost:${APP_PORT}/swagger-ui/index.html" &>/dev/null; then
        echo -e "${GREEN}Respondendo${NC}"
        registrar_metrica "APP" "health" "UP"
    else
        echo -e "${YELLOW}Sem resposta${NC}"
        registrar_metrica "APP" "health" "SEM_RESPOSTA"
    fi

    local conexoes
    conexoes=$(ss -tn 2>/dev/null | grep ":${APP_PORT}" | wc -l)
    echo -e "  Conexões:     ${conexoes}"
    registrar_metrica "APP" "conexoes" "${conexoes}"
}

analisar_logs_app() {
    echo -e "${CYAN}-- LOGS DA APLICACAO --${NC}"

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
        echo -e "  Log do Spring Boot nao encontrado."
        echo -e "  Configure logging.file.name no application.properties"
        return
    fi

    local erros warnings logins cadastros
    erros=$(tail -n 100 "${app_log}" | grep -i "ERROR" | wc -l)
    warnings=$(tail -n 100 "${app_log}" | grep -i "WARN" | wc -l)
    logins=$(tail -n 100 "${app_log}" | grep -i "POST /api/auth/login" | wc -l)
    cadastros=$(tail -n 100 "${app_log}" | grep -i "POST /api/auth/register" | wc -l)

    echo -e "  ERRORs (ultimas 100 linhas): ${erros}"
    echo -e "  WARNINGs:                    ${warnings}"
    echo -e "  Logins /auth/login:          ${logins}"
    echo -e "  Cadastros /register:         ${cadastros}"

    registrar_metrica "LOG" "erros"    "${erros}"
    registrar_metrica "LOG" "warnings" "${warnings}"
    registrar_metrica "LOG" "logins"   "${logins}"
    registrar_metrica "LOG" "cadastros" "${cadastros}"

    if (( erros > 0 )); then
        echo -e "  ${RED}Ultimos erros:${NC}"
        grep -i "ERROR" "${app_log}" | tail -n 3 | while IFS= read -r linha; do
            echo -e "    -> ${linha:0:100}"
        done
    fi
}

gerar_relatorio() {
    local relatorio="${RELATORIO_DIR}/relatorio_$(date +%Y%m%d_%H%M%S).txt"

    {
        echo "================================================"
        echo "  RELATORIO DE MONITORAMENTO - MAYARPG BACKEND"
        echo "  Gerado em: $(timestamp)"
        echo "  Hostname:  $(hostname)"
        echo "================================================"
        echo ""
        echo "-- SISTEMA --"
        uname -a
        echo "Uptime: $(uptime)"
        echo ""
        echo "-- CPU --"
        top -bn1 | head -5
        echo ""
        echo "-- MEMORIA --"
        free -h
        echo ""
        echo "-- DISCO --"
        df -h
        echo ""
        echo "-- PROCESSO MAYARPG --"
        ps aux | grep "${JAR_NAME}" | grep -v grep || echo "Aplicacao nao esta rodando"
        echo ""
        echo "-- CONEXOES NA PORTA ${APP_PORT} --"
        ss -tn | grep ":${APP_PORT}" || echo "Nenhuma conexao ativa"
        echo ""
        echo "-- ULTIMAS METRICAS --"
        tail -n 30 "${METRICS_LOG}" 2>/dev/null || echo "Sem metricas ainda"
        echo ""
        echo "-- ALERTAS RECENTES --"
        tail -n 20 "${ALERT_LOG}" 2>/dev/null || echo "Sem alertas"
        echo "================================================"
    } > "${relatorio}"

    echo -e "${GREEN}[INFO]${NC} Relatorio salvo em: ${relatorio}"
}

main() {
    inicializar_dirs

    case "${1:-}" in
        --watch)
            echo -e "${GREEN}Modo continuo ativado. Atualizando a cada ${INTERVALO}s (Ctrl+C para sair)${NC}"
            while true; do
                clear
                echo -e "${MAGENTA}  MayaRPG Monitor - $(timestamp)${NC}"
                echo ""
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
                echo -e "${YELLOW}Proxima atualizacao em ${INTERVALO}s...${NC}"
                sleep "${INTERVALO}"
            done
            ;;
        --relatorio)
            coletar_cpu
            coletar_memoria
            coletar_disco
            coletar_status_app
            analisar_logs_app
            gerar_relatorio
            ;;
        *)
            echo ""
            echo -e "${MAGENTA}  MayaRPG Monitor - $(timestamp)${NC}"
            echo ""
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
            echo -e "${BLUE}Metricas salvas em: ${METRICS_LOG}${NC}"
            ;;
    esac
}

main "$@"
