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

package com.openexchange.chronos.scheduling.impl.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.ical.ICalService;
import com.openexchange.chronos.scheduling.ITipProcessor;
import com.openexchange.chronos.scheduling.ITipProcessorService;
import com.openexchange.chronos.scheduling.IncomingSchedulingMailFactory;
import com.openexchange.chronos.scheduling.MessageStatusService;
import com.openexchange.chronos.scheduling.SchedulingBroker;
import com.openexchange.chronos.scheduling.TransportProvider;
import com.openexchange.chronos.scheduling.changes.SentenceFactory;
import com.openexchange.chronos.scheduling.common.MailPushListener;
import com.openexchange.chronos.scheduling.impl.ITipProcessorServiceImpl;
import com.openexchange.chronos.scheduling.impl.MessageStatusServiceImpl;
import com.openexchange.chronos.scheduling.impl.SchedulingBrokerImpl;
import com.openexchange.chronos.scheduling.impl.incoming.IncomingSchedulingMailFactoryImpl;
import com.openexchange.chronos.scheduling.impl.incoming.IncomingSchedulingMailListener;
import com.openexchange.chronos.scheduling.impl.transport.AllServingTransportProvider;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.contact.ContactService;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.html.HtmlService;
import com.openexchange.lock.LockService;
import com.openexchange.mail.MailFetchListener;
import com.openexchange.mail.api.crypto.CryptographicAwareMailAccessFactory;
import com.openexchange.mail.login.resolver.MailLoginResolverService;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailmapping.MailResolverService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.ServiceSet;
import com.openexchange.regional.RegionalSettingsService;
import com.openexchange.user.UserService;
import com.openexchange.version.VersionService;

/**
 * {@link SchedulingActivator}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class SchedulingActivator extends HousekeepingActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulingActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { ConfigurationService.class, ContextService.class, ICalService.class, UserService.class,//
            DatabaseService.class, LeanConfigurationService.class, SentenceFactory.class, RecurrenceService.class,//
            LockService.class, MailResolverService.class, MailLoginResolverService.class };
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class[] { CryptographicAwareMailAccessFactory.class, RegionalSettingsService.class, ContactService.class, HtmlService.class, UserService.class, VersionService.class, MailAccountStorageService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        LOGGER.info("Starting calendar scheduling related services");
        /*
         * Register processor service
         */
        ServiceSet<ITipProcessor> processors = new ServiceSet<ITipProcessor>();
        track(ITipProcessor.class, processors);
        registerService(ITipProcessorService.class, new ITipProcessorServiceImpl(processors, this));
        /*
         * Register message status service
         */
        MessageStatusServiceImpl messageStatusService = new MessageStatusServiceImpl(this);
        registerService(MessageStatusService.class, messageStatusService);

        SchedulingBrokerImpl broker = new SchedulingBrokerImpl(context);
        /*
         * Register service tracker
         */
        track(TransportProvider.class, broker);
        openTrackers();
        /*
         * Register broker as service and IItipProcessor
         */
        registerService(SchedulingBroker.class, broker);
        registerService(ITipProcessor.class, broker);
        /*
         * Register factory
         */
        IncomingSchedulingMailFactoryImpl factory = new IncomingSchedulingMailFactoryImpl(this);
        registerService(IncomingSchedulingMailFactory.class, factory);
        /*
         * Register listener for auto-scheduling
         */
        IncomingSchedulingMailListener mailListener = new IncomingSchedulingMailListener(this, factory, messageStatusService);
        registerService(MailFetchListener.class, mailListener);
        registerService(MailPushListener.class, mailListener);
        /*
         * register default general-purpose transport provider
         */
        registerService(TransportProvider.class, new AllServingTransportProvider(this));
    }

}
