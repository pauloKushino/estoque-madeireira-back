# Evidências de funcionamento

Capturas feitas em 23/11/2026 com a API rodando localmente (`java -jar`, MySQL 8.4 em Docker, banco recém-criado pelas migrations V1–V4) e o frontend Angular em modo de desenvolvimento.

## API (Swagger UI — `http://localhost:8080/swagger-ui.html`)

| # | Arquivo | O que mostra | Status |
|---|---|---|---|
| 01 | [01-swagger-visao-geral.png](01-swagger-visao-geral.png) | Todos os endpoints documentados no Swagger | — |
| 02 | [02-produtos-listar-filtro.png](02-produtos-listar-filtro.png) | `GET /produtos?nome=pinus`: lista filtrada por nome | 200 |
| 03 | [03-produtos-consultar-estoque.png](03-produtos-consultar-estoque.png) | `GET /produtos/5/estoque`: saldo do Dormente de Acácia (8 un) | 200 |
| 04 | [04-produtos-buscar-404.png](04-produtos-buscar-404.png) | `GET /produtos/999`: produto inexistente, erro padronizado | 404 |
| 05 | [05-produtos-cadastrar-201.png](05-produtos-cadastrar-201.png) | `POST /produtos`: cadastro de produto | 201 |
| 06 | [06-produtos-cadastrar-400-validacao.png](06-produtos-cadastrar-400-validacao.png) | `POST /produtos` com nome vazio, estoque negativo e preço zero | 400 |
| 07 | [07-produtos-atualizar.png](07-produtos-atualizar.png) | `PUT /produtos/7`: alteração de preço | 200 |
| 08 | [08-clientes-listar-filtro.png](08-clientes-listar-filtro.png) | `GET /clientes?nome=marcenaria` | 200 |
| 09 | [09-clientes-cadastrar-201.png](09-clientes-cadastrar-201.png) | `POST /clientes` com CNPJ válido | 201 |
| 10 | [10-clientes-cadastrar-400-cpf-invalido.png](10-clientes-cadastrar-400-cpf-invalido.png) | `POST /clientes` com CPF de dígito verificador errado | 400 |
| 11 | [11-enderecos-consultar-cep.png](11-enderecos-consultar-cep.png) | `GET /enderecos/85853000`: endereço vindo da ViaCEP | 200 |
| 12 | [12-enderecos-cep-inexistente-404.png](12-enderecos-cep-inexistente-404.png) | `GET /enderecos/99999999`: CEP inexistente | 404 |
| 13 | [13-vendas-registrar-201.png](13-vendas-registrar-201.png) | `POST /vendas`: 15 m de Viga de Eucalipto + 40 Ripões de Cedro | 201 |
| 14 | [14-vendas-estoque-insuficiente-422.png](14-vendas-estoque-insuficiente-422.png) | `POST /vendas` pedindo 9 Dormentes (há 8): venda inteira recusada | 422 |
| 15 | [15-estoque-apos-venda-e-rollback.png](15-estoque-apos-venda-e-rollback.png) | `GET /produtos/3/estoque`: Ripão com 160 (200 − 40 vendidos). Os 5 Ripões da venda recusada no print 14 **não** foram debitados | 200 |
| 16 | [16-vendas-buscar-por-id.png](16-vendas-buscar-por-id.png) | `GET /vendas/2`: venda com itens, preço congelado e subtotais | 200 |
| 17 | [17-produtos-excluir-204.png](17-produtos-excluir-204.png) | `DELETE /produtos/7` | 204 |

## Interface web (Angular)

| # | Arquivo | O que mostra |
|---|---|---|
| 20 | [20-tela-login.png](20-tela-login.png) | Tela de login (acesso de demonstração) |
| 21 | [21-tela-produtos-estoque-baixo.png](21-tela-produtos-estoque-baixo.png) | Lista de produtos com o selo "Estoque baixo" no Dormente de Acácia (8 < mínimo 10) |
| 22 | [22-tela-clientes.png](22-tela-clientes.png) | Lista de clientes |
| 23 | [23-tela-vendas.png](23-tela-vendas.png) | Histórico de vendas |
| 24 | [24-tela-nova-venda.png](24-tela-nova-venda.png) | Nova venda com dois itens, subtotais e total calculados na hora |
| 25 | [25-tela-venda-registrada.png](25-tela-venda-registrada.png) | Confirmação "Venda registrada", total R$ 800,80 |

Os testes automatizados também servem de evidência: `.\mvnw.cmd verify` executa 100 testes (0 falhas) e gera o relatório de cobertura em `target/site/jacoco/index.html` (99% das linhas).
