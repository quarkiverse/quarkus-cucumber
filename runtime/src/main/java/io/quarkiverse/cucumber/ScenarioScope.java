package io.quarkiverse.cucumber;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.NormalScope;

@NormalScope
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD })
public @interface ScenarioScope {
}
