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

package com.openexchange.ajax.chronos.factory;

import static com.openexchange.ajax.chronos.util.DateTimeUtil.formatZuluDate;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.java.Autoboxing.l;
import static com.openexchange.java.Strings.isNotEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.regex.Pattern;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.DateTimeData;
import com.openexchange.testing.httpclient.models.EventData;
import net.fortuna.ical4j.data.FoldingWriter;

/**
 * {@link ICalFactories} - Creates different iCals
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.3
 */
public class ICalFactories {

    private ICalFactories() {
        super();
    }

    /** The timezone ID for {@value #EUROPE_BERLIN} */
    public static final String EUROPE_BERLIN = "Europe/Berlin";

    /*
     * ============================== TEMPLATES ==============================
     */

    /** The template for a single event send from Thunderbird */
    public final static String THUNDERBRID_SINGLE_EVENT = """
        BEGIN:VCALENDAR
        PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN
        VERSION:2.0
        METHOD:{{METHOD}}
        BEGIN:VTIMEZONE
        TZID:Europe/Berlin
        BEGIN:DAYLIGHT
        TZOFFSETFROM:+0100
        TZOFFSETTO:+0200
        TZNAME:CEST
        DTSTART:19700329T020000
        RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3
        END:DAYLIGHT
        BEGIN:STANDARD
        TZOFFSETFROM:+0200
        TZOFFSETTO:+0100
        TZNAME:CET
        DTSTART:19701025T030000
        RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10
        END:STANDARD
        END:VTIMEZONE
        BEGIN:VEVENT
        CREATED:{{CREATED}}
        LAST-MODIFIED:20220615T073216Z
        DTSTAMP:20220615T073216Z
        UID:{{UID}}
        SUMMARY:{{SUMMARY}}
        ORGANIZER;PARTSTAT=NEEDS-ACTION;ROLE=REQ-PARTICIPANT:mailto:{{ORGANIZER_MAIL}}
        {{ATTENDEES}}
        DTSTART;TZID=Europe/Berlin:{{START}}
        DTEND;TZID=Europe/Berlin:{{END}}
        TRANSP:OPAQUE
        SEQUENCE:{{SEQUENCE}}
        END:VEVENT
        END:VCALENDAR
                 """;

    private final static String ATTENDEE_TEMPLATE = "ATTENDEE;CN={{ATTENDEE_DISPLAYNAME}};PARTSTAT={{PART_STAT}};ROLE=REQ-PARTICIPANT:mailto:{{ATTENDEE_MAIL}}\n";
    private final static String ATTENDEE_MAILTO_TEMPLATE = "ATTENDEE;CN={{ATTENDEE_DISPLAYNAME}};PARTSTAT={{PART_STAT}};ROLE=REQ-PARTICIPANT:{{ATTENDEE_MAIL}}\n";

    /*
     * ============================== REGEX ==============================
     */

    private static final String METHOD = "{{METHOD}}";
    private static final String CREATED = "{{CREATED}}";
    private static final String START = "{{START}}";
    private static final String END = "{{END}}";
    private static final String SUMMARY = "{{SUMMARY}}";
    private static final String UID = "{{UID}}";
    private static final String SEQUENCE = "{{SEQUENCE}}";
    private static final String ATTENDEES = "{{ATTENDEES}}";
    private static final String ATTENDEE_DISPLAYNAME = "{{ATTENDEE_DISPLAYNAME}}";
    private static final String ATTENDEE_PART_STAT = "{{PART_STAT}}";
    private static final String ATTENDEE_MAIL = "{{ATTENDEE_MAIL}}";
    private static final String ORGANIZER_MAIL = "{{ORGANIZER_MAIL}}";

    /*
     * ============================== GENERATING ==============================
     */

    private static String generateICal(String iCal, SchedulingMethod method, EventData event) throws ParseException, IOException, OXException {
        Long created = event.getCreated();
        String uid = event.getUid();
        String summary = event.getSummary();
        List<Attendee> attendees = event.getAttendees();
        DateTimeData start = event.getStartDate();
        DateTimeData end = event.getEndDate();
        Integer sequence = event.getSequence();

        Map<String, String> map = new HashMap<>(8, 0.95f);
        map.put(METHOD, method.name().toUpperCase());
        if (null == created || l(created) <= 0) {
            created = L(System.currentTimeMillis());
            event.setCreated(created);
        }
        map.put(CREATED, DateTimeUtil.formatZuluDate(new Date(l(created))));
        if (Strings.isEmpty(uid)) {
            uid = UUID.randomUUID().toString();
            event.setUid(uid);
        }
        map.put(UID, uid);
        if (Strings.isEmpty(summary)) {
            summary = "Some summary" + UUID.randomUUID().toString();
            event.setSummary(summary);
        }
        map.put(SUMMARY, summary);
        map.put(ORGANIZER_MAIL, event.getOrganizer().getEmail());
        map.put(ATTENDEES, generateAttendees(attendees));
        map.put(START, formatZuluDate(start));
        map.put(END, formatZuluDate(end));
        if (null == sequence || i(sequence) < 0) {
            sequence = I(0);
            event.setSequence(sequence);
        }
        map.put(SEQUENCE, sequence.toString());
        return generateICal(iCal, map);
    }

