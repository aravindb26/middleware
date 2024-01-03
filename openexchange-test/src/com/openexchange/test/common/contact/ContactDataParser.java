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

package com.openexchange.test.common.contact;

import static com.openexchange.java.Autoboxing.I;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.ArrayList;
import java.util.List;
import com.openexchange.ajax.framework.ClientCommons;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.ContactsResponse;
import com.openexchange.testing.httpclient.modules.AddressbooksApi;
import com.openexchange.tools.arrays.Collections;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

/**
 * {@link ContactDataParser}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.3
 */
public class ContactDataParser {

    /** Some standard columns to request for contacts */
    public static final TIntList DEFAULT_CONTACT_COLUMNS = new TIntArrayList(new int[] { 1, 20, 223, 500, 501, 502, 524, 555, 556, 557, 606 });

    /** The global address book folder identifier */
    public static final String GAB = "con://0/6";

    /**
     * Initializes a new {@link ContactDataParser}.
     */
    private ContactDataParser() {
        super();
    }

    /**
     * Wrapper to {@link AddressbooksApi#getAllContactsFromAddressbook(String, String, String, String, String)} to hide casting
     *
     * @param api The API to use
     * @param folder The folder identifier
     * @param columns The columns to request
     * @param sort The column to sort after
     * @return The result
     * @throws ApiException In case of error
     */
    @SuppressWarnings("unchecked")
    public static List<List<Object>> getAllContactsFromAddressbook(AddressbooksApi api, String folder, TIntList columns, int sort) throws ApiException {
        String col = "";
        for (int i = 0; i < columns.size(); i++) {
            col += columns.get(i) + ",";
        }
        col = col.substring(0, col.length() - 1);
        ContactsResponse contactResponse = api.getAllContactsFromAddressbook(folder, col, I(sort).toString(), "ASC", null);
        return (List<List<Object>>) ClientCommons.checkResponse(contactResponse.getError(), contactResponse.getErrorDesc(), contactResponse.getData());
    }

    /**
     * Parse a list of contact data
     *
     * @param data The contacts data
     * @param columns The requested columns, in order
     * @return The parsed {@link ContactData}
     * @throws AssertionError In case data of a single contact is empty or their size of contact data and columns don't match
     */
    public static List<ContactData> parseContactsData(List<List<Object>> data, TIntList columns) throws AssertionError {
        ArrayList<ContactData> result = new ArrayList<>(data.size());
        for (List<Object> list : data) {
            result.add(parseContactData(list, columns));
        }
        return result;
    }

    /**
     * Parse a contact data
     *
     * @param data The contact data as array
     * @param columns The requested columns, in order
     * @return The parsed {@link ContactData}
     * @throws AssertionError In case data or columns are empty, or their size don't match
     */
    public static ContactData parseContactData(List<Object> data, TIntList columns) throws AssertionError {
        assertThat("Data and cloumns size don't match", Collections.isNotEmpty(data) && null != columns && data.size() == columns.size());
        ExtendedContactData contactData = new ExtendedContactData();
        for (int i = 0; i < data.size(); i++) {
            Object object = data.get(i);
            if (null != object) {
                parse(contactData, object, columns.get(i));
            }
        }
        return contactData;
    }

    // TODO Fill cases
    private static void parse(ExtendedContactData contactData, Object object, int i) {
        String data = object.toString();
        switch (i) {
            case 1:
                contactData.setId(data);
                break;
            case 20:
                contactData.setFolderId(data);
                break;
            case 223:
                contactData.setUid(data);
                break;
            case 500:
                contactData.setDisplayName(data);
                break;
            case 501:
                contactData.setFirstName(data);
                break;
            case 502:
                contactData.setLastName(data);
                break;
            case 503:
                contactData.setSecondName(data);
                break;
            case 504:
                contactData.setSuffix(data);
                break;
            case 505:
                break;
            case 506:
                break;
            case 507:
                break;
            case 508:
                break;
            case 509:
                break;
            case 510:
                break;
            case 511:
                break;
            case 512:
                break;
            case 513:
                break;
            case 514:
                break;
            case 515:
                break;
            case 516:
                break;
            case 517:
                break;
            case 518:
                break;
            case 519:
                break;
            case 520:
                break;
            case 521:
                break;
            case 522:
                break;
            case 523:
                break;
            case 524:
                contactData.setUserId(I(Double.valueOf(Double.parseDouble(data)).intValue()));
                break;
            case 525:
                break;
            case 526:
                break;
            case 527:
                break;
            case 528:
                break;
            case 529:
                break;
            case 530:
                break;
            case 531:
                break;
            case 532:
                break;
            case 533:
                break;
            case 534:
                break;
            case 535:
                break;
            case 536:
                break;
            case 537:
                break;
            case 538:
                break;
            case 539:
                break;
            case 540:
                break;
            case 541:
                break;
            case 542:
                break;
            case 543:
                break;
            case 544:
                break;
            case 545:
                break;
            case 546:
                break;
            case 547:
                break;
            case 548:
                break;
            case 549:
                break;
            case 550:
                break;
            case 551:
                break;
            case 552:
                break;
            case 553:
                break;
            case 554:
                break;
            case 555:
                contactData.setEmail1(data);
                break;
            case 556:
                contactData.setEmail2(data);
                break;
            case 557:
                contactData.setEmail3(data);
                break;
            case 558:
                break;
            case 559:
                break;
            case 560:
                break;
            case 561:
                break;
            case 562:
                break;
            case 563:
                break;
            case 564:
                break;
            case 565:
                break;
            case 566:
                break;
            case 567:
                break;
            case 568:
                break;
            case 569:
                break;
            case 570:
                break;
            case 571:
                break;
            case 572:
                break;
            case 573:
                break;
            case 574:
                break;
            case 575:
                break;
            case 576:
                break;
            case 577:
                break;
            case 578:
                break;
            case 579:
                break;
            case 580:
                break;
            case 581:
                break;
            case 582:
                break;
            case 583:
                break;
            case 584:
                break;
            case 585:
                break;
            case 586:
                break;
            case 587:
                break;
            case 588:
                break;
            case 589:
                break;
            case 590:
                break;
            case 592:
                break;
            case 594:
                break;
            case 596:
                break;
            case 597:
                break;
            case 598:
                break;
            case 599:
                break;
            case 601:
                break;
            case 602:
                break;
            case 605:
                break;
            case 606:
                contactData.setImage1Url(data);
                break;
            case 608:
                break;
            case 616:
                break;
            case 617:
                break;
            case 618:
                break;
            case 619:
                break;
            case 620:
                break;
            case 621:
                break;
            default:
                break;
        }

    }

    private static class ExtendedContactData extends ContactData {

        private Integer userId;

        /**
         * Initializes a new {@link ContactDataParser.ExtendedContactData}.
         *
         */
        public ExtendedContactData() {
            super();
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        @Override
        public Integer getUserId() {
            return userId;
        }
    }

}
