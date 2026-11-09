package io.quarkiverse.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.cucumber.plugin.event.Location;

/**
 * Coverage for {@link CucumberQuarkusTest#getTestSourceUri(URI, Location)}: features and scenarios are linked to
 * their position in the feature file so IDEs can navigate from a test result to the source.
 */
class TestSourceUriTest {

    @Test
    void resolvesClasspathFeatureToFileUriWithLine() {
        URI sourceUri = CucumberQuarkusTest.getTestSourceUri(URI.create("classpath:simple.feature"), new Location(3, 3));

        assertNotNull(sourceUri);
        assertEquals("file", sourceUri.getScheme());
        assertTrue(sourceUri.getPath().endsWith("/simple.feature"), sourceUri.getPath());
        assertEquals("line=3", sourceUri.getQuery());
    }

    @Test
    void resolvesClasspathFeatureInJarToJarUriWithLine(@TempDir Path tempDir) throws IOException {
        Path jar = tempDir.resolve("features.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("jarred.feature"));
            out.write("Feature: Jarred\n".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (URLClassLoader jarLoader = new URLClassLoader(new URL[] { jar.toUri().toURL() }, null)) {
            thread.setContextClassLoader(jarLoader);

            URI sourceUri = CucumberQuarkusTest.getTestSourceUri(URI.create("classpath:jarred.feature"),
                    new Location(3, 3));

            assertNotNull(sourceUri);
            assertEquals("jar", sourceUri.getScheme());
            assertEquals("jar:" + jar.toUri().toURL() + "!/jarred.feature?line=3", sourceUri.toString());
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @Test
    void returnsNullForMissingClasspathResource() {
        assertNull(CucumberQuarkusTest.getTestSourceUri(URI.create("classpath:missing.feature"), new Location(1, 1)));
    }

    @Test
    void keepsFileSystemFeatureUriWithLine() {
        URI fileFeature = URI.create("file:/tmp/features/simple.feature");

        assertEquals(URI.create("file:/tmp/features/simple.feature?line=5"),
                CucumberQuarkusTest.getTestSourceUri(fileFeature, new Location(5, 1)));
    }
}
