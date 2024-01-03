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

import static com.openexchange.chronos.scheduling.common.Utils.getDisplayName;
import static com.openexchange.chronos.scheduling.common.Utils.isResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.scheduling.RecipientSettings;
import com.openexchange.chronos.scheduling.changes.MessageContext;
import com.openexchange.chronos.scheduling.changes.Sentence.ArgumentType;
import com.openexchange.chronos.scheduling.common.Messages;
import com.openexchange.chronos.scheduling.common.Utils;
import com.openexchange.html.HtmlService;
import com.openexchange.html.tools.HTMLUtils;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;

/**
 * {@link LabelHelper} - For external recipients
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a> - Adjusted to new stack
 */
public class LabelHelper {

    final Event update;
    final CalendarUser originator;

    final MessageContext messageContext;

    private final String comment;
    private final DelegationState delegationState;
    private final DateHelper dateHelper;
    private final ServiceLookup serviceLookup;
    private final Event seriesMaster;
    private final RecipientSettings recipientSettings;

    /**
     * Initializes a new {@link LabelHelper}.
     *
     * @param serviceLookup The services
     * @param update The {@link Event} to generate the mail for
     * @param seriesMaster The series master event if changes affect a recurrence instance, <code>null</code>, otherwise
     * @param originator The originator
     * @param recipientSettings The regional settings
     * @param comment The comment to set
     * @param messageContext The message context to use
     */
    public LabelHelper(ServiceLookup serviceLookup, Event update, Event seriesMaster, CalendarUser originator, RecipientSettings recipientSettings, String comment, MessageContext messageContext) {
        super();
        this.messageContext = messageContext;
        this.update = update;
        this.recipientSettings = recipientSettings;
        this.originator = originator;
        this.comment = comment;
        this.seriesMaster = seriesMaster;
        this.delegationState = getDelegationState(update, originator, recipientSettings, messageContext);
        this.dateHelper = new DateHelper(update, recipientSettings.getLocale(), recipientSettings.getTimeZone(), recipientSettings.getRegionalSettings());
        this.serviceLookup = serviceLookup;
    }

    /*
     * ============================== HELPERS ==============================
     */

    /**
     * Gets a certain delegation state from which context sensitive sentences for the message are built.
     *
     * @param update The {@link Event} to generate the mail for
     * @param originator The originator
     * @param recipientSettings The regional settings
     * @param messageContext The message context to use
     * @return The {@link DelegationState}
     */
    private static DelegationState getDelegationState(Event update, CalendarUser originator, RecipientSettings settings, MessageContext messageContext) {
        /*
         * Check if a delegate acted on behalf of a calendar user
         */
        if (null != originator.getSentBy()) {
            if (CalendarUtils.matches(originator, settings.getRecipient())) {
                /*
                 * Message to the calendar user from its delegate
                 */
                return new OnMyBehalf(messageContext, originator);
            }
            if (isResource(originator, update.getAttendees())) {
                /*
                 * Message from or to the booking delegate
                 */
                return new OnBehalfOfResource(messageContext, originator, update);
            }
            /*
             * Message to e.g. organizer from the delegate on behalf of the calendar user
             */
            return new OnBehalfOfAnother(messageContext, originator, update);
        }
        /*
         * Message e.g. from calendar user to organizer
         */
        return new OnNoOnesBehalf(messageContext, originator, update);
    }

    private boolean useInstanceIntroduction() {
        return null != update.getRecurrenceId() && null != seriesMaster && Strings.isNotEmpty(seriesMaster.getSummary());
    }

    /*
     * ============================== Sentences written to mail ==============================
     * Methods are called from within the templating engine, do not delete unused methods!
     */

    private String getStatusChangeIntroduction(ParticipationStatus status) {
        if (useInstanceIntroduction()) {
            return delegationState.statusChangeInstance(status, seriesMaster.getSummary());
        }
        return delegationState.statusChange(status);
    }

    public String getShowAs() {
        if (update.getTransp() != null && Transp.TRANSPARENT.equals(update.getTransp().getValue())) {
            return new SentenceImpl(Messages.FREE).getMessage(messageContext);
        }
        return new SentenceImpl(Messages.RESERVERD).getMessage(messageContext);
    }

    public String getShowAsClass() {
        if (update.getTransp() != null && update.getTransp().getValue() != null && Transp.TRANSPARENT.equals(update.getTransp().getValue())) {
            return "free";
        }
        return "reserved";
    }

    public String getNoteAsHTML() {
        final String note = update.getDescription();
        if (note == null) {
            return "";
        }
        HtmlService htmlService = serviceLookup.getOptionalService(HtmlService.class);
        if (null == htmlService) {
            return "";
        }
        return new HTMLUtils(htmlService).htmlFormat(note);
    }

    // Sentences
    public String getAcceptIntroduction() {
        return getStatusChangeIntroduction(ParticipationStatus.ACCEPTED);
    }

    public String getDeclineIntroduction() {
        return getStatusChangeIntroduction(ParticipationStatus.DECLINED);
    }

    public String getTentativeIntroduction() {
        return getStatusChangeIntroduction(ParticipationStatus.TENTATIVE);
    }

