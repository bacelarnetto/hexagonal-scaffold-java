# Setup MySQL — referência para bootstrap-hexagonal-project

## 1. `infrastructure/pom.xml`

Remover a dependência `com.h2database:h2`. Adicionar:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

Não fixar versão do `mysql-connector-j` — o BOM do `spring-boot-starter-parent` já gerencia.

## 2. `starter/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: "jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:app}"
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: "${DB_USER:root}"
    password: "${DB_PASSWORD:root}"

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  flyway:
    enabled: true
    locations: classpath:db/migration
```

Troque `app` pelo nome do banco combinado com o usuário (geralmente o nome da aplicação). Mantenha
o bloco `management:` (probes do Actuator) que já está no scaffold — só o bloco `spring:` acima
muda de H2 para MySQL.

## 3. `docker-compose.yml` (raiz do projeto)

Só para rodar a aplicação localmente — os testes usam Testcontainers (container efêmero próprio,
não este).

```yaml
services:
  mysql:
    image: mysql:8.0.32
    environment:
      MYSQL_DATABASE: app
      MYSQL_USER: app
      MYSQL_PASSWORD: root
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

## 4. Testcontainers no `starter/pom.xml` (test scope, sem versão explícita — BOM gerencia)

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
```

## 5. Base de container reutilizável

Criar `starter/src/test/java/<pacote>/configuration/DatabaseContainerConfiguration.java`:

```java
package <pacote>.configuration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

public abstract class DatabaseContainerConfiguration {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("app")
            .withUsername("root")
            .withPassword("123456")
            .withReuse(true);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

## 6. Reescrever o teste de integração

Trocar `@SpringBootTest(classes = ...) @AutoConfigureMockMvc` isolado por H2 pelo padrão:

```java
@SpringBootTest(classes = <App>Application.class)
@AutoConfigureMockMvc
@Testcontainers
class <Entidade>ControllerIntegrationTest extends DatabaseContainerConfiguration {
    // corpo do teste igual ao do scaffold original (MockMvc get/post) -- só a fonte de dados muda
}
```

Mantenha as asserções de negócio como estavam; só a origem do banco muda (H2 em memória →
container MySQL real via Testcontainers).

## 7. Docker no macOS com Colima

Se `mvn test` não achar o Docker, configure as properties `docker.host`/`docker.api.version` e a
configuração de `maven-surefire-plugin` que passa `DOCKER_HOST`/`DOCKER_API_VERSION` como variável
de ambiente do processo de teste — copie esse trecho de outro projeto local que já use
Testcontainers com Colima, se disponível.
