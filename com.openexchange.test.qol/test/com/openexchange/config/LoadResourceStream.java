
package com.openexchange.config;

import java.io.IOException;
import java.io.InputStream;
import org.slf4j.LoggerFactory;

/**
 * {@link LoadResourceStream}
 *
 * @author <a href="mailto:pascal.bleser@open-xchange.com">Pascal Bleser</a>
 */
public abstract class LoadResourceStream<X> {

    private final X result;

    protected LoadResourceStream(final Object anchor, final String resource) throws Exception {
        this(anchor.getClass(), resource);
    }

    protected LoadResourceStream(final Class<?> anchor, final String resource) throws Exception {
        final String fqname = anchor.getPackage().getName().replaceAll("\\.", "/") + "/" + resource;
        InputStream is = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fqname);
            if (is == null) {
                throw new IOException(String.format("failed to find resource '%s'", fqname));
            }
            result = withResource(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    LoggerFactory.getLogger(this.getClass()).error(String.format("failed to close input stream for resource '%s'", fqname), e);
                }
            }
        }
    }

    protected abstract X withResource(final InputStream inputStream) throws Exception;

    public final X result() {
        return result;
    }

}
