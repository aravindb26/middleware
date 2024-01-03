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
 * {@link Bug49014VulTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class Bug49014VulTest extends AbstractSanitizing {

    public Bug49014VulTest() {
        super();
    }

     @Test
     public void testStartTagSanitizing1() {
        String content = "<!DOCTYPE html>\n" +
            "<html><head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "</head><body><p><br><a< onmouseover='prompt(document.domain)' src=x>Just another XSS!<br></p></body></html>";
        AssertionHelper.assertSanitizedDoesNotContain(getHtmlService(), content, "< onmouseover");
    }

     @Test
     public void testStartTagSanitizing2() {
        String content = "<!DOCTYPE html>\n" +
            "<html><head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "</head><body><p><br><a~ ~onmouseover='prompt(document.domain)' src=x>Just another XSS!<br></p></body></html>";
        AssertionHelper.assertSanitizedDoesNotContain(getHtmlService(), content, "onmouseover");
    }

     @Test
     public void testStartTagSanitizing3() {
        String content = "<!DOCTYPE html>\n" +
            "<html><head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "</head><body><p><a href=\"http://google.de\">bar</a></p><img/ src=x ~onerror='alert(1)'></body></html>";
        AssertionHelper.assertSanitizedDoesNotContain(getHtmlService(), content, "onerror");
    }
}
