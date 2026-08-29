# Pirâmide de testes

3 níveis, cada um testando uma coisa diferente — não redundantes entre si.

```mermaid
graph TD
    A["Integração — 1 por controller<br/>@SpringBootTest + MockMvc + banco real<br/>(H2 no scaffold, MySQL+Testcontainers no projeto gerado)"] --> B
    B["Application (mockado) — poucos<br/>JUnit5 + Mockito, mocka a porta<br/>verifica orquestração + tratamento de erro"] --> C
    C["Domain (puro) — muitos<br/>JUnit5, zero mock, zero Spring<br/>testa a regra de negócio isolada"]

    style C fill:#ecfdf5,stroke:#10b981
    style B fill:#eff6ff,stroke:#3b82f6
    style A fill:#fef2f2,stroke:#ef4444
```

## Base — domain, sem framework

`domain/src/test/java/.../ProdutoPrecoLogicUnitTest.java` — instancia a classe direto, sem mock,
sem contexto Spring. É o teste mais barato e mais rápido de rodar; deveria ser a maioria.

## Meio — application, um teste por use case

`application/src/test/java/.../usecase/` — um arquivo de teste por caso de uso, Mockito
(`@ExtendWith(MockitoExtension.class)`, `@Mock`) só na dependência direta:
`CadastrarProdutoUseCaseImplTest` mocka `ProdutoRepositoryPort`,
`CadastrarProdutoEmLoteUseCaseImplTest` mocka `CadastrarProdutoUseCase` (o use case vizinho que ele
injeta), não a porta. Cada teste isola exatamente a orquestração daquele caso de uso.

**Regra que existe por causa de um erro real visto num projeto de referência:** sempre chame o
método real do use case **primeiro**, e só **depois** verifique o mock e o valor retornado. Nunca
invoque o mock diretamente no corpo do teste como se fosse o código em teste — isso faz o teste
passar não importa o que o use case faça.

```java
// Errado — não testa nada de verdade
port.save(mockProduto);
verify(port, only()).save(mockProduto);
useCase.executar(dto);   // chamado depois, sem nenhuma asserção

// Certo — chama o use case, depois verifica o efeito
useCase.executar(dto);
verify(port).save(savedCaptor.capture());
assertEquals(0, expected.compareTo(savedCaptor.getValue().valorVenda()));
```

## Topo — integração, banco real

`starter/src/test/java/.../ProdutoControllerIntegrationTest.java` — `@SpringBootTest` +
`@AutoConfigureMockMvc`, sobe o contexto Spring inteiro com banco real (H2 no scaffold; MySQL via
Testcontainers no projeto gerado pelo skill de bootstrap). Prova que os 4 módulos Maven realmente
se conectam — é o único nível que teria pego o problema do projeto de referência (`genetic`), que
não builda fora da rede da empresa original por depender de uma lib privada.

Um teste de integração por controller basta. Casos de borda de regra de negócio vão no nível de
domain, não aqui.

De propósito essa classe de teste **não** tem `@Transactional` — se tivesse, o Spring TestContext
envolveria cada `@Test` numa transação só revertida no final do método, escondendo a diferença
entre "linha commitada" e "linha revertida" no meio do teste (que é exatamente o que `pci3_...`
precisa observar — ver abaixo). A limpeza entre testes é feita manualmente, por um `@BeforeEach` que
chama `produtoJpaRepository.deleteAll()`.

### pci3 — prova de rollback real

`pci3_loteComItemInvalidoNoFinalNaoDeixaNenhumProdutoSalvo` chama `POST /produto/lote` com 3 itens
(2 válidos + 1 com margem inválida) e depois um `GET /produto` simples. Se o rollback do
`@Transactional` em `CadastrarProdutoEmLoteUseCaseImpl.executar()` funcionar, a lista volta vazia —
nenhum dos 2 itens válidos fica salvo, mesmo já tendo passado por `port.save()` antes do item
inválido estourar a exceção. Ver a explicação completa do mecanismo em
[Módulo de exemplo — produto](modulo-exemplo#transação-e-rollback-cadastrarprodutoemloteusecase).

Esse é o único nível da pirâmide que pode provar rollback de verdade: um teste com mock na porta
(nível 2) só provaria que `port.save()` parou de ser chamado, não que o que já tinha sido chamado
foi desfeito — rollback é uma propriedade da transação real, não do código Java em si.
