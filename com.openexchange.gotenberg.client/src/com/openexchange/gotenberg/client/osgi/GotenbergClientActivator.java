
package com.openexchange.gotenberg.client.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.gotenberg.client.GotenbergClientAccess;
import com.openexchange.gotenberg.client.impl.GotenbergClientAccessImpl;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link GotenbergClientActivator}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class GotenbergClientActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(GotenbergClientActivator.class);
    private GotenbergClientAccessImpl gotenbergClientAccess;

    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            LOG.info("Starting bundle '{}'", context.getBundle().getSymbolicName());
            gotenbergClientAccess = new GotenbergClientAccessImpl(this);
            registerService(GotenbergClientAccess.class, gotenbergClientAccess);
        } catch (Exception e) {
            LOG.error("Failed to start bundle {}", context.getBundle().getSymbolicName(), e);
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("Stopping bundle '{}'", context.getBundle().getSymbolicName());
        try {
            if (gotenbergClientAccess != null) {
                gotenbergClientAccess.destroyClient();
            }
        } catch (OXException e) {
            LOG.error(e.getMessage(), e);
        }
        super.stopBundle();
    }
}
