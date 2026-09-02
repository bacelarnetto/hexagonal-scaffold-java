# TODO

## Variante orquestradora com Temporal.io

Permitir evoluir este mesmo scaffold (sem criar um repo/template separado) para gerar uma aplicação
orquestradora usando [Temporal.io](https://temporal.io).

Ideia de desenho, a confirmar na implementação:

- Novo módulo `orchestration/`, mesmo padrão já usado pro Lombok em `infrastructure/`: o SDK do
  Temporal fica restrito a esse módulo só, `domain/` continua puro (só JDK). Depende de `domain/`
  e possivelmente de `application/` (Activities como wrappers finos dos services existentes).
- **Activities** delegam pra `application/usecase/` (a interface do use case, não a Impl) ou direto
  pra uma port de `domain/` — reuso quase total do que já existe.
- **Workflows** não podem morar em `domain/` (dependem do SDK do Temporal) nem são bem
  `application/` (não rodam sob `@Transactional`/ciclo de vida normal do Spring, rodam sob o motor
  de replay determinístico do Temporal).
- `starter/` ganha a config do `WorkerFactory`/`WorkflowClient`.
- Testes: `TestWorkflowEnvironment` do próprio SDK (workflow em memória, sem servidor real) cobre o
  nível 1/2 da pirâmide; um teste de integração com servidor Temporal real (Testcontainers ou
  docker-compose, mesmo padrão do MySQL hoje) cobre o nível 3.
- Ver adendo em [`CLAUDE.md`](CLAUDE.md) (seção "Transação e rollback") sobre por que
  `@Transactional` não cobre rollback de Workflow multi-Activity — vai precisar de uma seção nova
  quando isso for implementado (Saga pattern do SDK ou Activities de compensação escritas à mão).

Possivelmente expor isso como opção na skill `bootstrap-hexagonal-project` (pergunta extra: "app
orquestrador com Temporal?"), gerando o módulo `orchestration/` condicionalmente.

## Checkstyle como primeira linha de defesa (opcional)

O enforcement de naming conventions via ArchUnit já está implementado
(`starter/src/test/java/.../architecture/NamingConventionArchTest.java`, roda com `mvn test` — ver
`doc/guide/naming-conventions.md`). Uma regra de Checkstyle (`TypeName` com regex proibindo
`.*(Manager|Processor|Helper)$`) daria feedback mais cedo (no editor/IDE, antes de rodar os testes)
como complemento — mais barata de configurar, mas menos precisa (não entende pacote/anotação).
Não é essencial; ArchUnit já cobre a regra que importa.
