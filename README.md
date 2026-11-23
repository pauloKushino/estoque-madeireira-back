# Estoque Madeireira — Backend

![CI/CD](https://github.com/pauloKushino/estoque-madeireira-back/actions/workflows/ci-cd.yml/badge.svg)
![Coverage](https://img.shields.io/badge/Coverage_gate-90%25-brightgreen)

API REST para controle de estoque e vendas de uma madeireira, desenvolvida no Projeto Integrador de Extensão (PIE IV — Spring Boot + Problema Real) do Centro Universitário Descomplica Uniamérica.

## Projeto

| | |
|---|---|
| **Projeto** | Estoque Madeireira |
| **Demandante** | Castelo de Madeira |
| **Integrantes** | Felipe Klaus, Erick Mertz, Paulo Kushino e Wendell Klering |
| **Professora** | Isabel Fernandes |
| **Frontend** | https://github.com/pauloKushino/estoque-madeireira-front |

### Problema

Na Castelo de Madeira, o saldo de estoque, as vendas e os dados dos clientes ficavam espalhados entre controles manuais, planilhas e comunicação verbal. Isso gera divergência entre o saldo registrado e o que está no pátio, venda de produto sem saldo e dificuldade para saber quais itens estão abaixo do mínimo. Como a madeira é vendida em unidades diferentes (m³, metro linear e unidade), o controle manual fica ainda mais sujeito a erro.

### Solução

Uma API REST em Spring Boot, consumida por uma interface web em Angular, que centraliza:

- **Produtos e estoque**: cadastro com unidade de medida, preço, saldo e estoque mínimo; consulta de saldo por produto e indicador de estoque baixo na interface.
- **Clientes**: cadastro com validação de CPF/CNPJ e preenchimento automático do endereço pelo CEP (ViaCEP).
- **Vendas**: registro de venda com vários itens; o estoque é validado e debitado na mesma transação, e a venda inteira é recusada se algum item não tiver saldo.

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

As migrations (V1–V4) criam as tabelas e carregam dados de exemplo automaticamente.

## Testes

```powershell
.\mvnw.cmd verify    # testes unitarios + integracao + checagem de cobertura JaCoCo (minimo 90%)
```

- **Unitários** (JUnit 5 + Mockito): `src/test/.../service` e `src/test/.../validation`
- **Integração** (`@SpringBootTest` + MockMvc, H2 com as mesmas migrations do Flyway): `src/test/.../integration`
- Relatório de cobertura: `target/site/jacoco/index.html`

## Documentação e evidências

- Documentação completa (requisitos atendidos e trabalhos futuros, modelo de dados, diagramas e endpoints com exemplos): [`docs/DOCUMENTACAO.md`](docs/DOCUMENTACAO.md)
- Prints da API (Swagger) e das telas em funcionamento: [`docs/evidencias/`](docs/evidencias/README.md)

## Teste de carga

Plano do JMeter, como executar, resultados e análise em [`carga/`](carga/README.md).

## Demo

A API ainda não tem deploy público. Para rodar localmente, use `docker compose up -d` e `.\mvnw.cmd spring-boot:run`; o Swagger fica em http://localhost:8080/swagger-ui.html.

Para publicar a API, configure o secret `RAILWAY_TOKEN` e a variável de repositório `DEPLOY_ENABLED=true`.

## Endpoints principais

| Módulo | Endpoints |
|---|---|
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