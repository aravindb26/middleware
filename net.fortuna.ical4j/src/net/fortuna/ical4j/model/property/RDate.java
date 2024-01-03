/**
 * Copyright (c) 2012, Ben Fortuna
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  o Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *  o Neither the name of Ben Fortuna nor the names of any other contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.fortuna.ical4j.model.property;

import java.text.ParseException;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.util.ParameterValidator;
import net.fortuna.ical4j.util.Strings;

/**
 * $Id$
 * 
 * Created: [Apr 6, 2004]
 *
 * Defines an RDATE iCalendar component property.
 * 
 * <pre>
 *     4.8.5.3 Recurrence Date/Times
 *     
 *        Property Name: RDATE
 *     
 *        Purpose: This property defines the list of date/times for a
 *        recurrence set.
 *     
 *        Value Type: The default value type for this property is DATE-TIME.
 *        The value type can be set to DATE or PERIOD.
 *     
 *        Property Parameters: Non-standard, value data type and time zone
 *        identifier property parameters can be specified on this property.
 *     
 *        Conformance: The property can be specified in &quot;VEVENT&quot;, &quot;VTODO&quot;,
 *        &quot;VJOURNAL&quot; or &quot;VTIMEZONE&quot; calendar components.
 *     
 *        Description: This property can appear along with the &quot;RRULE&quot; property
 *        to define an aggregate set of repeating occurrences. When they both
 *        appear in an iCalendar object, the recurring events are defined by
 *        the union of occurrences defined by both the &quot;RDATE&quot; and &quot;RRULE&quot;.
 *     
 *        The recurrence dates, if specified, are used in computing the
 *        recurrence set. The recurrence set is the complete set of recurrence
 *        instances for a calendar component. The recurrence set is generated
 *        by considering the initial &quot;DTSTART&quot; property along with the &quot;RRULE&quot;,
 *        &quot;RDATE&quot;, &quot;EXDATE&quot; and &quot;EXRULE&quot; properties contained within the
 *        iCalendar object. The &quot;DTSTART&quot; property defines the first instance
 *        in the recurrence set. Multiple instances of the &quot;RRULE&quot; and &quot;EXRULE&quot;
 *        properties can also be specified to define more sophisticated
 *        recurrence sets. The final recurrence set is generated by gathering
 *        all of the start date/times generated by any of the specified &quot;RRULE&quot;
 *        and &quot;RDATE&quot; properties, and excluding any start date/times which fall
 *        within the union of start date/times generated by any specified
 *        &quot;EXRULE&quot; and &quot;EXDATE&quot; properties. This implies that start date/times
 *        within exclusion related properties (i.e., &quot;EXDATE&quot; and &quot;EXRULE&quot;)
 *        take precedence over those specified by inclusion properties (i.e.,
 *        &quot;RDATE&quot; and &quot;RRULE&quot;). Where duplicate instances are generated by the
 *        &quot;RRULE&quot; and &quot;RDATE&quot; properties, only one recurrence is considered.
 *        Duplicate instances are ignored.
 *     
 *        Format Definition: The property is defined by the following notation:
 *     
 *          rdate      = &quot;RDATE&quot; rdtparam &quot;:&quot; rdtval *(&quot;,&quot; rdtval) CRLF
 *     
 *          rdtparam   = *(
 *     
 *                     ; the following are optional,
 *                     ; but MUST NOT occur more than once
 *     
 *                     (&quot;;&quot; &quot;VALUE&quot; &quot;=&quot; (&quot;DATE-TIME&quot;
 *                      / &quot;DATE&quot; / &quot;PERIOD&quot;)) /
 *                     (&quot;;&quot; tzidparam) /
 *     
 *                     ; the following is optional,
 *                     ; and MAY occur more than once
 *     
 *                     (&quot;;&quot; xparam)
 *     
 *                     )
 *     
 *          rdtval     = date-time / date / period
 *          ;Value MUST match value type
 *     
 *        Example: The following are examples of this property:
 *     
 *          RDATE:19970714T123000Z
 *     
 *          RDATE;TZID=US-EASTERN:19970714T083000
 *     
 *          RDATE;VALUE=PERIOD:19960403T020000Z/19960403T040000Z,
 *           19960404T010000Z/PT3H
 *     
 *          RDATE;VALUE=DATE:19970101,19970120,19970217,19970421
 *           19970526,19970704,19970901,19971014,19971128,19971129,19971225
 * </pre>
 * 
 * @author Ben Fortuna
 */
