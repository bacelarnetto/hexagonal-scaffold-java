# Naming Conventions — Guia Definitivo

Fonte de verdade para nomear classes neste scaffold e em qualquer projeto gerado a partir dele.
Não é um padrão genérico de mercado colado por cima — é a formalização do que os arquivos deste
próprio repositório já fazem (ver [Módulo de exemplo — produto](modulo-exemplo)). Onde este guia
diverge do vocabulário "Controller/Service/Repository" mais comum na indústria, é porque este
scaffold é hexagonal de propósito: `Port`, `Adapter` e `UseCase` são termos técnicos com significado
arquitetural específico aqui, não sinônimos estilísticos de "Repository"/"Service".

## Regras gerais

1. **PascalCase (UpperCamelCase) sempre** — sem exceção, inclusive em interfaces.
2. **Composição obrigatória: `[Domínio][PapelTécnico]`** — o domínio é o substantivo do bounded
   context (`Produto`, `Pedido`, `Pagamento`); o papel técnico é um dos sufixos da tabela abaixo.
   Uma classe sem domínio no nome (`Service`, `Handler`, `Manager` sozinhos) não diz nada sobre o
   que ela faz — é o primeiro sintoma de responsabilidade mal definida.
3. **Sufixos vagos são proibidos por padrão**: `Manager`, `Handler` (desacompanhado), `Processor`,
   `Util`/`Utils`, `Helper`, `Data`, `Info`, `Base` genérico. Não dizem qual é o papel arquitetural
   — só que "faz alguma coisa com". Toda exceção precisa de justificativa documentada (ver seção
   [Exceções documentadas](#exceções-documentadas) abaixo); não é "proibido para sempre", é
   "proibido por padrão, permitido com motivo escrito".
4. **O nome da classe é a primeira documentação.** Quem lê `CadastrarProdutoUseCase` sabe o que ela
   faz sem abrir o arquivo. Quem lê `ProdutoManager` não sabe se ela cadastra, valida, envia email
   ou as três coisas — e é exatamente esse "faz de tudo" que o sufixo `Manager` tende a acobertar.

## Tabela principal

| Camada / Papel | Sufixo obrigatório | Exemplo | Módulo | Responsabilidade / escopo |
|---|---|---|---|---|
| Modelo de domínio | *(nenhum — só o substantivo)* | `Produto` | `domain/model` | `record` imutável que representa o conceito de negócio. Zero framework — nem Lombok. |
| Porta de saída (Domain Port) | `Port` | `ProdutoRepositoryPort` | `domain/port` | Interface que o domínio define para depender de algo externo (banco, HTTP, mensageria) sem conhecer a tecnologia. Implementada em `infrastructure/`. |
| Lógica de domínio | `Logic` | `ProdutoPrecoLogic` | `domain/service` | Regra de negócio pura — sem `@Service`, sem I/O, testável sem Spring. Só existe se houver cálculo/validação real (ver `CLAUDE.md`). |
| Exceção de domínio | `Exception` | `RegraDeNegocioException` | `domain/exception` (shared) | Erro de regra de negócio. Não sabe o que é HTTP. |
| Caso de uso — contrato | `UseCase` | `CadastrarProdutoUseCase` | `application/usecase` | `@FunctionalInterface` com um único método `executar(...)`. Define o que o caso de uso faz, não como. Arquivo próprio (`XxxUseCase.java`) — Java não permite interface + classe pública no mesmo arquivo. |
| Caso de uso — implementação | `UseCaseImpl` | `CadastrarProdutoUseCaseImpl` | `application/usecase` | `@Service` (+ `@Transactional` quando escreve). Orquestra domain + infrastructure; nunca contém regra de negócio. Arquivo próprio (`XxxUseCaseImpl.java`). |
| Controller (adapter de entrada HTTP) | `Controller` | `ProdutoController` | `application/controller` | `@RestController`. Traduz HTTP ↔ chamada de `UseCase`. Não orquestra nem valida regra. |
| Consumer/Listener (adapter de entrada mensageria) | `EventConsumer` (ou `KafkaListener`/`SqsListener` se o time preferir nomear a tecnologia — ver nota) | `ProdutoEventConsumer` | `application/consumer` | Recebe mensagem, desserializa, chama um `UseCase`. É o mesmo papel arquitetural do Controller — só troca HTTP por um broker. |
| DTO de entrada | `InsertFormDTO` / `UpdateFormDTO` | `ProdutoInsertFormDTO` | `application/dto` | `record` com Bean Validation (`@NotBlank`, `@DecimalMin`...). Nunca reaproveita o model de domínio diretamente. |
| DTO de saída | `ViewDTO` | `ProdutoViewDTO` | `application/dto` | `record` de saída da API. |
| Mapper | `Mapper` | `ProdutoViewMapper` | `application/mapper` | Tradução explícita entre tipos (`Mapper<In, Out>`). Nunca `BeanUtils.copyProperties()`. |
| Exception handler global | `ExceptionHandler` | `GlobalExceptionHandler` | `application/exception` (shared) | `@RestControllerAdvice`. Traduz exceção de domínio/validação → resposta HTTP. |
| Entidade JPA | `Entity` | `ProdutoEntity` | `infrastructure/entity` | `@Entity`, classe Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`). Nunca vaza para fora do próprio adapter. |
| Repositório Spring Data | `JpaRepository` | `ProdutoJpaRepository` | `infrastructure/repository` | Interface `JpaRepository<Entity, Id>`. Acesso cru ao banco, sem regra. |
| Adapter de repositório | `RepositoryAdapter` | `ProdutoRepositoryAdapter` | `infrastructure/adapter` | `@Repository`. Implementa a `Port` do domínio; `toDomain()`/`toEntity()` explícitos. |
| Cliente de integração HTTP | `HttpClient` (genérico) ou `[SistemaExterno]Client` (nome próprio do sistema externo) | `PagamentoHttpClient` / `ViaCepClient` | `infrastructure/client` | Implementa uma `Port` de saída. Chama sistema externo via HTTP/Feign/RestClient. Ver nota abaixo sobre qual variante usar. |
| Producer/Publisher de eventos | `EventProducer` | `ProdutoEventProducer` | `infrastructure/messaging` | Implementa uma `Port` de saída. Publica evento (Kafka/RabbitMQ/SQS) depois que um `UseCase` termina. |
| Configuração de framework | `Config` | `SecurityConfig` | `starter/config` | `@Configuration`. Wiring de infraestrutura (beans, filtros, CORS). Nunca regra de negócio. |

## Onde `Consumer`/`Producer`/`HttpClient` entram nos 4 módulos

Não são uma 5ª camada — encaixam na mesma distinção que já existe entre **adapter que aciona a
aplicação** (*driving*) e **adapter que a aplicação aciona** (*driven*):

```
driving (entra na app, chama um UseCase)     -> application/
  Controller     (entrada HTTP)
  EventConsumer  (entrada mensageria)

driven (a app chama, implementa uma Port)    -> infrastructure/
  RepositoryAdapter  (saída banco)
  HttpClient         (saída HTTP/API externa)
  EventProducer      (saída mensageria)
```

`application/` já depende de Spring Web para o Controller (ver `CLAUDE.md`) — um `EventConsumer`
com `@KafkaListener` é o mesmo tipo de dependência de framework, então vive no mesmo lugar. Um
`HttpClient`/`EventProducer`, por outro lado, implementa uma `Port` definida em `domain/`, exatamente
como `ProdutoRepositoryAdapter` — por isso ficam em `infrastructure/`, nunca em `application/`.

**Nota — nome genérico vs. nome da tecnologia:** prefira `ProdutoEventConsumer`/`ProdutoEventProducer`
(genérico) quando a tecnologia de mensageria puder mudar sem reescrever a regra de negócio ao redor.
Use `ProdutoKafkaListener`/`ProdutoKafkaProducer` (nomeando a tecnologia) só quando o time decidir
que o acoplamento ao Kafka é permanente e prefere que isso fique explícito no nome — decisão de
time, documentada aqui se adotada, não uma regra que este scaffold impõe.

**Nota — `HttpClient` genérico vs. `[Sistema]Client`:** use `PagamentoHttpClient` quando o cliente
existe para satisfazer uma `Port` de domínio (`PagamentoPort`) — o nome reflete o conceito de
negócio que ele resolve. Use `ViaCepClient` quando é acesso a um sistema externo nomeado, sem
conceito de domínio próprio por trás (CEP não é um agregado deste sistema, é só um dado auxiliar).

## Exceções documentadas

O sufixo `Handler` é banido por padrão (vago — não diz o que é tratado) **exceto** em
`GlobalExceptionHandler`: aqui `Handler` está qualificado por `Exception`, o nome inteiro descreve
exatamente o papel (`@RestControllerAdvice` que trata exceções), e é o nome que o próprio
ecossistema Spring usa para esse conceito (`@ExceptionHandler`). Justificativa documentada = pode
usar. Qualquer outro `XxxHandler` sem esse nível de precisão continua proibido.

O padrão `interface + Impl` (`CadastrarProdutoUseCase` + `CadastrarProdutoUseCaseImpl`) é a única
duplicação de sufixo aceita neste guia — decorre de uma limitação da linguagem (Java não permite
duas classes públicas top-level no mesmo arquivo), não de indecisão de nomenclatura.

Se o time decidir abrir outra exceção no futuro (ex: um `Util` genuinamente sem estado e sem
alternativa melhor), documente aqui — motivo, escopo permitido, e por que não virou
`domain/service/XxxLogic`.

## Validação automática no CI

Regra sem enforcement automático é sugestão, não convenção. **Isto já está implementado e
rodando** — `starter/src/test/java/.../architecture/NamingConventionArchTest.java`, junto com
`mvn test`. Uma classe fora do padrão (ex: um `ProdutoManager` novo) quebra o build ali, não só a
revisão de código.

```java
// starter/src/test/java/br/com/scaffold/architecture/NamingConventionArchTest.java
class NamingConventionArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importarClasses() {
        classes = new ClassFileImporter().importPackages("br.com.scaffold");
    }

    @Test
    void useCaseInterfacesTerminamEmUseCase() {
        classes().that().resideInAPackage("..application.usecase..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("UseCase")
                .check(classes);
    }

    @Test
    void nenhumaClasseUsaSufixoVago() {
        noClasses()
                .should().haveSimpleNameEndingWith("Manager")
                .orShould().haveSimpleNameEndingWith("Processor")
                .orShould().haveSimpleNameEndingWith("Helper")
                .check(classes);
    }

    // + 12 outras regras, uma por linha da tabela acima
}
```

**Pegadinha real encontrada ao implementar isto** (documentada aqui para quem for portar o padrão
para outro projeto): a forma "oficial" do ArchUnit para JUnit5 — dependência `archunit-junit5`,
anotações `@AnalyzeClasses`/`@ArchTest` — registra seu **próprio motor de testes JUnit5**
(`archunit-junit5-engine`), separado do Jupiter. Neste projeto, com Maven Surefire, esse motor
roda sem erro nenhum mas reporta `Tests run: 0` — nenhuma regra é verificada de verdade, e o build
passa mesmo com violações reais no código (confirmado empiricamente: um `ProdutoManager` de teste
não quebrou o build nessa configuração). Isso persistiu mesmo depois de alinhar todas as versões
do `junit-platform-*` e declarar `junit-platform-launcher` como dependência do próprio
`maven-surefire-plugin` (o fix mais comumente documentado para esse tipo de problema). A solução
que funcionou: usar só a lib **core** do ArchUnit (`com.tngtech.archunit:archunit`, sem o sufixo
`-junit5`) e chamar `ArchRule.check(classes)` dentro de métodos `@Test` comuns do Jupiter — motor
de testes que já é comprovadamente confiável neste build. Se for tentar a variante
`archunit-junit5` em outro projeto, valide com uma violação de propósito antes de confiar que o
`mvn test` está barrando alguma coisa.

**Outra pegadinha, específica de Java:** o Lombok `@Builder` em `ProdutoEntity` gera uma classe
aninhada em tempo de compilação (`ProdutoEntity.ProdutoEntityBuilder`) — a regra que escaneia
`infrastructure/entity/` por sufixo precisa excluir explicitamente
`haveSimpleNameNotEndingWith("Builder")`, senão a classe builder gerada pelo annotation processor
vira um falso positivo.

**Checkstyle** (mais barato de configurar, menos preciso — só olha o nome, não a camada): uma
regra de `TypeName` com regex proibindo `.*(Manager|Processor|Helper)$` já pega os casos mais
óbvios sem precisar entender pacote/anotação. Pode complementar o ArchUnit como primeira linha de
defesa (feedback mais rápido no editor), mas não substitui — só ArchUnit entende papel
arquitetural, não só sufixo de string.
