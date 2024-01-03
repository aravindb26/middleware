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

package com.openexchange.dav.carddav.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.reports.SyncCollectionResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug46641Test}
 *
 * immediate deletion of contacts group which was created via OS X contacts app fails with NPE
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.2
 */
public class Bug46641Test extends CardDAVTest {

    /**
     * Initializes a new {@link Bug46641Test}.
     */
    public Bug46641Test() {
        super();
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testBulkImportContactGroup(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * try to create contact group using bulk-import
         */
        String uid = randomUID();
        String vCard = "BEGIN:VCARD" + "\r\n" + "VERSION:3.0" + "\r\n" + "PRODID:-//Apple Inc.//AddressBook 9.0//EN" + "\r\n" + "N:untitled group" + "\r\n" + "FN:untitled group" + "\r\n" + "X-ADDRESSBOOKSERVER-KIND:group" + "\r\n" + "REV:" + formatAsUTC(new Date()) + "\r\n" + "UID:" + uid + "\r\n" + "END:VCARD" + "\r\n";
        postVCard(vCard, 0);
        /*
         * check that no contact was created on server
         */
        assertNull(getContact(uid));
        /*
         * check that sync-collection reports the resource as deleted
         */
        SyncCollectionResponse syncCollectionResponse = syncCollection(syncToken);
        assertEquals(1, syncCollectionResponse.getHrefsStatusNotFound().size(), "no resource deletions reported on sync collection");
        assertTrue(syncCollectionResponse.getHrefsStatusNotFound().get(0).contains(uid));
    }
}
