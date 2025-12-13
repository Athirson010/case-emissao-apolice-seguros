package io.github.athirson010.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.github.athirson010");
    }

    @Test
    @DisplayName("Domain layer should not depend on core layer")
    void domainShouldNotDependOnCore() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..core..")
                .because("Domain layer must be independent of core layer");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain layer should not depend on adapters")
    void domainShouldNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")
                .because("Domain layer must be independent of adapter layer");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain layer should not depend on Spring Framework")
    void domainShouldNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Domain layer must be framework-agnostic (no Spring dependencies)");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain layer should not depend on AWS SDK")
    void domainShouldNotDependOnAws() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("software.amazon..")
                .because("Domain layer must be infrastructure-agnostic (no AWS dependencies)");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain layer should not depend on Spring Data")
    void domainShouldNotDependOnSpringData() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")
                .because("Domain layer must not depend on persistence frameworks");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Core layer should not depend on adapters")
    void coreShouldNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..core..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")
                .because("Core layer must be independent of adapter implementations");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Input adapters should not depend on output adapters")
    void inputAdaptersShouldNotDependOnOutputAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapters.in..")
                .should().dependOnClassesThat().resideInAPackage("..adapters.out..")
                .because("Input and output adapters must be independent");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Output adapters should not depend on input adapters")
    void outputAdaptersShouldNotDependOnInputAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapters.out..")
                .should().dependOnClassesThat().resideInAPackage("..adapters.in..")
                .because("Output and input adapters must be independent");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("RestController annotations should only be in web adapters")
    void restControllersShouldOnlyBeInWebPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..adapters.in.web..")
                .because("REST controllers must reside only in input web adapters");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain layer should only contain pure Java classes")
    void domainShouldOnlyContainPureJava() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "software.amazon..",
                        "org.springframework.data..",
                        "io.awspring..",
                        "..adapters..",
                        "..core..",
                        "..application.."
                )
                .because("Domain must be pure Java without external framework dependencies");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Core layer should only depend on domain")
    void coreShouldOnlyDependOnDomain() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapters..",
                        "..application.."
                )
                .because("Core layer should only depend on domain layer");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Naming convention: Ports should be interfaces")
    void portsShouldBeInterfaces() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..core.port.in..", "..core.port.out..")
                .should().beInterfaces()
                .because("Ports must be interfaces following hexagonal architecture");

        rule.check(importedClasses);
    }
}
