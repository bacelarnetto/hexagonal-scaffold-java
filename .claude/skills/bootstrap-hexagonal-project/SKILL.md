---
name: bootstrap-hexagonal-project
description: Bootstraps a new Java + Spring Boot backend project from the hexagonal-scaffold-java template at ~/work/hexagonal-scaffold-java (Maven multi-module: domain/infrastructure/application/starter, MySQL + Flyway + docker-compose, tests across the pyramid — pure domain unit tests, mocked application-service unit tests, one Testcontainers integration test). Asks for app name, package, first business domain and entity, then generates a working project with a real CRUD already wired end to end. Use when starting a new backend service, bootstrapping a new Java/Spring project, or when the user asks to scaffold, initialize, or create a new project from the hexagonal template.
---

# Bootstrap Hexagonal Project

Gera um projeto novo a partir de `~/work/hexagonal-scaffold-java` (Java 21 + Spring Boot 3, Maven
multi-módulo: `domain/infrastructure/application/starter`). O scaffold já vem validado — copiar e
ajustar em vez de escrever do zero. Não pule a validação final (Passo 7): renomear pacote é uma
operação fácil de fazer parcialmente sem perceber.

## Passo 1 — Perguntar (AskUserQuestion, uma única rodada)

Colete:
- Nome da aplicação (vira artifactId raiz e nome de pasta, ex: `padaria-api`)
- Pacote base Java (ex: `br.com.padaria`)
- Onde criar o projeto (path absoluto, ex: `~/work/padaria-api` — deve ser um diretório que ainda não existe)
- Nome do primeiro domínio de negócio / bounded context e o nome da entidade de exemplo dentro dele
  (pode ser o mesmo singular, ex: domínio `pedido`, entidade `Pedido`)

Banco de dados: **MySQL é o único suportado por este skill hoje** (ver `mysql-setup.md`). Se o
usuário pedir outro banco, avise que ainda não está coberto e pergunte se quer prosseguir mesmo
assim com MySQL como base para adaptar depois.

## Passo 2 — Copiar o scaffold

```bash
cp -R ~/work/hexagonal-scaffold-java <destino>
find <destino> -name target -type d -prune -exec rm -rf {} \;
rm -rf <destino>/.git
```

## Passo 3 — Renomear pacote e coordenadas Maven

1. `grep -rl "br.com.scaffold"` no destino para ver o escopo completo antes de mexer
2. Trocar `br.com.scaffold` → `<pacote base>` em todo arquivo `.java` e `.xml` (declarações de
   package, imports, `<groupId>`)
3. Mover os diretórios de pacote (`src/main/java/br/com/scaffold/...` e
   `src/test/java/br/com/scaffold/...`) para espelhar o novo pacote
4. Trocar o `artifactId` raiz (`hexagonal-scaffold-java` → nome da aplicação) e o `<name>` nos 5 `pom.xml`
5. Renomear os artifactId dos módulos filhos: `scaffold-domain` → `<app>-domain`,
   `scaffold-infrastructure` → `<app>-infrastructure`, `scaffold-application` → `<app>-application`,
   `scaffold-starter` → `<app>-starter` — em todo `<artifactId>` E em toda referência a esses
   artifactId dentro do `dependencyManagement`/`dependencies` dos outros poms
6. Renomear a classe principal `starter/.../ScaffoldApplication.java` (arquivo + classe + a
   referência em `SpringApplication.run(...)`) para `<AppNomeEmCamelCase>Application` — o `grep` do
   passo 1 não pega esse arquivo porque o nome da classe é `ScaffoldApplication` (S maiúsculo), não
   `br.com.scaffold` literal. Fácil de esquecer; confira com
   `grep -rl "ScaffoldApplication"` no destino depois de renomear.

## Passo 4 — Renomear o módulo de exemplo `produto/`

Renomear pastas/classes `produto`→`<domínio informado>` e `Produto`→`<Entidade informada>`
(capitalizada) nos 3 módulos de negócio (domain/infrastructure/application) + o teste de domínio +
o teste de application + a migration `V1__create_produto.sql` (nome do arquivo e conteúdo). Mantenha
a lógica como está — é só um exemplo de CRUD com uma regra de preço pura (`calcularValorVenda`); o
objetivo é dar um primeiro módulo já funcionando com o nome certo, não outra funcionalidade.

## Passo 5 — Trocar H2 por MySQL

Ver [mysql-setup.md](mysql-setup.md) para o passo a passo completo: dependências do pom,
`application.properties`, `docker-compose.yml`, e a troca do teste de integração de MockMvc+H2 para
`@SpringBootTest` + Testcontainers+MySQL.

## Passo 6 — Renomear `docker/` e `kubernetes/`

`grep -rl "scaffold"` em `kubernetes/*.yaml` — o nome do app aparece em `metadata.name` (Deployment,
Service, ConfigMap, Secret), em `matchLabels.app`/`labels.app`, no `configMapKeyRef.name`/
`secretKeyRef.name` de cada env var do Deployment, e no path de exemplo da imagem em
`kubernetes/deployment.yaml`. Trocar `scaffold` → `<app>` em todas essas ocorrências, mantendo
Deployment/Service/ConfigMap/Secret apontando uns para os outros consistentemente (ex: se o
ConfigMap vira `padaria-api-config`, o `configMapKeyRef.name` no Deployment tem que virar
`padaria-api-config` também — não é só o `metadata.name` do próprio arquivo). `docker/Dockerfile`
não tem referência ao nome do app, não precisa mexer.

## Passo 7 — Validar

```bash
cd <destino> && mvn test
docker compose up -d          # sobe o MySQL local para rodar a app (não é usado pelos testes)
mvn spring-boot:run -pl starter
docker build -f docker/Dockerfile -t <app>:test .   # confirma que a imagem builda depois do Passo 6
```

Só reporte a tarefa como concluída depois de `mvn test` passar de verdade. Se usar Testcontainers e
o Docker local for Colima (comum neste ambiente), pode ser necessário configurar as properties
`docker.host`/`docker.api.version` e passar `DOCKER_HOST`/`DOCKER_API_VERSION` como variável de
ambiente do processo de teste via `maven-surefire-plugin` — copie esse trecho de outro projeto
local que já use Testcontainers com Colima, se os testes não acharem o socket do Docker.

## O que NÃO fazer

- Não deixar `br.com.scaffold` sobrando em nenhum arquivo — quebra o component scan do Spring
  silenciosamente (classes ficam fora do package raiz escaneado)
- Não deixar a classe principal como `ScaffoldApplication` — some do `grep "br.com.scaffold"` do
  passo 3 porque o nome da classe não contém esse literal; some do `@SpringBootTest(classes = ...)`
  do teste de integração também, se esquecido
- Não pular a renomeação dos artifactId dos módulos filhos — dois projetos gerados a partir daqui
  compartilhando o mesmo `~/.m2` local vão colidir em `scaffold-domain` se isso for esquecido
- Não deixar `scaffold` sobrando em `kubernetes/*.yaml` — dois projetos gerados a partir daqui
  aplicados no mesmo cluster/namespace vão colidir nos mesmos nomes de Deployment/Service/
  ConfigMap/Secret se isso for esquecido, exatamente pelo mesmo motivo do artifactId Maven acima
- Não gerar o projeto sem rodar `mvn test` no final — renomeação de pacote quebra silenciosamente
  com frequência (import esquecido, diretório não movido)
- Não adicionar Lombok em `domain/` ao criar o domínio novo — fica restrito a `infrastructure/`
  (ver `CLAUDE.md`)
