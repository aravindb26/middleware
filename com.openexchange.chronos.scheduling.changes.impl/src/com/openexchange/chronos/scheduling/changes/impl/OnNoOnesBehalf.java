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

class OnNoOnesBehalf implements DelegationState {

    private MessageContext messageContext;
    private CalendarUser originator;
    private Event update;

    /**
     * Initializes a new {@link OnNoOnesBehalf}.
     *
     * @param labelHelper
     */
    OnNoOnesBehalf(MessageContext messageContext, CalendarUser originator, Event update) {
        this.messageContext = messageContext;
        this.originator = originator;
        this.update = update;
    }

    @Override
    public String statusChange(ParticipationStatus status) {
        String msg = null;
        String statusString = "";
        if (ParticipationStatus.ACCEPTED.equals(status) || ParticipationStatus.TENTATIVE.equals(status) || ParticipationStatus.DECLINED.equals(status)) {
            msg = Messages.STATUS_CHANGED_INTRO;
        } else {
            msg = Messages.NONE_INTRO;
            statusString = Messages.NONE;
        }
        return new SentenceImpl(msg)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(statusString, ArgumentType.STATUS, status)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String statusChangeInstance(ParticipationStatus status, String ofSeries) {
        String msg;
        String statusString;
        if (ParticipationStatus.ACCEPTED.matches(status) || ParticipationStatus.DECLINED.matches(status) || ParticipationStatus.TENTATIVE.matches(status)) {
            msg = Messages.STATUS_CHANGED_INSTANCE_INTRO;
            statusString = "";
        } else {
            msg = Messages.NONE_INSTANCE_INTRO;
            statusString = Messages.NONE;
        }
        return new SentenceImpl(msg)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(statusString, ArgumentType.STATUS, status)
            .add(ofSeries, ArgumentType.ITALIC)
            .getMessage(messageContext)//@formatter:on
        ;
    }

    @Override
    public String getDeleteIntroduction() {
        return new SentenceImpl(Messages.DELETE_INTRO)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getDeleteInstanceIntroduction(String ofSeries) {
        return new SentenceImpl(Messages.DELETE_INSTANCE_INTRO)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(ofSeries, ArgumentType.ITALIC)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getUpdateIntroduction() {
        return new SentenceImpl(Messages.UPDATE_INTRO)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getUpdateInstanceIntroduction(String ofSeries) {
        return new SentenceImpl(Messages.UPDATE_INSTANCE_INTRO)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(ofSeries, ArgumentType.ITALIC)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getDeclineCounterIntroduction() {
        return new SentenceImpl(Messages.DECLINECOUNTER_INTRO)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .add(update.getSummary(), ArgumentType.UPDATED)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getCreateIntroduction() {
        return new SentenceImpl(Messages.CREATE_INTRO)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .getMessage(messageContext);//@formatter:on
    }

    @Override
    public String getCreateInstanceIntroduction(String ofSeries) {
        return new SentenceImpl(Messages.CREATE_INTRO)//@formatter:off
            .add(getDisplayName(originator), ArgumentType.PARTICIPANT)
            .getMessage(messageContext);//@formatter:on
    }
}
