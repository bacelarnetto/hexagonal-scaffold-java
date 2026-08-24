# Prompting e CLAUDE.md

Este repo (e qualquer projeto gerado a partir dele) vem com um [`CLAUDE.md`](../../CLAUDE.md) na
raiz — lido automaticamente por assistentes de IA compatíveis (Claude Code, e outros que seguem a
mesma convenção). Ele existe para que você não precise repetir as regras de arquitetura a cada
prompt.

## O que colocar no prompt (e o que não precisa)

**Não precisa repetir** — já está no `CLAUDE.md`: a separação dos 4 módulos, a regra de
`domain/service/` sem `@Service`, o mapeamento explícito nos adapters, o padrão de teste em 3
níveis, onde usar record vs. Lombok. Um assistente que leu o `CLAUDE.md` já sabe disso.

**Vale dizer no prompt:**
- O nome do domínio/entidade novo e os campos que ele tem
- Se existe alguma regra de negócio pura a extrair (nem todo CRUD tem — ver
  [Arquitetura](arquitetura))
- Se a mudança cruza módulo de negócio (ex: `pedido` referenciando `produtoId` de outro módulo) —
  lembrar a regra de referência por ID, nunca import de entity/domain de outro módulo de negócio

## Peça para rodar, não só compilar

Compilar prova sintaxe. Só `mvn test` prova que a regra de negócio funciona e que os módulos
realmente se conectam (ver [Pirâmide de testes](testes)). Ao pedir uma mudança, peça
explicitamente para rodar os testes depois — ou inclua isso como critério de conclusão no prompt.

## Mantendo o CLAUDE.md vivo

Sempre que uma decisão de arquitetura for tomada (novo módulo, nova convenção, exceção à regra
geral), atualize o `CLAUDE.md` — a documentação que vale é a que vai para o Git, não a memória
local do assistente.

## Exemplo de prompt para adicionar um domínio novo

> "Adicione um módulo `estoque` com uma entidade `ItemEstoque` (produtoId: Long, quantidade:
> BigDecimal, quantidadeMinima: BigDecimal). A regra de negócio: `estaAbaixoDoMinimo()` — pura,
> compara quantidade com quantidadeMinima. Siga a mesma estrutura do módulo `produto` (ver
> `doc/guide/modulo-exemplo.md`). Rode os testes no final."

Note o que esse prompt NÃO precisa explicar: onde fica cada camada, como nomear os DTOs, se usa
`BeanUtils` ou mapper explícito, se o modelo é record ou classe Lombok — tudo isso já está no
`CLAUDE.md` e no exemplo `produto`.

## Gerando um projeto novo via IA

Ver [Skill de bootstrap](bootstrap) — em vez de copiar este repo manualmente, um assistente Claude
Code pode fazer isso via prompt, perguntando os dados do projeto novo interativamente.
