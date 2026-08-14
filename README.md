# Segundão System

<p align="center">
  <img src="img.png" width="320" alt="Turma 2º Informática A">
</p>

<p align="center">
  Sistema desenvolvido para a turma <strong>2º Informática A</strong>, com foco na organização da rotina escolar.
</p>

---

<h2 align="center">🛠 Technologies</h2>

<h3 align="center">☕ Backend</h3>

<p align="center">
  <img src="https://skillicons.dev/icons?i=java,spring,mysql,maven" />
</p>

<h3 align="center">🎨 Frontend</h3>

<p align="center">
  <img src="https://skillicons.dev/icons?i=react,typescript,vite" />
</p>

<h3 align="center">🧰 Tools</h3>

<p align="center">
  <img src="https://skillicons.dev/icons?i=git,github,docker,linux,idea,postman" />
</p>

---

## Sobre o projeto

O **Segundão System** foi criado para auxiliar a organização da turma **2º Informática A**.

A proposta é reunir em um único lugar informações importantes da rotina escolar, como:

- atividades;
- prazos;
- avisos;
- disciplinas;
- professores;
- horários.

Cada aluno possui sua própria conta e pode acompanhar suas atividades individualmente.

Quando um aluno conclui uma atividade, essa conclusão vale apenas para ele e não altera o status da atividade para os demais colegas.

---

## Funcionalidades
![Tela inicial](docs/screenshots/dashboard.png)
---
![Tela de atividades](docs/screenshots/activities.png)
---
![Painel administrativo](docs/screenshots/admin.png)
### Para alunos

- acompanhamento de atividades;
- marcação individual de atividades concluídas;
- consulta de avisos;
- visualização dos horários da turma;
- consulta de disciplinas;
- consulta de professores;
- visualização da próxima aula;
- perfil individual.

### Para administradores

- criação e gerenciamento de alunos;
- criação e gerenciamento de avisos;
- cadastro de disciplinas;
- cadastro e edição de professores;
- associação de professores às disciplinas;
- cadastro e remoção de horários;
- ativação e desativação de contas;
- visão geral da turma.

---

## Primeiro acesso

As contas dos alunos são criadas pelo administrador.

No primeiro acesso, o aluno utiliza uma senha inicial e deve alterá-la antes de utilizar normalmente o sistema.

Não existe cadastro público.

---

## Horários

O sistema possui uma grade semanal com as aulas do **2º Informática A**, permitindo que os alunos consultem rapidamente:

- dia da semana;
- horário;
- disciplina;
- professor.

A administração também pode atualizar a grade conforme necessário.

---

## Atividades

As atividades são compartilhadas com a turma, mas cada aluno possui seu próprio status de conclusão.

O sistema também permite organizar atividades por:

- disciplina;
- prazo;
- pendentes;
- concluídas.

---

## Avisos

O mural de avisos permite centralizar informações importantes da turma.

Administradores podem criar, editar, excluir e destacar avisos importantes.

---

## Administração

O **Segundão System** possui uma área exclusiva para administradores.

Nela é possível gerenciar:

- alunos;
- avisos;
- disciplinas;
- professores;
- horários.

O objetivo é permitir que a organização do sistema seja feita pela própria interface, sem necessidade de acessar diretamente o banco de dados.

---

## Sobre a escola

O projeto foi desenvolvido para alunos do [Centro Territorial de Educação Profissional Piemonte do Paraguaçu II](https://escolas.educacao.ba.gov.br/node/12568).

O **Centro Territorial de Educação Profissional Piemonte do Paraguaçu II** é uma escola técnica localizada em **Mundo Novo, Bahia**.

---

## Tecnologias

### Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- JWT
- BCrypt
- Maven

### Frontend

- React
- TypeScript
- Vite
- Axios
- React Router

---

## Status

O projeto está em desenvolvimento, com backend e frontend já integrados.

### Próximas etapas

- Dockerização da aplicação;
- documentação OpenAPI / Swagger;
- migrações de banco;
- melhorias no painel administrativo;
- notificações;
- chat com inteligência artificial.
