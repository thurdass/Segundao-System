# 2A System

API REST do 2º Informática A, preparada para organizar atividades, avisos, disciplinas, professores e horários da turma.

## Stack e execução

Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA, MySQL, Bean Validation, Lombok, Spring Security, JWT e BCrypt.

Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (ao menos 32 caracteres) e opcionalmente `DDL_AUTO`. O padrão usa MySQL em `2a_system` e timezone `America/Sao_Paulo`. Execute com `./mvnw spring-boot:run` ou `./mvnw test`.

Em desenvolvimento, a aplicação cria uma turma inicial `2A / Informática / 2026 / Manhã`. O cadastro recebe o `classroomId` dessa turma. A promoção do primeiro usuário para administrador deve ser feita de forma controlada no banco (`role = 'ADMIN'`); não há credencial administrativa padrão.

## Endpoints principais

- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `GET /api/subjects?classroomId=1`, `GET /api/teachers`, `GET /api/teachers/{id}/subjects`
- `GET /api/schedules/classroom/{id}`; alterações de catálogo exigem `ADMIN`
- `GET/POST /api/activities`, `POST/DELETE /api/activities/{id}/complete`; use `status=pending|completed`, `subjectId` e `dueBefore`
- `GET/POST/PUT/DELETE /api/announcements`
- `GET /api/subjects/{subjectId}/next-class` consulta a próxima ocorrência da disciplina da turma do aluno autenticado
- `GET/PATCH /api/admin/users`, `GET /api/admin/users/{id}`, `GET /api/admin/dashboard`

Envie o token como `Authorization: Bearer <jwt>`. Alunos só enxergam dados da própria turma; criadores editam/removem seus conteúdos e administradores podem gerenciar qualquer conteúdo. Conclusões de atividades são individuais. Ausência ou falha de autenticação retorna `401`; falta de permissão retorna `403`.

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

Os testes usam H2 em memória e cobrem autenticação, registro, login, JWT, atividades, conclusão individual, filtros, permissões STUDENT/ADMIN, horários e os endpoints de próxima aula e administração. Execute `./mvnw clean test` para reproduzi-los.

## Estado e próximos passos

O backend MVP está implementado sem frontend. A integração de chat com IA, histórico, limites de uso e troca segura de provedor será uma etapa futura; nenhuma chave ou API de IA é usada no repositório. Também ficam para evolução a auditoria segura de acessos, migrações de banco, documentação OpenAPI e preparação formal do contrato para o frontend. `AccessLog` ainda não está implementado.
