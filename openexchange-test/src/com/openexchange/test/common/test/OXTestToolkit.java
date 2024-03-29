
package com.openexchange.test.common.test;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Assertions;

public class OXTestToolkit {

    public static void assertEqualsAndNotNull(final String message, final Date expect, final Date value) throws AssertionError {
        if (expect != null) {
            Assertions.assertNotNull(value, message + " is null");
            Assertions.assertEquals(expect, value, message);
        }
    }

    public static void assertEqualsAndNotNull(final String message, final byte[] expect, final byte[] value) throws AssertionError {
        if (expect != null) {
            Assertions.assertNotNull(value, message + " is null");
            Assertions.assertEquals(expect.length, value.length, message + " byte array size is not equals");
            for (int a = 0; a < expect.length; a++) {
                Assertions.assertEquals(expect[a], value[a], message + " byte in pos (" + a + ") is not equals");
            }
        }
    }

    public static void assertImageBytesEqualsAndNotNull(final String message, final byte[] expect, final byte[] value) throws AssertionError {
        if (expect != null) {
            Assertions.assertNotNull(value, message + " is null");
            Assertions.assertTrue(expect.length >= value.length, message + " byte array size is not equals");
            //            for (int a = 0; a < expect.length; a++) {
            //                Assertions.assertEquals(message + " byte in pos (" + a + ") is not equals", expect[a], value[a]);
            //            }
        }
    }

    public static void assertEqualsAndNotNull(final String message, final Object expect, final Object value) throws AssertionError {
        if (expect != null) {
            Assertions.assertNotNull(value, message + " is null");
            Assertions.assertEquals(expect, value, message);
        }
    }

    public static void assertSameContent(final InputStream is1, final InputStream is2) throws IOException {
        assertSameContent("", is1, is2);
    }

    public static void assertSameContent(final String message, final InputStream is1, final InputStream is2) throws IOException {
        int i = 0;
        while ((i = is1.read()) != -1) {
            Assertions.assertEquals(i, is2.read(), message);
        }
        Assertions.assertEquals(-1, is2.read(), message);
    }

    /**
     * Asserts that two dates, when looked at in the same time zone, are on the same day of the year and in the same year.
     */
    public void assertSameDay(final String message, final Date date1, final Date date2) {
        final Calendar c1 = new GregorianCalendar(), c2 = new GregorianCalendar();
        c1.setTime(date1);
        c2.setTime(date2);
        c1.setTimeZone(TimeZone.getTimeZone("UTC"));
        c2.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals(I(c1.get(Calendar.DAY_OF_YEAR)), I(c2.get(Calendar.DAY_OF_YEAR)), message + " (Day of the year)");
        assertEquals(I(c1.get(Calendar.YEAR)), I(c2.get(Calendar.YEAR)), message + " (Year)");
    }

    public static void assertSameStream(final InputStream expected, final InputStream actual) {
        assertSameStream("", expected, actual);
    }

    public static void assertSameStream(String message, final InputStream expected, final InputStream actual) {
        if (message == null || message.equals("")) {
            message = "Comparing InputStreams";
        }
        final byte[] buff = new byte[256];
        final byte[] buff2 = new byte[256];
        final CRC32 crcActual = new CRC32();
        final CRC32 crcExpected = new CRC32();
        try {
            while (expected.read(buff) > 0) {
                try {
                    actual.read(buff2);
                } catch (IOException e) {
                    fail(message + ": 'actual' stream was shorter than 'expected' stream.");
                }
                crcActual.update(buff);
                crcExpected.update(buff2);
            }
            assertEquals(-1, actual.read(buff2), message + ":'actual' stream was longer than 'expected' stream.");
        } catch (IOException e) {
            fail(message + ": Could not read from 'expected' stream.");
        } finally {
            try {
                actual.close();
            } catch (IOException e) {
            }
            try {
                expected.close();
            } catch (IOException e) {
            }
        }
        assertEquals(crcExpected.getValue(), crcActual.getValue(), message + ": Both streams should have the same checksum");
    }

    public static String readStreamAsString(final InputStream is) throws IOException {
        int len;
        byte[] buffer = new byte[0xFFFF];
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((len = is.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        is.close();
        buffer = baos.toByteArray();
        baos.close();
        return new String(buffer, com.openexchange.java.Charsets.UTF_8);
    }
}
