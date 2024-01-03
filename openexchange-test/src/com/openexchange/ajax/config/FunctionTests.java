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

package com.openexchange.ajax.config;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Random;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.config.actions.GetRequest;
import com.openexchange.ajax.config.actions.GetResponse;
import com.openexchange.ajax.config.actions.SetRequest;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.test.common.tools.RandomString;
import org.junit.jupiter.api.TestInfo;

/**
 * This class contains tests for added funtionalities of the configuration tree.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class FunctionTests extends AbstractAJAXSession {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FunctionTests.class);

    private AJAXClient client;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = getClient();
    }

    /**
     * Tests if the idle timeout for uploaded files is sent to the GUI.
     * 
     * @throws Throwable if an exception occurs.
     */
    @Test
    public void testMaxUploadIdleTimeout() throws Throwable {
        final int value = client.execute(new GetRequest(Tree.MaxUploadIdleTimeout)).getInteger();
        LOG.info("Max upload idle timeout: {}", I(value));
        assertTrue(value > 0, "Got no value for the maxUploadIdleTimeout configuration " + "parameter.");
    }

    /**
     * Maximum time difference between server and client. This test fails if
     * a greater difference is detected.
     */
    private static final long MAX_DIFFERENCE = 1000;

    /**
     * Tests if the current time is sent by the server through the configuration
     * interface.
     */
    @Test
    public void testCurrentTime() throws Throwable {
        final long firstServerTime;
        {
            final GetRequest request = new GetRequest(Tree.CurrentTime);
            final GetResponse response = Executor.execute(client, request);
            firstServerTime = response.getLong();
        }
        final int randomWait = new Random(System.currentTimeMillis()).nextInt(10000);
        Thread.sleep(randomWait);
        final long secondServerTime;
        final long totalDuration;
        {
            final GetRequest request = new GetRequest(Tree.CurrentTime);
            final GetResponse response = Executor.execute(client, request);
            secondServerTime = response.getLong();
            totalDuration = response.getTotalDuration();
        }
        final Date sTime = client.getValues().getServerTime();
        final long localTime = System.currentTimeMillis();
        LOG.info("Local time: " + localTime + " Server time: " + sTime.getTime());
        final long difference = Math.abs(secondServerTime - totalDuration - randomWait - firstServerTime);
        LOG.info("Time difference: " + difference);
        assertTrue(difference < MAX_DIFFERENCE, "Too big time difference: " + difference);
    }

    /**
     * Tests if the server gives the context identifier.
     */
    @Test
    public void testContextID() throws Throwable {
        final int value = client.execute(new GetRequest(Tree.ContextID)).getInteger();
        LOG.info("Context identifier: " + value);
        assertTrue(value > 0, "Got no value for the contextID configuration parameter.");
    }

    /**
     * Tests if the GUI value can be written and read correctly.
     */
    @Test
    public void testGUI() throws Throwable {
        GetResponse origGet = client.execute(new GetRequest(Tree.GUI));
        String testValue = RandomString.generateChars(20);
        try {
            client.execute(new SetRequest(Tree.GUI, testValue));
            GetResponse testGet = client.execute(new GetRequest(Tree.GUI));
            assertEquals(testValue, testGet.getString(), "Written GUI value differs from read one.");
        } finally {
            client.execute(new SetRequest(Tree.GUI, origGet.getData()));
        }
    }

    /**
     * Checks if the new preferences entry extras works.
     */
    @Test
    public void testConfigJumpFlag() throws Throwable {
        final GetResponse get = client.execute(new GetRequest(Tree.Extras));
        LOG.info("Should extras link be displayed: " + get.getBoolean());
    }

    @Test
    public void testMailAddressAutoSearchFlag() throws Throwable {
        final GetResponse get = client.execute(new GetRequest(Tree.MailAddressAutoSearch));
        LOG.info("Is search triggered on opened recipient dialog: " + get.getBoolean());
    }

    @Test
    public void testMinimumSearchCharacters() throws Throwable {
        final GetResponse response = client.execute(new GetRequest(Tree.MinimumSearchCharacters));
        LOG.info("Minimum of characters for a search pattern: " + response.getInteger());
    }

    @Test
    public void testSingleFolderSearch() throws Throwable {
        final GetResponse response = client.execute(new GetRequest(Tree.SingleFolderSearch));
        LOG.info("User is only allowed to search in a single folder: " + response.getBoolean());
    }

    @Test
    public void testNotifySwitches() throws Throwable {
        for (final Tree param : new Tree[] { Tree.CalendarNotifyNewModifiedDeleted, Tree.CalendarNotifyNewAcceptedDeclinedAsCreator, Tree.CalendarNotifyNewAcceptedDeclinedAsParticipant, Tree.TasksNotifyNewModifiedDeleted, Tree.TasksNotifyNewAcceptedDeclinedAsCreator, Tree.TasksNotifyNewAcceptedDeclinedAsParticipant }) {
            testBoolean(param, true);
        }
    }

    @Test
    public void testCharacterSearch() throws Throwable {
        final GetResponse response = client.execute(new GetRequest(Tree.CharacterSearch));
        LOG.info("User is only allowed to search via character side bar in contacts: " + response.getBoolean());
    }

    @Test
    public void testAllFolderForAutoComplete() throws Throwable {
        final GetResponse response = client.execute(new GetRequest(Tree.AllFolderForAutoComplete));
        LOG.info("User is allowed to search via auto complete in all folders: " + response.getBoolean());
    }

    @Test
    public void testFolderTree() throws Throwable {
        final int defaultValue = client.execute(new GetRequest(Tree.FolderTree)).getInteger();
        client.execute(new SetRequest(Tree.FolderTree, I(0)));
        assertEquals(0, client.execute(new GetRequest(Tree.FolderTree)).getInteger(), "Selecting OX folder tree did not work.");
        client.execute(new SetRequest(Tree.FolderTree, I(1)));
        assertEquals(1, client.execute(new GetRequest(Tree.FolderTree)).getInteger(), "Selecting new Outlook folder tree did not work.");
        // Restore default
        client.execute(new SetRequest(Tree.FolderTree, I(defaultValue)));
    }

    @Test
    public void testAvailableTimeZones() throws Throwable {
        final GetResponse response = client.execute(new GetRequest(Tree.AvailableTimeZones));
        JSONObject json = response.getJSON();
        for (Entry<String, Object> entry : json.entrySet()) {
            LOG.info("Time zone: " + entry.getKey() + ", localized name: " + entry.getValue());
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testOLOX20Module() throws Throwable {
        final GetResponse response = client.execute(new GetRequest(Tree.OLOX20Module));
        assertFalse(response.getBoolean(), "Module for OLOX20 must be always false to prevent UI plugin loading.");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testOLOX20Active() throws Throwable {
        final GetResponse response = client.execute(new GetRequest(Tree.OLOX20Active));
        LOG.info("Is the user allowed to use OXtender for Microsoft Outlook 2: " + response.getBoolean());
    }

    private void testBoolean(final Tree param, final boolean testWrite) throws Throwable {
        // Remember for restore.
        final boolean oldValue = client.execute(new GetRequest(param)).getBoolean();
        if (testWrite) {
            testWriteTrue(param);
            testWriteFalse(param);
            // Restore original value.
            client.execute(new SetRequest(param, Boolean.valueOf(oldValue)));
        }
    }

    private void testWriteTrue(final Tree param) throws Throwable {
        client.execute(new SetRequest(param, Boolean.TRUE));
        assertTrue(client.execute(new GetRequest(param)).getBoolean());
    }

    private void testWriteFalse(final Tree param) throws Throwable {
        client.execute(new SetRequest(param, Boolean.FALSE));
        assertFalse(client.execute(new GetRequest(param)).getBoolean());
    }
}
