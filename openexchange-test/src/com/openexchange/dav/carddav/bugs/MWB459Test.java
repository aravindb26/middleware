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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.carddav.CardDAVTest;
import com.openexchange.dav.carddav.VCardResource;
import com.openexchange.groupware.container.Contact;
import net.sourceforge.cardme.vcard.types.TelType;
import net.sourceforge.cardme.vcard.types.params.TelParamType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MWB459Test}
 *
 * appsuite adds additional PREF field to vcard export
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.5
 */
public class MWB459Test extends CardDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateWithFourEmails(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * prepare vCard
         */
        String uid = randomUID();
        String vCard = // @formatter:off
            "BEGIN:VCARD" + "\r\n" +
            "VERSION:3.0" + "\r\n" +
            "FN:MWB459Test" + "\r\n" +
            "N:;MWB459Test;;;" + "\r\n" +
            "TEL;CELL:+49176123456\r\n" +
            "TEL;HOME;PREF:+49123456789\r\n" +
            "PRODID:-//dmfs.org//mimedir.vcard//EN" + "\r\n" +
            "REV:20200111T150424Z" + "\r\n" +
            "UID:" + uid + "\r\n" +
            "END:VCARD" + "\r\n"
        ; // @formatter:on
        /*
         * create vCard resource on server
         */
        assertEquals(StatusCodes.SC_CREATED, putVCard(uid, vCard), "response code wrong");
        /*
         * get & verify created contact on server
         */
        Contact contact = getContact(uid);
        rememberForCleanUp(contact);
        assertEquals(uid, contact.getUid(), "uid wrong");
        assertEquals("+49176123456", contact.getCellularTelephone1(), "cell phone 1 wrong");
        assertEquals("+49123456789", contact.getTelephoneHome1(), "home phone 1 wrong");
        /*
         * get & verify created vCard from server
         */
        VCardResource vCardResource = getVCard(uid);
        TelType telCell = null;
        TelType telHome = null;
        List<TelType> tels = vCardResource.getVCard().getTels();
        for (TelType tel : tels) {
            if ("+49176123456".equals(tel.getTelephone())) {
                telCell = tel;
            } else if ("+49123456789".equals(tel.getTelephone())) {
                telHome = tel;
            } else {
                fail("unexepcted tel" + tel.getTelephone());
            }
        }
        assertNotNull(telCell);
        assertTrue(telCell.getParams().contains(TelParamType.CELL));
        assertFalse(telCell.getParams().contains(TelParamType.PREF));
        assertNotNull(telHome);
        assertTrue(telHome.getParams().contains(TelParamType.HOME));
        assertTrue(telHome.getParams().contains(TelParamType.PREF));
    }


}
