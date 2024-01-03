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

package com.openexchange.chronos.alarm.mail.notification;

import java.util.Locale;
import java.util.TimeZone;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.scheduling.changes.MessageContext;
import com.openexchange.chronos.scheduling.changes.Sentence;
import com.openexchange.chronos.scheduling.changes.Sentence.ArgumentType;
import com.openexchange.chronos.scheduling.changes.SentenceFactory;
import com.openexchange.chronos.scheduling.common.Messages;
import com.openexchange.chronos.scheduling.common.description.TypeWrapper;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptions;
import com.openexchange.html.HtmlService;
import com.openexchange.html.tools.HTMLUtils;
import com.openexchange.regional.RegionalSettings;
import com.openexchange.server.ServiceLookup;

/**
 * {@link LabelHelper}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class LabelHelper {

    private final HTMLUtils html;
    private TimeZone timezone;
    private final ServiceLookup services;

    protected final AlarmNotificationMail mail;
    protected final TypeWrapper wrapper;
    protected final Locale locale;

    public LabelHelper(final TimeZone timezone, final AlarmNotificationMail mail, final Locale locale, final TypeWrapper wrapper, final ServiceLookup services) throws OXException {
        super();
        this.services = services;
        this.mail = mail;
        this.locale = locale;
        this.wrapper = wrapper;
        this.html = new HTMLUtils(services.getService(HtmlService.class));
        this.timezone = timezone;
        if (timezone == null) {
            this.timezone = TimeZone.getDefault(); // Fallback
        }

        if (this.mail == null || this.mail.getEvent() == null) {
            throw OXExceptions.general("Mandatory field mail/event missing.");
        }
    }

    public String getShowAs() {
        final Event event = mail.getEvent();

        if (event.getTransp() != null && Transp.TRANSPARENT.equals(event.getTransp().getValue())) {
            return getSentence(Messages.FREE).getMessage(wrapper.getFormat(), locale, null, null);
        }
        return getSentence(Messages.RESERVERD).getMessage(wrapper.getFormat(), locale, null, null);
    }

    public String getShowAsClass() {
        final Event event = mail.getEvent();
        if (event.getTransp() != null && event.getTransp().getValue() != null && Transp.TRANSPARENT.equals(event.getTransp().getValue())) {
            return "free";
        }
        return "reserved";
    }

    public String getNoteAsHTML() {
        final String note = mail.getEvent().getDescription();
        if (note == null) {
            return "";
        }
        return html.htmlFormat(note);
    }

    public String getWhenLabel() {
        return getSentence(Messages.LABEL_WHEN).getMessage(wrapper.getFormat(), locale, null, null);
    }

    public String getWhereLabel() {
        return getSentence(Messages.LABEL_WHERE).getMessage(wrapper.getFormat(), locale, null, null);
    }

    public String getParticipantsLabel() {
        return getSentence(Messages.LABEL_PARTICIPANTS).getMessage(wrapper.getFormat(), locale, null, null);
    }

    public String getResourcesLabel() {
        return getSentence(Messages.LABEL_RESOURCES).getMessage(wrapper.getFormat(), locale, null, null);
    }

    public String getDetailsLabel() {
        return getSentence(Messages.LABEL_DETAILS).getMessage(wrapper.getFormat(), locale, null, null);
    }

    public String getShowAsLabel() {
        return getSentence(Messages.LABEL_SHOW_AS).getMessage(wrapper.getFormat(), locale, null, null);
    }

    public String getCreatedLabel() {
        return getSentence(Messages.LABEL_CREATED).getMessage(wrapper.getFormat(), locale, null, null);
    }

    public String getTimezoneInfo() {
        return getSentence(Messages.TIMEZONE).add(timezone.getDisplayName(locale), ArgumentType.EMPHASIZED).getMessage(wrapper.getFormat(), locale, null, null);
    }

    Sentence getSentence(String message) {
        SentenceFactory factory = services.getService(SentenceFactory.class);
        if (null == factory) {
            return new Sentence() {

                @Override
                public String getMessage(String format, Locale locale, TimeZone timeZone, RegionalSettings regionalSettings) {
                    return "";
                }

                @Override
                public String getMessage(MessageContext messageContext) {
                    return "";
                }

                @Override
                public Sentence addStatus(ParticipationStatus status) {
                    return this;
                }

                @Override
                public Sentence add(Object argument, ArgumentType type, Object... extra) {
                    return this;
                }
            };
        }
        return factory.create(message);
    }
}
