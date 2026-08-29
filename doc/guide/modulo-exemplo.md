# Módulo de exemplo — produto

CRUD completo com uma regra de negócio real (não um `Hello World`): calcular o preço de venda a
partir do custo e da margem. Serve de template para qualquer bounded context novo.

```
domain/model/Produto.java                          record puro (comId/comValorVenda no lugar do copy() do Kotlin)
domain/port/ProdutoRepositoryPort.java              interface pura, findById devolve Optional<Produto>
domain/service/ProdutoPrecoLogic.java               regra pura: calcularValorVenda()

infrastructure/entity/ProdutoEntity.java            @Entity, Lombok (@Getter/@Setter/@Builder/...)
infrastructure/repository/ProdutoJpaRepository.java JpaRepository
infrastructure/adapter/ProdutoRepositoryAdapter.java @Repository, toDomain()/toEntity() explícitos

application/dto/ProdutoInsertFormDTO.java           record + validação (@NotBlank, @DecimalMin, ...)
application/dto/ProdutoViewDTO.java                 record
application/mapper/ProdutoFormMapper.java           Mapper<In, Out>
application/mapper/ProdutoViewMapper.java
application/usecase/ListarProdutosUseCase(Impl).java             interface + Impl, @Service
application/usecase/BuscarProdutoPorIdUseCase(Impl).java         interface + Impl, devolve o domain model
application/usecase/BuscarProdutoViewPorIdUseCase(Impl).java     interface + Impl, injeta o use case acima
application/usecase/CadastrarProdutoUseCase(Impl).java           interface + Impl, instancia ProdutoPrecoLogic direto
application/usecase/CadastrarProdutoEmLoteUseCase(Impl).java     interface + Impl, injeta CadastrarProdutoUseCase
application/usecase/ExcluirProdutoUseCase(Impl).java             interface + Impl, injeta BuscarProdutoPorIdUseCase
application/controller/ProdutoController.java       @RestController, depende das interfaces acima
```

## A regra

```java
// domain/service/ProdutoPrecoLogic.java — sem @Service, sem I/O
public BigDecimal calcularValorVenda(BigDecimal custo, BigDecimal margemPercentual) {
    if (margemPercentual.compareTo(CEM) >= 0) {
        throw new RegraDeNegocioException("Margem percentual deve ser menor que 100%");
    }
    BigDecimal margemFracao = margemPercentual.divide(CEM);
    return custo.divide(BigDecimal.ONE.subtract(margemFracao), 2, RoundingMode.HALF_UP);
}
```

`RegraDeNegocioException` mora em `domain/exception/` (não em `application/`) — é a exceção que o
`GlobalExceptionHandler` (em `application/`) sabe traduzir para HTTP 400. O domínio lança a
exceção de domínio; a aplicação decide o que isso significa em HTTP.

## Como o use case usa isso

```java
@FunctionalInterface
public interface CadastrarProdutoUseCase {
    ProdutoViewDTO executar(ProdutoInsertFormDTO dto);
}

@Service
public class CadastrarProdutoUseCaseImpl implements CadastrarProdutoUseCase {

    private final ProdutoRepositoryPort port;
    private final ProdutoFormMapper formMapper;
    private final ProdutoViewMapper viewMapper;
    private final ProdutoPrecoLogic precoLogic = new ProdutoPrecoLogic(); // objeto Java puro, não é bean Spring

    // construtor omitido

    @Override
    @Transactional
    public ProdutoViewDTO executar(ProdutoInsertFormDTO dto) {
        Produto produto = formMapper.map(dto);
        BigDecimal valorVenda = precoLogic.calcularValorVenda(produto.custo(), produto.margemPercentual());
        Produto salvo = port.save(produto.comValorVenda(valorVenda));
        return viewMapper.map(salvo);
    }
}
```

`ProdutoController` recebe `CadastrarProdutoUseCase` (a interface) no construtor, não
`CadastrarProdutoUseCaseImpl` — Dependency Inversion: o controller não conhece a implementação, só
o contrato de um método `executar(...)`.

