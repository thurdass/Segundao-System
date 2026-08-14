# Segundão System

<p align="center">
  <img src="img.png" width="320" alt="Turma 2º Informática A">
</p>

<p align="center">
  API REST desenvolvida para a turma <strong>2º Informática A</strong>, com foco na organização de atividades, avisos, disciplinas, professores e horários.
</p>

<h2 align="center">🛠 Technologies</h2>

<h3 align="center">☕ Backend</h3>

<p align="center">
  <img src="https://skillicons.dev/icons?i=java,spring,mysql,maven" />
</p>

<h3 align="center">🧰 Tools</h3>

<p align="center">
  <img src="https://skillicons.dev/icons?i=git,github,docker,linux,idea,postman" />
</p>

---

## Sobre o projeto

O **Segundão System** foi criado para auxiliar a organização da turma **2º Informática A**.

A plataforma permite que cada aluno utilize sua própria conta para acompanhar:

* atividades;
* prazos;
* avisos;
* disciplinas;
* professores;
* horários.

As atividades pertencem à turma, mas a conclusão é individual para cada aluno.

Isso significa que um aluno pode marcar uma atividade como concluída sem alterar o status dela para os demais.

O projeto começou como uma **API REST em Spring Boot** e agora possui uma primeira versão de frontend em React, no diretório `frontend/`, consumindo os contratos reais da API.

---

## Sobre a escola

