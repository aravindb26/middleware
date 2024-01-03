
package com.openexchange.config;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

/**
 * {@link TestProperties}
 *
 * @author <a href="mailto:myles.anderson@open-xchange.com">Myles Anderson</a>
 * @author <a href="mailto:pascal.bleser@open-xchange.com">Pascal Bleser</a>
 * @since v7.8.2
 */
public class TestProperties {

    public static ImmutableMap<String, String> getPropertiesFromFile(final String fileName) {
        final ImmutableMap<String, String> properties;
        {
            final File file = new File(fileName);
            if (!file.exists()) {
                throw new Error(String.format("Failed to find the file \"%s\". Please provide a valid file path.", file.getName()));
            }
            final ImmutableMap.Builder<String, String> b = ImmutableMap.<String, String> builder();
            {
                try (final InputStream is = Files.asByteSource(file).openBufferedStream()) {
                    final Properties props = new Properties();
                    props.load(is);
                    for (final Map.Entry<Object, Object> e : props.entrySet()) {
                        final String key = Objects.toString(e.getKey());
                        final String value = Objects.toString(e.getValue());
                        if (null == key || null == value) {
                            throw new Error(String.format("key \"%s\" or value \"%s\" is null", key, value));
                        }
                        b.put(key, value);
                    }
                } catch (final Exception e) {
                    throw new Error(String.format("failed to find or read the file \"%s\"", file.getAbsolutePath()), e);
                }
            }
            properties = b.build();
        }

        return properties;
    }

    public static ImmutableMap<String, String> getPropertiesFromResource(final Object anchor, final String resource) {
        try {
            return new LoadResourceStream<ImmutableMap<String, String>>(anchor, resource) {

                @Override
                protected ImmutableMap<String, String> withResource(InputStream inputStream) throws Exception {
                    final ImmutableMap<String, String> properties;
                    {
                        final ImmutableMap.Builder<String, String> b = ImmutableMap.<String, String> builder();
                        {
                            final Properties props = new Properties();
                            props.load(inputStream);
                            for (final Map.Entry<Object, Object> e : props.entrySet()) {
                                final String key = Objects.toString(e.getKey());
                                final String value = Objects.toString(e.getValue());
                                if (null == key || null == value) {
                                    throw new Error(String.format("key \"%s\" or value \"%s\" is null", key, value));
                                }
                                b.put(key, value);
                            }
                        }
                        properties = b.build();
                    }

                    return properties;
                }
            }.result();
        } catch (Exception e) {
            throw new Error(String.format("failed to find or read the file \"%s\"", resource), e);
        }
    }

    private TestProperties() {}
}
