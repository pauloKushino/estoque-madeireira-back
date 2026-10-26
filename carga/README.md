# Teste de carga (JMeter)

Plano: [`estoque-madeireira.jmx`](estoque-madeireira.jmx) · Relatório HTML: `resultados/dashboard/index.html`

## Cenários

Os cenários rodam **em sequência** (um termina, o próximo começa), para que cada um seja medido isolado.

| Cenário | Usuários (threads) | Ramp-up | Duração | Requisições por iteração | Tempo de pensar |
|---|---|---|---|---|---|
| **setUp** – massa de dados | 1 | – | 1 iteração | cria 1 cliente e 2 produtos com estoque 1.000.000 | – |
| **1 – Consulta de catálogo** (leitura) | 50 | 10 s | 60 s | `GET /produtos`, `GET /produtos/{id}`, `GET /produtos/{id}/estoque`, `GET /produtos?nome=pinus`, `GET /clientes` | 100–300 ms |
| **2 – Registro de vendas** (escrita concorrente) | 20 | 10 s | 60 s | `POST /vendas` (2 itens, sempre os **mesmos** 2 produtos) + `GET /vendas/{id}` | 100–300 ms |
| **3 – Pico misto** (estresse) | 200 | 20 s | 60 s | `GET /produtos`, `GET /produtos/{id}/estoque`, `POST /vendas` e, em 10% das iterações, `GET /vendas` | 50–150 ms |
| **tearDown** – consistência | 1 | – | 1 iteração | confere se `estoque final = estoque inicial − vendas aprovadas` | – |

Critérios de sucesso: asserção de status HTTP (200 nas consultas, 201 na criação). O endpoint `/enderecos/{cep}` ficou **fora** de propósito: ele chama a API pública do ViaCEP, e o teste mediria (e sobrecarregaria) um serviço de terceiros.

## Como executar

```powershell
docker compose up -d
# API apontando para um banco separado, para nao mexer nos dados de desenvolvimento
java -jar target\estoque-madeireira-0.0.1-SNAPSHOT.jar --server.port=8081 "--spring.datasource.url=jdbc:mysql://localhost:3306/estoque_madeireira_carga?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true"

# Em outro terminal (modo CLI, recomendado para carga)
jmeter -n -t carga\estoque-madeireira.jmx -Jport=8081 -l carga\resultados\resultados.jtl -e -o carga\resultados\dashboard
```

Os parâmetros de cada cenário podem ser alterados sem editar o plano: `-Jleitura.usuarios=100 -Jpico.duracao=120` etc. (veja os `${__P(...)}` no `.jmx`).

## Ambiente da execução (25/10/2026)

- API: Spring Boot 4.1.1, JDK 21.0.12, configuração padrão (Tomcat 200 threads, HikariCP com 10 conexões)
- Banco: MySQL 8.4 em Docker Desktop (WSL2)
- JMeter 5.6.3 em modo CLI
- Tudo na **mesma máquina**: Intel Core i5-6500 (4 núcleos), 12 GB RAM. O JMeter disputa CPU com a API e o banco, então os números absolutos são conservadores; o que vale é a comparação entre cenários.

## Resultados

### Por cenário

| Cenário | Requisições | Vazão | Média | p95 | Máximo | Erros |
|---|---:|---:|---:|---:|---:|---:|
| 1 – Leitura (50 usuários) | 11.497 | **192 req/s** | 38 ms | 166 ms | 583 ms | **0%** |
| 2 – Vendas (20 usuários) | 4.328 | 72 req/s | 54 ms | 168 ms | 445 ms | **24,5%** |
| 3 – Pico (200 usuários) | 2.374 | **30 req/s** | **4.772 ms** | **13.747 ms** | 24.876 ms | 3,6% |

### Por endpoint

| Endpoint | Amostras | Erros | Média | Mediana | p90 | p95 | p99 | Máx |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| C1 `GET /produtos` | 2.317 | 0% | 38 | 14 | 89 | 173 | 344 | 527 |
| C1 `GET /produtos/{id}` | 2.310 | 0% | 36 | 13 | 84 | 161 | 393 | 518 |
| C1 `GET /produtos/{id}/estoque` | 2.301 | 0% | 36 | 13 | 86 | 161 | 373 | 501 |
| C1 `GET /produtos?nome=` | 2.287 | 0% | 40 | 15 | 99 | 179 | 390 | 583 |
| C1 `GET /clientes` | 2.282 | 0% | 37 | 13 | 87 | 168 | 397 | 493 |
| C2 `POST /vendas` | 2.698 | **39,3%** | 66 | 45 | 138 | 197 | 305 | 445 |
| C2 `GET /vendas/{id}` | 1.630 | 0% | 32 | 18 | 66 | 99 | 227 | 390 |
| C3 `GET /produtos` | 833 | 0% | 4.285 | 4.194 | 8.730 | 12.634 | 15.314 | 15.583 |
| C3 `GET /produtos/{id}/estoque` | 778 | 0% | 4.735 | 4.308 | 11.567 | 13.611 | 15.466 | 15.610 |
| C3 `POST /vendas` | 698 | 12,3% | 4.498 | 4.400 | 7.739 | 13.503 | 15.465 | 15.734 |
| C3 `GET /vendas` (lista completa) | 65 | 0% | **14.387** | 13.328 | 23.384 | 24.271 | 24.876 | 24.876 |

Tempos em milissegundos.

