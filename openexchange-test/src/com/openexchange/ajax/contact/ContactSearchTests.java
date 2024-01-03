
package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.search.ContactSearchObject;
import org.junit.jupiter.api.TestInfo;

public class ContactSearchTests extends AbstractManagedContactTest {

    private static final String ALICE = "Alice";
    private static final String ALICE_MAIL1 = "alice@wonderland.org";
    private static final String BOB_LASTNAME = "Bob";
    private static final String BOB_DISPLAYNAME = "Carol19";
    private static final String BOB_MAIL2 = "bob@thebuilder.org";

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        Contact c1 = generateContact();
        c1.setSurName(ALICE);
        c1.setEmail1(ALICE_MAIL1);
        Contact c2 = generateContact();
        c2.setSurName(BOB_LASTNAME);
        c2.setDisplayName(BOB_DISPLAYNAME);
        c2.setEmail2(BOB_MAIL2);

        cotm.newAction(c1, c2);
    }

    @Test
    public void testSearchByInitial() {
        Contact[] results = cotm.searchFirstletterAction("B", folderID);
        assertEquals(1, results.length);
        assertEquals(BOB_LASTNAME, results[0].getSurName(), "Should find the right contact");
    }

    @Test
    public void testAsteriskSearch() {
        Contact[] results = cotm.searchAction("*", folderID);
        assertEquals(2, results.length, "Should find two contacts");
    }

    @Test
    public void testSearchWorksOnlyOnDisplayNameByDefault() {
        Contact[] results = cotm.searchAction("*" + BOB_LASTNAME + "*", folderID);
        assertEquals(0, results.length, "Should find no contact when searching for last name");

        results = cotm.searchAction("*" + BOB_DISPLAYNAME + "*", folderID);
        assertEquals(1, results.length, "Should find one contact when searching for display_name");
    }

    @Test
    public void testGuiLikeSearch() {
        ContactSearchObject search = new ContactSearchObject();
        search.addFolder(folderID);
        String b = BOB_LASTNAME;
        search.setGivenName(b);
        search.setSurname(b);
        search.setDisplayName(b);
        search.setEmail1(b);
        search.setEmail2(b);
        search.setEmail3(b);
        search.setCatgories(b);
        search.setOrSearch(true);
        Contact[] results = cotm.searchAction(search);
        assertEquals(1, results.length, "Should find one contact");
        assertEquals(BOB_LASTNAME, results[0].getSurName(), "Should find the right contact");
    }

    @Test
    public void testExactMatch() {
        ContactSearchObject search = new ContactSearchObject();
        search.addFolder(folderID);
        search.setOrSearch(true);
        search.setEmail1(ALICE_MAIL1);
        search.setEmail2(ALICE_MAIL1);
        search.setEmail3(ALICE_MAIL1);
        search.setExactMatch(true);
        Contact[] results = cotm.searchAction(search);
        assertEquals(1, results.length, "Should find one contact");
        assertEquals(ALICE_MAIL1, results[0].getEmail1(), "Should find the right contact");
        search.setExactMatch(false);
        results = cotm.searchAction(search);
        assertEquals(1, results.length, "Should find one contact");
        assertEquals(ALICE_MAIL1, results[0].getEmail1(), "Should find the right contact");

        String partialAddress = BOB_MAIL2.substring(0, BOB_MAIL2.lastIndexOf('.'));
        search.setEmail1(partialAddress);
        search.setEmail2(partialAddress);
        search.setEmail3(partialAddress);
        search.setExactMatch(true);
        results = cotm.searchAction(search);
        assertEquals(0, results.length, "Should find no contact");
        search.setExactMatch(false);
        results = cotm.searchAction(search);
        assertEquals(1, results.length, "Should find one contact");
        assertEquals(BOB_MAIL2, results[0].getEmail2(), "Should find the right contact");

    }

}
