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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.scheduling.changes.ContextSensitiveMessages;
import com.openexchange.chronos.scheduling.changes.impl.osgi.Services;
import com.openexchange.i18n.I18nService;
import com.openexchange.i18n.I18nServiceRegistry;

/**
 * {@link ContextSensitiveMessagesImpl}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.8.1
 */
public class ContextSensitiveMessagesImpl implements ContextSensitiveMessages {

    private static final Logger LOG = LoggerFactory.getLogger(ContextSensitiveMessagesImpl.class);
    private final Locale locale;

    /**
     * Initializes a new {@link ContextSensitiveMessagesImpl}.
     * 
     * @param locale The user local
     */
    public ContextSensitiveMessagesImpl(Locale locale) {
        super();
        this.locale = locale;
    }

    /**
     * Generates a message describing an <code>accepted</code> event
     *
     * @param ctxt The context to put the message in
     * @return The message
     */
    @Override
    public String accepted(Context ctxt) {
        I18nService i18nService = getI18Service();
        if (i18nService == null) {
            LOG.debug("No service for {}  found. Using default for bundle ", locale);
            return "accepted";
        }

        switch (ctxt) {
            case VERB:
                // The verb "accepted", like "User A has accepted an appointment."
                return i18nService.getL10NContextLocalized("verb", "accepted");
            case ADJECTIVE:
                // The adjective "accepted", like "The users status is 'accepted'."
                return i18nService.getL10NContextLocalized("adjective", "accepted");
            default:
                return "accepted";
        }
    }

    /**
     * Generates a message describing an <code>declined</code> event
     *
     * @param ctxt The context to put the message in
     * @return The message
     */
    @Override
    public String declined(Context ctxt) {
        I18nService i18nService = getI18Service();
        if (i18nService == null) {
            LOG.debug("No service for {}  found. Using default for bundle ", locale);
            return "declined";
        }

        switch (ctxt) {
            case VERB:
                // The verb "declined", like "User A has declined an appointment."
                return i18nService.getL10NContextLocalized("verb", "declined");
            case ADJECTIVE:
                // The adjective "declined", like "The users status is 'declined'."
                return i18nService.getL10NContextLocalized("adjective", "declined");
            default:
                return "declined";
        }
    }

    /**
     * Generates a message describing an <code>tentetive accepted</code> event
     *
     * @param ctxt The context to put the message in
     * @return The message
     */
    @Override
    public String tentative(Context ctxt) {
        I18nService i18nService = getI18Service();
        if (i18nService == null) {
            LOG.debug("No service for {}  found. Using default for bundle ", locale);
            return "tentatively accepted";
        }

        switch (ctxt) {
            case VERB:
                // The verb "tentatively accepted", like "User A has tentatively accepted an appointment."
                return i18nService.getL10NContextLocalized("verb", "tentatively accepted");
            case ADJECTIVE:
                // The adjective "tentatively accepted", like "The users status is 'tentatively accepted'."
                return i18nService.getL10NContextLocalized("adjective", "tentatively accepted");
            default:
                return "tentatively accepted";
        }
    }

    /**
     * Describes the participant status
     *
     * @param status The status
     * @param ctxt The context to put the message in
     * @return The message
     */
    @Override
    public String partStat(ParticipationStatus status, Context ctxt) {
        if (ParticipationStatus.ACCEPTED.matches(status)) {
            return accepted(ctxt);
        } else if (ParticipationStatus.DECLINED.matches(status)) {
            return declined(ctxt);
        } else if (ParticipationStatus.TENTATIVE.matches(status)) {
            return tentative(ctxt);
        }
        return status.getValue();
    }

    /**
     * Returns the {@link I18nService} or null if none found
     *
     * @return The {@link I18nService}
     */
    private I18nService getI18Service() {
        I18nServiceRegistry registry = Services.getService(I18nServiceRegistry.class);
        if (registry == null) {
            return null;
        }
        return registry.getI18nService(locale);
    }

}
