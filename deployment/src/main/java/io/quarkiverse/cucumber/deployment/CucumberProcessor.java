package io.quarkiverse.cucumber.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.cucumber.java.StepDefinitionAnnotation;
import io.cucumber.java.StepDefinitionAnnotations;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
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
        return AdditionalBeanBuildItem.builder().addBeanClasses(stepClasses).setDefaultScope(DotNames.SINGLETON)
                .setUnremovable().build();
    }
}