`produto.comValorVenda(valorVenda)` é o equivalente Java do `produto.copy(valorVenda = valorVenda)`
do Kotlin — como `record` não tem `copy()` embutido, o próprio `Produto` expõe um método `comX(...)`
explícito para cada campo que precisa ser substituído depois da criação (ver `Produto.java`).

## Por que use case em vez de um `XxxService` com vários métodos

Cada caso de uso vira uma classe/interface própria (Single Responsibility) em vez de mais um método
numa classe `ProdutoService` que cresce a cada funcionalidade nova. Isso também evita forçar quem só
precisa cadastrar a depender de um contrato que também tem excluir/listar (Interface Segregation) e
permite compor casos de uso entre si (ver a seguir) sem precisar de uma classe "guarda-chuva".

## Transação e rollback: `CadastrarProdutoEmLoteUseCase`

Desafio clássico da arquitetura hexagonal: a regra de negócio mora em `domain/`, que não pode ter
Spring como dependência — mas rollback de transação é `@Transactional`, uma anotação Spring. Como
conciliar os dois? A resposta está em `CadastrarProdutoEmLoteUseCaseImpl.executar()`:

```java
@Service
public class CadastrarProdutoEmLoteUseCaseImpl implements CadastrarProdutoEmLoteUseCase {

    private final CadastrarProdutoUseCase cadastrarProduto;

    // construtor omitido

    @Override
    @Transactional
    public List<ProdutoViewDTO> executar(List<ProdutoInsertFormDTO> dtos) {
        return dtos.stream().map(cadastrarProduto::executar).toList();
    }
}
```

`precoLogic.calcularValorVenda()` (chamada dentro de `CadastrarProdutoUseCaseImpl.executar()`) não
sabe que está rodando dentro de uma transação — ela só lança `RegraDeNegocioException` (uma
`RuntimeException` comum) quando a margem de um item é inválida, igual sempre fez. O que muda é que
agora essa exceção escapa de um método anotado `@Transactional` em `application/`: o proxy Spring
por trás dessa anotação intercepta a exceção e marca a transação inteira para rollback —
**inclusive os itens anteriores do lote que já tinham sido enviados a `port.save()`**, porque
continuam fazendo parte da mesma transação JDBC até ela ser desfeita.

Repare que `CadastrarProdutoEmLoteUseCaseImpl` chama `cadastrarProduto.executar(dto)` — um bean
Spring *diferente*, não `this.cadastrar(dto)`. Isso não é coincidência: é o que permite reusar
`CadastrarProdutoUseCase` aqui sem cair na armadilha de self-invocation do `@Transactional` (ver
`CLAUDE.md`, seção "Transação e rollback") — a chamada passa pelo proxy normalmente porque é uma
chamada entre dois beans, não dois métodos da mesma classe.

Ou seja: `POST /produto/lote` com 3 itens, onde o 3º tem margem inválida, não deixa **nenhum**
produto salvo — nem o 1º nem o 2º, mesmo que `port.save()` já tenha sido chamado para eles antes do
erro. O domínio continua 100% livre de Spring; quem decide que isso vira rollback é só o
`@Transactional` em `application/usecase/`.

Isso só é verificável com banco real, não com mock — ver `ProdutoControllerIntegrationTest.pci3_...`
(`doc/guide/testes.md`), que cadastra um lote com um item inválido e confirma, com uma segunda
chamada `GET /produto`, que a lista voltou vazia.

## Usando como template para um domínio novo

1. Copie os 3 arquivos de `domain/` (model, port, service — se houver lógica pura)
2. Copie os 3 arquivos de `infrastructure/` (entity, repository, adapter) + a migration
3. Copie os arquivos de `application/` (dto, mapper, usecase, controller)
4. Renomeie `Produto`/`produto` para a entidade nova em todos eles
5. Rode os testes (ver [Pirâmide de testes](testes)) antes de considerar pronto
