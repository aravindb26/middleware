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

package com.openexchange.mail.exportpdf.test;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.junit.Test;
import com.openexchange.mail.exportpdf.converter.ReplacingInputStream;
import com.openexchange.mail.exportpdf.converter.ReplacingInputStream.Replacement;

/**
 * {@link ReplacingInputStreamTest}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class ReplacingInputStreamTest {

    private String replace(ReplacingInputStream s) throws IOException {
        var bos = new ByteArrayOutputStream();
        s.transferTo(bos);
        return bos.toString(StandardCharsets.UTF_8);
    }

    @Test
    public void testReplace0() throws Exception {
        var replacingInputStream = new ReplacingInputStream("Hello World! This is a Test!", StandardCharsets.UTF_8, "World", "OX");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "Hello OX! This is a Test!", replaced);
    }

    @Test
    public void testReplace1() throws Exception {
        var replacingInputStream = new ReplacingInputStream("Hello World! Hello World!", StandardCharsets.UTF_8, "World", "OX");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "Hello OX! Hello OX!", replaced);
    }

    @Test
    public void testReplace2() throws Exception {
        var replacingInputStream = new ReplacingInputStream("Hello World!", StandardCharsets.UTF_8, "No match", "No used");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should NOT have been replaced", "Hello World!", replaced);
    }

    @Test
    public void testReplace3() throws Exception {
        var replacingInputStream = new ReplacingInputStream("Hello World!", StandardCharsets.UTF_8, "XXXX", "");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should NOT have been replaced", "Hello World!", replaced);
    }

    @Test
    public void testReplace3a() throws Exception {
        var replacingInputStream = new ReplacingInputStream("Hello World!", StandardCharsets.UTF_8, "Hello", ReplacingInputStream.REMOVE);
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", " World!", replaced);
    }

    @Test
    public void testReplace4() throws Exception {
        var replacingInputStream = new ReplacingInputStream("Ein schönes FahrvergnÜgen.", StandardCharsets.UTF_8, "Ü", "ü");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "Ein schönes Fahrvergnügen.", replaced);
    }

    @Test
    public void testReplace5() throws Exception {
        var replacingInputStream = new ReplacingInputStream("öTestö", StandardCharsets.UTF_8, "Test", "Fest");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "öFestö", replaced);
    }

    @Test
    public void testReplace51() throws Exception {
        var replacingInputStream = new ReplacingInputStream("xa", StandardCharsets.UTF_8, "x", "y");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "ya", replaced);
    }

    @Test
    public void testReplace6() throws Exception {
        var replacingInputStream = new ReplacingInputStream("ö", StandardCharsets.UTF_8, "ö", "o");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "o", replaced);
    }

    @Test
    public void testReplace7() throws Exception {
        var replacingInputStream = new ReplacingInputStream("Aö", StandardCharsets.UTF_8, "A", "B");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "Bö", replaced);
    }

    @Test
    public void testReplace8() throws Exception {
        var replacingInputStream = new ReplacingInputStream("测试" /* "test" */, StandardCharsets.UTF_8, "测试", "test");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "test", replaced);
    }

    @Test
    public void testReplace9() throws Exception {
        var replacingInputStream = new ReplacingInputStream("x test x test", StandardCharsets.UTF_8, "x", "test");
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "test test test test", replaced);
    }

    @Test
    public void testReplace10() throws Exception {
        var replacingInputStream = new ReplacingInputStream("x test x test", StandardCharsets.UTF_8, "x", new Replacement() {

            @Override
            public InputStream getReplacementData(int matchCount, MatchResult match) {
                return new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
            }

        });
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", "test test test test", replaced);
    }

    @Test
    public void replaceCIDinHTML() throws Exception {
        String htmlContent = """
                 <!doctype html>
                  <html>
                 <head>
                  <meta charset="UTF-8">
                 </head>
                 <body>
                  <div class="default-style">
                  </div>
                  <div class="default-style">
                   <img class="aspect-ratio" style="max-width: 100%;" src="CID:0137b93a1c924e3b8f7b0c65fae861e1@open-xchange.com" alt="">
                  </div>
                  <div class="default-style">
                   <img class="aspect-ratio" style="max-width: 100%;" SRC="CID:77b93a1c924e3b8f7b0c65fae861e1@open-xchange.com" alt="">
                  </div>
                 </body>
                </html>
            """;
        String expectedHtmlContent = """
                 <!doctype html>
                  <html>
                 <head>
                  <meta charset="UTF-8">
                 </head>
                 <body>
                  <div class="default-style">
                  </div>
                  <div class="default-style">
                   <img class="aspect-ratio" style="max-width: 100%;" src="http://www.example.org/image1.jpg" alt="">
                  </div>
                  <div class="default-style">
                   <img class="aspect-ratio" style="max-width: 100%;" src="http://www.example.org/image2.jpg" alt="">
                  </div>
                 </body>
                </html>
            """;

        var replacingInputStream = new ReplacingInputStream(htmlContent, StandardCharsets.UTF_8, "src=\"CID:(.*?)\"", Pattern.CASE_INSENSITIVE, new Replacement() {

            @Override
            public InputStream getReplacementData(int matchCount, MatchResult match) {
                if ("0137b93a1c924e3b8f7b0c65fae861e1@open-xchange.com".equals(match.group(1))) {
                    return new ByteArrayInputStream("src=\"http://www.example.org/image1.jpg\"".getBytes(StandardCharsets.UTF_8));
                } else if ("77b93a1c924e3b8f7b0c65fae861e1@open-xchange.com".equals(match.group(1))) {
                    return new ByteArrayInputStream("src=\"http://www.example.org/image2.jpg\"".getBytes(StandardCharsets.UTF_8));
                }
                return null;
            }
        });
        String replaced = replace(replacingInputStream);
        replacingInputStream.close();
        assertEquals("The content should have been replaced", expectedHtmlContent, replaced);
    }
}
