# Build self-contido: nao exige `mvn package` local antes (diferente do padrao antigo de
# `ARG JAR_FILE` + copiar um jar ja pronto) -- so precisa do Docker instalado. `docker build .`
# a partir da raiz do repo (o contexto precisa enxergar os 4 modulos).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Poms primeiro, sozinhos, para cachear a camada de dependencias -- so muda quando um pom muda,
# nao a cada alteracao de codigo.
COPY pom.xml .
COPY domain/pom.xml domain/pom.xml
COPY infrastructure/pom.xml infrastructure/pom.xml
COPY application/pom.xml application/pom.xml
COPY starter/pom.xml starter/pom.xml
RUN mvn -q -pl starter -am dependency:go-offline

COPY domain/src domain/src
COPY infrastructure/src infrastructure/src
COPY application/src application/src
COPY starter/src starter/src
RUN mvn -q -pl starter -am package -DskipTests

# Runtime: so JRE, sem Maven/JDK completo -- imagem final bem menor, superficie de ataque menor.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
# O glob *.jar pega so o jar reempacotado pelo spring-boot-maven-plugin -- o *.jar.original
# (sem as dependencias embutidas) nao bate no padrao porque nao termina em ".jar".
COPY --from=build /app/starter/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
