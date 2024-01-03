
package com.openexchange.multifactor.provider.webauthn.osgi;

import org.slf4j.Logger;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.multifactor.MultifactorProvider;
import com.openexchange.multifactor.provider.u2f.storage.U2FMultifactorDeviceStorage;
import com.openexchange.multifactor.provider.webauthn.impl.MultifactorWebAuthnProvider;
import com.openexchange.multifactor.provider.webauthn.impl.WebAuthnToken;
import com.openexchange.multifactor.storage.impl.MemoryMultifactorTokenStorage;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link WebAuthnProviderActivator}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.6
 */
public class WebAuthnProviderActivator extends HousekeepingActivator {

    static final Logger logger = org.slf4j.LoggerFactory.getLogger(WebAuthnProviderActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { LeanConfigurationService.class, U2FMultifactorDeviceStorage.class };
    }

    @Override
    protected void startBundle() throws Exception {
        logger.info("Starting bundle {}", context.getBundle().getSymbolicName());

        //@formatter:off
        final MultifactorWebAuthnProvider webAuthnProvider = new MultifactorWebAuthnProvider(
            this, 
            new MemoryMultifactorTokenStorage<WebAuthnToken>());
        //@formatter:on
        registerService(MultifactorProvider.class, webAuthnProvider);
    }

    @Override
    protected void stopBundle() throws Exception {
        logger.info("Stopping bundle {}", context.getBundle().getSymbolicName());
        super.stopBundle();
    }

}
