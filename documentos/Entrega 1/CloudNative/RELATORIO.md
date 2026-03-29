# Relatório Técnico — Scripts de Infraestrutura e Automação
## MayaRPG Backend

**Disciplina:** Infraestrutura e Automação com Linux  
**Projeto:** MayaRPG Backend (Spring Boot 3.5 + Java 21 + SQLite + JWT)  
**Grupo:** com.noxcrew  

---

## 1. Introdução

O MayaRPG Backend é uma API REST desenvolvida em Java com Spring Boot, responsável por autenticação de usuários via JWT e gerenciamento de pacientes de uma aplicação RPG. O projeto utiliza SQLite como banco de dados e Maven como ferramenta de build.

Este relatório descreve os quatro scripts Shell/Bash desenvolvidos para automatizar o ciclo de desenvolvimento do projeto, reduzir erros manuais e aumentar a produtividade da equipe.

---

## 2. Problema: O Ciclo Manual de Desenvolvimento

Sem automação, cada membro da equipe precisaria executar manualmente uma sequência de passos para configurar o ambiente, iniciar a aplicação, monitorá-la e garantir backups. Esse processo é lento, propenso a erros e dificulta a integração de novos desenvolvedores.

Tarefas repetitivas identificadas no projeto MayaRPG:

- Instalar Java 21 e Maven na versão correta para o `spring-boot-starter-parent 3.5.11`
- Configurar variáveis de ambiente como `JAVA_HOME` e `JWT_SECRET`
- Compilar o projeto com `mvn clean package -DskipTests`
- Iniciar o JAR gerado (`mayarpg-0.0.1-SNAPSHOT.jar`) com os parâmetros corretos
- Verificar se os endpoints `/api/auth/login`, `/api/auth/register` e `/api/admin/pacientes` estão respondendo
- Fazer backup do `mayarpg.db` antes de alterações no banco
- Monitorar uso de memória do processo Java em execução

---

## 3. Solução: Scripts de Automação

### 3.1 `setup_ambiente.sh` — Configuração do Ambiente

**Problema resolvido:** Garantir que todo desenvolvedor trabalhe com as mesmas versões de Java (21) e Maven (3.9.6), independente do sistema operacional.

**Como funciona:** O script verifica se cada ferramenta já está instalada antes de instalar, evitando duplicações. Cria um arquivo `.env` com as variáveis de ambiente do projeto, incluindo o segredo JWT, a porta da aplicação e o caminho do banco SQLite.

**Impacto no ciclo de desenvolvimento:** Reduz o tempo de onboarding de um novo desenvolvedor de horas para minutos. Elimina erros causados por versões incompatíveis de Java (o projeto exige Java 21 conforme `<java.version>21</java.version>` no `pom.xml`).

**Conceitos utilizados:**
- Variáveis de ambiente exportadas para o sistema (`/etc/environment`)
- Redirecionamento de saída (`>>`) para log de instalação
- Pipes para verificar versões instaladas (`java -version 2>&1 | grep -q "21"`)
- Permissões Linux (`chmod 600`) para proteger o arquivo `.env` com credenciais

---

### 3.2 `monitoramento.sh` — Coleta de Métricas e Logs

**Problema resolvido:** Ter visibilidade sobre o comportamento da aplicação em tempo real, especialmente o consumo de memória do processo Java (que pode crescer) e os acessos aos endpoints de autenticação.

**Como funciona:** Coleta métricas de CPU, memória e disco do sistema operacional, além de métricas específicas do processo Java do MayaRPG. Analisa os logs do Spring Boot para contar acessos aos endpoints `/api/auth/login` e `/api/auth/register`, e registra alertas quando limites configuráveis são ultrapassados.

**Impacto no ciclo de desenvolvimento:** Permite identificar gargalos de performance antes que causem problemas em produção. O modo `--watch` (execução contínua a cada 30 segundos) é especialmente útil durante testes de carga. O modo `--relatorio` gera documentação de performance que pode ser compartilhada com a equipe.

**Conceitos utilizados:**
- Pipes encadeados para filtrar informações de processos (`ps aux | grep jar | grep -v grep | awk '{print $2}'`)
- Redirecionamento duplo com `tee` para exibir e salvar simultaneamente
- Variáveis de ambiente como limiares configuráveis de alerta
- Análise de logs por pipes (`tail -n 100 | grep -i "ERROR" | wc -l`)

---

### 3.3 `backup.sh` — Backup Automatizado

**Problema resolvido:** O `mayarpg.db` (SQLite) é o único ponto de persistência de dados do projeto em desenvolvimento. Uma corrupção ou exclusão acidental do arquivo significa perda total dos dados de pacientes e usuários cadastrados.

**Como funciona:** Realiza dump SQL do banco via `sqlite3` (garantindo consistência mesmo com a aplicação em execução) e compacta com `gzip`. Para o código-fonte, cria um `.tar.gz` excluindo automaticamente a pasta `target/`, `.git/` e arquivos `.class`, que podem ser regenerados pelo Maven. Cada backup recebe um checksum MD5 para verificação de integridade. A rotação automática mantém apenas os 7 backups mais recentes.

