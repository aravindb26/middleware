package com.openexchange.mailmapping.ldap.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.mailmapping.MailResolver;
import com.openexchange.mailmapping.ldap.LDAPMailMappingConfig;
import com.openexchange.mailmapping.ldap.impl.LDAPMailMappingService;
import com.openexchange.osgi.HousekeepingActivator;

public class LDAPMailMappingActivator extends HousekeepingActivator implements Reloadable {
    
    private static final Logger LOG = LoggerFactory.getLogger(LDAPMailMappingActivator.class);

    /**
     * Initializes a new {@link LDAPMailMappingActivator}.
     */
    public LDAPMailMappingActivator() {
        super();
    }
	
    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        LDAPMailMappingConfig config = new LDAPMailMappingConfig(this);
        if (config.isEnabled()) {
            registerService(MailResolver.class, new LDAPMailMappingService(this, config));
        }
        registerService(Reloadable.class, this);
    }

    @Override
    public Interests getInterests() {
        return DefaultInterests.builder().propertiesOfInterest(
            LDAPMailMappingConfig.PROPERTY_ENABLED,
            LDAPMailMappingConfig.PROPERTY_CLIENTID, 
            LDAPMailMappingConfig.PROPERTY_SEARCH_FILTER, 
            LDAPMailMappingConfig.PROPERTY_SEARCH_SCOPE, 
            LDAPMailMappingConfig.PROPERTY_USERID_ATTRIBUTE, 
            LDAPMailMappingConfig.PROPERTY_CONTEXTID_ATTRIBUTE, 
            LDAPMailMappingConfig.PROPERTY_CACHE_EXPIRE
        ).build();
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        unregisterService(LDAPMailMappingService.class);
        try {
            LDAPMailMappingConfig config = new LDAPMailMappingConfig(this);
            if (config.isEnabled()) {
                registerService(MailResolver.class, new LDAPMailMappingService(this, config)); 
            }
        } catch (Exception e) {
            LOG.error("Unexpected error registering mail resolver", e);
        }
    }

}
