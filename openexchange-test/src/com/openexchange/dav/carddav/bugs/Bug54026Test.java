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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import com.openexchange.ajax.user.actions.GetRequest;
import com.openexchange.dav.Config;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.UserAgents;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import net.sourceforge.cardme.vcard.types.OrgType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug54026Test}
 *
 * If changes are made on global address book on eM Client, corresponding changes do not update on Web UI and eM Client.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug54026Test extends CardDAVTest {

    /**
     * Initializes a new {@link Bug54026Test}.
     */
    public Bug54026Test() {
        super();
    }

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.EM_CLIENT_FOR_APP_SUITE;
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateOwnUserContact(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        String gabCollection = String.valueOf(FolderObject.SYSTEM_LDAP_FOLDER_ID);
        SyncToken syncToken = new SyncToken(fetchSyncToken(gabCollection));
        /*
         * identify uid of own user contact
         */
        GetRequest userGetRequest = new GetRequest(getClient().getValues().getUserId(), getClient().getValues().getTimeZone());
        Contact contact = getClient().execute(userGetRequest).getContact();
        assertNotNull(contact);
        /*
         * get vcard resource
         */
        String href = Config.getPathPrefix() + "/carddav/" + gabCollection + "/" + contact.getUid() + ".vcf";
        VCardResource card = getVCardResource(href);
        assertNotNull(card);
        /*
         * update vcard resource on client
         */
        String updatedCompany = randomUID();
        card.getVCard().setOrg(new OrgType(updatedCompany));
        assertEquals(StatusCodes.SC_CREATED, putVCardUpdate(card.getUID(), card.toString(), gabCollection, card.getETag()), "response code wrong");
        /*
         * verify updated contact on server
         */
        Contact updatedContact = getClient().execute(userGetRequest).getContact();
        assertNotNull(updatedContact);
        assertEquals(updatedCompany, updatedContact.getCompany(), "conmpany wrong");
        /*
         * verify updated contact on client
         */

        Map<String, String> eTags = syncCollection(syncToken, "/carddav/" + gabCollection + "/").getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<VCardResource> addressData = addressbookMultiget(gabCollection, eTags.keySet());
        card = assertContains(contact.getUid(), addressData);
        assertEquals(updatedCompany, card.getVCard().getOrg().getOrgName(), "ORG wrong");
    }

}
