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

package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DistributionListEntryObject;

/**
 * {@link MWB1499Test}
 * 
 * cannot delete contacts in Distribution List
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 8.0.0
 */
public class MWB1499Test extends AbstractManagedContactTest {

    @Test
    public void testDeleteMember() throws OXException {
        /*
         * create distribution list with three members
         */
        Contact distributionList = generateContact("List");
        DistributionListEntryObject[] members = new DistributionListEntryObject[3];
        for (int i = 0; i < members.length; i++) {
            members[i] = new DistributionListEntryObject();
            members[i].setDisplayname("Member_" + i);
            members[i].setEmailaddress("member_" + i + "@example.com");
            members[i].setEmailfield(DistributionListEntryObject.INDEPENDENT);
        }
        distributionList.setDistributionList(members);
        distributionList = cotm.newAction(distributionList);
        /*
         * get & verify created distribution list
         */
        distributionList = cotm.getAction(distributionList);
        assertNotNull(distributionList, "distibution list not found");
        assertTrue(distributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(3, distributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(3, distributionList.getDistributionList().length, "member count wrong");
        /*
         * update distribution list & remove 2nd member
         */
        DistributionListEntryObject[] updatedMembers = new DistributionListEntryObject[2];
        updatedMembers[0] = distributionList.getDistributionList()[0];
        updatedMembers[1] = distributionList.getDistributionList()[2];
        Contact updatedDistributionList = new Contact();
        updatedDistributionList.setObjectID(distributionList.getObjectID());
        updatedDistributionList.setParentFolderID(distributionList.getParentFolderID());
        updatedDistributionList.setLastModified(distributionList.getLastModified());
        updatedDistributionList.setDistributionList(updatedMembers);
        updatedDistributionList = cotm.updateAction(updatedDistributionList);
        /*
         * get & verify updated distribution list
         */
        updatedDistributionList = cotm.getAction(updatedDistributionList);
        assertNotNull(updatedDistributionList, "distibution list not found");
        assertTrue(updatedDistributionList.getMarkAsDistribtuionlist(), "not marked as distribution list");
        assertEquals(2, updatedDistributionList.getNumberOfDistributionLists(), "member count wrong");
        assertEquals(2, updatedDistributionList.getDistributionList().length, "member count wrong");
        for (int i = 0; i < updatedDistributionList.getDistributionList().length; i++) {
            assertNotEquals(distributionList.getDistributionList()[1].getEmailaddress(), updatedDistributionList.getDistributionList()[i].getEmailaddress());
        }
    }

}
