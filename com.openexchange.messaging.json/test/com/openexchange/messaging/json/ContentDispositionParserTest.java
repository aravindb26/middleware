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

package com.openexchange.messaging.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import com.openexchange.exception.OXException;
import com.openexchange.messaging.ContentDisposition;
import com.openexchange.messaging.MessagingHeader;
import com.openexchange.messaging.generic.internet.MimeContentDisposition;

/**
 * {@link ContentDispositionParserTest}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class ContentDispositionParserTest {

    @Test
    public void testParseComplex() throws JSONException, OXException {
        final ContentDispositionParser parser = new ContentDispositionParser();

        assertTrue(parser.handles("content-disposition", null));

        final JSONObject jsonCDisp = new JSONObject();
        jsonCDisp.put("type", "attachment");

        final JSONObject params = new JSONObject();
        params.put("filename", "foo.dat");

        jsonCDisp.put("params", params);

        final Map<String, Collection<MessagingHeader>> headers = new HashMap<String, Collection<MessagingHeader>>();

        parser.parseAndAdd(headers, "content-disposition", jsonCDisp);

        assertTrue(!headers.isEmpty());

        final Collection<MessagingHeader> collection = headers.get(MimeContentDisposition.getContentDispositionName());
        assertNotNull("Missing content-disposition header", collection);
        assertEquals(1, collection.size());

        final ContentDisposition cDisp = (ContentDisposition) collection.iterator().next();

        assertEquals("attachment", cDisp.getDisposition());

        assertEquals("foo.dat", cDisp.getFilenameParameter());
    }

    @Test
    public void testParseBasic() throws OXException, JSONException {
        final ContentDispositionParser parser = new ContentDispositionParser();

        final String stringCDisp = "attachment;filename=foo.dat";

        final Map<String, Collection<MessagingHeader>> headers = new HashMap<String, Collection<MessagingHeader>>();

        parser.parseAndAdd(headers, "content-disposition", stringCDisp);

        assertTrue(!headers.isEmpty());

        final Collection<MessagingHeader> collection = headers.get(MimeContentDisposition.getContentDispositionName());
        assertNotNull("Missing content-disposition header", collection);
        assertEquals(1, collection.size());

        final ContentDisposition cDisp = (ContentDisposition) collection.iterator().next();

        assertEquals("attachment", cDisp.getDisposition());

        assertEquals("foo.dat", cDisp.getFilenameParameter());

    }


}
