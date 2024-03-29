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

package com.openexchange.tools.tnef;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.mail.internet.AddressException;
import com.openexchange.mail.mime.QuotedInternetAddress;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.freeutils.tnef.MAPIProp;
import net.freeutils.tnef.MAPIPropName;
import net.freeutils.tnef.MAPIProps;

/**
 * {@link TNEF2ICalUtility}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class TNEF2ICalUtility {

    /**
     * Initializes a new {@link TNEF2ICalUtility}.
     */
    private TNEF2ICalUtility() {
        super();
    }

    /**
     * Gets the valid email address from given string
     *
     * @param s The string which is possibly an email address
     * @return The valid email address or <code>null</code>
     */
    public static String getEmailAddress(final String s) {
        if (isEmpty(s)) {
            return null;
        }
        String address;
        {
            final int pos = s.indexOf(':');
            if (pos >= 0) {
                address = s.substring(pos + 1).trim();
            } else {
                address = s.trim();
            }
        }
        try {
            final String verifiedAddr = new QuotedInternetAddress(address, false).getAddress();
            return new StringBuilder(48).append("mailto:").append(verifiedAddr).toString();
        } catch (AddressException e) {
            // No valid email address
            return null;
        }
    }

    /**
     * Generates an appropriate {@link DateTime} instance from given date.
     *
     * @param date The date
     * @return The {@link DateTime} instance
     */
    public static DateTime toDateTime(final java.util.Date date) {
        // TODO add default time zone
        final DateTime retval = new DateTime(true);
        retval.setTime(date.getTime());
        return retval;
    }

    private static final TimeZoneRegistry TIME_ZONE_REGISTRY = TimeZoneRegistryFactory.getInstance().createRegistry();

    /**
     * Generates an appropriate {@link DateTime} instance from given date.
     *
     * @param date The date
     * @param tzid The optional time zone identifier; may be <code>null</code>
     * @return The {@link DateTime} instance
     */
    public static DateTime toDateTime(final java.util.Date date, final String tzid) {
        if (tzid == null) {
            return toDateTime(date);
        }

        final net.fortuna.ical4j.model.TimeZone ical4jTimezone = TIME_ZONE_REGISTRY.getTimeZone(tzid);
        if (ical4jTimezone == null) {
            return toDateTime(date);
        }

        final DateTime retval = new DateTime(false);
        retval.setTimeZone(ical4jTimezone);
        retval.setTime(date.getTime());
        return retval;
    }

    public static Date pureISOToLocalQDateTime(final String dtStr) {
        return pureISOToLocalQDateTime(dtStr, false);
    }

    public static Date pureISOToLocalQDateTime(final String dtStr, final boolean bDateOnly) {
        final Calendar cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        if (bDateOnly) {
            cal.set(Calendar.YEAR, Integer.parseInt(left(dtStr, 4)));
            cal.set(Calendar.MONTH, Integer.parseInt(mid(dtStr, 4, 2)) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mid(dtStr, 6, 2)));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        } else {
            cal.set(Calendar.YEAR, Integer.parseInt(left(dtStr, 4)));
            cal.set(Calendar.MONTH, Integer.parseInt(mid(dtStr, 4, 2)) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mid(dtStr, 6, 2)));
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(mid(dtStr, 9, 2)));
            cal.set(Calendar.MINUTE, Integer.parseInt(mid(dtStr, 11, 2)));
            cal.set(Calendar.SECOND, Integer.parseInt(mid(dtStr, 13, 2)));
        }

        /*
         * Correct for GMT ( == Zulu time == UTC )
         */
        if (!bDateOnly && dtStr.charAt(dtStr.length() - 1) == 'Z') {
            // TODO:
        }

        return cal.getTime();
    }

    /**
     * Extracts the substring from specified position with given length.
     *
     * @param s The string to extract from
     * @param pos The position
     * @param len The length
     * @return The extracted string
     */
    public static String mid(final String s, final int pos, final int len) {
        if (null == s) {
            return null;
        }
        if (len < 0) {
            return s.substring(pos);
        }
        final int length = Math.min(s.length(), pos + len);
        final StringBuilder sb = new StringBuilder(len);
        for (int i = pos; i < length; i++) {
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Gets the <i>n</i> leftmost characters from specified string.
     *
     * @param s The string
     * @param n The number of leftmost characters
     * @return The string with <i>n</i> leftmost characters
     */
    private static String left(final String s, final int n) {
        if (null == s) {
            return null;
        }
        final int length = s.length();
        final StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n && i < length; i++) {
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Removes specified characters from given string.
     *
     * @param s The string
     * @param chars The characters to remove
     * @return The string with characters removed
     */
    public static String remove(final String s, final char... chars) {
        if (null == s) {
            return null;
        }
        final int length = s.length();
        final StringBuilder sb = new StringBuilder(length);
        Arrays.sort(chars);
        for (int i = 0; i < length; i++) {
            final char cur = s.charAt(i);
            if (Arrays.binarySearch(chars, cur) < 0) {
                sb.append(cur);
            }
        }
        return sb.toString();
    }

    /**
     * Checks if given string is empty.
     *
     * @param s The string to check
     * @return <code>true</code> if empty; otherwise <code>false</code>
     */
    public static boolean isEmpty(final String string) {
        return com.openexchange.java.Strings.isEmpty(string);
    }

    /**
     * Finds a property by ID.
     *
     * @param <V> The return type to cast to
     * @param id The property ID
     * @param mapiProps The MAPI properties to search in
     * @return The found property's value or <code>null</code>
     * @throws IOException If an I/O error occurs
     */
    public static <V> V findProp(final int id, final MAPIProps mapiProps) throws IOException {
        return findProp(id, (V) null, mapiProps);
    }

    /**
     * Finds a property by ID.
     *
     * @param <V> The return type to cast to
     * @param id The property ID
     * @param fallback The fallback if missing
     * @param mapiProps The MAPI properties to search in
     * @return The found property's value or fallback if missing
     * @throws IOException If an I/O error occurs
     */
    public static <V> V findProp(final int id, final V fallback, final MAPIProps mapiProps) throws IOException {
        @SuppressWarnings("unchecked") final V retval = (V) mapiProps.getPropValue(id);
        return null == retval ? fallback : retval;
    }

    /**
     * Finds a property by ID.
     *
     * @param id The property ID
     * @param mapiProps The MAPI properties to search in
     * @return The string of found property's value or <code>null</code>
     * @throws IOException If an I/O error occurs
     */
    public static String findPropString(final int id, final MAPIProps mapiProps) throws IOException {
        return findPropString(id, null, mapiProps);
    }

    /**
     * Finds a property by ID.
     *
     * @param id The property ID
     * @param fallback The fallback if missing
     * @param mapiProps The MAPI properties to search in
     * @return The string of found property's value or fallback if missing
     * @throws IOException If an I/O error occurs
     */
    public static String findPropString(final int id, final String fallback, final MAPIProps mapiProps) throws IOException {
        final Object retval = mapiProps.getPropValue(id);
        return null == retval ? fallback : retval.toString();
    }

    /**
     * Find property by name.
     *
     * @param name The name
     * @param mapiProps The MAPI properties to search in
     * @return The string of found property's value or <code>null</code>
     * @throws IOException If an I/O error occurs
     */
    public static <V> V findNamedProp(final String name, final MAPIProps mapiProps) throws IOException {
        return findNamedProp(name, (V) null, mapiProps);
    }

    /**
     * Find property by name.
     *
     * @param name The name
     * @param fallback The fallback value
     * @param mapiProps The MAPI properties to search in
     * @return The string of found property's value or fallback value
     * @throws IOException If an I/O error occurs
     */
    public static <V> V findNamedProp(final String name, final V fallback, final MAPIProps mapiProps) throws IOException {
        if (name.startsWith("0x")) {
            final int id = Integer.parseInt(name.substring(2), 16);
            for (final MAPIProp mapiProp : mapiProps.getProps()) {
                final MAPIPropName propName = mapiProp.getName();
                if (null != propName && propName.getID() == id) {
                    return (V) mapiProp.getValue();
                }
            }
        } else {
            for (final MAPIProp mapiProp : mapiProps.getProps()) {
                final MAPIPropName propName = mapiProp.getName();
                if (null != propName) {
                    final String n = propName.getName();
                    if (n != null && n.equals(name)) {
                        return (V) mapiProp.getValue();
                    }
                }
            }
        }
        return fallback;
    }

    /**
     * Find property by name.
     *
     * @param name The name
     * @param mapiProps The MAPI properties to search in
     * @return The string of found property's value or <code>null</code>
     * @throws IOException If an I/O error occurs
     */
    public static String findNamedPropString(final String name, final MAPIProps mapiProps) throws IOException {
        return findNamedPropString(name, null, mapiProps);
    }

    /**
     * Find property by name.
     *
     * @param name The name
     * @param fallback The fallback value
     * @param mapiProps The MAPI properties to search in
     * @return The string of found property's value or fallback value
     * @throws IOException If an I/O error occurs
     */
    public static String findNamedPropString(final String name, final String fallback, final MAPIProps mapiProps) throws IOException {
        if (name.startsWith("0x")) {
            final int id = Integer.parseInt(name.substring(2), 16);
            for (final MAPIProp mapiProp : mapiProps.getProps()) {
                final MAPIPropName propName = mapiProp.getName();
                if (null != propName && propName.getID() == id) {
                    return mapiProp.getValue().toString();
                }
            }
        } else {
            for (final MAPIProp mapiProp : mapiProps.getProps()) {
                final MAPIPropName propName = mapiProp.getName();
                if (null != propName) {
                    final String n = propName.getName();
                    if (n != null && n.equals(name)) {
                        return mapiProp.getValue().toString();
                    }
                }
            }
        }
        return fallback;
    }

    /**
     * Creates a String containing the hexadecimal representation of the given bytes.
     *
     * @param bytes a byte array whose content is to be displayed
     * @return a String containing the hexadecimal representation of the given bytes
     */
    public static String toHexString(final byte[] bytes) {
        return toHexString(bytes, 0, bytes != null ? bytes.length : 0, -1);
    }

    /**
     * Creates a String containing the hexadecimal representation of the given bytes.
     * <p>
     * If {@code max} is non-negative and {@code bytes.length > max}, then the first {@code max} bytes are returned, followed by a
     * human-readable indication that there are {@code bytes.length} total bytes of data including those that are not returned.
     *
     * @param bytes a byte array whose content is to be displayed
     * @param max the maximum number of bytes to be displayed (-1 means no limit)
     * @return a String containing the hexadecimal representation of the given bytes
     */
    public static String toHexString(final byte[] bytes, final int max) {
        return toHexString(bytes, 0, bytes != null ? bytes.length : 0, max);
    }

    /**
     * Creates a String containing the hexadecimal representation of the given bytes.
     * <p>
     * If {@code max} is non-negative and {@code len > max}, then the first {@code max} bytes are returned, followed by a human-readable
     * indication that there are {@code len} total bytes of data including those that are not returned.
     * <p>
     * In particular, {@code offset + len} can extend beyond the array boundaries, as long as {@code offset + max} is still within them,
     * resulting in {@code max} bytes returned followed by an indication that there are {@code len} total data bytes (including those that
     * are not returned).
     *
     * @param bytes a byte array whose content is to be displayed
     * @param offset the offset within the byte array to start at
     * @param len the number of bytes
     * @param max the maximum number of bytes to be displayed (-1 means no limit)
     * @return a String containing the hexadecimal representation of the given bytes
     */
    public static String toHexString(final byte[] bytes, final int offset, final int len, final int max) {
        if (bytes == null) {
            return "[null]";
        }
        final int count = max > -1 && max < len ? max : len;
        final StringBuilder s = new StringBuilder(count * 2);
        for (int i = 0; i < count; i++) {
            final String b = Integer.toHexString(bytes[offset + i] & 0xFF).toUpperCase();
            if (b.length() == 1) {
                s.append('0');
            }
            s.append(b);
        }
        if (count < len) {
            s.append("... (" + len + " bytes)");
        }
        return s.toString();
    }

}