    public String getNoneIntroduction() {
        return getStatusChangeIntroduction(ParticipationStatus.NEEDS_ACTION);
    }

    public String getCounterOrganizerIntroduction() {
        return new SentenceImpl(Messages.COUNTER_ORGANIZER_INTRO).add(getDisplayName(originator), ArgumentType.PARTICIPANT).getMessage(messageContext);
    }

    public String getCounterParticipantIntroduction() {
        return new SentenceImpl(Messages.COUNTER_PARTICIPANT_INTRO).add(getDisplayName(originator), ArgumentType.PARTICIPANT).add(getDisplayName(update.getOrganizer()), ArgumentType.PARTICIPANT).getMessage(messageContext);
    }

    public String getCreateIntroduction() {
        if (useInstanceIntroduction()) {
            return delegationState.getCreateInstanceIntroduction(seriesMaster.getSummary());
        }
        return delegationState.getCreateIntroduction();
    }

    public String getCreateExceptionIntroduction() {
        return new SentenceImpl(Messages.CREATE_EXCEPTION_INTRO).add(getDisplayName(originator), ArgumentType.PARTICIPANT).add(dateHelper.getRecurrenceDatePosition(), ArgumentType.UPDATED).getMessage(messageContext);
    }

    public String getRefreshIntroduction() {
        return new SentenceImpl(Messages.REFRESH_INTRO).add(getDisplayName(originator), ArgumentType.PARTICIPANT).add(update.getSummary(), ArgumentType.UPDATED).getMessage(messageContext);
    }

    public String getDeclineCounterIntroduction() {
        return delegationState.getDeclineCounterIntroduction();
    }

    public String getUpdateIntroduction() {
        if (useInstanceIntroduction()) {
            return delegationState.getUpdateInstanceIntroduction(seriesMaster.getSummary());
        }
        return delegationState.getUpdateIntroduction();
    }

    public String getComment() {
        if (Strings.isEmpty(comment)) {
            return null;
        }
        return new SentenceImpl(Messages.COMMENT_INTRO).add(comment, ArgumentType.ITALIC).getMessage(messageContext);
    }

    public String getDeleteIntroduction() {
        if (useInstanceIntroduction()) {
            return delegationState.getDeleteInstanceIntroduction(seriesMaster.getSummary());
        }
        return delegationState.getDeleteIntroduction();
    }

    public String getWhenLabel() {
        return new SentenceImpl(Messages.LABEL_WHEN).getMessage(messageContext);
    }

    public String getWhereLabel() {
        return new SentenceImpl(Messages.LABEL_WHERE).getMessage(messageContext);
    }

    public String getConferencesLabel() {
        return new SentenceImpl(Messages.LABEL_CONFERENCES).getMessage(messageContext);
    }

    public String getParticipantsLabel() {
        return new SentenceImpl(Messages.LABEL_PARTICIPANTS).getMessage(messageContext);
    }

    public String getResourcesLabel() {
        return new SentenceImpl(Messages.LABEL_RESOURCES).getMessage(messageContext);
    }

    public String getDetailsLabel() {
        return new SentenceImpl(Messages.LABEL_DETAILS).getMessage(messageContext);
    }

    public String getShowAsLabel() {
        return new SentenceImpl(Messages.LABEL_SHOW_AS).getMessage(messageContext);
    }

    public String getCreatedLabel() {
        return new SentenceImpl(Messages.LABEL_CREATED).getMessage(messageContext);
    }

    public String getModifiedLabel() {
        return new SentenceImpl(Messages.LABEL_MODIFIED).getMessage(messageContext);
    }

    public String getCreator() {
        return Utils.getDisplayName(update.getOrganizer());
    }

    public String getModifier() {
        if (update.getModifiedBy() == null) {
            return "Unknown";
        }
        return Utils.getDisplayName(update.getModifiedBy());
    }

    public String getTimezoneInfo() {
        String displayName = messageContext.getTimeZone().getDisplayName(messageContext.getLocale());
        return new SentenceImpl(Messages.TIMEZONE).add(displayName, ArgumentType.EMPHASIZED).getMessage(messageContext);
    }

    public String getJustification() {
        if (CalendarUtils.isResourceOrRoom(recipientSettings.getRecipient(), recipientSettings.getRecipientType())) {
            return new SentenceImpl(Messages.RESOURCE_MANAGER_JUSTIFICATION).add(Utils.getDisplayName(recipientSettings.getRecipient()), ArgumentType.PARTICIPANT).getMessage(messageContext);
        }
        if (isResource(originator, update.getAttendees()) && CalendarUtils.matches(originator, recipientSettings.getRecipient().getSentBy())) {
            return new SentenceImpl(Messages.RESOURCE_MANAGER_JUSTIFICATION).add(Utils.getDisplayName(originator), ArgumentType.PARTICIPANT).getMessage(messageContext);
        }
        if (CalendarUtils.matches(recipientSettings.getRecipient(), update.getOrganizer())) {
            return new SentenceImpl(Messages.ORGANIZER_JUSTIFICATION).getMessage(messageContext);
        }
        return null;
    }

}
