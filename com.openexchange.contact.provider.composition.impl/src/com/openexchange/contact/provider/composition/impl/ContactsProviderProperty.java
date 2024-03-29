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

package com.openexchange.contact.provider.composition.impl;

import static com.openexchange.java.Autoboxing.F;
import static com.openexchange.java.Autoboxing.I;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.Property;

/**
 * {@link ContactsProviderProperty}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public enum ContactsProviderProperty {
    ;

    static final Property ALL_FOLDERS_FOR_AUTOCOMPLETE = DefaultProperty.valueOf("com.openexchange.contacts.allFoldersForAutoComplete", Boolean.TRUE);

    static final Property MINIMUM_SEARCH_CHARACTERS = DefaultProperty.valueOf("com.openexchange.MinimumSearchCharacters", I(0));

    /**
     * Contacts in search results may get sorted based on their frequency of usage by the requesting user, so that users experience better suggestions e.g. 
     * when auto-completing names of mail recipients while typing. For contacts from external storages that don't have built-in counters for the individual 
     * usages, a certain look-ahead factor for the maximum number of results can be specified, which will get applied to the client-defined limit when 
     * forwarding the search request to the external storage.
     */
    static final Property USE_COUNT_LOOK_AHEAD = DefaultProperty.valueOf("com.openexchange.contacts.useCountLookAhead", F(20));

}
