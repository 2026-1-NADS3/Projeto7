# MayaRPG Backend - Scripts de Automação Linux

Scripts feitos para facilitar o desenvolvimento e manutenção do MayaRPG Backend. São 4 scripts no total, cada um responsável por uma parte do ciclo de desenvolvimento.

O projeto usa **Java 21 + Spring Boot 3.5 + Maven + SQLite**.

---

## Como usar

Antes de qualquer coisa, dê permissão de execução para os scripts:

```bash
chmod +x setup_ambiente.sh monitoramento.sh backup.sh gerenciar_servico.sh
```

---

## 1. setup_ambiente.sh

Instala tudo que é necessário pra rodar o projeto: Java 21, Maven 3.9.6, SQLite3 e as dependências do Android SDK. Também cria um arquivo `.env` com as configurações do projeto.

```bash
sudo bash setup_ambiente.sh
```

Depois de rodar, vai ter uma pasta `~/mayarpg/` com a estrutura de diretórios do projeto e um `.env` com as variáveis configuradas. **Não esqueça de trocar o `JWT_SECRET` antes de subir pra produção.**

---

## 2. monitoramento.sh

Coleta métricas do sistema (CPU, memória, disco) e da aplicação (PID, memória do processo Java, conexões ativas). Também analisa os logs do Spring Boot e conta erros, avisos e acessos nos endpoints `/api/auth/login` e `/api/auth/register`.

```bash
# Roda uma vez e mostra tudo no terminal
bash monitoramento.sh

# Fica atualizando a cada 30 segundos
bash monitoramento.sh --watch

# Gera um arquivo .txt com o relatório completo
bash monitoramento.sh --relatorio
```

As métricas ficam salvas em `~/mayarpg/logs/metrics_YYYYMMDD.log` e os alertas em `~/mayarpg/logs/alertas_YYYYMMDD.log`.

Você pode configurar os limites de alerta direto no topo do script:

```bash
CPU_LIMITE=80     # alerta quando CPU passar de 80%
MEM_LIMITE=85     # alerta quando memória passar de 85%
DISCO_LIMITE=90   # alerta quando disco passar de 90%
```

Para rodar automaticamente de 5 em 5 minutos, adicione no crontab (`crontab -e`):

```
*/5 * * * * /bin/bash /caminho/para/monitoramento.sh >> /dev/null 2>&1
```

---

## 3. backup.sh

Faz backup do código-fonte e do banco de dados `mayarpg.db`. O backup do banco usa `sqlite3` para fazer um dump SQL antes de compactar, o que garante consistência mesmo com a aplicação rodando. Cada backup gera um arquivo `.manifest` com o checksum MD5 pra verificar integridade depois.

```bash
# Backup completo (código + banco)
bash backup.sh

# Só o código-fonte
bash backup.sh --codigo

# Só o banco de dados
bash backup.sh --banco

# Lista os backups existentes
bash backup.sh --listar

# Restaura o backup mais recente do banco
bash backup.sh --restaurar
```

Por padrão mantém os **7 backups mais recentes** e apaga os mais antigos automaticamente. Para mudar isso, edite a variável `MANTER_BACKUPS` no topo do script.

Para backup automático todo dia às 2h da manhã:

```
0 2 * * * /bin/bash /caminho/para/backup.sh >> ~/mayarpg/logs/cron_backup.log 2>&1
```

---

## 4. gerenciar_servico.sh

Gerencia o processo do backend. Cuida de tudo: compilar, iniciar com os parâmetros certos, parar, reiniciar e monitorar. O modo watchdog fica de olho na aplicação e reinicia automaticamente se ela cair.

```bash
# Compila o projeto
bash gerenciar_servico.sh build

# Inicia (compila automaticamente se o JAR não existir)
bash gerenciar_servico.sh start

# Para
bash gerenciar_servico.sh stop

# Reinicia
bash gerenciar_servico.sh restart

# Mostra status, PID, memória e os endpoints disponíveis
bash gerenciar_servico.sh status

# Fica monitorando e reinicia automaticamente se cair
bash gerenciar_servico.sh watchdog

# Mostra os logs em tempo real com cores por nível
bash gerenciar_servico.sh logs
```

Para rodar o watchdog em background permanente:

```bash
nohup bash gerenciar_servico.sh watchdog >> ~/mayarpg/logs/watchdog.log 2>&1 &
```

---

## Fluxo normal de uso

```bash
# Primeira vez na máquina
sudo bash setup_ambiente.sh

# Iniciar o backend
bash gerenciar_servico.sh start

# Ver se está tudo certo
bash gerenciar_servico.sh status

# Monitorar enquanto desenvolve
bash monitoramento.sh --watch

# Antes de fazer alterações grandes, fazer backup
bash backup.sh
```

---

## Onde ficam os arquivos

```
~/mayarpg/
├── .env                    # variáveis de ambiente do projeto
├── mayarpg.pid             # PID da aplicação em execução
├── logs/
│   ├── app_YYYYMMDD.log    # log do Spring Boot
│   ├── metrics_YYYYMMDD.log
│   ├── alertas_YYYYMMDD.log
│   └── relatorios/
└── backup/
    ├── codigo/             # backups do código-fonte (.tar.gz)
    └── banco/              # backups do banco de dados (.db.gz)
```



<p align="center">
<img src="https://imgur.com/We5CocC" alt="Coder" border="0">
</p>
