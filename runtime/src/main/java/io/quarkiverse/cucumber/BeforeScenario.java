package io.quarkiverse.cucumber;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * CDI qualifier annotation for observing scenario start events.
 * <p>
 * Use this qualifier with {@code @Observes} to receive notifications
 * when a Cucumber scenario starts:
 *
 * <pre>
 * public void onScenarioStart(@Observes @BeforeScenario ScenarioEvent event) {
 *     // Setup test resources, reset state, etc.
 * }
 * </pre>
 *
 * @see AfterScenario
 * @see ScenarioEvent
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface BeforeScenario {

    /**
     * Literal implementation for programmatic use.
     */
    final class Literal extends AnnotationLiteral<BeforeScenario> implements BeforeScenario {
        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;
    }
}
