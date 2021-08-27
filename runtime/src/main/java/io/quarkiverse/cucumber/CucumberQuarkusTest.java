package io.quarkiverse.cucumber;

import java.time.Clock;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import io.cucumber.core.backend.ObjectFactory;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.options.CucumberOptionsAnnotationParser;
import io.cucumber.core.options.CucumberProperties;
import io.cucumber.core.options.CucumberPropertiesParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.options.RuntimeOptionsBuilder;
import io.cucumber.core.plugin.PluginFactory;
import io.cucumber.core.plugin.Plugins;
import io.cucumber.core.plugin.PrettyFormatter;
import io.cucumber.core.runner.Runner;
import io.cucumber.core.runtime.CucumberExecutionContext;
import io.cucumber.core.runtime.ExitStatus;
import io.cucumber.core.runtime.FeaturePathFeatureSupplier;
import io.cucumber.core.runtime.FeatureSupplier;
import io.cucumber.core.runtime.ObjectFactorySupplier;
import io.cucumber.core.runtime.ScanningTypeRegistryConfigurerSupplier;
import io.cucumber.core.runtime.TimeServiceEventBus;
import io.cucumber.core.runtime.TypeRegistryConfigurerSupplier;
import io.cucumber.java.JavaBackendProviderService;
import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestStepFinished;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public abstract class CucumberQuarkusTest {

    @TestFactory
    List<DynamicNode> getTests() {
        EventBus eventBus = new TimeServiceEventBus(Clock.systemUTC(), UUID::randomUUID);
        final FeatureParser parser = new FeatureParser(eventBus::generateId);

        RuntimeOptions propertiesFileOptions = new CucumberPropertiesParser()
                .parse(CucumberProperties.fromPropertiesFile())
                .build();

        RuntimeOptions environmentOptions = new CucumberPropertiesParser()
                .parse(CucumberProperties.fromEnvironment())
                .build(propertiesFileOptions);

        RuntimeOptions systemOptions = new CucumberPropertiesParser()
                .parse(CucumberProperties.fromSystemProperties())
                .build(environmentOptions);

        RuntimeOptions runtimeOptions;
        RuntimeOptionsBuilder runtimeOptionsBuilder = new RuntimeOptionsBuilder()
                .addDefaultFeaturePathIfAbsent()
                .addDefaultGlueIfAbsent()
                .addDefaultFormatterIfAbsent()
                .addDefaultSummaryPrinterIfAbsent();

        QuarkusCucumberOptionsProvider optionsProvider = new QuarkusCucumberOptionsProvider();
        if (optionsProvider.hasOptions(this.getClass())) {
            CucumberOptionsAnnotationParser annotationParser = new CucumberOptionsAnnotationParser()
                    .withOptionsProvider(optionsProvider);
            RuntimeOptions annotationOptions = annotationParser
                    .parse(this.getClass())
                    .build(systemOptions);
            runtimeOptions = runtimeOptionsBuilder.build(annotationOptions);
        } else {
            runtimeOptions = runtimeOptionsBuilder.build(systemOptions);
        }

        FeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(() -> Thread.currentThread().getContextClassLoader(),
                runtimeOptions, parser);

        final Plugins plugins = new Plugins(new PluginFactory(), runtimeOptions);
        plugins.addPlugin(new PrettyFormatter(System.out));

        final ExitStatus exitStatus = new ExitStatus(runtimeOptions);
        plugins.addPlugin(exitStatus);
        if (runtimeOptions.isMultiThreaded()) {
            plugins.setSerialEventBusOnEventListenerPlugins(eventBus);
        } else {
            plugins.setEventBusOnEventListenerPlugins(eventBus);
        }
        ObjectFactory objectFactory = new CdiObjectFactory();

        ObjectFactorySupplier objectFactorySupplier = () -> objectFactory;

        TypeRegistryConfigurerSupplier typeRegistryConfigurerSupplier = new ScanningTypeRegistryConfigurerSupplier(
                () -> Thread.currentThread()
                        .getContextClassLoader(),
                runtimeOptions);

        Runner runner = new Runner(eventBus,
                Collections.singleton(new JavaBackendProviderService().create(objectFactorySupplier.get(),
                        objectFactorySupplier.get(),
                        () -> Thread.currentThread()
                                .getContextClassLoader())),
                objectFactorySupplier.get(),
                typeRegistryConfigurerSupplier.get(),
                runtimeOptions);

        CucumberExecutionContext context = new CucumberExecutionContext(eventBus, exitStatus, () -> runner);

        List<DynamicNode> features = new LinkedList<>();
        features.add(DynamicTest.dynamicTest("Start Cucumber", context::startTestRun));

        featureSupplier.get().forEach(f -> {
            List<DynamicTest> tests = new LinkedList<>();
            tests.add(DynamicTest.dynamicTest("Start Feature", () -> context.beforeFeature(f)));
            f.getPickles().forEach(p -> tests.add(DynamicTest.dynamicTest(p.getName(), () -> {
                AtomicReference<TestStepFinished> resultAtomicReference = new AtomicReference<>();
                EventHandler<TestStepFinished> handler = event -> {
                    if (event.getResult().getStatus() != Status.PASSED) {
                        // save the first failed test step, so that we can get the line number of the cucumber file
                        resultAtomicReference.compareAndSet(null, event);
                    }
                };
                eventBus.registerHandlerFor(TestStepFinished.class, handler);
                context.runTestCase(r -> r.runPickle(p));
                eventBus.removeHandlerFor(TestStepFinished.class, handler);

                // if we have no main arguments, we are running as part of a junit test suite, we need to fail the junit test explicitly
                if (resultAtomicReference.get() != null) {
                    Assertions.fail(
                            "failed in " + f.getUri() + " at line "
                                    + ((PickleStepTestStep) resultAtomicReference.get().getTestStep()).getStep().getLocation()
                                            .getLine(),
                            resultAtomicReference.get().getResult().getError());
                }
            })));
            features.add(DynamicContainer.dynamicContainer(f.getName().orElse(f.getSource()), tests.stream()));
        });

        features.add(DynamicTest.dynamicTest("Finish Cucumber", context::finishTestRun));

        return features;
    }

    public static class CdiObjectFactory implements ObjectFactory {
        public CdiObjectFactory() {
        }

        public void start() {

        }

        public void stop() {

        }

        public boolean addClass(Class<?> clazz) {
            return true;
        }

        public <T> T getInstance(Class<T> type) {
            var old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(type.getClassLoader());
                Instance<T> selected = CDI.current().select(type);
                if (selected.isUnsatisfied()) {
                    throw new IllegalArgumentException(type.getName() + " is no CDI bean.");
                } else {
                    return selected.get();
                }
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }
}
