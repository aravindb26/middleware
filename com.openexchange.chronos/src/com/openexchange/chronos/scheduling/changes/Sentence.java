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

package com.openexchange.chronos.scheduling.changes;

import java.util.Locale;
import java.util.TimeZone;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.regional.RegionalSettings;

/**
 * {@link Sentence} - Object containing a sentence for a dedicated language. Contains arguments
 * to be filled in the generic sentence.
 * <p>
 * A {@link Sentence} can be acquired via {@link SentenceFactory}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public interface Sentence {

    /**
     * {@link ArgumentType} - Different types of highlighting for message arguments
     *
     * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
     * @since v7.10.6
     */
    enum ArgumentType {
        PARTICIPANT, ORIGINAL, UPDATED, STATUS, NONE, EMPHASIZED, REFERENCE, SHOWN_AS, ITALIC
    }

    /**
     * Adds an argument to an sentence
     *
     * @param argument The argument to add into a sentence
     * @return The sentence
     */
    default Sentence add(Object argument) {
        return add(argument, ArgumentType.NONE);
    }

    /**
     * Adds a participant status into a sentence. Will be highlighted in a special way
     *
     * @param status The status to add into the sentence
     * @return The sentence
     */
    Sentence addStatus(ParticipationStatus status);

    /**
     * Adds an argument to a sentence with dedicated highlighting for the argument
     *
     * @param argument The argument to add into a sentence
     * @param type The type of highlighting for the argument
     * @param extra Additional arguments to add to the sentence
     * @return The sentence
     */
    Sentence add(Object argument, ArgumentType type, Object... extra);

    /**
     * Get the message in a specific format
     * 
     * @param messageContext The context of the message
     * @return The message in a specific format, localized
     */
    String getMessage(MessageContext messageContext);

    /**
     * Get the message in a specific format
     * 
     * @param format The format of the message
     * @param locale The locale to use
     * @param timeZone The timezone to use
     * @param regionalSettings The regional settings
     * 
     * @return The message in a specific format, localized
     */
    String getMessage(String format, Locale locale, TimeZone timeZone, RegionalSettings regionalSettings);

}
