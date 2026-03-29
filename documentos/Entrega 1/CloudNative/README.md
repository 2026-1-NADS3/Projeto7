# MayaRPG Backend — Scripts de Infraestrutura e Automação Linux

Scripts Shell/Bash para automação do ambiente de desenvolvimento, monitoramento, backup e gerenciamento de processos do **MayaRPG Backend** (Spring Boot 3.5 + Java 21 + SQLite + JWT).

---

## Estrutura dos Scripts

```
maya-scripts/
├── setup_ambiente.sh        # Instalação de dependências e configuração do ambiente
├── monitoramento.sh         # Coleta de métricas e geração de logs
├── backup.sh                # Backup do código-fonte e banco de dados
├── gerenciar_servico.sh     # Gerenciamento do processo backend (start/stop/watchdog)
└── README.md                # Este arquivo
```

---

## Pré-requisitos

- Sistema operacional Linux (Ubuntu/Debian recomendado)
- Acesso `sudo` para o script de setup
- Conexão com a internet para instalação de dependências

---

## Script 1 — `setup_ambiente.sh`

**Objetivo:** Automatiza a instalação de todas as dependências necessárias para rodar o MayaRPG Backend.

### O que instala e configura:
- **Java 21** (OpenJDK) — runtime e compilador
- **Maven 3.9.6** — ferramenta de build do projeto
- **SQLite3** — cliente de linha de comando para o banco `mayarpg.db`
- **Ferramentas base para Android SDK** — dependências de sistema
- Arquivo `.env` com variáveis de ambiente do projeto

### Uso:

```bash
sudo bash setup_ambiente.sh
```

### Conceitos demonstrados:
- Variáveis de ambiente (`export JAVA_HOME`, `export M2_HOME`)
- Redirecionamento de saída (`>>` para logs, `>` para criar arquivos de configuração)
- Pipes (`java -version 2>&1 | grep -q "21"`)
- Permissões Linux (`chmod 600 .env`)

### Arquivo `.env` gerado em `~/mayarpg/.env`:

```env
APP_PORT=8080
JAR_NAME=mayarpg-0.0.1-SNAPSHOT.jar
JWT_SECRET=maya_dev_secret_change_in_production   # TROCAR EM PRODUÇÃO
DB_PATH=~/mayarpg/db/mayarpg.db
SWAGGER_URL=http://localhost:8080/swagger-ui/index.html
```

> **Atenção:** Altere `JWT_SECRET` antes de subir para produção.

---

## Script 2 — `monitoramento.sh`

**Objetivo:** Coleta métricas do sistema e da aplicação MayaRPG em tempo real, gera logs estruturados e dispara alertas quando limites são ultrapassados.

### Métricas coletadas:
| Categoria | Métricas |
|-----------|----------|
| CPU | Uso percentual, load average, top processos |
| Memória | Total, em uso, disponível, swap |
| Disco | Uso por partição, tamanho do `mayarpg.db`, tamanho dos logs |
| Aplicação | PID, uptime, CPU e memória do processo Java, conexões ativas |
| Logs da App | Contagem de ERRORs, WARNINGs, logins (`/api/auth/login`), cadastros (`/api/auth/register`) |

### Modos de uso:

```bash
# Coleta única com relatório no terminal
bash monitoramento.sh

# Modo contínuo — atualiza a cada 30 segundos
bash monitoramento.sh --watch

# Gera relatório em arquivo .txt
bash monitoramento.sh --relatorio
```

### Arquivos gerados:

```
~/mayarpg/logs/
├── metrics_YYYYMMDD.log       # Métricas em formato estruturado (timestamp | categoria | chave | valor)
├── alertas_YYYYMMDD.log       # Alertas de CPU, memória e disco
└── relatorios/
    └── relatorio_TIMESTAMP.txt  # Relatório completo
```

### Limites de alerta configuráveis (no topo do script):

```bash
CPU_LIMITE=80      # % de CPU
MEM_LIMITE=85      # % de memória
DISCO_LIMITE=90    # % de disco
INTERVALO=30       # segundos entre coletas no modo --watch
```

### Conceitos demonstrados:
- Pipes encadeados (`ps aux | grep jar | grep -v grep | awk '{print $2}'`)
- Redirecionamento (`>>` para logs, `tee` para exibir e salvar simultaneamente)
- Variáveis de ambiente como parâmetros configuráveis
- Funções Bash com retorno de valores

---

## Script 3 — `backup.sh`

**Objetivo:** Automatiza o backup do código-fonte e do banco de dados SQLite (`mayarpg.db`), com compressão, checksum MD5 para integridade, rotação automática e suporte a restauração.

### Modos de uso:

```bash
# Backup completo (código-fonte + banco de dados)
bash backup.sh

# Apenas código-fonte
bash backup.sh --codigo

# Apenas banco de dados
bash backup.sh --banco

# Listar backups existentes com tamanhos
bash backup.sh --listar

# Restaurar o backup mais recente do banco
bash backup.sh --restaurar
```

### Estrutura de backups gerada:

```
~/mayarpg/backup/
├── codigo/
│   ├── mayarpg_codigo_TIMESTAMP.tar.gz    # Código compactado (sem target/, .git/)
│   └── mayarpg_codigo_TIMESTAMP.manifest  # Metadados e checksum MD5
└── banco/
    ├── mayarpg_banco_TIMESTAMP.db.gz      # Dump SQL compactado do mayarpg.db
    └── mayarpg_banco_TIMESTAMP.manifest   # Metadados e checksum MD5
```

