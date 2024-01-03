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

package com.openexchange.chronos.impl.scheduling;

import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.util.Locale;
import java.util.TimeZone;
import org.dmfs.rfc5545.DateTime;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.scheduling.RecipientSettings;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.java.util.TimeZones;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.regional.RegionalSettings;
import com.openexchange.regional.RegionalSettingsService;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.functions.ErrorAwareFunction;

/**
 * {@link DefaultRecipientSettings}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.3
 */
public class DefaultRecipientSettings implements RecipientSettings {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultRecipientSettings.class);

    private final int contextId;
    private final CalendarUser recipient;
    private final CalendarUserType recipientType;
    private final Locale locale;
    private final TimeZone timeZone;
    private final RegionalSettings regionalSettings;
    private final int msgFormat;

    /**
     * Initializes a new {@link DefaultRecipientSettings}.
     *
     * @param services A service lookup reference
     * @param session The calendar session
     * @param originator The originator of the message
     * @param recipient The recipient of the message
     * @param recipientType The calendar user type of the recipient
     * @param resource The resource
     */
    public DefaultRecipientSettings(ServiceLookup services, CalendarSession session, CalendarUser originator, CalendarUser recipient, CalendarUserType recipientType, CalendarObjectResource resource) {
        super();
        this.contextId = session.getContextId();
        this.recipient = recipient;
        this.recipientType = recipientType;
        this.msgFormat = selectMsgFormat(session, recipient, recipientType);
        this.locale = selectLocale(session, originator, recipient, recipientType);
        this.timeZone = selectTimeZone(session, originator, recipient, resource, recipientType);
        this.regionalSettings = optRegionalSettings(services, contextId, recipient, recipientType);
    }

    @Override
    public CalendarUser getRecipient() {
        return recipient;
    }

    @Override
    public CalendarUserType getRecipientType() {
        return recipientType;
    }

    @Override
    public int getMsgFormat() {
        return msgFormat;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public TimeZone getTimeZone() {
        return timeZone;
    }

    @Override
    public RegionalSettings getRegionalSettings() {
        return regionalSettings;
    }

    private static RegionalSettings optRegionalSettings(ServiceLookup services, int contextId, CalendarUser recipient, CalendarUserType recipientType) {
        if (isInternal(recipient, recipientType) && CalendarUserType.INDIVIDUAL.matches(recipientType)) {
            RegionalSettingsService regionalSettingsService = services.getOptionalService(RegionalSettingsService.class);
            if (null == regionalSettingsService) {
                LOG.warn("", ServiceExceptionCode.absentService(RegionalSettingsService.class));
                return null;
            }
            return regionalSettingsService.get(contextId, recipient.getEntity());
        }
        return null;
    }

    private static int selectMsgFormat(CalendarSession session, CalendarUser recipient, CalendarUserType recipientType) {
        if (isInternal(recipient, recipientType) && CalendarUserType.INDIVIDUAL.matches(recipientType)) {
            return session.getConfig().getMsgFormat(recipient.getEntity());
        }
        return UserSettingMail.MSG_FORMAT_BOTH;
    }

    private static Locale selectLocale(CalendarSession session, CalendarUser originator, CalendarUser recipient, CalendarUserType recipientType) {
        ErrorAwareFunction<Integer, Locale> localeFunction = (entity) -> session.getEntityResolver().getLocale(i(entity));
        return selectSetting(localeFunction, LocaleTools.DEFAULT_LOCALE, recipient, recipientType, originator.getSentBy(), originator);
    }

    private static TimeZone selectTimeZone(CalendarSession session, CalendarUser originator, CalendarUser recipient, CalendarObjectResource resource, CalendarUserType recipientType) {
        ErrorAwareFunction<Integer, TimeZone> timeZoneFunction = (entity) -> session.getEntityResolver().getTimeZone(i(entity));
        TimeZone fallback = TimeZones.UTC;
        if (null != resource) {
            DateTime startDate = resource.getFirstEvent().getStartDate();
            if (null != startDate && false == startDate.isFloating()) {
                fallback = startDate.getTimeZone();
            }
        }
        return selectSetting(timeZoneFunction, fallback, recipient, recipientType, originator.getSentBy(), originator);
    }

    private static <T> T selectSetting(ErrorAwareFunction<Integer, T> function, T fallback, CalendarUser recipient, CalendarUserType recipientType, CalendarUser... fallbackUsers) {
        if (isInternal(recipient, recipientType) && CalendarUserType.INDIVIDUAL.matches(recipientType)) {
            try {
                return function.apply(I(recipient.getEntity()));
            } catch (OXException e) {
                LOG.warn("Unable to evaluate recipient settings for {}, using fallback settings.", recipient, e);
            }
        }
        if (null != fallbackUsers) {
            for (CalendarUser calendarUser : fallbackUsers) {
                if (null == calendarUser) {
                    continue;
                }
                try {
                    return function.apply(I(calendarUser.getEntity()));
                } catch (OXException e) {
                    LOG.debug("Unable to evaluate recipient settings for {}, using fallback settings.", calendarUser, e);
                }
            }
        }
        return fallback;
    }

    @Override
    public String toString() {
        return "DefaultRecipientSettings [contextId=" + contextId + ", recipient=" + recipient + ", recipientType=" + recipientType + ", locale=" + locale + ", timeZone=" + timeZone + "]";
    }

}
