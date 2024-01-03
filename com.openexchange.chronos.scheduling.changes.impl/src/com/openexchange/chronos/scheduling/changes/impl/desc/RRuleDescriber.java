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

package com.openexchange.chronos.scheduling.changes.impl.desc;

import static com.openexchange.chronos.common.CalendarUtils.initRecurrenceRule;
import static com.openexchange.chronos.common.CalendarUtils.optExtendedPropertyValue;
import static com.openexchange.chronos.scheduling.changes.impl.ChangesUtils.getDateArgument;
import static org.slf4j.LoggerFactory.getLogger;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Duration;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import com.openexchange.annotation.NonNull;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.scheduling.changes.Description;
import com.openexchange.chronos.scheduling.changes.MessageContext;
import com.openexchange.chronos.scheduling.changes.Sentence.ArgumentType;
import com.openexchange.chronos.scheduling.changes.impl.ChangeDescriber;
import com.openexchange.chronos.scheduling.changes.impl.SentenceImpl;
import com.openexchange.chronos.scheduling.common.Messages;
import com.openexchange.chronos.scheduling.common.description.FormattableArgument;
import com.openexchange.chronos.scheduling.common.description.HumanReadableRecurrences;
import com.openexchange.chronos.service.EventUpdate;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.regional.RegionalSettingsUtil;
import com.openexchange.server.ServiceLookup;

/**
 * {@link RRuleDescriber}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class RRuleDescriber implements ChangeDescriber {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link RRuleDescriber}.
     *
     * @param services A service lookup reference
     */
    public RRuleDescriber(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    @NonNull
    public EventField[] getFields() {
        return new EventField[] { EventField.RECURRENCE_RULE };
    }

    @Override
    public Description describe(EventUpdate eventUpdate) {
        if (false == eventUpdate.getUpdatedFields().contains(EventField.RECURRENCE_RULE)) {
            return null;
        }
        /*
         * check if only the series UNTIL data was updated
         */
        DateTime newLastOccurrence = optNewLastOccurrence(eventUpdate);
        if (null != newLastOccurrence) {
            /*
             * yes; now check if new UNTIL originated in a series split operation, by evaluating if X-OX-SPLIT-FROM was newly set in detached series
             */
            DateTime newUntil = optRRuleUntil(eventUpdate.getUpdate().getRecurrenceRule());
            String originalSplitFrom = optExtendedPropertyValue(eventUpdate.getOriginal().getExtendedProperties(), "X-OX-SPLIT-FROM", String.class);
            String updatedSplitFrom = optExtendedPropertyValue(eventUpdate.getUpdate().getExtendedProperties(), "X-OX-SPLIT-FROM", String.class);
            if (null != updatedSplitFrom && false == updatedSplitFrom.equals(originalSplitFrom) && null != newUntil) {
                /*
                 * yes; so use alternative description for this 'detached' part of split series, based on derived split point
                 */
                DateTime splitPoint = newUntil.addDuration(newUntil.isAllDay() ? new Duration(1, 1, 0) : new Duration(1, 0, 1));
                List<SentenceImpl> sentences = Arrays.asList(new SentenceImpl(Messages.HAS_SPLIT).add(getDateArgument(splitPoint, DateFormat.LONG), ArgumentType.UPDATED),
                    new SentenceImpl(Messages.HAS_CHANGED_ORIGINAL_RRULE_UNTIL).add(getDateArgument(newLastOccurrence, DateFormat.LONG), ArgumentType.UPDATED));
                return new DefaultDescription(sentences , EventField.RECURRENCE_RULE);
            }
            /*
             * use regular description for a new UNTIL with new last occurrence
             */
            return new DefaultDescription(new SentenceImpl(Messages.HAS_CHANGED_RRULE_UNTIL).add(
                getDateArgument(newLastOccurrence, DateFormat.LONG), ArgumentType.UPDATED), EventField.RECURRENCE_RULE);
        }
        /*
         * use generic description for updated RRULE, otherwise
         */
        FormattableArgument argument = new FormattableArgument() {

            @Override
            public Object format(MessageContext context) {
                DateFormat dateFormat = RegionalSettingsUtil.getDateFormat(context.getRegionalSettings(), DateFormat.FULL, context.getLocale());
                HumanReadableRecurrences readableRecurrences = new HumanReadableRecurrences(eventUpdate.getUpdate(), context.getLocale());
                String string = readableRecurrences.getString();
                StringBuilder stringBuilder = new StringBuilder();
                if (Strings.isNotEmpty(string)) {
                    stringBuilder.append(string);
                }
                String end = readableRecurrences.getEnd(dateFormat);
                if (Strings.isNotEmpty(end)) {
                    if (0 < stringBuilder.length()) {
                        stringBuilder.append(", ");
                    }
                    stringBuilder.append(end);
                }
                return stringBuilder.toString();
            }
        };
        return new DefaultDescription(new SentenceImpl(Messages.HAS_CHANGED_RRULE).add(argument, ArgumentType.NONE), EventField.RECURRENCE_RULE);
    }

    /**
     * Checks if only the <code>UNTIL</code> part of a recurring event series was adjusted so that updated series now ends with a
     * different last occurrence as before. If that's the case, the date of the new last occurrence is calculated and returned.
     *
     * @param eventUpdate The event update
     * @return The new last occurrence of the event series if the <code>UNTIL</code> part of the recurrence rule was updated, otherwise <code>null</code>
     */
    private DateTime optNewLastOccurrence(EventUpdate eventUpdate) {
        RecurrenceRule originalRule = null;
        RecurrenceRule updatedRule = null;
        try {
            if (null != eventUpdate.getOriginal().getRecurrenceRule()) {
                originalRule = initRecurrenceRule(eventUpdate.getOriginal().getRecurrenceRule());
            }
            if (null != eventUpdate.getUpdate().getRecurrenceRule()) {
                updatedRule = initRecurrenceRule(eventUpdate.getUpdate().getRecurrenceRule());
            }
        } catch (OXException e) {
            getLogger(RRuleDescriber.class).warn("Unexpected error initializing recurrence rule", e);
            return null;
        }
        if (null == originalRule || null == updatedRule || null == updatedRule.getUntil() || updatedRule.getUntil().equals(originalRule.getUntil())) {
            return null; // no or same UNTIL in rules
        }
        originalRule.setUntil(updatedRule.getUntil());
        if (false == originalRule.toString().equals(updatedRule.toString())) {
            return null; // further changes in rule
        }
        try {
            RecurrenceId lastOccurrence = services.getServiceSafe(RecurrenceService.class).getLastOccurrence(new DefaultRecurrenceData(eventUpdate.getUpdate()));
            return null != lastOccurrence ? lastOccurrence.getValue() : null;
        } catch (OXException e) {
            getLogger(RRuleDescriber.class).warn("Unexpected error determining new last occurrence", e);
            return null;
        }
    }

    private static DateTime optRRuleUntil(String rrule) {
        try {
            return initRecurrenceRule(rrule).getUntil();
        } catch (OXException e) {
            getLogger(RRuleDescriber.class).warn("Unexpected error initializing recurrence rule", e);
            return null;
        }
    }

}
