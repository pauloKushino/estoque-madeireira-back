# Estoque Madeireira — Backend

![CI/CD](https://github.com/pauloKushino/estoque-madeireira-back/actions/workflows/ci-cd.yml/badge.svg)
![Coverage](https://img.shields.io/badge/Coverage_gate-90%25-brightgreen)

API REST para controle de estoque e vendas de uma madeireira, desenvolvida como projeto avaliativo.

## Stack

- Java 21 + Spring Boot 4.1.1
- Spring Data JPA (Hibernate), Bean Validation
- MySQL 8.4 em Docker (docker-compose)
- Flyway (migrations versionadas — `ddl-auto: validate`, nada de `update`)
- Spring Cloud OpenFeign (integração com a API externa ViaCEP)
- SpringDoc OpenAPI (Swagger UI)
- Tratamento global de erros com `@RestControllerAdvice`

## Como rodar

Pré-requisitos: Docker Desktop e Java 21.

```powershell
docker compose up -d          # sobe o MySQL (banco estoque_madeireira, porta 3306)
.\mvnw.cmd spring-boot:run    # sobe a API na porta 8080
```

Documentação interativa (Swagger): http://localhost:8080/swagger-ui.html

As migrations (V1–V5) criam as tabelas e carregam dados de exemplo automaticamente.

## Autenticação (JWT)

A API usa Spring Security com token JWT (HS256). Usuários de teste criados pela migration V5:

| Usuário | Senha | Perfil |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `user` | `user123` | USER |

1. Faça login para receber o token:

```powershell
curl.exe -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{\"username\":\"admin\",\"senha\":\"admin123\"}'
```

Resposta: `{ "token": "...", "expiraEm": 7200, "username": "admin", "nome": "Administrador", "role": "ADMIN" }` (`expiraEm` em segundos).

2. Envie o token no header `Authorization` das próximas requisições:

```powershell
curl.exe -s http://localhost:8080/auth/me -H "Authorization: Bearer <TOKEN>"
```

Login errado devolve **401**. Variáveis de ambiente: `JWT_SECRET` (mínimo de 32 caracteres; **obrigatório definir em produção**) e `JWT_EXPIRACAO_MINUTOS` (padrão 120).

## Testes

```powershell
.\mvnw.cmd verify    # testes unitarios + integracao + checagem de cobertura JaCoCo (minimo 90%)
```

- **Unitários** (JUnit 5 + Mockito): `src/test/.../service` e `src/test/.../validation`
- **Integração** (`@SpringBootTest` + MockMvc, H2 com as mesmas migrations do Flyway): `src/test/.../integration`
- Relatório de cobertura: `target/site/jacoco/index.html`

## Teste de carga

Plano do JMeter, como executar, resultados e análise em [`carga/`](carga/README.md).

## Demo

A API ainda não tem deploy público. Para rodar localmente, use `docker compose up -d` e `.\mvnw.cmd spring-boot:run`; o Swagger fica em http://localhost:8080/swagger-ui.html.

Para publicar a API, configure o secret `RAILWAY_TOKEN` e a variável de repositório `DEPLOY_ENABLED=true`.

## Endpoints principais

| Módulo | Endpoints |
|---|---|
| Autenticação | `POST /auth/login` (público), `GET /auth/me` (exige token) |
| Produtos | `GET/POST /produtos`, `GET/PUT/DELETE /produtos/{id}`, `GET /produtos/{id}/estoque` |
| Clientes | `GET/POST /clientes`, `GET/PUT/DELETE /clientes/{id}` (com validação de CPF/CNPJ) |
| Vendas | `GET /vendas`, `GET /vendas/{id}`, `POST /vendas` (baixa de estoque em transação atômica) |
| Endereços | `GET /enderecos/{cep}` (integração ViaCEP via OpenFeign) |

Filtros: `GET /produtos?nome=...` e `GET /clientes?nome=...`

## Regras de negócio

- Toda venda valida o estoque de **todos** os itens antes de persistir; se algum produto não tiver quantidade suficiente, a venda inteira é rejeitada (HTTP 422) e nada é debitado.
- Estoque nunca fica negativo (validação no service, trava na entidade e `CHECK` no banco).
- Preço do item é congelado no momento da venda (`precoUnitarioNoMomento`).

## Estrutura

```
src/main/java/com/pi/estoquemadeireira/
├── config/       (CORS, OpenAPI)
├── controller/   (REST + anotações Swagger)
├── service/      (regras de negócio, @Transactional, logs SLF4J)
├── repository/   (Spring Data JPA)
├── dto/          (Request/Response + mappers manuais)
├── entity/       (JPA)
├── exception/    (@RestControllerAdvice + ApiError padronizado)
├── client/       (Feign: ViaCEP)
└── validation/   (@CpfCnpj customizado)
src/main/resources/db/migration/  (Flyway V1–V4)
```

## Frontend

Interface Angular 19 em repositório separado:
https://github.com/pauloKushino/estoque-madeireira-front