/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.log.audit.slf4j.osgi;

import java.util.Locale;
import java.util.TimeZone;
import org.osgi.framework.ServiceRegistration;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.java.Strings;
import com.openexchange.log.audit.AuditLogFilter;
import com.openexchange.log.audit.AuditLogService;
import com.openexchange.log.audit.slf4j.Configuration;
import com.openexchange.log.audit.slf4j.SimpleDateFormatter;
import com.openexchange.log.audit.slf4j.Slf4jAuditLogService;
import com.openexchange.log.audit.slf4j.Slf4jAuditLoggingDateProperty;
import com.openexchange.log.audit.slf4j.Slf4jAuditLoggingProperty;
import com.openexchange.log.audit.slf4j.Slf4jLogLevel;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.NearRegistryServiceTracker;

/**
 * {@link Slf4jAuditLogActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.2
 */
public class Slf4jAuditLogActivator extends HousekeepingActivator {

    private ServiceRegistration<AuditLogService> serviceRegistration;
    private Slf4jAuditLogService service;

    private NearRegistryServiceTracker<AuditLogFilter> filters;

    /**
     * Initializes a new {@link Slf4jAuditLogActivator}.
     */
    public Slf4jAuditLogActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { LeanConfigurationService.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        NearRegistryServiceTracker<AuditLogFilter> filters = new FilterTracker(context);
        this.filters = filters;
        rememberTracker(filters);
        openTrackers();

        Reloadable reloadable = new Reloadable() {

            @Override
            public void reloadConfiguration(ConfigurationService configService) {
                unregisterService();
                registerService();
            }

            @Override
            public Interests getInterests() {
                //@formatter:off
                return DefaultInterests.builder().propertiesOfInterest(
                    Slf4jAuditLoggingProperty.delimiter.getFQPropertyName(),
                    Slf4jAuditLoggingProperty.enabled.getFQPropertyName(),
                    Slf4jAuditLoggingProperty.includeAttributeNames.getFQPropertyName(),
                    Slf4jAuditLoggingProperty.level.getFQPropertyName(),
                    Slf4jAuditLoggingDateProperty.locale.getFQPropertyName(),
                    Slf4jAuditLoggingDateProperty.pattern.getFQPropertyName(),
                    Slf4jAuditLoggingDateProperty.timezone.getFQPropertyName()
                ).build();
                //@formatter:on
            }
        };

        registerService();
        registerService(Reloadable.class, reloadable);
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        unregisterService();
        super.stopBundle();
    }

    /**
     * Registers audit logging.
     */
    synchronized void registerService() {
        if (null == serviceRegistration) {
            try {
                LeanConfigurationService leanConfigService = getService(LeanConfigurationService.class);
                Configuration configuration = buildConfiguration(leanConfigService);
                if (configuration.isEnabled()) {
                    Slf4jAuditLogService service = Slf4jAuditLogService.initInstance(configuration, filters);
                    this.service = service;
                    serviceRegistration = context.registerService(AuditLogService.class, service, null);
                }
            } catch (Exception e) {
                org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Slf4jAuditLogActivator.class);
                logger.error("Failed to register {}", AuditLogService.class.getSimpleName(), e);
            }
        }
    }

    /**
     * Unregisters audit logging.
     */
    synchronized void unregisterService() {
        ServiceRegistration<AuditLogService> serviceRegistration = this.serviceRegistration;
        if (null != serviceRegistration) {
            this.serviceRegistration = null;
            serviceRegistration.unregister();
        }

        Slf4jAuditLogService service = this.service;
        if (null != service) {
            this.service = null;
            service.shutDown();
        }
    }

    private Configuration buildConfiguration(LeanConfigurationService leanConfigService) {
        Configuration.Builder builder = new Configuration.Builder();

        builder.enabled(leanConfigService.getBooleanProperty(Slf4jAuditLoggingProperty.enabled));
        builder.level(Slf4jLogLevel.valueFor(leanConfigService.getProperty(Slf4jAuditLoggingProperty.level)));
        builder.delimiter(Strings.unquote(leanConfigService.getProperty(Slf4jAuditLoggingProperty.delimiter).trim()));
        builder.includeAttributeNames(leanConfigService.getBooleanProperty(Slf4jAuditLoggingProperty.includeAttributeNames));

        String datePattern = leanConfigService.getProperty(Slf4jAuditLoggingDateProperty.pattern);
        if (Strings.isNotEmpty(datePattern)) {
            Locale locale = LocaleTools.getLocale(leanConfigService.getProperty(Slf4jAuditLoggingDateProperty.locale).trim());
            TimeZone tz = TimeZone.getTimeZone(leanConfigService.getProperty(Slf4jAuditLoggingDateProperty.timezone).trim());
            builder.dateFormatter(SimpleDateFormatter.newInstance(datePattern, locale, tz));
        }

        return builder.build();
    }

}
