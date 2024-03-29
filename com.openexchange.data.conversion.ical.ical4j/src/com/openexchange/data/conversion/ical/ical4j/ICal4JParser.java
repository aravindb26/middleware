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

package com.openexchange.data.conversion.ical.ical4j;

import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.time.FastDateFormat;
import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.DefaultParseResult;
import com.openexchange.data.conversion.ical.ICalParser;
import com.openexchange.data.conversion.ical.ParseResult;
import com.openexchange.data.conversion.ical.TruncationInfo;
import com.openexchange.data.conversion.ical.ical4j.internal.AttributeConverter;
import com.openexchange.data.conversion.ical.ical4j.internal.ParserTools;
import com.openexchange.data.conversion.ical.ical4j.internal.TaskConverters;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.UnsynchronizedStringReader;
import com.openexchange.tools.TimeZoneUtils;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.util.CompatibilityHints;

/**
 * {@link ICal4JParser} - The {@link ICalParser} using <a href="http://ical4j.sourceforge.net/">ICal4j</a> library.
 *
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 * @author Tobias Prinz <tobias.prinz@open-xchange.com> (bug workarounds)
 */
public class ICal4JParser implements ICalParser {

    private static final Charset UTF8 = Charsets.UTF_8;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ICal4JParser.class);

	private int limit = -1;

    public ICal4JParser() {
        CompatibilityHints.setHintEnabled(
                CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
        CompatibilityHints.setHintEnabled(
                CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY, true);
        CompatibilityHints.setHintEnabled(
                CompatibilityHints.KEY_NOTES_COMPATIBILITY, true);
        CompatibilityHints.setHintEnabled(
        		CompatibilityHints.KEY_RELAXED_PARSING, true);
        CompatibilityHints.setHintEnabled(
              	CompatibilityHints.KEY_RELAXED_VALIDATION, true);

    }

    @Override
    public ParseResult<Task> parseTasks(final String icalText, final TimeZone defaultTZ, final Context ctx, final List<ConversionError> errors, final List<ConversionWarning> warnings) throws ConversionError {
        try {
            return parseTasks(Streams.newByteArrayInputStream(icalText.getBytes(UTF8)), defaultTZ, ctx, errors, warnings);
        } catch (UnsupportedCharsetException e) {
            LOG.error("", e);
        }
        return DefaultParseResult.emptyParseResult();
    }

    @Override
    public ParseResult<Task> parseTasks(final InputStream ical, final TimeZone defaultTZ, final Context ctx, final List<ConversionError> errors, final List<ConversionWarning> warnings) throws ConversionError {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(ical, UTF8));
            TruncationInfo truncationInfo = null;
            List<Task> tasks = null;
            for (net.fortuna.ical4j.model.Calendar calendar; (calendar = parse(reader)) != null;) {
                ComponentList todos = calendar.getComponents("VTODO");
                int size = todos.size();
                int myLimit;
                if (limit >= 0 && limit < size) {
                    myLimit = limit;
                    if (null == tasks) {
                        tasks = new ArrayList<>(myLimit);
                    }
                    truncationInfo = new TruncationInfo(myLimit, size);
                } else {
                    myLimit = size;
                    if (null == tasks) {
                        tasks = new ArrayList<>(myLimit);
                    }
                }

                for(int i = 0; i < myLimit; i++) {
                    final Component vtodo = (Component) todos.get(i);
                    try {
                        tasks.add(convertTask(i, (VToDo) vtodo, defaultTZ, ctx, warnings ));
                    } catch (ConversionError conversionError) {
                        errors.add(conversionError);
                    }
                }
            }

            return DefaultParseResult.<Task> builder().importedObjects(tasks).truncationInfo(truncationInfo).build();
        } catch (UnsupportedCharsetException e) {
            // IGNORE
        } finally {
            closeSafe(reader);
        }

        return DefaultParseResult.emptyParseResult();
    }

    protected static void closeSafe(final Closeable closeable) {
        Streams.close(closeable);
    }

    protected Task convertTask(final int index, final VToDo vtodo, final TimeZone defaultTZ, final Context ctx, final List<ConversionWarning> warnings) throws ConversionError{
        final TimeZone tz = determineTimeZone(vtodo, defaultTZ);
        final Task task = new Task();
        for (final AttributeConverter<VToDo, Task> converter : TaskConverters.ALL) {
            if (converter.hasProperty(vtodo)) {
                converter.parse(index, vtodo, task, tz, ctx, warnings);
            }
            converter.verify(index, task, warnings);
        }
        return task;
    }


    private static final TimeZone determineTimeZone(final CalendarComponent component,
        final TimeZone defaultTZ){
        for (final String name : new String[] { Property.DTSTART, Property.DTEND, Property.DUE, Property.COMPLETED }) {
            final DateProperty dateProp = (DateProperty) component.getProperty(name);
            if (dateProp != null) {
                return chooseTimeZone(dateProp, defaultTZ);
            }
        }

        return defaultTZ;
    }

    private static final TimeZone chooseTimeZone(final DateProperty dateProperty, final TimeZone defaultTZ) {
        TimeZone tz = defaultTZ;
        if (dateProperty.getParameter("TZID") == null
        || dateProperty.getParameter("TZID").getValue() == null){
                return defaultTZ;
        }
        if (dateProperty.isUtc()) {
        	tz = TimeZone.getTimeZone("UTC");
        }
        Parameter tzid = dateProperty.getParameter("TZID");
		String tzidName = tzid.getValue();
		TimeZone inTZID = TimeZone.getTimeZone(tzidName);

        /* now, if the Java core devs had been smart, they'd made TimeZone.getTimeZone(name,fallback) public. But they did not, so have to do this: */
		if (inTZID.getID().equals("GMT") && ! tzidName.equals("GMT")){
			inTZID = ParserTools.findTzidBySimilarity(tzidName);
		}

		if (inTZID == null) {
		    inTZID = ParserTools.findTimeZoneReplacement(tzidName);
		}

        if (null != inTZID) {
            tz = inTZID;
        }
        return tz;
    }

	protected net.fortuna.ical4j.model.Calendar parse(final BufferedReader reader) throws ConversionError {
	    return parse(reader, null);
	}

    protected net.fortuna.ical4j.model.Calendar parse(final BufferedReader reader, final Collection<Exception> exceptions) throws ConversionError {
        final CalendarBuilder builder = new CalendarBuilder();
        try {
            final StringBuilder chunk = new StringBuilder();
            boolean read = false;
            boolean timezoneStarted = false; //hack to fix bug 11958
            boolean timezoneEnded = false; //hack to fix bug 11958
            boolean timezoneRead = false; //hack to fix bug 11958
            final StringBuilder timezoneInfo = new StringBuilder(); //hack to fix bug 11958
            // Copy until we find an END:VCALENDAR
            boolean beginFound = false;
            for (String line; (line = reader.readLine()) != null;) {
            	if (!beginFound && line.endsWith("BEGIN:VCALENDAR")){
            		line = removeByteOrderMarks(line);
            	}
                if (line.startsWith("BEGIN:VCALENDAR")) {
                    beginFound = true;
                } else if ( !beginFound && !"".equals(line)) {
                    continue; // ignore bad lines between "VCALENDAR" Tags.
                }
                if (!line.startsWith("END:VCALENDAR")){ //hack to fix bug 11958
                	if (line.matches("^\\s*BEGIN:VTIMEZONE")){
                		timezoneStarted = true;
                	}
                    if (!line.matches("\\s*")) {
                        read = true;
                        if (timezoneStarted && !timezoneEnded){ //hack to fix bug 11958
                        	timezoneInfo.append(line).append('\n');
                        } else {
                        	chunk.append(line).append('\n');
                        }
                    }
                	if (line.matches("^\\s*END:VTIMEZONE")){ //hack to fix bug 11958
                		timezoneEnded = true;
                		timezoneRead = true && timezoneStarted;
                	}
                } else {
                    break;
                }
            }
            if (!read) {  return null; }
            chunk.append("END:VCALENDAR\n");
            if (timezoneRead){
            	int locationForInsertion = chunk.indexOf("BEGIN:");
            	if (locationForInsertion > -1){
            		locationForInsertion = chunk.indexOf("BEGIN:", locationForInsertion + 1);
            		if (locationForInsertion > -1){
            			chunk.insert(locationForInsertion, timezoneInfo);
            		}
            	}
            }
            final UnsynchronizedStringReader chunkedReader = new UnsynchronizedStringReader(
            	workaroundFor19463(
            	workaroundFor16895(
            	workaroundFor16613(
            	workaroundFor16367(
            	workaroundFor17492(
            	workaroundFor17963(
            	workaroundFor20453(
            	workaroundFor27706And28942(
            	workaroundFor29282(
            	workaroundFor30027(
            	removeAnnoyingWhitespaces(chunk.toString()
                )))))))))))
            ); // FIXME: Encoding?
            try {
                return builder.build(chunkedReader);
            } catch (NullPointerException e) {
                LOG.warn(composeErrorMessage(e, chunkedReader.getString()), e);
                throw new ConversionError(-1, ConversionWarning.Code.PARSE_EXCEPTION, e, e.getMessage());
            }
        } catch (IOException e) {
            if (null == exceptions) {
                LOG.error("", e);
            } else {
                exceptions.add(e);
            }
        } catch (ParserException e) {
            LOG.warn("", e);
            throw new ConversionError(-1, ConversionWarning.Code.PARSE_EXCEPTION, e, e.getMessage());
        }
        return null;
    }

    private static String workaroundFor17963(final String input) {
    	return input.replaceAll("EXDATE:(\\d+)([\\n\\r])", "EXDATE:$1T000000$2");
	}

	private static String workaroundFor17492(final String input) {
    	return input.replaceAll(";SCHEDULE-AGENT=", ";X-CALDAV-SCHEDULE-AGENT=");
	}

	private static String workaroundFor19463(final String input) {
		return input
			.replaceAll("TZOFFSETFROM:\\s*(\\d\\d\\d\\d)", "TZOFFSETFROM:+$1")
			.replaceAll("TZOFFSETTO:\\s*(\\d\\d\\d\\d)",   "TZOFFSETTO:+$1")
			;
	}

	private static String workaroundFor20453(final String input) {
		return input
			.replaceAll("DTEND;\\s*\n", "")
			;
	}

	private static String workaroundFor29282(final String input) {
	    return input.replaceAll("0000([0-9]{4}T[0-9]{6}Z)", "1970$1");
	}

	private static String workaroundFor27706And28942(final String input) {
	    Matcher m = Pattern.compile("DTSTAMP;TZID=([^:]+):\\s*([0-9]{8}T[0-9]{6})").matcher(input);
	    final StringBuffer sb = new StringBuffer(input.length());
	    while (m.find()) {
	        final TimeZone tz = getTimeZone(m.group(1));
	        m.appendReplacement(sb, Strings.quoteReplacement("DTSTAMP:" + getUtcPropertyFrom(m.group(2), tz)));
        }
	    m.appendTail(sb);

        // -------------------------------------------------------------------------------------------------- //

	    m = Pattern.compile("COMPLETED;TZID=([^:]+):\\s*([0-9]{8}T[0-9]{6})").matcher(sb.toString());
        sb.setLength(0);
        while (m.find()) {
            final TimeZone tz = getTimeZone(m.group(1));
            m.appendReplacement(sb, Strings.quoteReplacement("COMPLETED:" + getUtcPropertyFrom(m.group(2), tz)));
        }
        m.appendTail(sb);

        // -------------------------------------------------------------------------------------------------- //

        m = Pattern.compile("LAST-MODIFIED;TZID=([^:]+):\\s*([0-9]{8}T[0-9]{6})").matcher(sb.toString());
        sb.setLength(0);
        while (m.find()) {
            final TimeZone tz = getTimeZone(m.group(1));
            m.appendReplacement(sb, Strings.quoteReplacement("LAST-MODIFIED:" + getUtcPropertyFrom(m.group(2), tz)));
        }
        m.appendTail(sb);

        // -------------------------------------------------------------------------------------------------- //

        m = Pattern.compile("CREATED;TZID=([^:]+):\\s*([0-9]{8}T[0-9]{6})").matcher(sb.toString());
        sb.setLength(0);
        while (m.find()) {
            final TimeZone tz = getTimeZone(m.group(1));
            m.appendReplacement(sb, Strings.quoteReplacement("CREATED:" + getUtcPropertyFrom(m.group(2), tz)));
        }
        m.appendTail(sb);

        // -------------------------------------------------------------------------------------------------- //

        m = Pattern.compile("TRIGGER;TZID=([^:]+):\\s*([0-9]{8}T[0-9]{6})").matcher(sb.toString());
        sb.setLength(0);
        while (m.find()) {
            final TimeZone tz = getTimeZone(m.group(1));
            m.appendReplacement(sb, Strings.quoteReplacement("TRIGGER:" + getUtcPropertyFrom(m.group(2), tz)));
        }
        m.appendTail(sb);

        return sb.toString();
    }

	private static final FastDateFormat UTC_PROPERTY = FastDateFormat.getInstance("yyyyMMdd'T'HHmmss'Z'", TimeZoneUtils.getTimeZone("UTC"), Locale.US);

	private static String getUtcPropertyFrom(final String s, final TimeZone tz) {
	    try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
            sdf.setTimeZone(tz);
            final Date d = sdf.parse(s);
            return UTC_PROPERTY.format(d);
        } catch (ParseException e) {
            return s;
        }
	}

	private static String workaroundFor30027(final String input) {
        final Matcher m = Pattern.compile(":\\s{2,}([0-9]{8}T[0-9]{6}Z?)[ \\t]*").matcher(input);
        final StringBuffer sb = new StringBuffer(input.length());
        while (m.find()) {
            m.appendReplacement(sb, ": $1");
        }
        m.appendTail(sb);

        return sb.toString();
    }

	/**
     * Method written out of laziness: Because you can spread iCal attributes
     * over several lines with newlines followed by a white space while a normal
     * newline means a new attribute starts, one would need to parse the whole file
     * (with a lookahead) before fixing errors. That means no regular expressions
     * allowed. Since spreading just makes it nicer to read for humans, this method
     * strips those newline+whitespace elements so we can use simple regexps.
     */
    private static String removeAnnoyingWhitespaces(final String input) {
        /*
         * [http://www.ietf.org/rfc/rfc2445.txt]
         *
         * Long content lines SHOULD be split into a multiple line
         * representations using a line "folding" technique. That is, a long
         * line can be split between any two characters by inserting a CRLF
         * immediately followed by a single linear white space character (i.e.,
         * SPACE, US-ASCII decimal 32 or HTAB, US-ASCII decimal 9). Any sequence
         * of CRLF followed immediately by a single linear white space character
         * is ignored (i.e., removed) when processing the content type.
         */
        return input.replaceAll("(?:\\r\\n?|\\n)[ \t]", "");
	}

	private static String workaroundFor16895(final String input) {
		/* Bug in Zimbra: They like to use an EMAIL element for the
		 * ATTENDEE property, though there is none.
		 */
		return input.replaceAll("ATTENDEE([^\n]*?);EMAIL=", "ATTENDEE$1;X-ZIMBRA-EMAIL=");
	}

	private static String workaroundFor16367(final String input) {
        /* Bug in MS Exchange: If you use a CN element, it must have a value.
         * MS Exchange has an empty value, which we now replace properly.
         */
        return input.replaceAll("CN=:", "CN=\"\":");
    }

    private static String workaroundFor16613(final String input) {
        /*
         * Bug in Groupwise: There is no attribute ID for ATTACH. Experimental
         * ones are allowed, but they would start with X-GW for Groupwise.
         * We ignore those.
         */
        return input.replaceAll("\nATTACH(.*?);ID=(.+?)([:;])" , "\nATTACH$1$3");
    }

    private static String removeByteOrderMarks(String line){
    	char[] buf = line.toCharArray();
    	int length = buf.length;

		final char first = buf[0];
        if (length > 3) {
            if (Character.getNumericValue(first) < 0 && Character.getNumericValue(buf[1]) < 0 && Character.getNumericValue(buf[2]) < 0 && Character.getNumericValue(buf[3]) < 0){
				if (Character.getType(first) == 15 && Character.getType(buf[1]) == 15 && Character.getType(buf[2]) == 28 && Character.getType(buf[3]) == 28) {
                    return new String(Arrays.copyOfRange(buf, 3, length));
                }
				if (Character.getType(first) == 28 && Character.getType(buf[1]) == 28 && Character.getType(buf[2]) == 15 && Character.getType(buf[3]) == 15) {
                    return new String(Arrays.copyOfRange(buf, 3, length));
                }
			}
        }
		if (length > 1) {
		    if (Character.getNumericValue(first) < 0 && Character.getNumericValue(buf[1]) < 0) {
                if (Character.getType(first) == 28 && Character.getType(buf[1]) == 28) {
                    return new String(Arrays.copyOfRange(buf, 2, length));
                }
            }
        }
		if (length > 0) {
            if (Character.getNumericValue(first) < 0) {
                if (Character.getType(first) == 16) {
                    return new String(Arrays.copyOfRange(buf, 1, length));
                }
            }
        }
		return line;
    }

	@Override
	public void setLimit(int amount) {
		this.limit = amount;
	}

	private static String composeErrorMessage(Exception e, String ical) {
        final StringBuilder sb = new StringBuilder(ical.length() + 256);
        sb.append("Parsing of iCal content failed: ");
        sb.append(e.getMessage());
        if (LOG.isDebugEnabled()) {
            sb.append(". Affected iCal content:").append('\n');
            dumpIcal(ical, sb);
        }
        return sb.toString();
    }

	private static void dumpIcal(String ical, StringBuilder sb) {
        String[] lines = Strings.splitByCRLF(ical);
        DecimalFormat df = new DecimalFormat("0000");
        int count = 1;
        for (final String line : lines) {
            sb.append(df.format(count++)).append(' ').append(line).append('\n');
        }
        OutputStreamWriter writer = null;
        try {
            File file = File.createTempFile("parsefailed", ".ics", new File(System.getProperty("java.io.tmpdir")));
            writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8);
            writer.write(ical);
            writer.flush();
        } catch (IOException e) {
            LOG.error("Problem writing not parsable iCal to tmp directory.", e);
        } finally {
            Streams.close(writer);
        }
    }

}
