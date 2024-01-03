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
import java.util.function.Function;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.alarm.mail.osgi.Services;
import com.openexchange.chronos.scheduling.changes.ContextSensitiveMessages;
import com.openexchange.chronos.scheduling.changes.ContextSensitiveMessages.Context;
import com.openexchange.chronos.scheduling.changes.SentenceFactory;
import com.openexchange.chronos.scheduling.common.Messages;
import com.openexchange.i18n.tools.StringHelper;

/**
 * {@link ParticipantHelper}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ParticipantHelper {

    private final Locale recipientLocale;

    /**
     * Initializes a new {@link ParticipantHelper}.
     *
     * @param recipientLocale The locale for the recipient
     */
    public ParticipantHelper(final Locale recipientLocale) {
        super();
        this.recipientLocale = recipientLocale;
    }

    /**
     * Generates the textual description for an participant status change of an single user
     *
     * @param participant The participant
     * @return The line
     */
    public String participantLine(final NotificationParticipant participant) {
        final String sConfirmStatus;
        ParticipationStatus status = participant.getConfirmStatus();
        if (status == null) {
            sConfirmStatus = StringHelper.valueOf(recipientLocale).getString(Messages.WAITING);
        } else if (status.equals(ParticipationStatus.ACCEPTED)) {
            sConfirmStatus = getMessage("accepted", (m) -> m.accepted(Context.ADJECTIVE));
        } else if (status.equals(ParticipationStatus.DECLINED)) {
            sConfirmStatus = getMessage("declined", (m) -> m.declined(Context.ADJECTIVE));
        } else if (status.equals(ParticipationStatus.TENTATIVE)) {
            sConfirmStatus = getMessage("tentatively accepted", (m) -> m.tentative(Context.ADJECTIVE));
        } else {
            sConfirmStatus = StringHelper.valueOf(recipientLocale).getString(Messages.WAITING);
        }
        return new StringBuilder(24).append(participant.getDisplayName()).append(" (").append(sConfirmStatus).append(')').toString();
    }

    private String getMessage(String fallback, Function<ContextSensitiveMessages, String> f) {
        SentenceFactory factory = Services.getService(SentenceFactory.class);
        if (null == factory) {
            return fallback;
        }
        return f.apply(factory.create(recipientLocale));
    }

}
