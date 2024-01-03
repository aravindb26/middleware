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

package com.openexchange.chronos.scheduling.changes.impl;

import java.util.Locale;
import com.openexchange.chronos.ParticipantRole;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.scheduling.changes.ContextSensitiveMessages.Context;
import com.openexchange.chronos.scheduling.common.Messages;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.java.Strings;

/**
 * {@link ParticipantHelper}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.3
 */
public class ParticipantHelper {

    private final Locale recipientLocale;

    /**
     * Initializes a new {@link ParticipantHelper}.
     *
     * @param recipientLocale The locale for the recipient
     */
    public ParticipantHelper(Locale recipientLocale) {
        super();
        this.recipientLocale = recipientLocale;
    }

    /**
     * Generates the textual description for an attendee's participation role (currently only used for
     * {@link ParticipantRole#OPT_PARTICIPANT}.
     * 
     * @param participant The participant
     * @return The role string, or <code>null</code> if not applicable
     */
    public String role(NotificationParticipant participant) {
        if (ParticipantRole.OPT_PARTICIPANT.matches(participant.getRole())) {
            return StringHelper.valueOf(recipientLocale).getString(Messages.PARTICIPANT_ROLE_OPTIONAL);
        }
        return null;
    }

    /**
     * Generates the textual description for an participant status change of an single user
     *
     * @param participant The participant
     * @return The line
     */
    public String participantLine(NotificationParticipant participant) {
        /*
         * begin with display name
         */
        StringBuilder stringBuilder = new StringBuilder(64).append(participant.getDisplayName());
        /*
         * optionally append role
         */
        String role = role(participant);
        if (Strings.isNotEmpty(role)) {
            stringBuilder.append(' ').append(role); // already in parentheses
        }
        /*
         * append participation status
         */
        stringBuilder.append(" (");
        if (ParticipationStatus.ACCEPTED.matches(participant.getConfirmStatus())) {
            stringBuilder.append(new ContextSensitiveMessagesImpl(recipientLocale).accepted(Context.ADJECTIVE));
        } else if (ParticipationStatus.DECLINED.matches(participant.getConfirmStatus())) {
            stringBuilder.append(new ContextSensitiveMessagesImpl(recipientLocale).declined(Context.ADJECTIVE));
        } else if (ParticipationStatus.TENTATIVE.matches(participant.getConfirmStatus())) {
            stringBuilder.append(new ContextSensitiveMessagesImpl(recipientLocale).tentative(Context.ADJECTIVE));
        } else {
            stringBuilder.append(StringHelper.valueOf(recipientLocale).getString(Messages.WAITING));
        }
        stringBuilder.append(')');
        /*
         * optionally append comment
         */
        String comment = participant.getComment();
        if (Strings.isNotEmpty(comment)) {
            stringBuilder.append(" (\"").append(comment).append("\")");
        }
        return stringBuilder.toString();
    }

    /**
     * Generates the textual description for an changed conference
     *
     * @param conference The conference
     * @return The line
     */
    public String conferenceLine(NotificationConference conference) {
        if (Strings.isEmpty(conference.getLabel())) {
            return conference.getUri();
        }
        return conference.getLabel() + ": " + conference.getUri();
    }

}