O projeto foi desenvolvido para alunos do [Centro Territorial de Educação Profissional Piemonte do Paraguaçu II](https://escolas.educacao.ba.gov.br/node/12568).

O **Centro Territorial de Educação Profissional Piemonte do Paraguaçu II** é uma escola técnica localizada no município de **Mundo Novo, Bahia**.

---

## Stack

* Java 21
* Spring Boot 4
* Spring Web MVC
* Spring Data JPA
* Spring Security
* MySQL
* H2 para testes
* Bean Validation
* Lombok
* JWT
* BCrypt
* Maven
* React
* Vite
* TypeScript
* Axios
* React Router
* lucide-react

---

## Configuração

A aplicação utiliza variáveis de ambiente para configurações sensíveis.

Configure:

```text id="w5j12v"
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

Opcionalmente:

```text id="9fq7gi"
DDL_AUTO
FRONTEND_ORIGIN
INITIAL_ADMIN_ENABLED
INITIAL_ADMIN_USERNAME
INITIAL_ADMIN_PASSWORD
INITIAL_ADMIN_DISPLAY_NAME
```

Exemplo:

```text id="r7bpdn"
DB_URL=jdbc:mysql://localhost:3306/2a_system
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha
JWT_SECRET=uma_chave_com_pelo_menos_32_caracteres
FRONTEND_ORIGIN=http://localhost:5173
```

Nunca envie senhas, tokens ou segredos para o repositório.

O arquivo `.env.example` contém a lista de variáveis sem credenciais reais. Um arquivo `.env` local não é versionado.

A aplicação utiliza:

```text id="o4md78"
America/Sao_Paulo
```

como timezone padrão.

---

## Execução

Para iniciar a aplicação:

```bash id="4k4n9x"
./mvnw spring-boot:run
```

Para executar os testes:

```bash id="fxh2be"
./mvnw clean test
```

Para iniciar o frontend em outro terminal:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

O frontend utiliza `VITE_API_URL` para localizar o backend. O valor padrão do arquivo `.env.example` é:

```text
VITE_API_URL=http://localhost:8080
```

Validações disponíveis no frontend:

```bash
cd frontend
npm run lint
npm run build
```

O backend permite, por padrão, a origem `http://localhost:5173`. Em outro ambiente, configure `FRONTEND_ORIGIN` sem utilizar wildcard.

Em desenvolvimento, a aplicação cria automaticamente a turma inicial:

```text id="476yoc"
2A / Informática / 2026 / Manhã
```

---

## Autenticação

O sistema utiliza autenticação com **JWT**.

Cada aluno possui seu próprio usuário.

As contas são criadas exclusivamente por usuários com role:

```text id="aqhoho"
ADMIN
```

Não existe cadastro público.

Cada conta utiliza:

* username único;
* username normalizado em lowercase;
* senha criptografada com BCrypt;
* role `STUDENT` ou `ADMIN`;
* controle de usuário ativo ou desativado;
* troca obrigatória de senha no primeiro acesso.

O token deve ser enviado em requisições autenticadas:

```http id="94pfz3"
Authorization: Bearer <jwt>
```

---

## Login

```text id="st478m"
POST /api/auth/login
GET  /api/auth/me
```

Não existe cadastro público. O frontend oferece a tela `/login` e direciona contas com `mustChangePassword: true` para `/change-password` antes de liberar o restante do sistema.

## Bootstrap do primeiro administrador

O cadastro público está desativado. Para criar o primeiro administrador em um ambiente local, habilite explicitamente o bootstrap usando variáveis de ambiente no processo de execução:

```bash
export INITIAL_ADMIN_ENABLED=true
export INITIAL_ADMIN_USERNAME=seu_usuario_admin
export INITIAL_ADMIN_PASSWORD='sua_senha_inicial_local'
export INITIAL_ADMIN_DISPLAY_NAME='Administrador inicial'
./mvnw spring-boot:run
```

O bootstrap só cria uma conta quando ainda não existe nenhum usuário com role `ADMIN`. A turma inicial criada em desenvolvimento é associada automaticamente à conta. A senha é armazenada com BCrypt e `mustChangePassword` começa como `true`, exigindo a troca no primeiro acesso.

Depois que o administrador inicial for criado, desabilite `INITIAL_ADMIN_ENABLED` nas próximas execuções. Se a configuração estiver habilitada quando já existir um administrador, nenhuma conta adicional será criada.

As variáveis de bootstrap não possuem valores de credencial no repositório. Nunca registre ou compartilhe a senha, o hash BCrypt ou o JWT.

---

## Criação de usuários

Somente usuários com role `ADMIN` podem criar novas contas:

```text id="tvva6t"
POST /api/admin/users
```

Exemplo:

```json id="sqnhp0"
{
  "username": "arthur",
  "password": "senhaInicialFicticia",
  "displayName": "Arthur Almeida",
  "role": "STUDENT",
  "classroomId": 1
}
```

O username é normalizado para lowercase.

A senha inicial é armazenada com BCrypt.

A API nunca retorna:

* senha;
* hash BCrypt;
* tokens internos;
* informações sensíveis.

---

## Primeiro acesso

Contas criadas pelo administrador começam com:

```text id="x0djfl"
mustChangePassword: true
```

O aluno pode realizar o login inicial, mas precisa trocar a senha antes de utilizar o restante do sistema.

Endpoint:

```text id="jyarq1"
PATCH /api/auth/password
```

Exemplo:

```json id="8yon9e"
{
  "currentPassword": "senhaInicialFicticia",
  "newPassword": "novaSenhaFicticia"
}
```

Enquanto `mustChangePassword` for `true`, o usuário pode acessar apenas:

```text id="ytjycy"
POST  /api/auth/login
GET   /api/auth/me
PATCH /api/auth/password
```

Depois da troca:

```text id="qrvyop"
mustChangePassword = false
```

e o acesso normal é liberado.

---

## Disciplinas

```text id="hdmk0w"
GET  /api/subjects?classroomId=1
POST /api/subjects
```

Alterações exigem role `ADMIN`.

---

## Professores

```text id="afh4mn"
GET /api/teachers
GET /api/teachers/{id}
GET /api/teachers/{id}/subjects

POST /api/teachers
PUT  /api/teachers/{id}
```

Criação e alteração exigem `ADMIN`.

---

## Horários

```text id="gjp0qv"
GET    /api/schedules/classroom/{id}
POST   /api/schedules
DELETE /api/schedules/{id}
```

Alterações exigem `ADMIN`.

---

## Atividades

```text id="n6982e"
GET    /api/activities
GET    /api/activities/{id}
POST   /api/activities
PUT    /api/activities/{id}
DELETE /api/activities/{id}
```

Filtros disponíveis:

```text id="wsqi27"
GET /api/activities?status=pending
GET /api/activities?status=completed
GET /api/activities?subjectId=1
GET /api/activities?dueBefore=2026-08-30
```

Regras:

* atividades pertencem a uma turma;
* alunos veem apenas atividades da própria turma;
* o criador é obtido pelo usuário autenticado;
* alunos não podem criar atividades para outras turmas;
* o criador pode editar e excluir sua própria atividade;
* administradores podem editar e excluir qualquer atividade.

---

## Conclusão individual

A conclusão de uma atividade é individual para cada aluno.

Endpoints:

```text id="ewm25n"
POST   /api/activities/{id}/complete
DELETE /api/activities/{id}/complete
```

Quando um aluno conclui uma atividade, ela continua pendente para os demais alunos até que cada um conclua individualmente.

---

## Prazo pela próxima aula

Ao cadastrar uma atividade, o sistema suporta:

```text id="fjnd5v"
CUSTOM_DATE
NEXT_CLASS
```

Com:

```text id="bizyj8"
NEXT_CLASS
```

o sistema consulta automaticamente os horários da disciplina e calcula a próxima aula futura.

Também existe:

```text id="5kk26l"
GET /api/subjects/{subjectId}/next-class
```

Exemplo:

```http id="luh8md"
GET /api/subjects/1/next-class
Authorization: Bearer <jwt>
```

Resposta:

```json id="8q6ra3"
{
  "subjectId": 1,
  "subjectName": "Desenvolvimento Web",
  "nextClassDate": "2026-08-17",
  "dayOfWeek": "MONDAY",
  "startTime": "09:10:00",
  "endTime": "10:00:00"
}
```

O cálculo considera:

* dia e horário atual;
* aulas que já passaram;
* próximas ocorrências da semana;
* virada da semana;
* múltiplos horários da mesma disciplina;
* disciplinas sem horário cadastrado.

---

## Avisos

```text id="yzvl5v"
GET    /api/announcements
GET    /api/announcements/{id}
POST   /api/announcements
PUT    /api/announcements/{id}
DELETE /api/announcements/{id}
```

Regras:

* avisos pertencem à turma do criador;
* alunos veem apenas avisos da própria turma;
* criadores podem editar e excluir seus próprios avisos;
* administradores podem gerenciar qualquer aviso;
* administradores podem fixar avisos;
* avisos fixados aparecem primeiro.

---

## Administração

Endpoints administrativos:

```text id="1zdyy5"
POST  /api/admin/users
GET   /api/admin/users
GET   /api/admin/users/{id}
PATCH /api/admin/users/{id}/status
GET   /api/admin/dashboard
```

Todos exigem:

```text id="wjipzl"
ADMIN
```

O dashboard atualmente informa:

* quantidade total de usuários;
* usuários ativos;
* atividades ativas;
* avisos ativos.

O frontend também possui `/admin` para consultar o resumo, listar usuários, criar alunos com senha inicial, consultar detalhes e ativar ou desativar contas. A rota só é exibida para `ADMIN` e o backend continua responsável pela autorização.

---

## Frontend

A primeira versão da interface está organizada em páginas autenticadas:

* `/dashboard` — resumo com próxima aula, atividades, avisos e horário do dia;
* `/activities` — filtros, criação, edição, exclusão e conclusão individual;
* `/announcements` — mural com criação, edição, exclusão e avisos fixados;
* `/schedule` — horário semanal da turma;
* `/subjects` — disciplinas e consulta da próxima aula;
* `/teachers` — professores e disciplinas relacionadas;
* `/profile` — dados reais da conta autenticada;
* `/admin` — administração inicial para usuários com role `ADMIN`.

O JWT fica concentrado no cliente Axios e é enviado como `Authorization: Bearer <jwt>`. Não há refresh token nesta etapa. Ao receber `401`, a sessão é limpa e o usuário volta para o login; respostas `403` permanecem como acesso negado.

O frontend usa a identidade visual preto, branco, cinza e amarelo de destaque, com sidebar responsiva para telas pequenas. A imagem `img.png` é utilizada discretamente no branding.

---

## Segurança

Sem autenticação válida:

```text id="0fcy76"
401 Unauthorized
```

Usuário autenticado sem permissão:

```text id="ctoszb"
403 Forbidden
```

Alunos só podem acessar dados relacionados à própria turma.

---

## Testes

Os testes utilizam **H2 em memória**.

A suíte cobre:

* autenticação;
* login;
* JWT;
* criação administrativa de usuários;
* troca obrigatória de senha;
* permissões `STUDENT` e `ADMIN`;
* atividades;
* conclusão individual;
* filtros;
* horários;
* cálculo da próxima aula;
* endpoint de próxima aula;
* endpoints administrativos;
* restrições de acesso.

Execute:

```bash id="c83tvl"
./mvnw clean test
```

---

## Estado atual

O backend do MVP está funcional.

Já estão implementados:

* autenticação;
* criação de usuários pelo administrador;
* troca obrigatória de senha;
* usuários;
* turmas;
* disciplinas;
* professores;
* horários;
* atividades;
* conclusão individual;
* prazos;
* avisos;
* administração básica;
* testes automatizados;
* integração com MySQL.
* frontend React inicial integrado à API;
* CORS configurável para o frontend local;
* fluxo de primeiro acesso com troca obrigatória de senha na interface.

---

## Próximos passos

Planejado para próximas etapas:

* auditoria segura de acessos (`AccessLog`);
* OpenAPI / Swagger;
* migrações de banco;
* Docker;
* CI/CD;
* notificações;
* melhorias no painel administrativo;
* chat com inteligência artificial.

A integração com IA ainda não utiliza nenhuma API ou chave externa.

---

## Status

Backend e frontend inicial do MVP funcionais e em evolução.
