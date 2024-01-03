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

package com.openexchange.ajax.task;

import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import org.junit.jupiter.api.TestInfo;

/**
 * Super class for task ajax tests.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public abstract class AbstractTaskTest extends AbstractAJAXSession {

    private AJAXClient client;
    private int folderId;
    private TimeZone timeZone;

    protected AbstractTaskTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = getClient();
        folderId = client.getValues().getPrivateTaskFolder();
        timeZone = client.getValues().getTimeZone();
    }

    protected int getPrivateFolder() {
        return folderId;
    }

    protected TimeZone getTimeZone() {
        return timeZone;
    }
}
