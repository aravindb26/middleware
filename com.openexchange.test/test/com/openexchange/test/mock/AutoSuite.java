package com.openexchange.test.mock;

import java.io.File;
import java.util.Objects;
import java.util.stream.Stream;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import com.openexchange.annotation.NonNullByDefault;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;

@NonNullByDefault
public enum AutoSuite {
    ;

    public static TestSuite suite(Class<?> anchor) {
        return suite(name(anchor), anchor.getPackage().getName());
    }
    
    private static final String name(Class<?> anchor) {
        return String.format("Unit Tests in %s", anchor.getPackage().getName());
    }

    public static TestSuite suite(String name, Class<?> anchor) {
        return suite(name, anchor.getPackage().getName());
    }
    
    public static TestSuite suite(String name, String packagePrefix) {
        final TestSuite suite = new TestSuite(name);
        final ClassLoader classLoader = ClasspathHelper.contextClassLoader();
        new Reflections(new ConfigurationBuilder()
            .setScanners(new SubTypesScanner(false))
            .setUrls(ClasspathHelper.forClassLoader(classLoader))
            .filterInputsBy(new FilterBuilder()
                .include(FilterBuilder.prefix(packagePrefix))
                .include(FilterBuilder.prefix("com.openexchange.test"))
            )
        ).getAllTypes().stream()
        .filter(className -> className.endsWith("Test"))
        .filter(className -> new File(String.format("./test/%s.java", className.replaceAll("\\.", "/"))).exists())
        .map(className -> {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                System.err.println(e.getMessage());
                return null;
            }
        })
        .filter(Objects::nonNull)
        .map(testClass -> new JUnit4TestAdapter(testClass))
        .forEach(adapter -> suite.addTest(adapter));
        return suite;
    }

}
