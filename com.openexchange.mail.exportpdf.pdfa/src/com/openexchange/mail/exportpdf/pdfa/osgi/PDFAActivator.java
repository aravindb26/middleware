
package com.openexchange.mail.exportpdf.pdfa.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.mail.exportpdf.pdfa.PDFAService;
import com.openexchange.mail.exportpdf.pdfa.impl.PDFAServiceImpl;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link PDFAActivator}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class PDFAActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(PDFAActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            LOG.info("Starting bundle '{}'", context.getBundle().getSymbolicName());
            PDFAConverterTracker tracker = new PDFAConverterTracker(context);
            rememberTracker(tracker);
            openTrackers();
            registerService(PDFAService.class, new PDFAServiceImpl(tracker));
        } catch (Exception e) {
            LOG.error("Failed to start bundle {}", context.getBundle().getSymbolicName(), e);
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("Stopping bundle '{}'", context.getBundle().getSymbolicName());
        super.stopBundle();
    }
}
