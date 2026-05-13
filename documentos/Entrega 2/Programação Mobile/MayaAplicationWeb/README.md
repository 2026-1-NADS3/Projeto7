# \# Clínica Maya - Admin Panel (MayaRPG)

# 

# Repositório do painel administrativo da Clínica Maya. Este sistema web atua em conjunto com o aplicativo mobile dos pacientes (MayaRPG), permitindo que os fisioterapeutas gerenciem cadastros, prescrevam exercícios e acompanhem a evolução clínica à distância.

# 

# \---

# 

# \## Funcionalidades

# 

# O painel foi desenvolvido para otimizar o fluxo de trabalho da clínica, contendo:

# 

# \* \*\*Gestão de Pacientes:\*\* Cadastro completo, informações de contato e prontuários médicos.

# \* \*\*Banco de Exercícios:\*\* Catálogo interno para cadastro de exercícios base, suportando upload de imagens e links de vídeos.

# \* \*\*Prescrição de Rotinas:\*\* Criação de treinos personalizados, definindo séries, repetições e tempo para cada paciente.

# \* \*\*Acompanhamento de Evolução:\*\* Gráficos que exibem a média do nível de dor relatado pelo paciente através dos check-ins no app.

# \* \*\*Chat em Tempo Real:\*\* Canal de atendimento direto entre a clínica e o paciente.

# 

# \---

# 

# \## UI / UX

# 

# A interface foi construída sem frameworks de UI externos (como Bootstrap ou Tailwind), focando em performance e em uma identidade visual limpa:

# 

# \- \*\*Design Minimalista:\*\* Tema claro focado em usabilidade, utilizando a cor primária da marca (`#2EC4B6`).

# \- \*\*Toasts Customizados:\*\* Sistema de notificações construído do zero com animações CSS, substituindo alertas nativos do navegador.

# \- \*\*Gavetas (Drawers):\*\* Formulários dinâmicos que expandem na própria tela, evitando redirecionamentos desnecessários e mantendo o contexto do usuário.

# \- \*\*Autenticação:\*\* Proteção de rotas via token JWT, incluindo opção para manter a sessão ativa.

# 

# \---

# 

# \## Tecnologias Utilizadas

# 

# \* \*\*\[React.js](https://reactjs.org/) \& React Router:\*\* Estrutura base da aplicação SPA.

# \* \*\*CSS3:\*\* Estilização responsiva e animações 100% customizadas.

# \* \*\*\[Firebase Realtime Database](https://firebase.google.com/):\*\* Sincronização de dados do módulo de Chat.

# \* \*\*\[Cloudinary](https://cloudinary.com/):\*\* Serviço utilizado para upload e entrega otimizada das imagens do banco de exercícios.

# \* \*\*API REST:\*\* Consumo de endpoints do backend (Spring Boot) do projeto.

# 

# \---

# 

# \## Como rodar o projeto localmente

# 

# \*\*1. Clone este repositório:\*\*

# ```bash

# git clone \[https://github.com/SEU\_USUARIO/maya-admin-panel.git](https://github.com/SEU\_USUARIO/maya-admin-panel.git)

