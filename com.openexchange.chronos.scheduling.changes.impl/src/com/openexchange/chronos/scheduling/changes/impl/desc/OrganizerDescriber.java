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

package com.openexchange.chronos.scheduling.changes.impl.desc;

import static com.openexchange.chronos.scheduling.common.Utils.getDisplayName;
import java.util.Collections;
import java.util.List;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.scheduling.changes.Sentence.ArgumentType;
import com.openexchange.chronos.scheduling.changes.impl.SentenceImpl;
import com.openexchange.chronos.scheduling.common.Messages;

/**
 * {@link OrganizerDescriber}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class OrganizerDescriber extends AbstractChangeDescriber<Organizer> {

    /**
     * Initializes a new {@link OrganizerDescriber}.
     */
    public OrganizerDescriber() {
        super(EventField.ORGANIZER, Organizer.class);
    }

    @Override
    List<SentenceImpl> describe(Organizer original, Organizer updated) {
        /*
         * Only announce changed organizer if URI changed. Valid changes on organizer are e.g. a set SENT-BY which is not of interest for the user
         */
        if (null != updated && false == CalendarUtils.matches(original, updated)) {
            return Collections.singletonList(new SentenceImpl(Messages.ORGANIZER_CHANGE).add(getDisplayName(updated), ArgumentType.EMPHASIZED));
        }
        return Collections.emptyList();
    }

}