    private static String generateICal(String iCal, Map<String, String> replacements) throws IOException, OXException {
        String rendered = iCal;
        for (Entry<String, String> entry : replacements.entrySet()) {
            rendered = rendered.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
        }
        ByteArrayOutputStream holder = null;
        FoldingWriter writer = null;
        try {
            holder = new ByteArrayOutputStream();
            writer = new FoldingWriter(new OutputStreamWriter(holder, Charsets.UTF_8));
            writer.write(rendered);
            writer.close();
            return new String(holder.toByteArray(), Charsets.UTF_8);
        } finally {
            Streams.close(writer);
            Streams.close(holder);
        }
    }
//
    private static String generateAttendees(List<Attendee> attendees) {
        String result = "";
        for (Attendee attendee : attendees) {
            String res = "";
            if (isNotEmpty(attendee.getUri())) {
                res = ATTENDEE_MAILTO_TEMPLATE.replaceAll(Pattern.quote(ATTENDEE_MAIL), attendee.getUri());
            } else {
                res = ATTENDEE_TEMPLATE.replaceAll(Pattern.quote(ATTENDEE_MAIL), attendee.getEmail());
            }
            if (isNotEmpty(attendee.getCn())) {
                res = res.replaceAll(Pattern.quote(ATTENDEE_DISPLAYNAME), attendee.getCn());
            } else {
                res = res.replaceAll(Pattern.quote(ATTENDEE_DISPLAYNAME), "Displayname" + UUID.randomUUID().toString());
            }
            if (isNotEmpty(attendee.getPartStat())) {
                res = res.replaceAll(Pattern.quote(ATTENDEE_PART_STAT), attendee.getPartStat());
            } else {
                res = res.replaceAll(Pattern.quote(ATTENDEE_PART_STAT), "NEEDS-ACTION");
            }
            result += res;
        }
        return result.substring(0, result.length() - 1);//Remove last newline
    }

    /*
     * ============================== GETTERS ==============================
     */

    /**
     * Factory for Thunderbrid like iCALs
     *
     * @return The factory
     */
    public static ICalFactory forThunderbird() {
        return new PostProcessingFactory(new ThunderbirdFactory());
    }

    private static final class PostProcessingFactory implements ICalFactory {

        private final ICalFactory delegatee;

        /**
         * Initializes a new {@link ICalFactories.PostProcessingFactory}.
         * 
         * @param factory The factory to delegate to
         *
         */
        public PostProcessingFactory(ICalFactory factory) {
            super();
            this.delegatee = factory;
        }

        @Override
        public String generate(SchedulingMethod method, String id, List<EventData> data) throws AssertionError, Exception {
            return check(delegatee.generate(method, id, data));
        }

        @Override
        public String generate(SchedulingMethod method, String id, EventData data) throws AssertionError, Exception {
            return check(delegatee.generate(method, id, data));
        }

        @Override
        public String generateForSingleEvent(SchedulingMethod method, EventData data) throws AssertionError, Exception {
            return check(delegatee.generateForSingleEvent(method, data));
        }

        @Override
        public String generateForSeriesEvent(SchedulingMethod method, EventData master, List<EventData> exceptions) throws AssertionError, Exception {
            return check(delegatee.generateForSeriesEvent(method, master, exceptions));
        }

        private static String check(String iCal) throws AssertionError {
            assertThat("Not all placeholder have been replaced", iCal, not(containsString("{{")));
            assertThat("Not all placeholder have been replaced", iCal, not(containsString("}}")));
            return iCal;
        }
    }

    /*
     * ============================== Thunderbird ==============================
     */
    private static final class ThunderbirdFactory implements ICalFactory {

        @Override
        public String generate(SchedulingMethod method, String id, List<EventData> data) throws AssertionError, Exception {
            assertThat(method, is(notNullValue()));
            return switch (id) {
                case SINGLE -> single(method, data);
                default -> throw new AssertionError("Not implemented");
            };

        }

        private static String single(SchedulingMethod method, List<EventData> events) throws Exception {
            assertThat(I(events.size()), is(I(1)));
            assertThat("Template requieres static time zone", events.get(0).getStartDate().getTzid(), is(EUROPE_BERLIN));
            assertThat("Template requieres static time zone", events.get(0).getEndDate().getTzid(), is(EUROPE_BERLIN));
            return generateICal(THUNDERBRID_SINGLE_EVENT, method, events.get(0));
        }
    }

}