**Impacto no ciclo de desenvolvimento:** Permite fazer alterações experimentais no banco com segurança, sabendo que existe backup. O comando `--restaurar` permite desfazer migrações problemáticas rapidamente.

**Conceitos utilizados:**
- Pipes para dump compactado (`sqlite3 mayarpg.db .dump | gzip > backup.db.gz`)
- Redirecionamento para criação de manifests com metadados (`cat > arquivo.manifest`)
- Cron jobs para automação periódica sem intervenção manual
- Variáveis de ambiente para configurar política de retenção

---

### 3.4 `gerenciar_servico.sh` — Gerenciamento de Processos

**Problema resolvido:** Iniciar o MayaRPG Backend requer a combinação correta de parâmetros JVM, variáveis de ambiente Spring Boot e configurações JWT. Fazer isso manualmente a cada reinicialização é trabalhoso e sujeito a erros.

**Como funciona:** Encapsula toda a lógica de ciclo de vida da aplicação. No comando `start`, verifica se o JAR existe (compilando automaticamente se necessário), inicia o processo em background com `nohup`, salva o PID em arquivo e aguarda a confirmação de inicialização lendo o log do Spring Boot. O watchdog executa em loop, verificando a cada 15 segundos se o processo está ativo, e realiza restart automático após 3 falhas consecutivas.

**Impacto no ciclo de desenvolvimento:** Elimina a necessidade de lembrar parâmetros JVM e variáveis de ambiente a cada reinicialização. O watchdog é essencial em ambiente de desenvolvimento compartilhado, onde a aplicação pode ser derrubada acidentalmente. O comando `logs` com colorização por nível (vermelho para ERROR, amarelo para WARN) acelera o debug.

**Conceitos utilizados:**
- Gerenciamento de processos com `kill -TERM` (encerramento gracioso) e `kill -KILL` (forçado)
- PID file para rastrear o processo entre execuções do script
- Redirecionamento com `nohup` para execução em background (`>> app.log 2>&1 &`)
- Pipes para colorização de logs em tempo real (`tail -f | while IFS= read -r linha`)
- Loop de watchdog com contagem de falhas consecutivas

---

## 4. Conceitos Linux Demonstrados

| Conceito | Onde é usado | Exemplo |
|----------|-------------|---------|
| **Pipes** | Todos os scripts | `ps aux \| grep jar \| grep -v grep \| awk '{print $2}'` |
| **Redirecionamento** | Todos os scripts | `echo "PID" > mayarpg.pid`, `logs >> app.log` |
| **Variáveis de ambiente** | setup, gerenciar | `export JAVA_HOME`, `SPRING_PROFILES_ACTIVE=dev` |
| **Cron jobs** | backup, monitoramento | `0 2 * * * bash backup.sh` |
| **Permissões** | setup | `chmod 600 .env`, `chmod +x scripts` |
| **Gerenciamento de processos** | gerenciar | `kill -TERM $PID`, `nohup ... &`, PID file |
| **Funções Bash** | Todos os scripts | Funções `log()`, `obter_pid()`, `backup_banco()` |
| **Arrays e loops** | gerenciar, backup | Loop de watchdog, rotação de backups |
| **Condicionais** | Todos os scripts | Verificações de existência, limites de alerta |

---

## 5. Como os Scripts Facilitam o Ciclo de Desenvolvimento

### Fase de Setup (uma vez por máquina)
```
Antes: ~2 horas de configuração manual, documentação desatualizada
Depois: sudo bash setup_ambiente.sh → ambiente pronto em ~10 minutos
```

### Fase de Desenvolvimento (diário)
```
Antes: java -jar target/mayarpg-*.jar -Dserver.port=8080 -Djwt.secret=... (erro-propenso)
Depois: bash gerenciar_servico.sh start (um comando, configurações centralizadas)
```

### Fase de Debug
```
Antes: Procurar manualmente no log do Spring Boot por erros
Depois: bash gerenciar_servico.sh logs (logs coloridos em tempo real)
         bash monitoramento.sh (métricas + contagem de erros)
```

### Fase de Deploy/Atualização
```
Antes: Fazer backup manual, parar aplicação, compilar, iniciar, torcer
Depois:
  bash backup.sh                      # backup de segurança
  bash gerenciar_servico.sh stop      # parar graciosamente
  bash gerenciar_servico.sh build     # compilar nova versão
  bash gerenciar_servico.sh start     # iniciar
  bash gerenciar_servico.sh status    # confirmar endpoints ativos
```

---

## 6. Conclusão

Os quatro scripts desenvolvidos cobrem o ciclo completo de desenvolvimento do MayaRPG Backend, desde a configuração inicial do ambiente até o monitoramento contínuo em produção. A automação elimina tarefas repetitivas, padroniza configurações entre os membros da equipe e aumenta a confiabilidade do processo de desenvolvimento.

O uso consistente de pipes, redirecionamento, variáveis de ambiente e cron jobs demonstra como o Shell Bash é uma ferramenta poderosa para orquestrar aplicações Java complexas em ambiente Linux, mesmo sem ferramentas de terceiros como Docker ou Kubernetes.