### Rotação automática:
Por padrão, mantém os **7 backups mais recentes** de cada tipo. Configure com a variável `MANTER_BACKUPS`.

### Configuração de cron job para backup diário automático:

```bash
# Abre o crontab
crontab -e

# Adicione a linha abaixo para backup às 2h da manhã todos os dias
0 2 * * * /bin/bash /caminho/para/backup.sh >> ~/mayarpg/logs/cron_backup.log 2>&1
```

### Conceitos demonstrados:
- Pipes (`sqlite3 mayarpg.db .dump | gzip > arquivo.gz`)
- Redirecionamento (`>` para manifest, `>>` para log, `2>> stderr`)
- Variáveis de ambiente para configurar caminhos e retenção
- Cron jobs para automação periódica
- Permissões e verificações de integridade (MD5)

---

## Script 4 — `gerenciar_servico.sh`

**Objetivo:** Gerencia o ciclo de vida completo do MayaRPG Backend — compilar, iniciar, parar, reiniciar, verificar status e ativar watchdog com restart automático em caso de falha.

### Modos de uso:

```bash
# Compila o projeto com Maven
bash gerenciar_servico.sh build

# Inicia o backend (compila automaticamente se o JAR não existir)
bash gerenciar_servico.sh start

# Para o backend graciosamente (SIGTERM → SIGKILL se necessário)
bash gerenciar_servico.sh stop

# Reinicia o backend
bash gerenciar_servico.sh restart

# Exibe status detalhado (PID, CPU, memória, uptime, endpoints)
bash gerenciar_servico.sh status

# Ativa o watchdog — monitora e reinicia automaticamente em caso de falha
bash gerenciar_servico.sh watchdog

# Exibe os logs em tempo real com colorização por nível (INFO/WARN/ERROR)
bash gerenciar_servico.sh logs
```

### Saída do comando `status`:

```
════════════════════════════════════════════════
   STATUS — MayaRPG Backend
════════════════════════════════════════════════
  Status:      ● RODANDO
  PID:         12345
  Uptime:      00:42:17
  CPU:         2.3%
  Memória:     312 MB
  Porta 8080:  Respondendo ✓
  Conexões:    3 ativa(s)

  Endpoints disponíveis:
  → Swagger:   http://localhost:8080/swagger-ui/index.html
  → Login:     POST http://localhost:8080/api/auth/login
  → Register:  POST http://localhost:8080/api/auth/register
  → Pacientes: GET  http://localhost:8080/api/admin/pacientes
  → Ativos:    GET  http://localhost:8080/api/admin/pacientes/ativos
```

### Watchdog — restart automático:

O modo `watchdog` verifica o processo a cada `WATCHDOG_INTERVALO` segundos. Após `WATCHDOG_MAX_FALHAS` falhas consecutivas, executa restart automático.

```bash
# Para rodar o watchdog em background permanente:
nohup bash gerenciar_servico.sh watchdog >> ~/mayarpg/logs/watchdog.log 2>&1 &
```

### Conceitos demonstrados:
- Gerenciamento de processos (`kill -TERM`, `kill -KILL`, `kill -0`)
- PID file para rastrear o processo (`echo $! > mayarpg.pid`)
- Pipes (`ps aux | grep jar | grep -v grep | awk`)
- Redirecionamento (`nohup java -jar >> app.log 2>&1 &`)
- Variáveis de ambiente para configuração do Spring Boot (`SPRING_PROFILES_ACTIVE`, `JWT_SECRET`)
- Loop de watchdog com contagem de falhas

---

## Fluxo de Uso Recomendado

```bash
# 1. Primeira vez — configurar o ambiente
sudo bash setup_ambiente.sh

# 2. Compilar e iniciar o backend
bash gerenciar_servico.sh start

# 3. Verificar se está rodando
bash gerenciar_servico.sh status

# 4. Monitorar o sistema
bash monitoramento.sh --watch

# 5. Fazer backup antes de atualizações
bash backup.sh

# 6. Ativar watchdog em produção
nohup bash gerenciar_servico.sh watchdog >> ~/mayarpg/logs/watchdog.log 2>&1 &
```

---

## Cron Jobs Recomendados

Edite seu crontab com `crontab -e` e adicione:

```bash
# Monitoramento a cada 5 minutos (salva métricas)
*/5 * * * * /bin/bash /caminho/para/monitoramento.sh >> /dev/null 2>&1

# Backup completo todo dia às 2h da manhã
0 2 * * * /bin/bash /caminho/para/backup.sh >> ~/mayarpg/logs/cron_backup.log 2>&1

# Relatório de monitoramento toda segunda às 8h
0 8 * * 1 /bin/bash /caminho/para/monitoramento.sh --relatorio >> /dev/null 2>&1
```

---

## Permissões

Torne os scripts executáveis antes de usar:

```bash
chmod +x setup_ambiente.sh monitoramento.sh backup.sh gerenciar_servico.sh
```

---

## Tecnologias do Projeto

| Componente | Tecnologia |
|------------|-----------|
| Framework  | Spring Boot 3.5 |
| Linguagem  | Java 21 |
| Build      | Maven 3.9.6 |
| Banco      | SQLite (`mayarpg.db`) |
| Segurança  | Spring Security + JWT (jjwt 0.12.6) |
| Docs       | Swagger / OpenAPI (springdoc 2.8.6) |
| Testes     | JUnit + H2 (in-memory) |

---

## Autor

Projeto: **MayaRPG Backend**  
Grupo: com.noxcrew  
Artefato: `mayarpg-0.0.1-SNAPSHOT.jar`
