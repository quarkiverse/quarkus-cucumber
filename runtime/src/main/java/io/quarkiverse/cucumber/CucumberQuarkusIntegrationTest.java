package io.quarkiverse.cucumber;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.smallrye.config.inject.ConfigExtension;

@QuarkusIntegrationTest
@EnableWeld
public abstract class CucumberQuarkusIntegrationTest extends CucumberBaseTest {
    @WeldSetup
    @SuppressWarnings("unused")
    WeldInitiator weld = WeldInitiator.from(
            WeldInitiator.createWeld()
                    .addExtensions(new ConfigExtension())
                    .addPackages(true, packagesToScanRecursively()))
            .build();

    protected Package[] packagesToScanRecursively() {
        return new Package[] { this.getClass().getPackage() };
    }
}