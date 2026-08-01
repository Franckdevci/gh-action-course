package ci.ecotrack;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

@AnalyzeClasses(packages = "ci.ecotrack", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureConventionsTest {

    @ArchTest
    static final ArchRule domaine_ne_depend_ni_de_spring_ni_de_jpa =
            noClasses().that().resideInAPackage("..domaine..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "com.fasterxml.jackson..",
                            "org.hibernate..");

    @ArchTest
    static final ArchRule entites_jpa_dans_infrastructure_jpa =
            classes().that().areAnnotatedWith("jakarta.persistence.Entity")
                    .should().resideInAPackage("..infrastructure.jpa..");

    @ArchTest
    static final ArchRule rest_controllers_dans_infrastructure_rest =
            classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().resideInAPackage("..infrastructure.rest..");

    @ArchTest
    static final ArchRule releves_n_appelle_pas_alertes_service_directement =
            noClasses().that().resideInAPackage("..releves..")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("ci.ecotrack.alertes.AlertesService");

    @ArchTest
    static final ArchRule pas_d_injection_par_champ =
            noFields().should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");

    @ArchTest
    static final ArchRule pas_de_date_legacy =
            noClasses().should().dependOnClassesThat()
                    .haveFullyQualifiedName("java.util.Date")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName("java.sql.Timestamp")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName("java.util.Calendar");
}
