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

package com.openexchange.ajax.importexport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.importexport.actions.ICalImportRequest;
import com.openexchange.ajax.importexport.actions.ICalImportResponse;
import com.openexchange.groupware.container.FolderObject;

/**
 * Tests the ICAL imports and exports by using the servlets.
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb' Prinz</a>
 *
 */

@Disabled
public class ICalImportExportServletTest extends AbstractImportExportServletTest {

    @Test
    public void testIcalMessage() throws Exception {
        final InputStream is = new ByteArrayInputStream("BEGIN:VCALENDAR".getBytes());
        final int folderId = createFolder("ical-empty-file-" + UUID.randomUUID().toString(), FolderObject.CONTACT);
        try {
            ICalImportRequest importRequest = new ICalImportRequest(folderId, is, false);
            ICalImportResponse resp = getClient().execute(importRequest);
            Assertions.assertEquals("I_E-0500", resp.getException().getErrorCode());
        } finally {
            removeFolder(folderId);
        }
    }

}
