package br.com.scaffold.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Versao executavel de doc/guide/naming-conventions.md. Roda junto com `mvn test` -- uma classe
 * fora do padrao (ex: um `ProdutoManager` novo) quebra o build aqui, nao so a revisao de codigo.
 *
 * De proposito usa @Test comuns do Jupiter em vez do modulo archunit-junit5 (que registra seu
 * proprio motor de testes JUnit5): nesse motor, no Surefire deste projeto, os testes rodam sem
 * erro mas sao reportados como "Tests run: 0" -- nenhuma regra e verificada de verdade. Chamando
 * ArchRule.check(classes) diretamente dentro de um @Test comum evita depender dessa integracao.
 */
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
    void useCaseImplementationsTerminamEmUseCaseImplESaoService() {
        classes().that().resideInAPackage("..application.usecase..")
                .and().areNotInterfaces()
                .should().haveSimpleNameEndingWith("UseCaseImpl")
                .andShould().beAnnotatedWith(Service.class)
                .check(classes);
    }

    @Test
    void controllersTerminamEmControllerESaoRestController() {
        classes().that().resideInAPackage("..application.controller..")
                .should().haveSimpleNameEndingWith("Controller")
                .andShould().beAnnotatedWith(RestController.class)
                .check(classes);
    }

    @Test
    void mappersTerminamEmMapper() {
        classes().that().resideInAPackage("..application.mapper..")
                .should().haveSimpleNameEndingWith("Mapper")
                .check(classes);
    }

    @Test
    void dtosDeApplicationTerminamEmDTO() {
        classes().that().resideInAPackage("..application.dto..")
                .should().haveSimpleNameEndingWith("DTO")
                .check(classes);
    }

    @Test
    void dtosCompartilhadosTerminamEmDTO() {
        classes().that().resideInAPackage("..shared.application.dto..")
                .should().haveSimpleNameEndingWith("DTO")
                .check(classes);
    }

    @Test
    void exceptionHandlersSaoRestControllerAdviceETerminamEmExceptionHandler() {
        classes().that().areAnnotatedWith(RestControllerAdvice.class)
                .should().haveSimpleNameEndingWith("ExceptionHandler")
                .check(classes);
    }

    @Test
    void classesTerminadasEmHandlerSoSaoPermitidasComoExceptionHandler() {
        classes().that().haveSimpleNameEndingWith("Handler")
                .should().haveSimpleNameEndingWith("ExceptionHandler")
                .check(classes);
    }

    @Test
    void domainPortsSaoInterfacesETerminamEmPort() {
        classes().that().resideInAPackage("..domain.port..")
                .should().beInterfaces()
                .andShould().haveSimpleNameEndingWith("Port")
                .check(classes);
    }

    @Test
    void domainServicesTerminamEmLogic() {
        classes().that().resideInAPackage("..domain.service..")
                .should().haveSimpleNameEndingWith("Logic")
                .check(classes);
    }

    @Test
    void entitiesTerminamEmEntityESaoAnotadasComEntity() {
        // haveSimpleNameNotEndingWith("Builder") exclui a classe builder que o Lombok @Builder
        // gera (ex: ProdutoEntity.ProdutoEntityBuilder) -- e uma classe sintetica do compilador
        // (via annotation processor), nao uma entidade de verdade sujeita a essa convencao.
        classes().that().resideInAPackage("..infrastructure.entity..")
                .and().haveSimpleNameNotEndingWith("Builder")
                .should().haveSimpleNameEndingWith("Entity")
                .andShould().beAnnotatedWith(Entity.class)
                .check(classes);
    }

    @Test
    void jpaRepositoriesTerminamEmJpaRepository() {
        classes().that().resideInAPackage("..infrastructure.repository..")
                .should().haveSimpleNameEndingWith("JpaRepository")
                .check(classes);
    }

    @Test
    void repositoryAdaptersTerminamEmRepositoryAdapterESaoRepository() {
        classes().that().resideInAPackage("..infrastructure.adapter..")
                .should().haveSimpleNameEndingWith("RepositoryAdapter")
                .andShould().beAnnotatedWith(Repository.class)
                .check(classes);
    }

    @Test
    void excecoesDeDominioTerminamEmException() {
        classes().that().resideInAPackage("..domain.exception..")
                .should().haveSimpleNameEndingWith("Exception")
                .check(classes);
    }

    @Test
    void nenhumaClasseUsaSufixoVago() {
        noClasses()
                .should().haveSimpleNameEndingWith("Manager")
                .orShould().haveSimpleNameEndingWith("Processor")
                .orShould().haveSimpleNameEndingWith("Helper")
                .orShould().haveSimpleNameEndingWith("Util")
                .orShould().haveSimpleNameEndingWith("Utils")
                .check(classes);
    }
}
