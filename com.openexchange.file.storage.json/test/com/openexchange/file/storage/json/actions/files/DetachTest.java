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

package com.openexchange.file.storage.json.actions.files;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.FileStorageFileAccess;

/**
 * {@link DetachTest}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class DetachTest extends FileActionTest {

    @Test
    public void testMissingParameters() {
        try {
            action.handle(request());
            fail("Expected Exception due to missing parameters");
        } catch (@SuppressWarnings("unused") OXException x) {
            assertTrue(true);
        }
    }

    @Test
    public void testAction() throws OXException, JSONException {
        request().param("timestamp", "1337").param("id", "12").body(new JSONArray("[1,2,5]"));

        fileAccess().expectCall("removeVersion", "12", new String[] { "1", "2", "5" }).andReturn(new String[0]);

        final DefaultFile updated = new DefaultFile();
        updated.setLastModified(new Date());
        fileAccess().expectCall("getFileMetadata", "12", FileStorageFileAccess.CURRENT_VERSION).andReturn(updated);

        perform();

        fileAccess().assertAllWereCalled();
    }

    @Override
    public AbstractFileAction createAction() {
        return new DetachAction();
    }

}
