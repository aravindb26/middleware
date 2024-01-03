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
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.scheduling.changes.MessageContext;
import com.openexchange.chronos.scheduling.changes.Sentence.ArgumentType;
import com.openexchange.chronos.scheduling.common.Messages;

/**
 * {@link OnBehalfOfResource} - Generates sentences related to a resource
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
class OnBehalfOfResource implements DelegationState {

    private final MessageContext messageContext;
    private final CalendarUser originator;
    private final CalendarUser resource;
    private final Event update;

    /**
     * 
     * Initializes a new {@link OnBehalfOfResource}.
     *
     * @param messageContext The message context
     * @param originator The originator of the message
     * @param update The updated event
     */
    public OnBehalfOfResource(MessageContext messageContext, CalendarUser originator, Event update) {
        super();
        this.messageContext = messageContext;
        this.originator = originator.getSentBy();
        this.resource = originator;
        this.update = update;
    }

    /*
     * ============================== Send as booking delegate ==============================
     */

    @Override
    public String statusChange(ParticipationStatus status) {
        String msg;
        if (status.equals(ParticipationStatus.ACCEPTED)) {
            msg = Messages.RESOURCE_ACCEPTED;
        } else if (status.equals(ParticipationStatus.DECLINED)) {
            msg = Messages.RESOURCE_DECLINED;
        } else if (status.equals(ParticipationStatus.TENTATIVE)) {
            msg = Messages.RESOURCE_TENTATIVE;
        } else {
            msg = Messages.RESOURCE_NEEDS_ACTION;
        }
        return new SentenceImpl(msg)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
            .add(update.getSummary(), ArgumentType.ITALIC)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String statusChangeInstance(ParticipationStatus status, String ofSeries) {
        String msg;
        if (status.equals(ParticipationStatus.ACCEPTED)) {
            msg = Messages.RESOURCE_ACCEPTED_INSTANCE;
        } else if (status.equals(ParticipationStatus.DECLINED)) {
            msg = Messages.RESOURCE_DECLINED_INSTANCE;
        } else if (status.equals(ParticipationStatus.TENTATIVE)) {
            msg = Messages.RESOURCE_TENTATIVE_INSTANCE;
        } else {
            msg = Messages.RESOURCE_NEEDS_ACTION_INSTANCE;
        }
        return new SentenceImpl(msg)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
            .add(ofSeries, ArgumentType.ITALIC)
            .getMessage(messageContext);//@formatter:on
    }

    /*
     * ============================== Receive as booking delegate ==============================
     */

    @Override
    public String getDeleteIntroduction() {
        if (isOriginatorActing()) {
            return new SentenceImpl(Messages.RESOURCE_DELETE)//@formatter:off
                .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
                .add(update.getSummary(), ArgumentType.ITALIC)
                .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
                .getMessage(messageContext);//@formatter:on
        }
        return new SentenceImpl(Messages.RESOURCE_DELETE_ON_BEHALF)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
            .add(update.getSummary(), ArgumentType.ITALIC)
            .add(getDisplayName(originator.getSentBy()), ArgumentType.PARTICIPANT)
            .getMessage(messageContext);//@formatter:on

    }

    @Override
    public String getDeleteInstanceIntroduction(String ofSeries) {
        if (isOriginatorActing()) {
            return new SentenceImpl(Messages.RESOURCE_DELETE_INSTANCE)//@formatter:off
                .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
                .add(ofSeries, ArgumentType.ITALIC)
                .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
                .getMessage(messageContext);//@formatter:on
        }
        return new SentenceImpl(Messages.RESOURCE_DELETE_ON_BEHALF_INSTANCE)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
            .add(ofSeries, ArgumentType.ITALIC)
            .add(getDisplayName(originator.getSentBy()), ArgumentType.PARTICIPANT)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getUpdateIntroduction() {
        if (isOriginatorActing()) {
            return new SentenceImpl(Messages.RESOURCE_UPDATE)//@formatter:off
                .add(update.getSummary(), ArgumentType.ITALIC)
                .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
                .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
                .getMessage(messageContext);//@formatter:on
        }
        return new SentenceImpl(Messages.RESOURCE_UPDATE_ON_BEHALF)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(update.getSummary(), ArgumentType.ITALIC)
            .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
            .add(getDisplayName(originator.getSentBy()), ArgumentType.PARTICIPANT)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getUpdateInstanceIntroduction(String ofSeries) {
        if (isOriginatorActing()) {
            return new SentenceImpl(Messages.RESOURCE_UPDATE_INSTANCE)//@formatter:off
                .add(ofSeries, ArgumentType.ITALIC)
                .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
                .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
                .getMessage(messageContext);//@formatter:on
        }
        return new SentenceImpl(Messages.RESOURCE_UPDATE_ON_BEHALF_INSTANCE)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(ofSeries, ArgumentType.ITALIC)
            .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
            .add(getDisplayName(originator.getSentBy()), ArgumentType.PARTICIPANT)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getDeclineCounterIntroduction() {
        return ""; // Doesn't make sense here
    }

    @Override
    public String getCreateIntroduction() {
        if (isOriginatorActing()) {
            return new SentenceImpl(Messages.RESOURCE_CREATE)//@formatter:off
                .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
                .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
                .add(update.getSummary(), ArgumentType.ITALIC)
                .getMessage(messageContext);//@formatter:on
        }
        return new SentenceImpl(Messages.RESOURCE_CREATE_ON_BEHALF)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(getDisplayName(originator.getSentBy()), ArgumentType.PARTICIPANT)
            .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
            .add(update.getSummary(), ArgumentType.ITALIC)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getCreateInstanceIntroduction(String ofSeries) {
        if (isOriginatorActing()) {
            return new SentenceImpl(Messages.RESOURCE_CREATE_INSTANCE)//@formatter:off
                .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
                .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
                .add(ofSeries, ArgumentType.ITALIC)
                .getMessage(messageContext);//@formatter:on
        }
        return new SentenceImpl(Messages.RESOURCE_CREATE_ON_BEHALF_INSTANCE)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(getDisplayName(originator.getSentBy()), ArgumentType.PARTICIPANT)
            .add(getDisplayName(resource), ArgumentType.PARTICIPANT)
            .add(ofSeries, ArgumentType.ITALIC)
            .getMessage(messageContext);//@formatter:on
    }

    /*
     * ============================== HELPERS ==============================
     */

    /**
     * Gets a value indicating whether the originator of the message is acting on its own
     * or if a delegate has acted
     *
     * @return <code>true</code> if the originator acted, <code>false</code> if a delegate acted instead
     */
    private boolean isOriginatorActing() {
        return null == originator.getSentBy();
    }

}
