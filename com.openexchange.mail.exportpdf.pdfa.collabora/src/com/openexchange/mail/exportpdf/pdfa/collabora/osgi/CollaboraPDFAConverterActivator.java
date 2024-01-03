
package com.openexchange.mail.exportpdf.pdfa.collabora.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.mail.exportpdf.pdfa.PDFAConverter;
import com.openexchange.mail.exportpdf.pdfa.collabora.impl.CollaboraPDFAConverter;
import com.openexchange.osgi.HousekeepingActivator;

public class CollaboraPDFAConverterActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboraPDFAConverterActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("Starting bundle '{}'", context.getBundle().getSymbolicName());
        registerService(PDFAConverter.class, new CollaboraPDFAConverter(this), 100);
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("Stopping bundle '{}'", context.getBundle().getSymbolicName());
        super.stopBundle();
    }
}
