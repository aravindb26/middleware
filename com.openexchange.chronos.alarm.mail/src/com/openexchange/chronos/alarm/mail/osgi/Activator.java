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

package com.openexchange.chronos.alarm.mail.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.alarm.mail.impl.MailAlarmNotificationServiceImpl;
import com.openexchange.chronos.alarm.message.AlarmNotificationService;
import com.openexchange.chronos.scheduling.changes.SentenceFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.html.HtmlService;
import com.openexchange.i18n.TranslatorFactory;
import com.openexchange.notification.mail.NotificationMailFactory;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.regional.RegionalSettingsService;
import com.openexchange.resource.ResourceService;
import com.openexchange.serverconfig.ServerConfigService;
import com.openexchange.templating.TemplateService;
import com.openexchange.user.UserService;

/**
 * {@link Activator}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.1
 */
public class Activator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ContextService.class, ServerConfigService.class, NotificationMailFactory.class, TranslatorFactory.class, RegionalSettingsService.class,
            TemplateService.class, ResourceService.class, UserService.class, ConfigurationService.class, HtmlService.class, LeanConfigurationService.class, SentenceFactory.class};
    }

    @Override
    protected void startBundle() throws Exception {
        registerService(AlarmNotificationService.class, new MailAlarmNotificationServiceImpl(this));
        Services.setServiceLookup(this);
        LOG.info("Successfully started bundle {}", this.context.getBundle().getSymbolicName());
    }

    @Override
    protected void stopBundle() throws Exception {
        Services.setServiceLookup(null);
        super.stopBundle();
        LOG.info("Successfully stopped bundle {}", this.context.getBundle().getSymbolicName());
    }

}
