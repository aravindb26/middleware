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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.openexchange.chronos.Event;

/**
 * {@link AlarmNotificationMail}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.1
 */
public class AlarmNotificationMail {

    private String templateName;
    private String text;
    private String subject;

    private Event event;

    private NotificationParticipant recipient;
    private NotificationParticipant organizer;

    private List<NotificationParticipant> participants;
    private List<NotificationParticipant> resources;

    private NotificationParticipant actor;

    private boolean sortedParticipants;

    /**
     * Initializes a new {@link AlarmNotificationMail}.
     */
    public AlarmNotificationMail() {
        super();
    }

    /*
     * ============= SETTER and GETTER =============
     */

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event updated) {
        this.event = updated;
    }

    public NotificationParticipant getRecipient() {
        return recipient;
    }

    public void setRecipient(NotificationParticipant recipient) {
        this.recipient = recipient;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public NotificationParticipant getOrganizer() {
        return organizer;
    }

    public void setOrganizer(NotificationParticipant organizer) {
        this.organizer = organizer;
    }

    public void setParticipants(List<NotificationParticipant> recipients) {
        sortedParticipants = false;
        this.participants = recipients;
    }

    public List<NotificationParticipant> getParticipants() {
        if (!sortedParticipants) {
            if (participants == null) {
                return null;
            }
            Collections.sort(participants, new Comparator<NotificationParticipant>() {

                @Override
                public int compare(NotificationParticipant p1, NotificationParticipant p2) {
                    return p1.getDisplayName().compareTo(p2.getDisplayName());
                }

            });
        }
        return participants;
    }

    public void setResources(List<NotificationParticipant> resources) {
        this.resources = resources;
    }

    public List<NotificationParticipant> getResources() {
        return resources;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setActor(NotificationParticipant actor) {
        this.actor = actor;
    }

    public NotificationParticipant getActor() {
        return actor;
    }

    /*
     * ============= HELPERS FOR LOGGING =============
     */

    private Map<String, Object> environment;

    public Map<String, Object> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, Object> environment) {
        this.environment = environment;
    }
}
