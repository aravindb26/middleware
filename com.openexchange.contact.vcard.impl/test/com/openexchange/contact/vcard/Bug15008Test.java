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

package com.openexchange.contact.vcard;

import static org.junit.Assert.assertNotNull;
import org.junit.Assert;
import org.junit.Test;
import com.openexchange.groupware.container.Contact;

/**
 * {@link Bug15008Test}
 *
 * vcf import failed
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug15008Test extends VCardTest {

    /**
     * Initializes a new {@link Bug15008Test}.
     */
    public Bug15008Test() {
        super();
    }

         @Test
     public void testImportVCard() throws Exception {
        /*
         * import vCard
         */
        byte[] vCard = {
            66, 69, 71, 73, 78, 58, 86, 67, 65, 82, 68, 13, 10, 88, 45, 76, 79, 84, 85, 83, 45, 67, 72, 65, 82, 83, 69, 84, 58, 119, 105,
            110, 100, 111, 119, 115, 45, 49, 50, 53, 50, 13, 10, 86, 69, 82, 83, 73, 79, 78, 58, 51, 46, 48, 13, 10, 79, 82, 71, 58, 86,
            80, 67, 13, 10, 69, 77, 65, 73, 76, 59, 84, 89, 80, 69, 61, 73, 78, 84, 69, 82, 78, 69, 84, 58, 83, 116, 101, 102, 97, 110,
            46, 65, 100, 97, 109, 115, 64, 118, 105, 112, 99, 111, 109, 97, 103, 46, 100, 101, 13, 10, 69, 77, 65, 73, 76, 59, 84, 89, 80,
            69, 61, 73, 78, 84, 69, 82, 78, 69, 84, 58, 83, 116, 101, 102, 97, 110, 95, 95, 65, 100, 97, 109, 115, 64, 104, 111, 116, 109,
            97, 105, 108, 46, 99, 111, 109, 13, 10, 84, 69, 76, 59, 84, 89, 80, 69, 61, 87, 79, 82, 75, 58, 43, 52, 57, 56, 57, 53, 52,
            55, 53, 48, 49, 48, 56, 13, 10, 84, 69, 76, 59, 84, 89, 80, 69, 61, 72, 79, 77, 69, 58, 43, 52, 57, 32, 40, 52, 57, 53, 50,
            41, 32, 54, 49, 48, 52, 51, 48, 13, 10, 84, 69, 76, 59, 84, 89, 80, 69, 61, 67, 69, 76, 76, 58, 43, 52, 57, 32, 40, 49, 53,
            49, 41, 32, 53, 48, 49, 48, 52, 52, 51, 54, 13, 10, 65, 68, 82, 59, 84, 89, 80, 69, 61, 87, 79, 82, 75, 59, 69, 78, 67, 79,
            68, 73, 78, 71, 61, 81, 85, 79, 84, 69, 68, 45, 80, 82, 73, 78, 84, 65, 66, 76, 69, 58, 59, 59, 65, 108, 101, 114, 105, 99,
            104, 45, 69, 98, 101, 108, 105, 110, 103, 115, 45, 87, 101, 103, 32, 51, 56, 97, 61, 48, 65, 59, 82, 104, 97, 117, 100, 101,
            114, 102, 101, 104, 110, 59, 59, 50, 54, 56, 49, 55, 59, 68, 101, 117, 116, 115, 99, 104, 108, 97, 110, 100, 13, 10, 78, 58,
            65, 100, 105, 59, 59, 59, 59, 13, 10, 70, 78, 58, 65, 100, 105, 13, 10, 69, 78, 68, 58, 86, 67, 65, 82, 68, 13, 10
        };
        Contact contact = getMapper().importVCard(parse(vCard), null, null, null);
        /*
         * verify imported contact
         */
        assertNotNull(contact);
        Assert.assertEquals("VPC", contact.getCompany());
        Assert.assertEquals("Stefan.Adams@vipcomag.de", contact.getEmail1());
        Assert.assertEquals("+498954750108", contact.getTelephoneBusiness1());
        Assert.assertEquals("+49 (4952) 610430", contact.getTelephoneHome1());
        Assert.assertEquals("+49 (151) 50104436", contact.getCellularTelephone1());
        Assert.assertEquals("Alerich-Ebelings-Weg 38a\n", contact.getStreetBusiness());
        Assert.assertEquals("Rhauderfehn", contact.getCityBusiness());
        Assert.assertEquals("26817", contact.getPostalCodeBusiness());
        Assert.assertEquals("Deutschland", contact.getCountryBusiness());
        Assert.assertEquals("Adi", contact.getDisplayName());
        Assert.assertEquals("Adi", contact.getSurName());
    }

}
