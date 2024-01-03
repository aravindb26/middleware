
package com.openexchange.ajax.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.contact.action.AdvancedSearchRequest;
import com.openexchange.ajax.framework.CommonSearchResponse;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.test.ContactTestManager;
import org.junit.jupiter.api.TestInfo;

@SuppressWarnings("deprecation")
public class AdvancedSearchTest extends AbstractManagedContactTest {

    private static final String BOB_LASTNAME = "Rather complicated last name with timestamp (" + new Date().getTime() + ") that does not appear in other folders";

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        Contact alice = ContactTestManager.generateContact(folderID);
        alice.setGivenName("Alice");

        Contact bob = ContactTestManager.generateContact(folderID);
        bob.setGivenName("Bob");
        bob.setSurName(BOB_LASTNAME);

        Contact charlie = ContactTestManager.generateContact(folderID);
        charlie.setGivenName("Charlie");

        cotm.newAction(alice, bob, charlie);
    }

    @Test
    public void testSearchWithEquals() throws Exception {
        ContactField field = ContactField.GIVEN_NAME;

        JSONObject filter = new JSONObject("{'filter' : ['=' , {'field' : '" + field.getAjaxName() + "'} , 'Bob']}");

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { Contact.GIVEN_NAME }, -1, null);
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find a result");
        assertEquals(1, resultTable.length, "Should find one result");

        int columnPos = response.getColumnPos(field.getNumber());
        String actual = (String) resultTable[0][columnPos];

        assertEquals("Bob", actual);
    }

    @Test
    public void testSearchWithEqualsInAllFolders() throws Exception {
        ContactField field = ContactField.SUR_NAME;
        JSONObject filter = new JSONObject("{'filter' : [ '=' , {'field' : '" + field.getAjaxName() + "'} , '" + BOB_LASTNAME + "']}");

        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, null, Contact.ALL_COLUMNS, -1, null);
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find a result");
        assertEquals(1, resultTable.length, "Should find one result");

        int columnPos = response.getColumnPos(field.getNumber());
        String actual = (String) resultTable[0][columnPos];

        assertEquals(BOB_LASTNAME, actual);
    }

    /**
     * Tests a SQL injection using our good friend Bobby, from 'Exploits of a mom', http://xkcd.com/327/
     */
    @Test
    public void testLittleBobbyTables() throws Exception {
        ContactField field = ContactField.SUR_NAME;
        String bobby = "Robert\\\"); DROP TABLE prg_contacts; --";

        JSONObject filter = new JSONObject("{'filter' : [ 'or', " + "['=' , {'field' : '" + field.getAjaxName() + "'} , '" + BOB_LASTNAME + "'], " + "['=' , {'field' : '" + field.getAjaxName() + "'}, '" + bobby + "']" + "]})");

        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, null, Contact.ALL_COLUMNS, -1, null);
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find a result");
        assertEquals(1, resultTable.length, "Should find one result");

        int columnPos = response.getColumnPos(field.getNumber());
        String actual = (String) resultTable[0][columnPos];

        assertEquals(BOB_LASTNAME, actual);
    }

    @Test
    public void testSearchAlphabetRange() throws Exception {
        ContactField field = ContactField.GIVEN_NAME;
        JSONObject filter = new JSONObject( // @formatter:off
            "{'filter' : [ 'and', " +
                "['>=' , {'field' : '" + field.getAjaxName() + "'} , 'A'], " +
                "['<' , {'field' : '" + field.getAjaxName() + "'}, 'C'] " +
            "]}"); // @formatter:off

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { field.getNumber() }, -1, null);
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find at least a result");
        assertEquals(2, resultTable.length, "Should find two results");

        int columnPos = response.getColumnPos(field.getNumber());
        Set<String> names = new HashSet<String>();
        names.add((String) resultTable[0][columnPos]);
        names.add((String) resultTable[1][columnPos]);

        assertTrue(names.contains("Bob"));
        assertTrue(names.contains("Alice"));
    }

    @Test
    public void testSearchOrderingWithKana() throws Exception {
        cotm.newAction(ContactTestManager.generateContact(folderID, "\u30ef"), ContactTestManager.generateContact(folderID, "\u30ea"), ContactTestManager.generateContact(folderID, "\u30e9"), ContactTestManager.generateContact(folderID, "\u30e5"), ContactTestManager.generateContact(folderID, "\u30e4"), ContactTestManager.generateContact(folderID, "\u30df"), ContactTestManager.generateContact(folderID, "\u30de"), ContactTestManager.generateContact(folderID, "\u30d0"), ContactTestManager.generateContact(folderID, "\u30cf"), ContactTestManager.generateContact(folderID, "\u30cb"), ContactTestManager.generateContact(folderID, "\u30ca"), ContactTestManager.generateContact(folderID, "\u30c0"), ContactTestManager.generateContact(folderID, "\u30bf"), ContactTestManager.generateContact(folderID, "\u30b6"), ContactTestManager.generateContact(folderID, "\u30b5"), ContactTestManager.generateContact(folderID, "\u30ac"), ContactTestManager.generateContact(folderID, "\u30ab"), ContactTestManager.generateContact(folderID, "\u30a3"), ContactTestManager.generateContact(folderID, "\u30a2"));

        String[] letters = new String[] { "\u30a2", "\u30ab", "\u30b5", "\u30bf", "\u30ca", "\u30cf", "\u30de", "\u30e4", "\u30e9", "\u30ef" };

        ContactField field = ContactField.SUR_NAME;
        LinkedList<JSONObject> filters = new LinkedList<JSONObject>();
        for (int i = 0; i < letters.length - 1; i++) {
            filters.add(new JSONObject( // @formatter:off
                "{'filter' : [ 'and', " +
                    "['>=' , {'field' : '" + field.getAjaxName() + "'} , '" + letters[i] + "'], " +
                    "['<' , {'field' : '" + field.getAjaxName() + "'}, '" + letters[i + 1] + "'] " +
            "]}")); // @formatter:on
        }

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        int currentPosition = 0;
        for (JSONObject filter : filters) {
            String ident = "Step #" + currentPosition + " from " + letters[currentPosition] + " to " + letters[currentPosition + 1] + ": ";
            AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { field.getNumber() }, field.getNumber(), "asc");
            CommonSearchResponse response = getClient().execute(request);
            assertFalse(response.hasError(), ident + "Should work");

            Object[][] resultTable = response.getArray();
            assertNotNull(resultTable, ident + "Should find at least a result");
            assertEquals(2, resultTable.length, ident + "Should find two results");

            int columnPos = response.getColumnPos(field.getNumber());
            HashSet<String> names = new HashSet<String>();
            names.add((String) resultTable[0][columnPos]);
            names.add((String) resultTable[1][columnPos]);
            assertTrue(names.contains(letters[currentPosition]), ident + "Should be contained");

            currentPosition++;
        }
    }

    @Test
    public void testSearchOrderingWithHanzi() throws Exception {
        List<String> sinograph = Arrays.asList("\u963f", "\u6ce2", "\u6b21", "\u7684", "\u9e45", "\u5bcc", "\u54e5", "\u6cb3", "\u6d01", "\u79d1", "\u4e86", "\u4e48", "\u5462", "\u54e6", "\u6279", "\u4e03", "\u5982", "\u56db", "\u8e22", "\u5c4b", "\u897f", "\u8863", "\u5b50");
        for (String graphem : sinograph) {
            cotm.newAction(ContactTestManager.generateContact(folderID, graphem));
        }

        ContactField field = ContactField.SUR_NAME;
        LinkedList<JSONObject> filters = new LinkedList<JSONObject>();
        for (int i = 0; i < sinograph.size() - 1; i++) {
            filters.add(new JSONObject( // @formatter:off
                "{'filter' : [ 'and', " +
                    "['>=' , {'field' : '" + field.getAjaxName() + "'} , '" + sinograph.get(i) + "'], " +
                    "['<' , {'field' : '" + field.getAjaxName() + "'}, '" + sinograph.get(i + 1) + "'] " +
                "]}")); // @formatter:on
        }

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        int currentPosition = 0;
        for (JSONObject filter : filters) {
            String ident = "Step #" + currentPosition + " from " + sinograph.get(currentPosition) + " to " + sinograph.get(currentPosition + 1) + ": ";
            AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { field.getNumber() }, field.getNumber(), "asc", "gb2312");
            CommonSearchResponse response = getClient().execute(request);
            assertFalse(response.hasError(), ident + "Should work");

            Object[][] resultTable = response.getArray();
            assertNotNull(resultTable, ident + "Should find at least a result");
            assertEquals(1, resultTable.length, ident + "Should find one result");

            int columnPos = response.getColumnPos(field.getNumber());
            String actualName = (String) resultTable[0][columnPos];
            assertEquals(sinograph.get(currentPosition), actualName, ident + "Should be contained");

            currentPosition++;
        }
    }

    @Test
    public void testOrderByWithCollation() throws Exception {
        ContactField field = ContactField.SUR_NAME;

        List<String> sinograph = Arrays.asList("\u963f", "\u6ce2", "\u6b21", "\u7684", "\u9e45", "\u5bcc", "\u54e5", "\u6cb3", "\u6d01", "\u79d1", "\u4e86", "\u4e48", "\u5462", "\u54e6", "\u6279", "\u4e03", "\u5982", "\u56db", "\u8e22", "\u5c4b", "\u897f", "\u8863", "\u5b50");
        LinkedList<String> randomized = new LinkedList<String>(sinograph);
        Collections.shuffle(randomized);
        for (String graphem : randomized) {
            cotm.newAction(ContactTestManager.generateContact(folderID, graphem));
        }

        JSONObject filter = new JSONObject("{'filter' : [ '>=' , {'field':'" + field.getAjaxName() + "'}, '\u963f' ]})");

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { field.getNumber() }, field.getNumber(), "asc", "gb2312");
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find at least a result");
        int columnPos = response.getColumnPos(field.getNumber());

        LinkedList<String> actuals = new LinkedList<String>();
        for (int i = 0; i < resultTable.length; i++) {
            String actualName = (String) resultTable[i][columnPos];
            actuals.add(actualName);
        }

        for (int i = 0; i < actuals.size(); i++) {
            assertEquals(sinograph.get(i), actuals.get(i), "Graphen #" + i + " is wrong");
        }
    }

    @Test
    public void testNameThatAppearedTwice() throws Exception {
        String name = "\u7802\u7cd6";
        cotm.newAction(ContactTestManager.generateContact(folderID, name));

        ContactField field = ContactField.SUR_NAME;
        List<String> sinograph = Arrays.asList("\u963f", "\u6ce2", "\u6b21", "\u7684", "\u9e45", "\u5bcc", "\u54e5", "\u6cb3", "\u6d01", "\u79d1", "\u4e86", "\u4e48", "\u5462", "\u54e6", "\u6279", "\u4e03", "\u5982", "\u56db", "\u8e22", "\u5c4b", "\u897f", "\u8863", "\u5b50");

        LinkedList<JSONObject> filters = new LinkedList<JSONObject>();
        for (int i = 0; i < sinograph.size() - 1; i++) {
            filters.add(new JSONObject( // @formatter:off
                "{'filter' : [ 'and', " +
                    "['>=' , {'field' : '" + field.getAjaxName() + "'} , '" + sinograph.get(i) + "'], " +
                    "['<' , {'field' : '" + field.getAjaxName() + "'}, '" + sinograph.get(i + 1) + "'] " +
            "]}")); // @formatter:on
        }

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        int occurences = 0;
        for (JSONObject filter : filters) {
            AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { field.getNumber() }, field.getNumber(), "asc", "gb2312");
            CommonSearchResponse response = getClient().execute(request);
            Object[][] resultTable = response.getArray();
            occurences += resultTable.length;
        }
        assertEquals(1, occurences, "Should only appear once");
    }

    @Test
    public void testQuestionmarkWildcardInTheBeginning() throws Exception {
        ContactField field = ContactField.GIVEN_NAME;

        JSONObject filter = new JSONObject("{'filter' : ['=' , {'field' : '" + field.getAjaxName() + "'} , '?ob']}");

        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, Collections.singletonList(String.valueOf(folderID)), new int[] { Contact.GIVEN_NAME }, -1, null);
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find a result");
        assertEquals(1, resultTable.length, "Should find one result");

        int columnPos = response.getColumnPos(field.getNumber());
        String actual = (String) resultTable[0][columnPos];

        assertEquals("Bob", actual);
    }

    @Test
    public void testQuestionmarkWildcardInTheEnd() throws Exception {
        ContactField field = ContactField.GIVEN_NAME;

        JSONObject filter = new JSONObject("{'filter' : ['=' , {'field' : '" + field.getAjaxName() + "'} , 'Bo?']}");

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { Contact.GIVEN_NAME }, -1, null);
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find a result");
        assertEquals(1, resultTable.length, "Should find one result");

        int columnPos = response.getColumnPos(field.getNumber());
        String actual = (String) resultTable[0][columnPos];

        assertEquals("Bob", actual);
    }

    @Test
    public void testAsteriskWildcardInTheBeginning() throws Exception {
        ContactField field = ContactField.GIVEN_NAME;

        JSONObject filter = new JSONObject("{'filter' : ['=' , {'field' : '" + field.getAjaxName() + "'} , '*b']}");

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { Contact.GIVEN_NAME }, -1, null);
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find a result");
        assertEquals(1, resultTable.length, "Should find one result");

        int columnPos = response.getColumnPos(field.getNumber());
        String actual = (String) resultTable[0][columnPos];

        assertEquals("Bob", actual);
    }

    @Test
    public void testAsteriskWildcardInTheEnd() throws Exception {
        ContactField field = ContactField.GIVEN_NAME;

        JSONObject filter = new JSONObject("{'filter' : ['=' , {'field' : '" + field.getAjaxName() + "'} , 'B*']}");

        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        AdvancedSearchRequest request = new AdvancedSearchRequest(filter, folders, new int[] { Contact.GIVEN_NAME }, -1, null);
        CommonSearchResponse response = getClient().execute(request);
        assertFalse(response.hasError(), "Should work");

        Object[][] resultTable = response.getArray();
        assertNotNull(resultTable, "Should find a result");
        assertEquals(1, resultTable.length, "Should find one result");

        int columnPos = response.getColumnPos(field.getNumber());
        String actual = (String) resultTable[0][columnPos];

        assertEquals("Bob", actual);
    }

    @Test
    public void testFolderInFilter() throws Exception {
        /*
         * prepare illegal filters containing a 'folder_id' operand
         */
        String field = ContactField.GIVEN_NAME.getAjaxName();
        String folderField = ContactField.FOLDER_ID.getAjaxName();
        String[] testedFilters = { // @formatter:off
            "{'filter' : [ '<>' , {'field' : '" + folderField + "'} , '6']}",
            "{'filter' : [ 'and', ['=' , {'field' : '" + field + "'} , 'Bob'], ['=' , {'field' : '" + folderField + "'}, " + folderID + "]]}",
            "{'filter' : [ 'and', ['>=' , {'field' : '" + field + "'} , 'E'], ['NOT' , ['=' , {'field' : '" + folderField + "'}, '6']]]}"
        }; // @formatter:on
        /*
         * try to search by each filter w/o specifying searched folder ids explicitly
         */
        for (String filter : testedFilters) {
            AdvancedSearchRequest request = new AdvancedSearchRequest(new JSONObject(filter), null, new int[] { Contact.GIVEN_NAME }, -1, null);
            request.setFailOnError(false);
            CommonSearchResponse response = getClient().execute(request);
            assertTrue(response.hasError(), "Should have an error");
        }
        /*
         * try to search by each filter, specifying searched folder ids explicitly
         */
        List<String> folders = Collections.singletonList(String.valueOf(folderID));
        for (String filter : testedFilters) {
            AdvancedSearchRequest request = new AdvancedSearchRequest(new JSONObject(filter), folders, new int[] { Contact.GIVEN_NAME }, -1, null);
            request.setFailOnError(false);
            CommonSearchResponse response = getClient().execute(request);
            assertTrue(response.hasError(), "Should have an error");
        }
    }

    /*
     * TODO:
     * wrong collation
     */

}