public class RDate extends DateListProperty {

    private static final long serialVersionUID = -3320381650013860193L;

    private PeriodList periods;

    /**
     * Default constructor.
     */
    public RDate() {
        super(RDATE, PropertyFactoryImpl.getInstance());
        periods = new PeriodList(false, true);
    }

    /**
     * @param aList a list of parameters for this component
     * @param aValue a value string for this component
     * @throws ParseException where the specified value string is not a valid date-time/date representation
     */
    public RDate(final ParameterList aList, final String aValue)
            throws ParseException {
        super(RDATE, aList, PropertyFactoryImpl.getInstance());
        periods = new PeriodList(false, true);
        setValue(aValue);
    }

    /**
     * Constructor. Date or Date-Time format is determined based on the presence of a VALUE parameter.
     * @param dates a list of dates
     */
    public RDate(final DateList dates) {
        super(RDATE, dates, PropertyFactoryImpl.getInstance());
        periods = new PeriodList(false, true);
    }

    /**
     * Constructor. Date or Date-Time format is determined based on the presence of a VALUE parameter.
     * @param aList a list of parameters for this component
     * @param dates a list of dates
     */
    public RDate(final ParameterList aList, final DateList dates) {
        super(RDATE, aList, dates, PropertyFactoryImpl.getInstance());
        periods = new PeriodList(false, true);
    }

    /**
     * Constructor.
     * @param periods a list of periods
     */
    public RDate(final PeriodList periods) {
        super(RDATE, new DateList(true), PropertyFactoryImpl.getInstance());
        this.periods = periods;
    }

    /**
     * Constructor.
     * @param aList a list of parameters for this component
     * @param periods a list of periods
     */
    public RDate(final ParameterList aList, final PeriodList periods) {
        super(RDATE, aList, new DateList(true), PropertyFactoryImpl.getInstance());
        this.periods = periods;
    }

    /**
     * {@inheritDoc}
     */
    public final void validate() throws ValidationException {

        /*
         * ; the following are optional, ; but MUST NOT occur more than once (";" "VALUE" "=" ("DATE-TIME" / "DATE" /
         * "PERIOD")) / (";" tzidparam) /
         */
        ParameterValidator.getInstance().assertOneOrLess(Parameter.VALUE,
                getParameters());

        final Parameter valueParam = getParameter(Parameter.VALUE);

        if (valueParam != null && !Value.DATE_TIME.equals(valueParam)
                && !Value.DATE.equals(valueParam)
                && !Value.PERIOD.equals(valueParam)) {
            throw new ValidationException("Parameter [" + Parameter.VALUE
                    + "] is invalid");
        }

        ParameterValidator.getInstance().assertOneOrLess(Parameter.TZID,
                getParameters());

        /*
         * ; the following is optional, ; and MAY occur more than once (";" xparam)
         */
    }

    /**
     * @return Returns the period list.
     */
    public final PeriodList getPeriods() {
        return periods;
    }

    /**
     * {@inheritDoc}
     */
    public final void setValue(final String aValue) throws ParseException {
        if (Value.PERIOD.equals(getParameter(Parameter.VALUE))) {
            periods = new PeriodList(aValue);
        }
        else {
            super.setValue(aValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final String getValue() {
        if (periods != null && !(periods.isEmpty() && periods.isUnmodifiable())) {
            return Strings.valueOf(getPeriods());
        }
        return super.getValue();
    }
    
    /**
     * {@inheritDoc}
     */
    public final void setTimeZone(TimeZone timezone) {
        if (periods != null && !(periods.isEmpty() && periods.isUnmodifiable())) {
            periods.setTimeZone(timezone);
        }
        else {
            super.setTimeZone(timezone);
        }
    }
}
