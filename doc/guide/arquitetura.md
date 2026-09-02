# Arquitetura

4 módulos Maven físicos. A fronteira é o `pom.xml` de cada um — não uma convenção de pasta que
alguém pode violar sem perceber.

```mermaid
graph TD
    ST["starter<br/>@SpringBootApplication"] --> AP["application<br/>@Service @RestController"]
    AP --> IN["infrastructure<br/>@Entity @Repository"]
    AP --> DO["domain<br/>Java puro"]
    IN --> DO

    style DO fill:#ecfdf5,stroke:#10b981
    style IN fill:#eff6ff,stroke:#3b82f6
    style AP fill:#fef2f2,stroke:#ef4444
    style ST fill:#fef9c3,stroke:#f59e0b
```

## O que cada módulo pode depender

| Módulo | Pode depender de | Não pode ter |
|---|---|---|
| `domain` | nada (só JDK) | Spring, JPA, Lombok, qualquer framework/lib — **o `pom.xml` não tem essas dependências, então não compila mesmo se alguém tentar** |
| `infrastructure` | `domain` | regra de negócio — só tradução entity ↔ domain |
| `application` | `domain`, `infrastructure` | acesso direto a `infrastructure/entity` fora do próprio adapter |
| `starter` | `application` | qualquer classe de negócio — só `@SpringBootApplication` + `application.yml` |

## Records vs. Lombok

`domain/` usa `record` para modelos e `application/` usa `record` para DTOs — imutável, com
`equals`/`hashCode`/`toString` gerados pelo compilador, sem precisar de nenhuma dependência externa.
`infrastructure/entity/` usa Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder`) porque `@Entity` do JPA precisa de construtor sem args e se beneficia de campos
mutáveis — Lombok fica restrito a esse módulo.

## `domain/service/` vs. `application/usecase/`

```
domain/service/XxxLogic.java         sem @Service, sem @Transactional
                                      recebe ports por construtor (ou nenhuma dependência)
                                      contém a regra de negócio pura, testável sem Spring

application/usecase/XxxUseCase.java      @FunctionalInterface interface XxxUseCase { R executar(...); }
application/usecase/XxxUseCaseImpl.java  class XxxUseCaseImpl implements XxxUseCase (@Service, @Transactional quando escreve)
                                          instancia XxxLogic direto (new) se ela não tem dependências,
                                          ou recebe via construtor se tiver -- ou injeta outro XxxUseCase
                                          não contém regra de negócio — só orquestra
```

Um caso de uso por par de arquivos (Single Responsibility) — Java não permite duas classes públicas
no mesmo arquivo, por isso interface e implementação ficam separadas (em Kotlin ficam juntas no
mesmo arquivo). Controllers dependem da *interface* (Dependency Inversion), e cada interface só
expõe o método que aquele caso de uso precisa (Interface Segregation) — nada de uma classe
`XxxService` só com vários métodos não relacionados. Ver o exemplo completo em
[Módulo de exemplo — produto](modulo-exemplo).

## Regra prática antes de criar um `domain/service/` novo

Nem todo módulo de negócio tem lógica pura de verdade — CRUD simples não tem nada a ganhar com
essa separação. Antes de extrair, pergunte: "existe uma regra aqui que não depende de banco, HTTP
ou tempo de relógio?" Se a resposta for não, o service fica só em `application/`, sem
contrapartida em `domain/service/`.

## Validação automática (ArchUnit)

Os 4 módulos Maven garantem a *direção* de dependência entre módulos (ver tabela acima) — mas não
impedem tudo. Uma vez que `application/` já depende de `infrastructure/` (para o wiring do
adapter), nada no Maven impede que uma classe de `application/controller/` importe
`ProdutoEntity` diretamente, por exemplo — os dois estão no mesmo classpath. Essas regras "dentro"
de um módulo são verificadas por `starter/src/test/java/.../architecture/ArchitectureRulesArchTest.java`,
junto com `mvn test`:

- `application/controller/` e `application/usecase/` nunca dependem de `infrastructure/entity/`
  diretamente — só o `RepositoryAdapter` conhece a entity
- `@Transactional` só aparece em classes `application/usecase/*UseCaseImpl`
- Nenhum campo de código de produção usa `@Autowired` — injeção é sempre via construtor
- Toda implementação de uma interface `domain/port/*Port` mora em `infrastructure/adapter/`

Ver [Naming Conventions](naming-conventions#validação-automática-no-ci) para a nomeação de classe
por papel (`NamingConventionArchTest.java`, a outra metade do enforcement automático) e a
pegadinha de por que esses testes usam `@Test` comuns do Jupiter em vez do módulo `archunit-junit5`.
