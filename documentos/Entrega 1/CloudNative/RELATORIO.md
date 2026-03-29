# Relatório - Scripts de Infraestrutura e Automação Linux
## MayaRPG Backend

---

## Introdução

O objetivo deste trabalho foi desenvolver scripts Shell/Bash para automatizar tarefas repetitivas do ciclo de desenvolvimento do MayaRPG Backend, um projeto de API REST feito em Java com Spring Boot 3.5, Maven e SQLite.

Sem automação, configurar o ambiente, iniciar a aplicação, monitorar o processo e fazer backups exige vários comandos manuais que são fáceis de errar, especialmente na hora de lembrar os parâmetros do Java e as variáveis do Spring Boot. Os scripts foram feitos pra resolver exatamente isso.

---

## Scripts desenvolvidos

### setup_ambiente.sh

Este script foi o ponto de partida. Ele instala Java 21, Maven 3.9.6 e SQLite3, configura o `JAVA_HOME` e cria um arquivo `.env` com as variáveis do projeto (porta, JWT secret, caminho do banco).

A principal vantagem é que qualquer pessoa pode clonar o repositório e rodar `sudo bash setup_ambiente.sh` pra ter o ambiente configurado sem precisar seguir um passo a passo manual. O script já verifica se as ferramentas estão instaladas antes de instalar de novo, então não tem problema rodar mais de uma vez.

**Conceitos utilizados:** variáveis de ambiente exportadas via `/etc/environment`, redirecionamento de saída (`>>`) pra salvar o log da instalação, pipes para verificar versões (`java -version 2>&1 | grep -q "21"`), e `chmod 600` no arquivo `.env` pra proteger as credenciais.

---

### monitoramento.sh

A ideia aqui foi ter visibilidade sobre o que está acontecendo na máquina e na aplicação. O script coleta uso de CPU, memória, disco e informações do processo Java do MayaRPG (PID, memória consumida, conexões ativas).

Uma parte que achei interessante foi a análise dos logs do Spring Boot: o script usa `tail -n 100 | grep | wc -l` pra contar quantos erros e acessos nos endpoints `/api/auth/login` e `/api/auth/register` aconteceram nas últimas 100 linhas do log. Isso ajuda bastante na hora de debugar.

O modo `--watch` fica atualizando as métricas a cada 30 segundos, o que é útil pra acompanhar testes de carga. O modo `--relatorio` salva tudo em um arquivo `.txt` com data e hora.

**Conceitos utilizados:** pipes encadeados (`ps aux | grep jar | grep -v grep | awk`), `tee` para exibir e salvar ao mesmo tempo, variáveis de ambiente como limites configuráveis de alerta, e cron jobs para execução automática periódica.

---

### backup.sh

O `mayarpg.db` é o único arquivo de persistência de dados em ambiente de desenvolvimento, então perder ele seria um problema sério. O script faz um dump SQL usando `sqlite3` antes de compactar com gzip, o que é mais seguro do que só copiar o arquivo binário porque garante consistência mesmo com a aplicação rodando.

Para o código-fonte, cria um `.tar.gz` excluindo automaticamente a pasta `target/`, `.git/` e arquivos `.class`, que não precisam ser versionados manualmente. Cada backup gera um `.manifest` com checksum MD5 pra verificar a integridade depois.

A rotação automática remove os backups mais antigos quando passa do limite configurado (padrão: 7), então não precisa se preocupar com o disco enchendo.

**Conceitos utilizados:** pipe pra dump compactado (`sqlite3 mayarpg.db .dump | gzip > arquivo.gz`), redirecionamento pra criar manifests (`cat > arquivo`), cron jobs pra backup automático, e variáveis de ambiente pra configurar a política de retenção.

---

### gerenciar_servico.sh

Este foi o script mais trabalhoso. Ele encapsula toda a lógica de iniciar o backend com os parâmetros corretos do Java e do Spring Boot, o que no dia a dia evita erros de digitar o comando errado.

O comando `start` verifica se o JAR existe, compila automaticamente se não existir, inicia o processo em background com `nohup`, salva o PID em arquivo e fica lendo o log do Spring Boot esperando aparecer a mensagem `Started MayarpgApplication` pra confirmar que subiu certo.

O watchdog foi a parte mais interessante: fica num loop infinito verificando se o processo ainda está ativo a cada 15 segundos. Após 3 falhas consecutivas, executa o restart automaticamente. Isso é útil em ambiente de desenvolvimento compartilhado onde a aplicação pode ser derrubada por acidente.

O comando `logs` usa `tail -f` com pipe pra colorizar as linhas em vermelho (ERROR), amarelo (WARN) ou branco normal, o que facilita muito a leitura do log do Spring Boot.

**Conceitos utilizados:** gerenciamento de processos (`kill -TERM` pra encerramento gracioso e `kill -KILL` pra forçado), PID file pra rastrear o processo, `nohup` com redirecionamento pra rodar em background (`>> app.log 2>&1 &`), loop do watchdog com contador de falhas, e pipes pra colorização do log em tempo real.

---

## Como os scripts facilitam o desenvolvimento

Antes dos scripts, algumas tarefas eram bem chatas no dia a dia:

- Lembrar de passar `-Dspring.profiles.active=dev -Djwt.secret=... -Dserver.port=8080` toda vez que ia iniciar a aplicação
- Fazer backup manual do `mayarpg.db` antes de testar migrações
- Ficar abrindo o gerenciador de tarefas pra ver se o processo Java estava consumindo memória demais
- Quando a aplicação caía, perceber só quando alguém tentava acessar e aí ir reiniciar manualmente

Com os scripts isso tudo virou um comando só. O fluxo ficou bem mais simples: `bash gerenciar_servico.sh start` pra subir, `bash monitoramento.sh --watch` pra acompanhar, `bash backup.sh` antes de mudanças grandes, e o watchdog rodando em background pra garantir que a aplicação vai se recuperar sozinha se cair.

---

## Conceitos aplicados

| Conceito | Como foi usado |
|----------|---------------|
| Pipes | Filtrar processos, analisar logs, dump do banco |
| Redirecionamento | Gravar logs (`>>`), criar arquivos de config (`>`), rodar em background (`2>&1`) |
| Variáveis de ambiente | Configurar Java, Maven, Spring Boot, JWT |
| Cron jobs | Backup e monitoramento automáticos |
| Permissões | `chmod 600` no `.env`, `chmod +x` nos scripts |
| Gerenciamento de processos | PID file, SIGTERM, SIGKILL, watchdog |
