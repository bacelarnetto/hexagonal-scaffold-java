package br.com.scaffold.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Regras de dependência/estrutura que os 4 módulos Maven, sozinhos, não conseguem expressar --
 * eles já garantem a direção entre módulos (domain sem Spring, application depende de domain +
 * infrastructure, starter só de application), mas não impedem, por exemplo, que um controller
 * importe uma entity diretamente, já que controller e entity moram no mesmo classpath assim que
 * application depende de infrastructure. Ver doc/guide/arquitetura.md e CLAUDE.md.
 */
class ArchitectureRulesArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importarClasses() {
        classes = new ClassFileImporter().importPackages("br.com.scaffold");
    }

    @Test
    void controllersEUsecasesNaoAcessamInfrastructureEntityDiretamente() {
        // So o adapter (infrastructure/adapter) pode conhecer a entity -- e quem faz
        // toDomain()/toEntity(). O resto de application/ so deveria falar com domain/model.
        noClasses().that().resideInAnyPackage("..application.controller..", "..application.usecase..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.entity..")
                .check(classes);
    }

    @Test
    void transactionalSoAparecEmApplicationUsecaseImpl() {
        DescribedPredicate<JavaClass> contemMetodoTransactional = DescribedPredicate.describe(
                "contem metodo anotado com @Transactional",
                javaClass -> javaClass.getMethods().stream().anyMatch(m -> m.isAnnotatedWith(Transactional.class)));

        classes().that(contemMetodoTransactional)
                .should().resideInAPackage("..application.usecase..")
                .andShould().haveSimpleNameEndingWith("UseCaseImpl")
                .check(classes);
    }

    @Test
    void nenhumCampoDeCodigoDeProducaoUsaInjecaoViaAutowired() {
        // O scaffold e 100 por cento injecao via construtor -- um unico construtor e
        // autowired implicitamente pelo Spring, sem precisar de @Autowired em lugar nenhum.
        // Restrito a classes que NAO terminam em "Test": @Autowired em campo de teste
        // (ex: ProdutoControllerIntegrationTest.mockMvc) e idiomatico em @SpringBootTest,
        // nao e o mesmo problema que field injection em codigo de producao.
        noFields().that().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Test")
                .should().beAnnotatedWith(Autowired.class)
                .check(classes);
    }

    @Test
    void implementacoesDePortFicamEmInfrastructureAdapter() {
        DescribedPredicate<JavaClass> implementaUmaPortDeDominio = DescribedPredicate.describe(
                "implementa uma interface de domain.port",
                javaClass -> javaClass.getInterfaces().stream()
                        .anyMatch(i -> i.toErasure().getPackageName().contains("domain.port")));

        classes().that(implementaUmaPortDeDominio)
                .and().areNotInterfaces()
                .should().resideInAPackage("..infrastructure.adapter..")
                .check(classes);
    }
}
