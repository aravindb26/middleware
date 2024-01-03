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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.config.actions.GetRequest;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import org.junit.jupiter.api.TestInfo;

/**
 * Verifies that bug 15354 does not appear again.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class Bug15354Test extends AbstractAJAXSession {

    private static final int ITERATIONS = 1000;

    private final BetaWriter[] writer = new BetaWriter[5];
    private final Thread[] thread = new Thread[writer.length];

    private AJAXClient client;
    private Object[] origAliases;

    public Bug15354Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = getClient();
        origAliases = client.execute(new GetRequest(Tree.MailAddresses)).getArray();
        assertNotNull(origAliases, "Aliases are null.");
        Arrays.sort(origAliases);
        for (int i = 0; i < writer.length; i++) {
            writer[i] = new BetaWriter(testUser, true);
            thread[i] = new Thread(writer[i]);
        }
        for (int i = 0; i < thread.length; i++) {
            thread[i].start();
            Thread.sleep(10L); // Avoid concurrent modification of last login recorder
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        for (int i = 0; i < writer.length; i++) {
            writer[i].stop();
        }
        for (int i = 0; i < thread.length; i++) {
            thread[i].join();
        }
        for (int i = 0; i < writer.length; i++) {
            final Throwable throwable = writer[i].getThrowable();
            assertNull(throwable, "Expected no Throwable, but there is one: " + throwable);
        }
    }

    @Test
    public void testAliases() throws Throwable {
        boolean stop = false;
        for (int i = 0; i < ITERATIONS && !stop; i++) {
            Object[] testAliases = client.execute(new GetRequest(Tree.MailAddresses)).getArray();
            if (null == testAliases) {
                stop = true;
            } else if (origAliases.length != testAliases.length) {
                stop = true;
            } else {
                Arrays.sort(testAliases);
                boolean match = true;
                for (int j = 0; j < origAliases.length && match; j++) {
                    if (!origAliases[j].equals(testAliases[j])) {
                        match = false;
                    }
                }
                stop = stop || !match;
            }
            for (int j = 0; j < writer.length; j++) {
                stop = stop || null != writer[j].getThrowable();
            }
        }
        // Final test.
        Object[] testAliases = client.execute(new GetRequest(Tree.MailAddresses)).getArray();
        assertNotNull(origAliases, "Aliases are null.");
        assertNotNull(testAliases, "Aliases are null.");
        assertEquals(origAliases.length, testAliases.length, "Number of aliases are not equal.");
        Arrays.sort(origAliases);
        Arrays.sort(testAliases);
        for (int i = 0; i < origAliases.length; i++) {
            assertEquals(origAliases[i], testAliases[i], "Aliases are not the same.");
        }
    }
}
