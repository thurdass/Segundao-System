# 2A System

API REST do 2º Informática A, preparada para organizar atividades, avisos, disciplinas, professores e horários da turma.

## Stack e execução

Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA, MySQL, Bean Validation, Lombok, Spring Security, JWT e BCrypt.

Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (ao menos 32 caracteres) e opcionalmente `DDL_AUTO`. O padrão usa MySQL em `2a_system` e timezone `America/Sao_Paulo`. Execute com `./mvnw spring-boot:run` ou `./mvnw test`.

Em desenvolvimento, a aplicação cria uma turma inicial `2A / Informática / 2026 / Manhã`. A promoção do primeiro usuário para administrador deve ser feita de forma controlada no banco (`role = 'ADMIN'`); não há credencial administrativa padrão.

## Endpoints principais

- `POST /api/auth/login`, `GET /api/auth/me`, `PATCH /api/auth/password`
- `POST /api/admin/users`, `GET/PATCH /api/admin/users`, `GET /api/admin/users/{id}`
- `GET /api/subjects?classroomId=1`, `GET /api/teachers`, `GET /api/teachers/{id}/subjects`
- `GET /api/schedules/classroom/{id}`; alterações de catálogo exigem `ADMIN`
- `GET/POST /api/activities`, `POST/DELETE /api/activities/{id}/complete`; use `status=pending|completed`, `subjectId` e `dueBefore`
- `GET/POST/PUT/DELETE /api/announcements`
- `GET /api/subjects/{subjectId}/next-class` consulta a próxima ocorrência da disciplina da turma do aluno autenticado
- `GET/PATCH /api/admin/users`, `GET /api/admin/users/{id}`, `GET /api/admin/dashboard`

Envie o token como `Authorization: Bearer <jwt>`. Alunos só enxergam dados da própria turma; criadores editam/removem seus conteúdos e administradores podem gerenciar qualquer conteúdo. Conclusões de atividades são individuais. Ausência ou falha de autenticação retorna `401`; falta de permissão retorna `403`.

## Contas gerenciadas pelo administrador

Não existe cadastro público. Apenas um usuário com role `ADMIN` pode criar contas de alunos:

```text
POST /api/admin/users
```

Exemplo de requisição sem credenciais reais:

```json
{
  "username": "arthur",
  "password": "senhaInicialFicticia",
  "displayName": "Arthur Almeida",
  "role": "STUDENT",
  "classroomId": 1
}
```

O username é normalizado para lowercase, precisa ser único e a senha inicial é armazenada com BCrypt. A resposta nunca contém a senha nem o hash. O endpoint exige `ADMIN`; usuários não autenticados ou estudantes não podem criar contas.

Contas criadas pelo administrador começam com `mustChangePassword: true`. O aluno consegue fazer o login inicial, mas deve trocar a senha antes de acessar atividades, avisos, horários e demais recursos normais:

```text
PATCH /api/auth/password
```

```json
{
  "currentPassword": "senhaInicialFicticia",
  "newPassword": "novaSenhaFicticia"
}
```

Enquanto a troca for obrigatória, apenas `GET /api/auth/me` e `PATCH /api/auth/password` ficam disponíveis, além do login. Depois da troca, `mustChangePassword` passa a `false` e o acesso normal é liberado. O login e o `/api/auth/me` informam esse estado sem expor qualquer senha.

Exemplo de consulta da próxima aula:

```text
GET /api/subjects/1/next-class
Authorization: Bearer <jwt>
```

```json
{
  "subjectId": 1,
  "subjectName": "Desenvolvimento Web",
  "nextClassDate": "2026-08-17",
  "dayOfWeek": "MONDAY",
  "startTime": "09:10:00",
  "endTime": "10:00:00"
}
```

Os testes usam H2 em memória e cobrem autenticação, criação administrativa de usuários, login inicial, troca obrigatória de senha, JWT, atividades, conclusão individual, filtros, permissões STUDENT/ADMIN, horários e os endpoints de próxima aula e administração. Execute `./mvnw clean test` para reproduzi-los.

## Estado e próximos passos

O backend MVP está implementado sem frontend. A integração de chat com IA, histórico, limites de uso e troca segura de provedor será uma etapa futura; nenhuma chave ou API de IA é usada no repositório. Também ficam para evolução a auditoria segura de acessos, migrações de banco, documentação OpenAPI e preparação formal do contrato para o frontend. `AccessLog` ainda não está implementado.
