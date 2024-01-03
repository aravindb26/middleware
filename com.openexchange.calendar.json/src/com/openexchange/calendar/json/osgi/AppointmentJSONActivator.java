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

package com.openexchange.calendar.json.osgi;

import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.requesthandler.osgiservice.AJAXModuleActivator;
import com.openexchange.calendar.json.AppointmentActionFactory;
import com.openexchange.calendar.json.converters.AppointmentResultConverter;
import com.openexchange.calendar.json.converters.EventResultConverter;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.chronos.itip.json.actions.ITipActionFactory;
import com.openexchange.chronos.scheduling.ITipProcessorService;
import com.openexchange.chronos.scheduling.IncomingSchedulingMailFactory;
import com.openexchange.chronos.scheduling.MessageStatusService;
import com.openexchange.chronos.scheduling.SchedulingBroker;
import com.openexchange.chronos.service.CalendarService;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.conversion.ConversionService;
import com.openexchange.groupware.userconfiguration.Permission;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.user.UserService;

/**
 * {@link AppointmentJSONActivator}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class AppointmentJSONActivator extends AJAXModuleActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { CalendarService.class, UserService.class, RecurrenceService.class, ConversionService.class,//
            ITipProcessorService.class, MessageStatusService.class, SchedulingBroker.class, IncomingSchedulingMailFactory.class };
    }

    @Override
    protected void startBundle() throws Exception {
        //        final Dictionary<String, Integer> props = new Hashtable<String, Integer>(1, 1);
        //        props.put(TargetService.MODULE_PROPERTY, I(Types.APPOINTMENT));
        //        registerService(TargetService.class, new ModifyThroughDependant(), props);
        registerModule(new AppointmentActionFactory(this), "calendar");
        registerService(ResultConverter.class, new AppointmentResultConverter(this));
        registerService(ResultConverter.class, new EventResultConverter(this));

        ServiceTracker<CapabilityService, CapabilityService> capabilityTracker = track(CapabilityService.class);
        rememberTracker(capabilityTracker);
        openTrackers();

        registerModule(new ITipActionFactory(this), "calendar/itip");
        registerScope(RestrictedAction.Type.READ, AppointmentActionFactory.MODULE, OAuthScopeDescription.READ_ONLY, Permission.CALENDAR);
        registerScope(RestrictedAction.Type.WRITE, AppointmentActionFactory.MODULE, OAuthScopeDescription.WRITABLE, Permission.CALENDAR);

        trackService(ObjectUseCountService.class);
        openTrackers();
    }
}
