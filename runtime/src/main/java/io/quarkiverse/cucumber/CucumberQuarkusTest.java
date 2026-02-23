package io.quarkiverse.cucumber;

import java.net.URI;
import java.time.Clock;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.console.ConsoleLauncher;

import io.cucumber.core.backend.ObjectFactory;
import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.filter.Filters;
import io.cucumber.core.gherkin.Pickle;
import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.Constants;
import io.cucumber.core.options.CucumberOptionsAnnotationParser;
import io.cucumber.core.options.CucumberProperties;
import io.cucumber.core.options.CucumberPropertiesParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.options.RuntimeOptionsBuilder;
import io.cucumber.core.plugin.Options;
import io.cucumber.core.plugin.PluginFactory;
import io.cucumber.core.plugin.Plugins;
import io.cucumber.core.plugin.PrettyFormatter;
import io.cucumber.core.runner.Runner;
import io.cucumber.core.runtime.CucumberExecutionContext;
import io.cucumber.core.runtime.ExitStatus;
import io.cucumber.core.runtime.FeaturePathFeatureSupplier;
import io.cucumber.core.runtime.FeatureSupplier;
import io.cucumber.core.runtime.ObjectFactorySupplier;
import io.cucumber.core.runtime.TimeServiceEventBus;
import io.cucumber.java.JavaBackendProviderService;
import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestStep;
import io.cucumber.plugin.event.TestStepFinished;
import io.quarkus.arc.Arc;
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
                .addDefaultSummaryPrinterIfNotDisabled();

        QuarkusCucumberOptionsProvider optionsProvider = new QuarkusCucumberOptionsProvider();
        final Class<? extends CucumberQuarkusTest> testClassWithCucumberOptions = findCucumberOptionAnnotatedClassSkippingProxies();
        if (testClassWithCucumberOptions != null) {
            CucumberOptionsAnnotationParser annotationParser = new CucumberOptionsAnnotationParser()
                    .withOptionsProvider(optionsProvider);
            RuntimeOptions annotationOptions = annotationParser
                    .parse(testClassWithCucumberOptions)
                    .build(systemOptions);
            runtimeOptions = runtimeOptionsBuilder.build(annotationOptions);
        } else {
            runtimeOptions = runtimeOptionsBuilder.build(systemOptions);
        }

        FeatureSupplier featureSupplier = new FeaturePathFeatureSupplier(
                () -> Thread.currentThread().getContextClassLoader(),
                runtimeOptions, parser);

        final Plugins plugins = new Plugins(new PluginFactory(), runtimeOptions);
        if (testClassWithCucumberOptions == null
                || testClassWithCucumberOptions.getAnnotation(CucumberOptions.class).pretty()) {
            plugins.addPlugin(new PrettyFormatter(System.out));
        }

        final ExitStatus exitStatus = new ExitStatus(runtimeOptions);
        plugins.addPlugin(exitStatus);
        if (runtimeOptions.isMultiThreaded()) {
            plugins.setSerialEventBusOnEventListenerPlugins(eventBus);
        } else {
            plugins.setEventBusOnEventListenerPlugins(eventBus);
        }
        CucumberExecutionContext context = cucumberExecutionContext(eventBus, runtimeOptions, exitStatus);

        List<DynamicNode> features = new LinkedList<>();
        features.add(DynamicTest.dynamicTest("Start Cucumber", context::startTestRun));
        features.add(DynamicTest.dynamicTest("Before All Features", context::runBeforeAllHooks));

        Predicate<Pickle> filters = new Filters(runtimeOptions);

        EventHandler<TestCaseStarted> scenarioStartedHandler = event -> {
            fireScenarioEvent(new ScenarioEvent(event.getTestCase()), BeforeScenario.Literal.INSTANCE);
        };

        EventHandler<TestCaseFinished> scenarioFinishedHandler = event -> {
            fireScenarioEvent(
                    new ScenarioEvent(event.getTestCase(), event.getResult().getStatus()),
                    AfterScenario.Literal.INSTANCE);
            var scenarioContext = Arc.container().getActiveContext(ScenarioScope.class);
            if (scenarioContext != null) {
                scenarioContext.destroy();
            }
        };

        featureSupplier.get().forEach(f -> {
            List<DynamicTest> tests = new LinkedList<>();
            tests.add(DynamicTest.dynamicTest("Start Feature", () -> context.beforeFeature(f)));
            f.getPickles()
                    .stream()
                    .filter(filters)
                    .forEach(p -> tests.add(DynamicTest.dynamicTest(p.getName(), () -> {
                        AtomicReference<TestStepFinished> resultAtomicReference = new AtomicReference<>();
                        EventHandler<TestStepFinished> handler = event -> {
                            if (event.getResult().getStatus() != Status.PASSED) {
                                // save the first failed test step, so that we can get the line number of the
                                // cucumber file
                                resultAtomicReference.compareAndSet(null, event);
                            }
                        };

                        eventBus.registerHandlerFor(TestCaseStarted.class, scenarioStartedHandler);
                        eventBus.registerHandlerFor(TestCaseFinished.class, scenarioFinishedHandler);
                        eventBus.registerHandlerFor(TestStepFinished.class, handler);
                        context.runTestCase(r -> r.runPickle(p));
                        eventBus.removeHandlerFor(TestStepFinished.class, handler);
                        eventBus.removeHandlerFor(TestCaseFinished.class, scenarioFinishedHandler);
                        eventBus.removeHandlerFor(TestCaseStarted.class, scenarioStartedHandler);

                        // if we have no main arguments, we are running as part of a junit test suite,
                        // we need to fail the junit test explicitly
                        if (resultAtomicReference.get() != null) {
                            TestStep testStep = resultAtomicReference.get().getTestStep();
                            if (testStep instanceof PickleStepTestStep) {
                                // failed in step, we have a line in the feature file
                                Assertions.fail(
                                        "failed in " + f.getUri() + " at line "
                                                + ((PickleStepTestStep) testStep).getStep()
                                                        .getLocation()
                                                        .getLine(),
                                        resultAtomicReference.get().getResult().getError());
                            } else {
                                // failed somewhere in hooks
                                Assertions.fail(
                                        "failed in " + f.getUri() + " at "
                                                + testStep.getCodeLocation(),
                                        resultAtomicReference.get().getResult().getError());
                            }
                        }
                    })));

            if (tests.size() > 1) {
                features.add(DynamicContainer.dynamicContainer(f.getName().orElse(f.getSource()), tests.stream()));
            }
        });
        features.add(DynamicTest.dynamicTest("After All Features", context::runAfterAllHooks));
        features.add(DynamicTest.dynamicTest("Finish Cucumber", context::finishTestRun));

        return features;
    }

    private Class<? extends CucumberQuarkusTest> findCucumberOptionAnnotatedClassSkippingProxies() {
        for (Class<?> candidate = this.getClass(); candidate != CucumberQuarkusTest.class; candidate = candidate
                .getSuperclass()) {
            if (candidate.getAnnotation(CucumberOptions.class) != null) {
                @SuppressWarnings("unchecked")
                final var castCheckedInALoop = (Class<? extends CucumberQuarkusTest>) candidate;
                return castCheckedInALoop;
            }
        }
        return null;
    }

    private static CucumberExecutionContext cucumberExecutionContext(EventBus eventBus, RuntimeOptions runtimeOptions,
            ExitStatus exitStatus) {
        ObjectFactory objectFactory = new CdiObjectFactory();

        ObjectFactorySupplier objectFactorySupplier = () -> objectFactory;

        Runner runner = new Runner(eventBus,
                Collections.singleton(new JavaBackendProviderService().create(objectFactorySupplier.get(),
                        objectFactorySupplier.get(),
                        () -> Thread.currentThread()
                                .getContextClassLoader())),
                objectFactorySupplier.get(),
                runtimeOptions);

        return new CucumberExecutionContext(eventBus, exitStatus, () -> runner);
    }

    /**
     * Fires a CDI event for scenario lifecycle hooks.
     *
     * @param event the scenario event payload
     * @param qualifier the qualifier annotation (BeforeScenario or AfterScenario)
     */
    private static void fireScenarioEvent(ScenarioEvent event, java.lang.annotation.Annotation qualifier) {
        Arc.container().beanManager().getEvent()
                .select(ScenarioEvent.class, qualifier)
                .fire(event);
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

    protected static <T extends CucumberQuarkusTest> void runMain(Class<T> testClass, String[] args) {
        RuntimeOptions propertiesFileOptions = new CucumberPropertiesParser()
                .parse(CucumberProperties.fromPropertiesFile())
                .build();

        RuntimeOptions environmentOptions = new CucumberPropertiesParser()
                .parse(CucumberProperties.fromEnvironment())
                .build(propertiesFileOptions);

        RuntimeOptions systemOptions = new CucumberPropertiesParser()
                .parse(CucumberProperties.fromSystemProperties())
                .build(environmentOptions);

        CommandlineOptionsParser commandlineOptionsParser = new CommandlineOptionsParser(System.out);
        RuntimeOptions runtimeOptions = commandlineOptionsParser.parse(args).build(systemOptions);

        commandlineOptionsParser.exitStatus().ifPresent(System::exit);

        System.setProperty(Constants.ANSI_COLORS_DISABLED_PROPERTY_NAME, String.valueOf(runtimeOptions.isMonochrome()));
        // TODO: CUCUMBER_PROPERTIES_FILE_NAME
        System.setProperty(Constants.EXECUTION_DRY_RUN_PROPERTY_NAME, String.valueOf(runtimeOptions.isDryRun()));
        System.setProperty(Constants.EXECUTION_LIMIT_PROPERTY_NAME, String.valueOf(runtimeOptions.getLimitCount()));
        // TODO: EXECUTION_ORDER_PROPERTY_NAME runtimeOptions.getPickleOrder(); (how can
        // we convert this?)
        // --strict/--no-strict is already handled by the CommandlineOptionsParser
        // EXECUTION_STRICT_PROPERTY_NAME
        System.setProperty(Constants.WIP_PROPERTY_NAME, String.valueOf(runtimeOptions.isWip()));
        System.setProperty(Constants.FEATURES_PROPERTY_NAME,
                runtimeOptions.getFeaturePaths().stream().map(URI::toString).collect(Collectors.joining(",")));
        System.setProperty(Constants.FILTER_NAME_PROPERTY_NAME,
                runtimeOptions.getNameFilters().stream().map(Pattern::toString).collect(Collectors.joining(",")));
        System.setProperty(Constants.FILTER_TAGS_PROPERTY_NAME,
                runtimeOptions.getTagExpressions().stream().map(Object::toString).collect(Collectors.joining(",")));
        System.setProperty(Constants.GLUE_PROPERTY_NAME,
                runtimeOptions.getGlue().stream().map(URI::toString).collect(Collectors.joining(",")));
        Optional.ofNullable(runtimeOptions.getObjectFactoryClass())
                .ifPresent(s -> System.setProperty(Constants.OBJECT_FACTORY_PROPERTY_NAME, s.getName()));
        System.setProperty(Constants.PLUGIN_PROPERTY_NAME,
                runtimeOptions.plugins().stream().map(Options.Plugin::pluginString).collect(Collectors.joining(",")));
        // Not supported via CLI argument: PLUGIN_PUBLISH_ENABLED_PROPERTY_NAME
        // Not supported via CLI argument: PLUGIN_PUBLISH_TOKEN_PROPERTY_NAME
        // Not supported via CLI argument: PLUGIN_PUBLISH_URL_PROPERTY_NAME
        // Not supported via CLI argument: PLUGIN_PUBLISH_QUIET_PROPERTY_NAME
        System.setProperty(Constants.SNIPPET_TYPE_PROPERTY_NAME,
                runtimeOptions.getSnippetType().toString().toLowerCase());

        ConsoleLauncher.main("-c", testClass.getName());
    }
}
