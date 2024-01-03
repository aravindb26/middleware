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

package com.openexchange.tools.net;

import static com.openexchange.junit.Warn.warnEquals;
import static com.openexchange.tools.net.URIDefaults.IMAP;
import static org.junit.Assert.assertEquals;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;

/**
 * Verifies that the {@link URIParser} is able to handle all possible types of declaring a host for a backend connection.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
@SuppressWarnings("static-method")
public class URIParserTest {

    private static final boolean WARN = true;

    public URIParserTest() {
        super();
    }

     @Test
     public void testProtIPv6Port() throws URISyntaxException {
        test("imap://[fc00::2]:143", "imap", "[fc00::2]", 143);
    }

     @Test
     public void testProtIPv6() throws URISyntaxException {
        test("imap://[fc00::2]", "imap", "[fc00::2]", -1);
    }

     @Test
     public void testIPv6Port() throws URISyntaxException {
        test("[fc00::2]:143", null, "[fc00::2]", 143);
    }

     @Test
     public void testIPv6() throws URISyntaxException {
        test("fc00::2", null, "[fc00::2]", -1);
    }

     @Test
     public void testProtIPv4Port() throws URISyntaxException {
        test("imap://192.168.32.134:143", "imap", "192.168.32.134", 143);
    }

     @Test
     public void testProtIPv4() throws URISyntaxException {
        test("imap://192.168.32.134", "imap", "192.168.32.134", -1);
    }

     @Test
     public void testIPv4Port() throws URISyntaxException {
        test("192.168.32.134:143", null, "192.168.32.134", 143);
    }

     @Test
     public void testIPv4() throws URISyntaxException {
        test("192.168.32.134", null, "192.168.32.134", -1);
    }

     @Test
     public void testProtHostPort() throws URISyntaxException {
        test("imap://devel-mail.example.org:143", "imap", "devel-mail.example.org", 143);
    }

     @Test
     public void testProtHost() throws URISyntaxException {
        test("imap://devel-mail.example.org", "imap", "devel-mail.example.org", -1);
    }

     @Test
     public void testHostPort() throws URISyntaxException {
        test("devel-mail.example.org:143", null, "devel-mail.example.org", 143);
    }

     @Test
     public void testHost() throws URISyntaxException {
        test("devel-mail.example.org", null, "devel-mail.example.org", -1);
    }

    @Test
    public void imapProtIPv6Port() throws URISyntaxException {
        test("imap://[fc00::2]:143", "imap", "[fc00::2]", 143, IMAP);
    }

    @Test
    public void imapProtIPv6OtherPort() throws URISyntaxException {
        test("imap://[fc00::2]:144", "imap", "[fc00::2]", 144, IMAP);
    }

    @Test
    public void imapProtIPv6() throws URISyntaxException {
        test("imap://[fc00::2]", "imap", "[fc00::2]", 143, IMAP);
    }

    @Test
    public void imapIPv6Port() throws URISyntaxException {
        test("[fc00::2]:143", "imap", "[fc00::2]", 143, IMAP);
    }

    @Test
    public void imapIPv6OtherPort() throws URISyntaxException {
        test("[fc00::2]:144", "imap", "[fc00::2]", 144, IMAP);
    }

    @Test
    public void imapIPv6() throws URISyntaxException {
        test("fc00::2", "imap", "[fc00::2]", 143, IMAP);
    }

    @Test
    public void imapProtIPv4Port() throws URISyntaxException {
        test("imap://192.168.32.134:143", "imap", "192.168.32.134", 143, IMAP);
    }

    @Test
    public void imapProtIPv4therPort() throws URISyntaxException {
        test("imap://192.168.32.134:144", "imap", "192.168.32.134", 144, IMAP);
    }

    @Test
    public void imapProtIPv4() throws URISyntaxException {
        test("imap://192.168.32.134", "imap", "192.168.32.134", 143, IMAP);
    }

    @Test
    public void imapIPv4Port() throws URISyntaxException {
        test("192.168.32.134:143", "imap", "192.168.32.134", 143, IMAP);
    }

    @Test
    public void imapIPv4OtherPort() throws URISyntaxException {
        test("192.168.32.134:144", "imap", "192.168.32.134", 144, IMAP);
    }

    @Test
    public void imapIPv4() throws URISyntaxException {
        test("192.168.32.134", "imap", "192.168.32.134", 143, IMAP);
    }

    @Test
    public void imapProtHostPort() throws URISyntaxException {
        test("imap://devel-mail.example.org:143", "imap", "devel-mail.example.org", 143, IMAP);
    }

    @Test
    public void imapProtHostOtherPort() throws URISyntaxException {
        test("imap://devel-mail.example.org:144", "imap", "devel-mail.example.org", 144, IMAP);
    }

    @Test
    public void imapProtHost() throws URISyntaxException {
        test("imap://devel-mail.example.org", "imap", "devel-mail.example.org", 143, IMAP);
    }

    @Test
    public void imapHostPort() throws URISyntaxException {
        test("devel-mail.example.org:143", "imap", "devel-mail.example.org", 143, IMAP);
    }

    @Test
    public void imapHostOtherPort() throws URISyntaxException {
        test("devel-mail.example.org:144", "imap", "devel-mail.example.org", 144, IMAP);
    }

    @Test
    public void imapHost() throws URISyntaxException {
        test("devel-mail.example.org", "imap", "devel-mail.example.org", 143, IMAP);
    }

    @Test
    public void imapsProtIPv6Port() throws URISyntaxException {
        test("imaps://[fc00::2]:993", "imaps", "[fc00::2]", 993, IMAP);
    }

    @Test
    public void imapsProtIPv6OtherPort() throws URISyntaxException {
        test("imaps://[fc00::2]:994", "imaps", "[fc00::2]", 994, IMAP);
    }

    @Test
    public void imapsProtIPv6() throws URISyntaxException {
        test("imaps://[fc00::2]", "imaps", "[fc00::2]", 993, IMAP);
    }

    @Test
    public void imapsIPv6Port() throws URISyntaxException {
        test("[fc00::2]:993", "imaps", "[fc00::2]", 993, IMAP);
    }

    @Test
    public void imapsProtIPv4Port() throws URISyntaxException {
        test("imaps://192.168.32.134:993", "imaps", "192.168.32.134", 993, IMAP);
    }

    @Test
    public void imapsProtIPv4OtherPort() throws URISyntaxException {
        test("imaps://192.168.32.134:994", "imaps", "192.168.32.134", 994, IMAP);
    }

    @Test
    public void imapsProtIPv4() throws URISyntaxException {
        test("imaps://192.168.32.134", "imaps", "192.168.32.134", 993, IMAP);
    }

    @Test
    public void imapsIPv4Port() throws URISyntaxException {
        test("192.168.32.134:993", "imaps", "192.168.32.134", 993, IMAP);
    }

    @Test
    public void imapsProtHostPort() throws URISyntaxException {
        test("imaps://devel-mail.example.org:993", "imaps", "devel-mail.example.org", 993, IMAP);
    }

    @Test
    public void imapsProtHostOtherPort() throws URISyntaxException {
        test("imaps://devel-mail.example.org:994", "imaps", "devel-mail.example.org", 994, IMAP);
    }

    @Test
    public void imapsProtHost() throws URISyntaxException {
        test("imaps://devel-mail.example.org", "imaps", "devel-mail.example.org", 993, IMAP);
    }

    @Test
    public void imapsHostPort() throws URISyntaxException {
        test("devel-mail.example.org:993", "imaps", "devel-mail.example.org", 993, IMAP);
    }

    private static final void test(String s, String scheme, String host, int port) throws URISyntaxException {
        test(s, scheme, host, port, URIDefaults.NULL);
    }

    private static final void test(String s, String scheme, String host, int port, URIDefaults defaults) throws URISyntaxException {
        URI uri = URIParser.parse(s, defaults);
        assertEquals("Protocol not correctly detected: \"" + s + "\".", scheme, uri.getScheme());
        assertEquals("Host not correctly detected: \"" + s + "\".", host, uri.getHost());
        assertEquals("Port not correctly detected: \"" + s + "\".", port, uri.getPort());
        String expected = new URI(scheme, null, host, port, null, null, null).toString();
        assertEquals("toString does not work: \"" + s + "\".", expected, uri.toString());
        if (WARN) {
            try {
                uri = new URI(s);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            warnEquals("Protocol not correctly detected: \"" + s + "\".", scheme, uri.getScheme());
            warnEquals("Host not correctly detected: \"" + s + "\".", host, uri.getHost());
            warnEquals("Port not correctly detected.", port, uri.getPort());
            warnEquals("toString does not work: \"" + s + "\".", expected, uri.toString());
        } else {
            uri = new URI(s);
            assertEquals("Protocol not correctly detected: \"" + s + "\".", scheme, uri.getScheme());
            assertEquals("Host not correctly detected: \"" + s + "\".", host, uri.getHost());
            assertEquals("Port not correctly detected: \"" + s + "\".", port, uri.getPort());
            assertEquals("toString does not work: \"" + s + "\".", expected, uri.toString());
        }
    }

}
