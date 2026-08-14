# Frontend do Segundão System

Primeira versão do frontend React, Vite e TypeScript do Segundão System. A interface consome a API Spring Boot executada em `http://localhost:8080` por padrão.

## Executar

```bash
cp .env.example .env
npm install
npm run dev
```

Configure `VITE_API_URL` no `.env` quando o backend estiver em outro endereço. O arquivo `.env` não deve ser versionado.

## Validar

```bash
npm run lint
npm run build
```

O login usa o JWT da API. Contas com `mustChangePassword` são direcionadas para a troca obrigatória de senha antes do acesso às páginas autenticadas.
