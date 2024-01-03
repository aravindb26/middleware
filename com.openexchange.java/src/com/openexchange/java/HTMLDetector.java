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

package com.openexchange.java;

import static com.openexchange.java.Strings.toLowerCase;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import com.google.common.collect.ImmutableSet;

/**
 * {@link HTMLDetector} - Detects HTML tags in a byte sequence.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class HTMLDetector {

    private static final Set<String> JS_EVENT_HANDLER = ImmutableSet.of(
        "onabort",
        "onactivate",
        "onafterprint",
        "onanimationend",
        "onanimationiteration",
        "onanimationstart",

        "onbeforeprint",
        "onbeforeunload",
        "onblur",

        "oncanplay",
        "oncanplaythrough",
        "onchange",
        "onclick ",
        "oncontextmenu",
        "oncopy",
        "oncuechange",
        "oncut",

        "ondblclick",
        "ondomcontentloaded",
        "ondrag",
        "ondragend",
        "ondragenter",
        "ondragleave",
        "ondragover",
        "ondragstart",
        "ondrop",
        "ondurationchange",

        "onemptied",
        "onended",
        "onerror",

        "onfocus",
        "onfocusin",
        "onfocusout",

        "ongotpointercapture",

        "onhashchange",

        "oninput",
        "oninvalid",

        "onjavascript",

        "onkeydown",
        "onkeypress",
        "onkeyup",

        "onlanguagechange",
        "onload",
        "onloadeddata",
        "onloadedmetadata",
        "onloadstart",
        "onlostpointercapture",

        "onmessage",
        "onmousedown",
        "onmouseenter",
        "onmouseleave",
        "onmousemove",
        "onmouseout",
        "onmouseover",
        "onmouseup",
        "onmousewheel",

        "onoffline",
        "ononline",

        "onpageshow",
        "onpagehide",
        "onpaste",
        "onpause",
        "onplay",
        "onplaying",
        "onpointercancel",
        "onpointerdown",
        "onpointerenter",
        "onpointerleave",
        "onpointermove",
        "onpointerout",
        "onpointerover",
        "onpointerup",
        "onpopstate",
        "onprogress",

        "onratechange",
        "onrejectionhandled",
        "onreset",
        "onresize",

        "onscroll",
        "onsearch",
        "onseeked",
        "onseeking",
        "onselect",
        "onshow",
        "onstalled",
        "onstorage",
        "onsubmit",
        "onsuspend",

        "ontimeupdate",
        "ontoggle",
        "ontouchcancel",
        "ontouchend",
        "ontouchmove",
        "ontouchstart",
        "ontransitioned",

        "onunhandledrejection",
        "onunload",

        "onvolumechange",

        "onwaiting",
        "onwheel",

        "onzoom");

    /**
     * Initializes a new {@link HTMLDetector}.
     */
    private HTMLDetector() {
        super();
    }

    /**
     * Checks if given string contains common HTML tags.
     *
     * @param sequence The string to check
     * @param strict <code>true</code> for strict checking; otherwise <code>false</code>
     * @param eventHandlerIdentifiers The optional event handler identifiers to consider
     * @return <code>true</code> if given String contains common HTML tags; otherwise <code>false</code>
     */
    public static boolean containsHTMLTags(final String sequence, final boolean strict, Collection<String> eventHandlerIdentifiers) {
        return strict ? containsHTMLTags(sequence, eventHandlerIdentifiers, "<br", "<p>") : containsHTMLTags(sequence, eventHandlerIdentifiers);
    }

    /**
     * Checks if given string contains common HTML tags.
     *
     * @param sequence The string to check
     * @param tags Additional tags to look for
     * @return <code>true</code> if given String contains common HTML tags; otherwise <code>false</code>
     */
    public static boolean containsHTMLTags(final String sequence, final String... tags) {
        return containsHTMLTags(sequence, JS_EVENT_HANDLER, tags);
    }

    /**
     * Checks if given string contains common HTML tags.
     *
     * @param sequence The string to check
     * @param eventHandlerIdentifiers The optional event handler identifiers to consider
     * @param tags Additional tags to look for
     * @return <code>true</code> if given String contains common HTML tags; otherwise <code>false</code>
     */
    public static boolean containsHTMLTags(String sequence, Collection<String> eventHandlerIdentifiers, String... tags) {
        if (sequence == null) {
            throw new NullPointerException();
        }

        String lc = Strings.asciiLowerCase(sequence);
        if (lc.indexOf('<') >= 0) {
            if ((lc.indexOf("html>") >= 0)) {
                return true;
            }
            if ((lc.indexOf("head>") >= 0)) {
                return true;
            }
            if ((lc.indexOf("body>") >= 0)) {
                return true;
            }
            if ((lc.indexOf("<script") >= 0)) {
                return true;
            }
            if ((lc.indexOf("<img") >= 0)) {
                return true;
            }
            if ((lc.indexOf("<object") >= 0)) {
                return true;
            }
            if ((lc.indexOf("<embed") >= 0)) {
                return true;
            }
            if (null != tags && tags.length > 0) {
                for (int i = tags.length; i-- > 0;) {
                    final String tag = tags[i];
                    if (Strings.isNotEmpty(tag) && (lc.indexOf(tag) >= 0)) {
                        return true;
                    }
                }
            }
        }

        if ((lc.indexOf("javascript") >= 0)) {
            return true;
        }
        if (doContainsEventHandler(lc, (eventHandlerIdentifiers == null || eventHandlerIdentifiers.isEmpty() ? JS_EVENT_HANDLER : eventHandlerIdentifiers))) {
            return true;
        }

        return false;
    }
    
    private static boolean doContainsEventHandler(String lc, Collection<String> eventHandlerIdentifiers) {
        int pos = lc.indexOf("on");
        while (pos >= 0) {
            if (pos == 0 ? true : (false == isWordCharacter(lc.charAt(pos - 1)))) {
                for (String globalEventHandler : eventHandlerIdentifiers) {
                    if (lc.regionMatches(false, pos, globalEventHandler, 0, globalEventHandler.length())) {
                        int end = pos + globalEventHandler.length();
                        if ((end >= lc.length()) || (false == isWordCharacter(lc.charAt(end)))) {
                            // Ends with or contains global event handler
                            return true;
                        }
                    }
                }
            }
            pos = lc.indexOf("on", pos + 1);
        }
        return false;
    }

    /**
     * Checks if specified character is a word character: <code>[a-zA-Z_0-9-]</code>
     *
     * @return <code>true</code> if the indicated character is a word character; otherwise <code>false</code>
     */
    private static boolean isWordCharacter(char c) {
        return '-' == c || '_' == c || Strings.isDigit(c) || Character.isLetter(c);
    }

    /**
     * Checks if given string contains specified HTML tag.
     *
     * @param sequence The string to check
     * @param tag The HTML tag; e.g. <code>"body"</code>
     * @return <code>true</code> if given String contains specified HTML tag; otherwise <code>false</code>
     */
    public static boolean containsHTMLTag(final String sequence, final String tag) {
        if (sequence == null || tag == null) {
            throw new NullPointerException();
        }
        return containsIgnoreCase(sequence, tag.startsWith("<") ? tag : new StringBuilder(tag.length() + 2).append('<').append(tag).append('>').toString());
    }

    /**
     * Checks if given string contains specified string.
     *
     * @param sequence The string to check
     * @param str The string
     * @return <code>true</code> if given String contains specified string; otherwise <code>false</code>
     */
    private static boolean containsIgnoreCase(final String sequence, final String str) {
        return (toLowerCase(sequence).indexOf(toLowerCase(str)) >= 0);
    }

    // ----------------------------------------------------------------------------------------- //

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param in The byte stream to check
     * @param strict <code>true</code> for strict checking; otherwise <code>false</code>
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     * @throws IOException If reading from stream fails
     */
    public static boolean containsHTMLTags(InputStream in, boolean strict) throws IOException {
        return containsHTMLTags(in, strict, JS_EVENT_HANDLER);
    }

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param in The byte stream to check
     * @param strict <code>true</code> for strict checking; otherwise <code>false</code>
     * @param eventHandlerIdentifiers The optional event handler identifiers to consider
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     * @throws IOException If reading from stream fails
     */
    public static boolean containsHTMLTags(InputStream in, boolean strict, Collection<String> eventHandlerIdentifiers) throws IOException {
        return containsHTMLTags(in, strict, false, eventHandlerIdentifiers);
    }

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param in The byte stream to check
     * @param strict <code>true</code> for strict checking; otherwise <code>false</code>
     * @param oneShot <code>true</code> to only examine the first 8K chunk read from stream; otherwise <code>false</code> for full examination
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     * @throws IOException If reading from stream fails
     */
    public static boolean containsHTMLTags(InputStream in, boolean strict, boolean oneShot) throws IOException {
        return containsHTMLTags(in, strict, oneShot, JS_EVENT_HANDLER);
    }

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param in The byte stream to check
     * @param strict <code>true</code> for strict checking; otherwise <code>false</code>
     * @param oneShot <code>true</code> to only examine the first 8K chunk read from stream; otherwise <code>false</code> for full examination
     * @param eventHandlerIdentifiers The optional event handler identifiers to consider
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     * @throws IOException If reading from stream fails
     */
    public static boolean containsHTMLTags(InputStream in, boolean strict, boolean oneShot, Collection<String> eventHandlerIdentifiers) throws IOException {
        if (null == in) {
            return false;
        }
        try {
            final int buflen = 8192;
            byte[] buf = new byte[buflen];

            int read = in.read(buf, 0, buflen);
            if (read <= 0) {
                return false;
            }

            boolean found = containsHTMLTags(Charsets.toAsciiString(buf, 0, read), strict, eventHandlerIdentifiers);
            if (oneShot || found) {
                return found;
            }

            final int overlap = 1024;
            byte[] tail = new byte[overlap];
            int taillen = (overlap <= read) ? overlap : read;
            System.arraycopy(buf, read - taillen, tail, 0, taillen);

            byte[] toExamine = null;
            while (!found && (read = in.read(buf, 0, buflen)) > 0) {
                if (toExamine == null) {
                    toExamine = new byte[buflen + overlap];
                }
                System.arraycopy(tail, 0, toExamine, 0, taillen);
                System.arraycopy(buf, 0, toExamine, taillen, read);
                found = containsHTMLTags(Charsets.toAsciiString(toExamine, 0, read + taillen), strict, eventHandlerIdentifiers);
                if (!found) {
                    taillen = (overlap <= read) ? overlap : read;
                    System.arraycopy(buf, read - taillen, tail, 0, taillen);
                }
            }
            return found;
        } finally {
            Streams.close(in);
        }
    }

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param sequence The byte sequence to check
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     */
    public static boolean containsHTMLTags(final byte[] sequence) {
        if (sequence == null) {
            throw new NullPointerException();
        }
        return containsHTMLTags(Charsets.toAsciiString(sequence));
    }

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param sequence The byte sequence to check
     * @param strict <code>true</code> for strict checking; otherwise <code>false</code>
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     */
    public static boolean containsHTMLTags(final byte[] sequence, final boolean strict) {
        return containsHTMLTags(sequence, strict, JS_EVENT_HANDLER);
    }

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param sequence The byte sequence to check
     * @param strict <code>true</code> for strict checking; otherwise <code>false</code>
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     * @param eventHandlerIdentifiers The optional event handler identifiers to consider
     */
    public static boolean containsHTMLTags(final byte[] sequence, final boolean strict, Collection<String> eventHandlerIdentifiers) {
        if (sequence == null) {
            throw new NullPointerException();
        }
        return containsHTMLTags(Charsets.toAsciiString(sequence), strict, eventHandlerIdentifiers);
    }

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param sequence The byte sequence to check
     * @param tags Additional tags to look for
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     */
    public static boolean containsHTMLTags(final byte[] sequence, final String... tags) {
        if (sequence == null) {
            throw new NullPointerException();
        }
        return containsHTMLTags(Charsets.toAsciiString(sequence), tags);
    }

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param sequence The byte sequence to check
     * @param off The offset within byte array
     * @param len The length of valid bytes starting from offset
     * @param tags Additional tags to look for
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     */
    public static boolean containsHTMLTags(final byte[] sequence, final int off, final int len, final String... tags) {
        if (sequence == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > sequence.length - off) {
            throw new IndexOutOfBoundsException();
        }
        return containsHTMLTags(Charsets.toAsciiString(sequence, off, len), tags);
    }

    /**
     * Checks if given byte sequence contains common HTML tags.
     *
     * @param sequence The byte sequence to check
     * @param off The offset within byte array
     * @param len The length of valid bytes starting from offset
     * @return <code>true</code> if given byte sequence contains common HTML tags; otherwise <code>false</code>
     */
    public static boolean containsHTMLTags(final byte[] sequence, final int off, final int len) {
        if (sequence == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > sequence.length - off) {
            throw new IndexOutOfBoundsException();
        }
        return containsHTMLTags(Charsets.toAsciiString(sequence, off, len));
    }

    /**
     * Checks if given byte sequence contains specified HTML tag.
     *
     * @param sequence The byte sequence to check
     * @param tag The HTML tag; e.g. <code>"body"</code>
     * @return <code>true</code> if given byte sequence contains specified HTML tag; otherwise <code>false</code>
     */
    public static boolean containsHTMLTag(final byte[] sequence, final String tag) {
        if (sequence == null) {
            throw new NullPointerException();
        }
        return containsHTMLTag(Charsets.toAsciiString(sequence), tag);
    }

    /**
     * Checks if given byte sequence contains specified HTML tag.
     *
     * @param sequence The byte sequence to check
     * @param off The offset within byte array
     * @param len The length of valid bytes starting from offset
     * @param tag The HTML tag; e.g. <code>"body"</code>
     * @return <code>true</code> if given byte sequence contains specified HTML tag; otherwise <code>false</code>
     */
    public static boolean containsHTMLTag(final byte[] sequence, final int off, final int len, final String tag) {
        if (sequence == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > sequence.length - off) {
            throw new IndexOutOfBoundsException();
        }
        return containsHTMLTag(Charsets.toAsciiString(sequence, off, len), tag);
    }

}
