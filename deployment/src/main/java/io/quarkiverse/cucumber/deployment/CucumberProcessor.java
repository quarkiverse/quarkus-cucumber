package io.quarkiverse.cucumber.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.cucumber.java.StepDefinitionAnnotation;
import io.cucumber.java.StepDefinitionAnnotations;
import io.quarkiverse.cucumber.AfterScenario;
import io.quarkiverse.cucumber.BeforeScenario;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkiverse.cucumber.ScenarioContext;
import io.quarkiverse.cucumber.ScenarioScope;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem;
import io.quarkus.arc.deployment.CustomScopeBuildItem;
import io.quarkus.arc.deployment.QualifierRegistrarBuildItem;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

class CucumberProcessor {

    private static final String FEATURE = "cucumber";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Register @BeforeScenario and @AfterScenario as CDI qualifiers.
     */
    @BuildStep
    void registerScenarioQualifiers(BuildProducer<QualifierRegistrarBuildItem> qualifierRegistrar) {
        qualifierRegistrar.produce(new QualifierRegistrarBuildItem(new QualifierRegistrar() {
            @Override
            public java.util.Map<DotName, java.util.Set<String>> getAdditionalQualifiers() {
                return java.util.Map.of(
                        DotName.createSimple(BeforeScenario.class.getName()), java.util.Set.of(),
                        DotName.createSimple(AfterScenario.class.getName()), java.util.Set.of());
            }
        }));
    }

    /**
     * cucumber has a lot of annotations as they have locale specific ones
     * <p>
     * rather than hard coding them all we index them and discover them
     */
    @BuildStep
    IndexDependencyBuildItem indexCucumber() {
        return new IndexDependencyBuildItem("io.cucumber", "cucumber-java");
    }

    /**
     * Anything with a step defining annotation is automatically a bean
     */
    @BuildStep
    AdditionalBeanBuildItem beanDefiningAnnotation(CombinedIndexBuildItem indexBuildItem) {
        Set<String> stepClasses = new HashSet<>();
        for (var an : Arrays.asList(DotName.createSimple(StepDefinitionAnnotation.class.getName()),
                DotName.createSimple(StepDefinitionAnnotations.class.getName()))) {
            for (var annotationInst : indexBuildItem.getIndex().getAnnotations(an)) {
                for (var stepAnn : indexBuildItem.getIndex().getAnnotations(annotationInst.target().asClass().name())) {
                    stepClasses.add(stepAnn.target().asMethod().declaringClass().name().toString());
                }
            }
        }
        for (var i : indexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(CucumberQuarkusTest.class.getName()))) {
            stepClasses.add(i.name().toString());
        }
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(stepClasses)
                .setDefaultScope(DotName.createSimple(ScenarioScope.class.getName()))
                .setUnremovable()
                .build();
    }

    @BuildStep
    ContextConfiguratorBuildItem scenarioContext(ContextRegistrationPhaseBuildItem contextRegistrationPhase) {
        return new ContextConfiguratorBuildItem(
                contextRegistrationPhase.getContext()
                        .configure(ScenarioScope.class)
                        .normal()
                        .contextClass(ScenarioContext.class));
    }

    @BuildStep
    CustomScopeBuildItem scenarioScope() {
        return new CustomScopeBuildItem(DotName.createSimple(ScenarioScope.class.getName()));
    }
}