### Consistência do estoque (tearDown)

```
Vendas aprovadas (201): 2250 | estoque esperado: 997750 | estoque no banco: 998606 | baixas perdidas: 856
```

## Análise

### 1. Leitura: saudável

Com 50 usuários simultâneos as consultas ficaram com média de ~38 ms, p95 abaixo de 180 ms e **nenhum erro**, sustentando ~192 req/s. Para o porte de uma madeireira (poucos vendedores e consultas de balcão), a API de consulta está folgada.

### 2. Vendas concorrentes: problema de concorrência (o achado mais importante)

**a) Baixas de estoque perdidas (*lost update*).** Foram aprovadas 2.250 vendas de 1 unidade, mas o estoque do produto caiu só 1.394: **856 baixas sumiram (38%)**. O cliente recebeu `201 Created`, a venda foi gravada, mas o estoque não foi debitado. Na prática o sistema vende mercadoria que não existe e o estoque fica maior que o real, quebrando a regra "estoque nunca negativo".

Causa: o `VendaService.criar()` faz **ler → subtrair em memória → gravar** (`findById` + `debitarEstoque` + `UPDATE produto SET quantidade_estoque = ?`) sem nenhuma trava. Duas vendas simultâneas leem o mesmo saldo (ex.: 100), cada uma calcula 99 e a segunda sobrescreve a primeira. A entidade `Produto` não tem `@Version` (lock otimista) e o repositório não usa `SELECT ... FOR UPDATE` (lock pessimista).

**b) Deadlocks no MySQL → HTTP 500.** 1.146 vendas (39% no cenário 2) falharam com `Deadlock found when trying to get lock` (`CannotAcquireLockException` no log da API). No commit, o Hibernate executa primeiro os `INSERT` (venda e itens) e só depois o `UPDATE` do estoque. O `INSERT` em `item_venda` faz o InnoDB pegar um lock **compartilhado** na linha do produto (checagem da FK); em seguida o `UPDATE produto` precisa de lock **exclusivo** na mesma linha. Com duas vendas simultâneas, as duas seguram o compartilhado e esperam o exclusivo uma da outra: deadlock. O MySQL aborta uma delas e a API devolve **500**, quando o certo seria repetir a operação ou ao menos responder 409.

Correções sugeridas:
- Buscar os produtos da venda com **lock pessimista** (`@Lock(PESSIMISTIC_WRITE)` em um método do `ProdutoRepository`), **sempre na mesma ordem de id**: o lock exclusivo é obtido já na leitura, o que elimina a perda de baixa e a troca de lock compartilhado → exclusivo que causa o deadlock.
- Ou fazer a baixa de forma atômica no banco: `UPDATE produto SET quantidade_estoque = quantidade_estoque - :qtd WHERE id = :id AND quantidade_estoque >= :qtd`, verificando as linhas afetadas.
- Tratar `CannotAcquireLockException`/`PessimisticLockingFailureException` no `GlobalExceptionHandler` (409) ou com retry.

### 3. Pico: gargalo no pool de conexões e na listagem de vendas

Com 200 usuários a vazão **caiu** de 192 para 30 req/s e até a consulta mais simples (`GET /produtos`, que leva 14 ms em condições normais) passou para **4,3 s de média e 12,6 s no p95**. Quando um endpoint trivial fica 300× mais lento, o tempo está sendo gasto **na fila**, não processando:

- **Pool de conexões (HikariCP, padrão 10 conexões)** para até 200 requisições simultâneas no Tomcat: cada requisição espera uma conexão livre.
- **`GET /vendas` sem paginação + N+1.** O endpoint devolve **todas** as vendas (mais de 2.000 ao final do teste) e o `VendaMapper` acessa `venda.getItens()` (relação *lazy*) uma a uma, gerando uma consulta extra por venda. Cada chamada levou de **13 a 25 s**, segurando uma das 10 conexões o tempo todo e travando a fila para os demais. O tempo desse endpoint cresce junto com a base de vendas.
- CPU compartilhada entre JMeter, API e MySQL na mesma máquina de 4 núcleos (contribui, mas não explica sozinha a diferença entre 14 ms e 4 s).

Os erros caíram para 12% em `POST /vendas` no pico justamente porque as requisições ficaram enfileiradas no pool: menos transações chegavam ao banco ao mesmo tempo, então houve menos deadlocks.

Correções sugeridas:
- Paginar `GET /vendas` (`Pageable` no controller/repositório) e carregar os itens junto (`@EntityGraph(attributePaths = "itens")` ou `JOIN FETCH`, ou `hibernate.default_batch_fetch_size`).
- Ajustar `spring.datasource.hikari.maximum-pool-size` de acordo com o hardware do banco (com moderação: mais conexões não resolvem consultas lentas).
- Rodar o JMeter em outra máquina para medições de capacidade mais precisas.

## Resumo

| Item | Situação |
|---|---|
| Consultas (leitura) | Boas: ~38 ms de média, 0% de erro com 50 usuários |
| Integridade do estoque sob concorrência | **Falha**: 38% das baixas perdidas (lost update) |
| Vendas concorrentes | **Falha**: 39% de HTTP 500 por deadlock com 20 usuários |
| Escalabilidade no pico (200 usuários) | Degrada: p95 de ~13 s, vazão cai 84% |
| Principais gargalos | Ausência de lock na baixa de estoque; `GET /vendas` sem paginação e com N+1; pool de 10 conexões |
