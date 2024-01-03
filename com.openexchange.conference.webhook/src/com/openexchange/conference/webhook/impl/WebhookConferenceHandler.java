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

package com.openexchange.conference.webhook.impl;

import static com.openexchange.chronos.common.CalendarUtils.hasExternalOrganizer;
import static com.openexchange.conference.webhook.impl.Utils.TYPE_JITSI;
import static com.openexchange.conference.webhook.impl.Utils.TYPE_ZOOM;
import static com.openexchange.conference.webhook.impl.Utils.getConferences;
import static com.openexchange.conference.webhook.impl.Utils.matches;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Conference;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.service.CalendarEvent;
import com.openexchange.chronos.service.CalendarHandler;
import com.openexchange.chronos.service.DeleteResult;
import com.openexchange.chronos.service.UpdateResult;
import com.openexchange.conference.webhook.ConferenceWebhook;
import com.openexchange.conference.webhook.ConferenceWebhookConfiguration;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link WebhookConferenceHandler}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.4
 */
public class WebhookConferenceHandler implements CalendarHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookConferenceHandler.class);
    private static final String[] CONFERENCE_TYPES = { TYPE_ZOOM, TYPE_JITSI };

    private final ServiceLookup services;

    /**
     * Initializes a new {@link WebhookConferenceHandler}.
     *
     * @param services A service lookup reference
     */
    public WebhookConferenceHandler(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public void handle(CalendarEvent event) {
        try {
            ConferenceWebhookConfiguration config = getConfig(event);
            if (null == config) {
                LOG.debug("No webhook target configured, no handling of event {} necessary.", event);
                return;
            }
            for (UpdateResult update : event.getUpdates()) {
                handleUpdate(config, update, event);
            }
            for (DeleteResult delete : event.getDeletions()) {
                handleDelete(config, delete, event);
            }
        } catch (Exception e) {
            LOG.warn("Unexpected error while handling {}: {}", event, e.getMessage(), e);
        }
    }

    private void handleUpdate(ConferenceWebhookConfiguration config, UpdateResult update, CalendarEvent event) {
        if (hasExternalOrganizer(update.getOriginal())) {
            return;
        }

        List<Conference> originalConferences = getConferences(update.getOriginal(), CONFERENCE_TYPES);
        List<Conference> updateConferences = getConferences(update.getUpdate(), CONFERENCE_TYPES);

        if (originalConferences.isEmpty() && updateConferences.isEmpty()) {
            return;
        }

        if (originalConferences.isEmpty()) {
            // all new, nothing todo since this is already handled by the client (only appsuite UI adds conferences at this point)
        } else if (updateConferences.isEmpty()) {
            // all removed
            delete(config, originalConferences, update.getUpdate(), event);
        } else {
            // calculate diffs. Again, "added" is not relevant
            List<Conference> removed = new ArrayList<>(originalConferences);
            List<Conference> changed = new ArrayList<>();
            for (Conference upd : updateConferences) {
                removed.removeIf(c -> matches(c, upd));
                Optional<Conference> optional = originalConferences.stream().filter(c -> matches(c, upd)).findAny();
                if (optional.isPresent()) {
                    changed.add(optional.get());
                }
            }
            if (timeHasChanged(update)) {
                changed(config, changed, update.getUpdate(), event);
            }
            delete(config, removed, update.getUpdate(), event);
        }
    }

    private void handleDelete(ConferenceWebhookConfiguration config, DeleteResult delete, CalendarEvent event) {
        Event original = delete.getOriginal();
        if (hasExternalOrganizer(original)) {
            return;
        }

        List<Conference> conferences = getConferences(original, CONFERENCE_TYPES);
        delete(config, conferences, original, event);
    }

    /**
     * Checks if start or end date of the event have changed.
     *
     * @param update The Update
     * @return true if start or end date have changed, false otherwise
     */
    private boolean timeHasChanged(UpdateResult update) {
        return update.getUpdatedFields().contains(EventField.START_DATE) || update.getUpdatedFields().contains(EventField.END_DATE);
    }

    /**
     * Loads the configuration.
     *
     * @param event The handled event
     * @return The {@link ConferenceWebhookConfiguration}, or <code>null</code> if not configured
     */
    private ConferenceWebhookConfiguration getConfig(CalendarEvent event) throws OXException {
        int contextId = event.getContextId();
        int userId = event.getCalendarUser();
        return ConferenceWebhookConfiguration.getConfig(services.getServiceSafe(LeanConfigurationService.class), userId, contextId);
    }

    /**
     * Sends delete notifications to the webhook target.
     *
     * @param conferences The list of deleted conferences
     * @param event The event
     * @param calendarEvent The calendarEvent
     */
    private void delete(ConferenceWebhookConfiguration config, List<Conference> conferences, Event event, CalendarEvent calendarEvent) {
        ConferenceWebhook conferenceWebhook = new ConferenceWebhook(services, config);
        for (Conference conf : conferences) {
            conferenceWebhook.delete(conf, event, calendarEvent.getTimestamp());
        }
    }

    /**
     * Sends change notifications to the webhook target.
     *
     * @param conferences The list of changed conferences
     * @param updatedEvent The changed event itself
     * @param event The event
     */
    private void changed(ConferenceWebhookConfiguration config, List<Conference> conferences, Event updatedEvent, CalendarEvent event) {
        ConferenceWebhook conferenceWebhook = new ConferenceWebhook(services, config);
        for (Conference conf : conferences) {
            conferenceWebhook.update(conf, updatedEvent, event.getTimestamp());
        }
    }

}
