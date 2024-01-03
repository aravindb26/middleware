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

package com.openexchange.html.vulntests;

import org.junit.Test;
import com.openexchange.html.AbstractSanitizing;
import com.openexchange.html.AssertionHelper;

/**
 * {@link Bug28642VulTest}
 *
 * @author <a href="mailto:lars.hoogestraat@open-xchange.com">Lars Hoogestraat</a>
 */
public class Bug28642VulTest extends AbstractSanitizing {
     @Test
     public void testInsecureHref() {
        String content = "<b>test</b>\n" +
            "\n" +
            "<script>alert(42)</script>\n" +
            "\n" +
            "<img onerror=\"alert(43)\">plaatje</img>\n" +
            "\n" +
            "<b>test</b>";

        //TODO: support multiple does not contain expressions
        AssertionHelper.assertSanitizedDoesNotContain(getHtmlService(), content, "<script>alert(42)</script>");
        AssertionHelper.assertSanitizedDoesNotContain(getHtmlService(), content, "alert(43)");
    }
}
