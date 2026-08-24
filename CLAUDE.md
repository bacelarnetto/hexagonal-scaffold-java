# hexagonal-scaffold-java — Instruções para assistentes de IA

Este arquivo é lido automaticamente por assistentes compatíveis (Claude Code e outros que seguem a
mesma convenção). Se você clonou este repo para começar um projeto novo, mantenha este arquivo —
adapte só os nomes de pacote/domínio conforme for renomeando.

Este projeto é a versão Java do [`hexagonal-scaffold-kotlin`](https://github.com/bacelarnetto/hexagonal-scaffold-kotlin)
— mesma arquitetura, mesmo módulo de exemplo, mesma pirâmide de testes. Diferenças deliberadas de
linguagem/lib estão marcadas ao longo deste arquivo.

## Documentação de referência

Antes de qualquer decisão de arquitetura, consulte `doc/guide/` (docsify — `npx serve doc/guide`):
Arquitetura, Módulo de exemplo, Pirâmide de testes, Prompting e CLAUDE.md, Skill de bootstrap,
Docker e Kubernetes.

## Stack

- Java 21 + Spring Boot 3.5.3 · Maven multi-módulo (4 módulos físicos) · MySQL (H2 só no scaffold,
  sem infra) · Flyway · Spring Data JPA · Undertow · Lombok (só em `infrastructure/`) · Actuator
  (probes de K8s)
- Deploy: `docker/Dockerfile` (multi-stage) + `kubernetes/` (Deployment/Service/ConfigMap/Secret),
  voltado para GKE/GCP — ver `doc/guide/deploy.md`

## Arquitetura — regras obrigatórias

### Os 4 módulos Maven

```
domain/          Java puro (só JDK). pom.xml não tem Spring/JPA/Lombok como dependência — não tem
                 como vazar framework pra dentro por acidente.
infrastructure/  @Entity, JpaRepository, @Repository adapter. Depende só de domain.
application/     @Service/@Transactional, @RestController, DTOs, mappers. Depende de domain + infrastructure.
starter/         @SpringBootApplication + application.properties. Depende só de application.
```

Nunca adicione Spring/JPA/Lombok como dependência de `domain/pom.xml`. Se uma classe em `domain/`
"precisa" de Spring, ela não pertence a `domain/`.

### Records vs. Lombok — onde usar cada um

- `domain/model/`, DTOs de `application/dto/` e `shared/*/dto/`: **Java `record`**. Imutável,
  `equals`/`hashCode`/`toString` gerados pelo compilador, zero dependência externa — é o
  equivalente Java da `data class` do Kotlin, e por isso mantém `domain/` sem nenhuma lib além do
  JDK. Para "copiar com um campo alterado" (o `copy()` do Kotlin), escreva um método `comX(...)`
  explícito no record em vez de introduzir uma lib de terceiros (ver `Produto.comValorVenda()`).
- `infrastructure/entity/` (`@Entity`): classe Lombok (`@Getter @Setter @NoArgsConstructor
  @AllArgsConstructor @Builder`) — JPA precisa de construtor sem args e idealmente de campos
  mutáveis para o proxy do Hibernate; records não se prestam bem a isso. Lombok fica **restrito a
  `infrastructure/`** — não é dependência de `domain/` nem de `application/`.

### Dois tipos de "service"

- `domain/service/XxxLogic.java` — sem `@Service`, sem `@Transactional`. Recebe ports por
  construtor (ou nenhuma dependência, se for cálculo/validação pura). Contém a regra de negócio.
  Nunca usa `Instant.now()` internamente — recebe como parâmetro. Nunca lança exceção HTTP — só
  `RegraDeNegocioException` (`shared/domain/exception/`) ou exceções padrão de Java.
- `application/service/XxxService.java` — `@Service`, orquestra. Instancia o domain service direto
  (`= new XxxLogic()`) se ele não tiver dependências; senão recebe por construtor. Não contém regra
  de negócio, só orquestração + transação.

**Antes de criar um `domain/service/` novo**, confirme que existe lógica pura real a extrair — CRUD
simples sem validação/cálculo não ganha nada com essa separação (não crie a pasta só por hábito).

### Transação e rollback — onde isso mora

`@Transactional` é Spring, então só pode estar em `application/service/`, nunca em
`domain/service/`. O desafio prático: como uma regra de negócio que vive num módulo sem Spring
consegue disparar rollback de uma transação Spring? Resposta: ela não dispara rollback
diretamente — ela só lança uma exceção normal (`RegraDeNegocioException` ou outra unchecked).
Quem transforma essa exceção em rollback é o proxy AOP por trás do `@Transactional` do método
`application/service/` que a chamou: por padrão, qualquer `RuntimeException` que escapa de um
método `@Transactional` marca a transação para rollback. O domínio não precisa saber que Spring
existe; o rollback é inteiramente uma decisão de `application/`.

Exemplo real no scaffold: `ProdutoService.cadastrarEmLote()` — processa uma lista de produtos numa
única transação; se o item N tiver margem inválida, `ProdutoPrecoLogic` (domain) lança
`RegraDeNegocioException` normalmente, sem saber que está dentro de uma transação. Como o método
inteiro está anotado `@Transactional`, os itens 1..N-1 já enviados a `port.save()` **também são
revertidos**, porque ainda fazem parte da mesma transação JDBC quando ela é desfeita — mesmo já
tendo "sido salvos" do ponto de vista do código Java. Prova de que isso funciona de verdade só é
possível com banco real (ver `ProdutoControllerIntegrationTest.pci3_...`, `doc/guide/testes.md`) —
um teste com mock não revela nada sobre transação, porque o mock não participa de commit/rollback.

**Cuidado com self-invocation:** se um método `@Transactional` chamar outro método `@Transactional`
da mesma classe (`this.outroMetodo(...)`), o proxy Spring é contornado e a segunda anotação não tem
efeito — os dois acabam na mesma transação do método externo (ou nenhuma, se nenhum dos dois foi
chamado de fora do bean). É por isso que `cadastrarEmLote()` não reaproveita `cadastrar()` por
dentro — usa um método privado (`cadastrarItemDoLote`, sem `@Transactional`) para o passo comum,
em vez de encadear dois métodos `@Transactional` públicos da mesma classe.

**`@Transactional` não cobre orquestração multi-serviço (ex: Temporal.io).** Todo esse mecanismo de
rollback depende de uma única transação JDBC compartilhada entre as chamadas. Um processo orquestrado
por um Workflow (Temporal ou similar), onde cada Activity pode tocar um serviço/banco diferente, não
tem essa transação única pra reverter — `@Transactional` simplesmente não se aplica entre Activities.
Compensação nesse cenário é responsabilidade explícita (Saga pattern do próprio orquestrador, ou
Activities de compensação escritas à mão), não um efeito colateral de anotação. Ver `TODO.md`.

### Mapeamento entity ↔ domain

Sempre explícito (`toDomain()`/`toEntity()` escritos à mão no adapter). Nunca
`BeanUtils.copyProperties()` — falha silenciosamente em runtime quando os nomes de campo divergem.

### Referências cross-module

Se este projeto crescer para vários domínios de negócio dentro do mesmo módulo (ex: `pedido` e
`produto` dentro de `application/`), comunicação entre eles é por **ID (Long)** — nunca import de
domain/entity de um domínio de negócio dentro de outro.

## Padrões de código

- Controllers: `@RestController` + `@RequestMapping`, `ResponseEntity<ViewDTO>`, `201 Created` no POST
- Delete: `@ResponseStatus(HttpStatus.NO_CONTENT)`
- Mapper base: `Mapper<In, Out>` (`@FunctionalInterface`, em `application/`)
- Ports que podem não achar um registro retornam `Optional<T>` (não `null`) — ex:
  `ProdutoRepositoryPort.findById(Long id): Optional<Produto>`
- Campos `id` são `Long` (não primitivo); datas são `Instant`
- `Serializable` + `serialVersionUID = 1L` em domain models (records) e entities

## Testes — pirâmide obrigatória (ver `doc/guide/testes.md`)

1. `domain/` — JUnit5 puro, zero mock, zero Spring. A maioria dos testes deveria estar aqui.
2. `application/` — JUnit5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`), mocka só a
   porta. **Sempre chame o método real do service primeiro, verifique o mock/resultado depois** —
   nunca invoque o mock diretamente no corpo do teste como se fosse o código em teste (isso faz o
   teste passar não importa o que o service faça).
3. `starter/` — `@SpringBootTest` + MockMvc + banco real (Testcontainers em projeto com MySQL). Um
   teste de integração por controller basta; casos de borda de regra de negócio vão no nível 1.

Uma mudança só está pronta depois que `mvn test` passa — compilar não é suficiente.

## Regra de ouro — atualização obrigatória de documentação

Sempre que criar, alterar ou planejar algo relevante (novo domínio de negócio, nova migration,
decisão de arquitetura), atualize este `CLAUDE.md` sem precisar ser pedido. É o que garante que o
próximo prompt (seu ou de outra pessoa) não repita as mesmas perguntas.

## O que NÃO fazer

- Não adicionar Spring/JPA/Lombok/qualquer framework como dependência de `domain/pom.xml`
- Não usar `BeanUtils.copyProperties()` nos adapters
- Não criar `domain/service/` para lógica que é só CRUD
- Não usar Lombok em `domain/` ou `application/` — fica restrito a `infrastructure/`
- Não considerar uma tarefa concluída sem rodar `mvn test`
- Não colocar o banco de dados rodando dentro do Kubernetes (Deployment + PVC) — no GCP o banco é
  gerenciado (Cloud SQL), acessado via Cloud SQL Auth Proxy como sidecar; ver `doc/guide/deploy.md`
- Não commitar `kubernetes/secret.yaml` com credenciais reais — só `secret.example.yaml` (template)
  fica versionado; o real já está no `.gitignore`
