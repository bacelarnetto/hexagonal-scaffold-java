# hexagonal-scaffold-java

Template Maven multi-módulo (Java 21 + Spring Boot 3.5.3) para arquitetura hexagonal pura. Nasce
validado — os 4 módulos compilam, os 10 testes passam, o jar sobe e responde de verdade.

Scaffold de **app atômico**: gera uma única aplicação Spring Boot, com um único domínio de negócio
por vez — não é um monorepo nem multi-serviço.

Versão Java de [`hexagonal-scaffold-kotlin`](https://github.com/bacelarnetto/hexagonal-scaffold-kotlin)
— mesma arquitetura, mesmo módulo de exemplo `produto`, mesma pirâmide de testes. Ver
[Diferenças em relação à versão Kotlin](#diferenças-em-relação-à-versão-kotlin) abaixo.

Guia completo (docsify, com diagramas): `npx serve doc/guide` → http://localhost:3000
Instruções para assistentes de IA (Claude Code e afins): [`CLAUDE.md`](CLAUDE.md)

## Por que este repo existe

Projetos de referência hexagonal costumam ter a estrutura de módulos certa, mas acumulam problemas
que não queremos herdar: dependência de libs privadas que não buildam fora da rede original, testes
que fazem asserção sobre chamadas ao mock em vez de sobre o comportamento do código real, e stack
datada. Este scaffold nasce validado — sem essas pendências — e corrige esses pontos desde o
início. Ver detalhes em `doc/guide/testes.md`.

## O que tem aqui

```
domain/          Java puro (só JDK). Sem Spring, sem JPA, sem Lombok — o pom.xml não tem essas
                 dependências, não tem como "vazar" framework pra dentro do domínio por acidente.
infrastructure/  @Entity, JpaRepository, @Repository adapter. Mapeamento entity<->domain explícito
                 (toDomain()/toEntity()), nunca BeanUtils.copyProperties(). Único módulo com Lombok.
application/     @Service/@Transactional, @RestController, DTOs, mappers, GlobalExceptionHandler.
starter/         @SpringBootApplication + application.yml. Nenhuma regra de negócio aqui.
docker/          Dockerfile multi-stage self-contido (build + runtime, sem precisar de mvn local).
kubernetes/      Deployment + Service + ConfigMap + Secret (template), voltado para GKE/GCP.
doc/guide/       Guia docsify (arquitetura, módulo de exemplo, pirâmide de testes, workflow com IA).
```

Módulo de exemplo: `produto/`, presente nas 3 camadas de negócio, com uma regra de preço de
verdade (`valorVenda = custo / (1 - margem/100)`) em `domain/service/ProdutoPrecoLogic.java` — sem
`@Service`, lança `RegraDeNegocioException` quando a margem é inválida. Passeio completo,
arquivo por arquivo: `doc/guide/modulo-exemplo.md`.

Também inclui um exemplo de **rollback via `@Transactional`** (`ProdutoService.cadastrarEmLote()` +
`POST /produto/lote`): mostra como uma regra que mora em `domain/` (sem Spring) consegue disparar
rollback de uma transação Spring sem nunca saber que ela existe — só lançando uma exceção normal.
Ver `doc/guide/modulo-exemplo.md#transação-e-rollback-cadastraremlote`.

## Rodar o scaffold como está

```bash
mvn test                          # roda os 10 testes (domain puro, application com Mockito, integração com H2)
mvn package -DskipTests
java -jar starter/target/scaffold-starter-*.jar
curl -X POST localhost:8080/produto -H "Content-Type: application/json" \
  -d '{"nome":"Brigadeiro","custo":"2.00","margemPercentual":"50"}'
```

Banco: H2 em memória (modo MySQL), zero infra externa necessária — é só o scaffold em si. Um
projeto gerado a partir dele já nasce com MySQL de verdade (ver abaixo).

## Docker e Kubernetes (deploy no GCP)

```bash
docker build -f docker/Dockerfile -t REGION-docker.pkg.dev/PROJECT_ID/REPOSITORY/scaffold:TAG .
kubectl apply -f kubernetes/configmap.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
```

`docker/Dockerfile` é multi-stage e self-contido — não precisa rodar `mvn package` antes, funciona
direto num pipeline de CI que só tenha Docker. `kubernetes/` traz Deployment (com liveness/readiness
via `spring-boot-starter-actuator`) + Service (LoadBalancer) + ConfigMap + um template de Secret
(`secret.example.yaml` — nunca commite o `secret.yaml` real, já está no `.gitignore`). De propósito
**não** inclui banco dentro do cluster — o padrão recomendado é Cloud SQL gerenciado, acessado via
Cloud SQL Auth Proxy como sidecar. Detalhes e o exemplo completo do sidecar: `doc/guide/deploy.md`.

## Gerando um projeto novo a partir daqui

Dois caminhos para o mesmo "formulário" — os campos são os mesmos, só muda quem preenche.

### Caminho A — via prompt de IA (Claude Code)

Existe um skill em `.claude/skills/bootstrap-hexagonal-project/` (dentro deste repo — chega junto
quando você clona) que faz a cópia, renomeação e troca de banco por você. Peça a um assistente
Claude Code, por exemplo:

> "Cria um projeto novo a partir do hexagonal-scaffold-java — app `padaria-api`, pacote
> `br.com.padaria`, domínio inicial `pedido`, banco MySQL."

O assistente vai perguntar (ou confirmar) os mesmos 5 campos da tabela abaixo antes de rodar
qualquer coisa, e só entrega a tarefa como concluída depois de `mvn test` passar no projeto gerado.
Detalhe do que o skill faz passo a passo: `doc/guide/bootstrap.md`.

### Caminho B — via comando, preenchendo o "formulário" manualmente

| Campo | Exemplo | Onde entra |
|---|---|---|
| Nome da aplicação | `padaria-api` | `artifactId` raiz + nome de pasta |
| Pacote base | `br.com.padaria` | `groupId` + pacote Java |
| Diretório de destino | `~/work/padaria-api` | destino do `cp -R` |
| Primeiro domínio de negócio | `pedido` | substitui o módulo de exemplo `produto` |
| Entidade de exemplo | `Pedido` | substitui a classe `Produto` |
| Banco de dados | MySQL (único suportado hoje) | `infrastructure/pom.xml` + `application.yml` |

```bash
cp -R hexagonal-scaffold-java <destino>
find <destino> -name target -type d -prune -exec rm -rf {} \;
rm -rf <destino>/.git

# 1. Trocar br.com.scaffold -> <pacote base> em todo .java e .xml (grep -rl primeiro)
# 2. Mover diretórios de pacote para espelhar o novo pacote
# 3. Trocar groupId/artifactId (raiz + cada um dos 4 módulos filhos) nos 5 pom.xml
# 4. Renomear a classe starter/.../ScaffoldApplication.java (arquivo + classe) -- não cai no grep do
#    passo 1 porque o nome não contém "br.com.scaffold" literal
# 5. Renomear produto/Produto -> <domínio>/<Entidade> nas 3 camadas de negócio + migration
# 6. Trocar H2 por MySQL + Testcontainers (ver .claude/skills/bootstrap-hexagonal-project/mysql-setup.md)

cd <destino> && mvn test
```

Não considere o projeto pronto sem rodar `mvn test` no final — renomeação de pacote quebra
silenciosamente com frequência (import esquecido, diretório não movido).

## Convenções (resumo — detalhes em `CLAUDE.md` e `doc/guide/`)

- `domain/service/` nunca leva `@Service`, nunca lança exceção HTTP, nunca injeta `application/service/`
- Mapper explícito sempre, nunca `BeanUtils.copyProperties()`
- Antes de extrair um `domain/service/`, confirme que existe lógica pura real — CRUD puro não ganha nada com isso
- Pirâmide de testes: domain (muitos, sem mock) → application (poucos, Mockito na porta) → integração (um por controller, banco real)

## Diferenças em relação à versão Kotlin

Mesma arquitetura, mesmo módulo de exemplo, mesmo número de testes (10) — só o que a troca de
linguagem/lib exige mudou:

| Kotlin | Java | Por quê |
|---|---|---|
| `data class` | `record` | Records são a forma nativa do JDK de ter imutabilidade + `equals`/`hashCode`/`toString` gerados, sem lib externa — mantém `domain/` sem dependências além do JDK, igual ao Kotlin puro |
| `produto.copy(valorVenda = x)` | `produto.comValorVenda(x)` | Records não têm `copy()` embutido; método `comX(...)` explícito no próprio record cobre os pontos de uso reais sem introduzir uma lib de "with-er" genérica |
| `@Entity` como `data class` (com plugins `kotlin-allopen`/`kotlin-noarg`) | `@Entity` como classe Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`) | Java não tem o problema de classe `final` por padrão que motiva os plugins Kotlin; Lombok resolve boilerplate de getter/setter/construtor. Fica restrito a `infrastructure/` |
| `ProdutoRepositoryPort.findById(): Produto?` | `ProdutoRepositoryPort.findById(): Optional<Produto>` | `Optional` é o idiomático em Java para "pode não existir"; nulabilidade explícita no tipo como no Kotlin `?` não existe na linguagem |
| MockK | Mockito (`spring-boot-starter-test`, já incluso) | Lib de mock padrão do ecossistema Java/Spring; mesma regra de teste (chamar o service real primeiro, verificar depois) |
| `fun interface Mapper<In, Out>` | `@FunctionalInterface interface Mapper<In, Out>` | Equivalente direto |
| `kotlin-maven-plugin` + plugins `allopen`/`jpa`/`noarg` no pom raiz | nenhum plugin de linguagem extra | Desnecessário em Java — classes já não são `final` por padrão e `@Entity` não exige plugin de compilador |
