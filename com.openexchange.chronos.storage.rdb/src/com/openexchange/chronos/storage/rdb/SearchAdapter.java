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

package com.openexchange.chronos.storage.rdb;

import static com.openexchange.java.Autoboxing.I;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RelatedTo;
import com.openexchange.chronos.ResourceId;
import com.openexchange.chronos.TimeTransparency;
import com.openexchange.chronos.Transp;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.database.DbMapping;
import com.openexchange.java.Enums;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.Operand;
import com.openexchange.search.Operand.Type;
import com.openexchange.search.Operation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SearchTerm.OperationPosition;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ColumnFieldOperand;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.openexchange.tools.StringCollection;

/**
 * {@link SearchAdapter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class SearchAdapter {

    private final int contextID;
    private final StringBuilder stringBuilder;
    private final List<Object> parameters;
    private final String charset;
    private final String prefixEvents;
    private final String prefixAttendees;

    private boolean usesAttendees;
    private boolean usesEvents;

    /**
     * Initializes a new {@link SearchAdapter}.
     *
     * @param contextID The context identifier
     * @param charset The optional charset to use for string comparisons, or <code>null</code> if not specified
     * @param prefixEvents The prefix to use when inserting column operands for event fields
     * @param prefixAttendees The prefix to use when inserting column operands for internal attendee fields
     */
    public SearchAdapter(int contextID, String charset, String prefixEvents, String prefixAttendees) {
        super();
        this.contextID = contextID;
        this.charset = charset;
        this.prefixEvents = prefixEvents;
        this.prefixAttendees = prefixAttendees;
        this.parameters = new ArrayList<Object>();
        this.stringBuilder = new StringBuilder(256);
    }

    /**
     * Appends the supplied search term to the resulting SQL statement.
     *
     * @param searchTerm The search term to append
     * @return A self reference
     */
    public SearchAdapter append(SearchTerm<?> searchTerm) throws OXException {
        if (null != searchTerm) {
            SearchTerm<?> term = adjustIfNeeded(searchTerm);
            if ((term instanceof SingleSearchTerm)) {
                append((SingleSearchTerm) term);
            } else if ((term instanceof CompositeSearchTerm)) {
                append((CompositeSearchTerm) term);
            } else {
                throw new IllegalArgumentException("Need either an 'SingleSearchTerm' or 'CompositeSearchTerm'.");
            }
        }
        return this;
    }

    /**
     * Gets a value indicating whether properties of attendees are present in the search term or not.
     *
     * @return <code>true</code> if attendees are used, <code>false</code>, otherwise
     */
    public boolean usesAttendees() {
        return usesAttendees;
    }

    /**
     * Gets a value indicating whether properties of events are present in the search term or not.
     *
     * @return <code>true</code> if events are used, <code>false</code>, otherwise
     */
    public boolean usesEvents() {
        return usesEvents;
    }

    /**
     * Gets the constructed <code>WHERE</code>-clause for the search term to be used in the database statement, without the leading
     * <code>WHERE</code>.
     *
     * @return The search clause
     */
    public String getClause() {
        String clause = stringBuilder.toString().trim();
        return 0 < clause.length() ? clause : "TRUE";
    }

    /**
     * Inserts the constant parameters discovered in the search term into the supplied prepared statement.
     *
     * @param stmt The prepared statement to populate
     * @param parameterIndex The parameter index to begin with
     * @return The incremented parameter index
     */
    public int setParameters(PreparedStatement stmt, int parameterIndex) throws SQLException {
        for (Object parameter : parameters) {
            stmt.setObject(parameterIndex++, parameter);
        }
        return parameterIndex;
    }

    private void append(SingleSearchTerm term) {
        /*
         * check and apply special or generic handlings as needed
         */
        Operand<?> columnOperand = getFirstOperandOfType(term, Operand.Type.COLUMN);
        if (null == columnOperand) {
            throw new IllegalArgumentException("No column operand found in term " + term);
        }
        if (EventField.ATTENDEES.equals(columnOperand.getValue())) {
            /*
             * special handling for EventField#ATTENDEES
             */
            appendAttendeesTerm(term);
        } else if (EventField.ATTACHMENTS.equals(columnOperand.getValue())) {
            /*
             * special handling for EventField#ATTACHMENTS
             */
            appendAttachmentsTerm(term);
        } else {
            /*
             * common handling for other fields
             */
            Map<String, DbMapping<? extends Object, ?>> mappings = getMappings(columnOperand.getValue());
            if (null == mappings) {
                throw new IllegalArgumentException("Term contains unmappable field.");
            }
            /*
             * append operation
             */
            Operation operation = term.getOperation();
            Operand<?>[] operands = term.getOperands();
            Iterator<Entry<String, DbMapping<? extends Object, ?>>> iterator = mappings.entrySet().iterator();
            Entry<String, DbMapping<? extends Object, ?>> firstMapping = iterator.next();
            if (false == iterator.hasNext()) {
                appendOperation(operation, operands, firstMapping.getValue());
            } else {
                stringBuilder.append('(');
                appendOperation(operation, operands, firstMapping.getValue());
                do {
                    stringBuilder.append(" OR ");
                    Entry<String, DbMapping<? extends Object, ?>> mapping = iterator.next();
                    appendOperation(operation, operands, mapping.getValue());
                } while (iterator.hasNext());
                stringBuilder.append(')');
            }
        }
    }

    /**
     * Appends a search term matching against the {@link EventField#ATTENDEES} field in form of sub-selects against the
     * <code>calendar_attendee</code> table.
     *
     * @param term The term to append with the {@link EventField#ATTENDEES} column operand
     */
    private void appendAttendeesTerm(SingleSearchTerm term) {
        if (false == SingleOperation.EQUALS.equals(term.getOperation()) ) {
            throw new IllegalArgumentException("Unsupported operation for " + EventField.ATTENDEES + ": " + term.getOperation());
        }
        Operand<?> constantOperand = getFirstOperandOfType(term, Operand.Type.CONSTANT);
        if (null == constantOperand || null == constantOperand.getValue()) {
            throw new IllegalArgumentException("Unsupported operand for " + EventField.ATTENDEES + ": " + constantOperand);
        }
        Object value = constantOperand.getValue();
        if ((value instanceof Number) || (value instanceof String) && -1 != Strings.parsePositiveInt((String) value)) {
            /*
             * match against attendee entity
             */
            stringBuilder.append("EXISTS (SELECT 1 FROM calendar_attendee WHERE calendar_attendee.cid=").append(prefixEvents)
                .append("cid AND calendar_attendee.account=").append(prefixEvents).append("account AND calendar_attendee.event=")
                .append(prefixEvents).append("id AND calendar_attendee.entity ").append(term.getOperation().getSqlRepresentation());
            appendConstantOperand(constantOperand.getValue(), Types.INTEGER);
            stringBuilder.append(')');
            return;
        } else if ((constantOperand.getValue() instanceof String)) {
            /*
             * match against attendee uri, trimming the mailto: prefix as needed, and alternatively against the attendee's cn
             */
            stringBuilder.append("EXISTS (SELECT 1 FROM calendar_attendee WHERE calendar_attendee.cid=").append(prefixEvents)
                .append("cid AND calendar_attendee.account=").append(prefixEvents)
                .append("account AND calendar_attendee.event=").append(prefixEvents).append("id AND (")
                .append("TRIM(LEADING 'mailto:' FROM LOWER(calendar_attendee.uri)) ").append(term.getOperation().getSqlRepresentation());
            appendConstantOperand(constantOperand.getValue(), Types.VARCHAR);
            stringBuilder.append(" OR calendar_attendee.cn ").append(term.getOperation().getSqlRepresentation());
            appendConstantOperand(constantOperand.getValue(), Types.VARCHAR);
            stringBuilder.append("))");
            return;
        }
        throw new IllegalArgumentException("Unsupported operand for " + EventField.ATTENDEES + ": " + constantOperand);
    }

    /**
     * Appends a search term matching against the {@link EventField#ATTACHMENTS} field in form of sub-selects against the
     * <code>prg_attachment</code> table.
     *
     * @param term The term to append with the {@link EventField#ATTACHMENTS} column operand
     */
    private void appendAttachmentsTerm(SingleSearchTerm term) {
        if (false == SingleOperation.EQUALS.equals(term.getOperation()) ) {
            throw new IllegalArgumentException("Unsupported operation for " + EventField.ATTACHMENTS + ": " + term.getOperation());
        }
        Operand<?> constantOperand = getFirstOperandOfType(term, Operand.Type.CONSTANT);
        if (null == constantOperand || null == constantOperand.getValue()) {
            throw new IllegalArgumentException("Unsupported operand for " + EventField.ATTACHMENTS + ": " + constantOperand);
        }
        Object value = constantOperand.getValue();
        stringBuilder.append("EXISTS (SELECT 1 FROM prg_attachment WHERE prg_attachment.cid=").append(prefixEvents)
            .append("cid AND prg_attachment.attached=").append(prefixEvents)
            .append("id AND prg_attachment.filename ").append(term.getOperation().getSqlRepresentation());
        appendConstantOperand(value, Types.VARCHAR);
        stringBuilder.append(')');
    }

    /**
     * Adjusts an incoming search term based on storage internals, which includes:
     * <ul>
     * <li>{@link EventField#ATTENDEES} column operands are routed to {@link AttendeeField#URI} or {@link AttendeeField#ENTITY}, and their
     * respective constant operands are adjusted accordingly</li>
     * <li>Matches against {@link EventField#ORGANIZER} are translated to their encoded representation, using either the internal resource
     * identifier, or a <code>mailto:</code> calendar user address</li>
     * <li>Matches against {@link EventField#TRANSP} are translated to their encoded representation</li>
     * </ul>
     *
     * @param term The term to adjust if applicable
     * @return The adjusted search term to use, or the passed search term reference if not applicable
     */
    private SearchTerm<?> adjustIfNeeded(SearchTerm<?> term) {
        if (false == (term instanceof SingleSearchTerm)) {
            return term;
        }
        SingleSearchTerm singleSearchTerm = (SingleSearchTerm) term;
        Operand<?> columnOperand = getFirstOperandOfType(singleSearchTerm, Operand.Type.COLUMN);
        if (null == columnOperand) {
            return term;
        }
        if (EventField.ORGANIZER.equals(columnOperand.getValue())) {
            return adjustOrganizerTerm(singleSearchTerm);
        }
        if (EventField.TRANSP.equals(columnOperand.getValue())) {
            return adjustEncodedPropertyTerm(singleSearchTerm, TimeTransparency.class,
                (value) -> Enums.parse(TimeTransparency.class, value),
                (transp) -> I(TimeTransparency.TRANSPARENT.equals(transp) ? 0 : 1)
            );
        }
        return term;
    }

    /**
     * Adjusts a single search term targeting an enumeration property to match against the encoded storage representation instead.
     *
     * @param singleSearchTerm The search term to adjust
     * @param clazz The property's class
     * @param parser The function to parse the client-supplied textual representation of the property value
     * @param encoder The function to get the storage representation for the enumeration value
     * @return The adjusted search term
     */
    private <E> SingleSearchTerm adjustEncodedPropertyTerm(SingleSearchTerm singleSearchTerm, Class<E> clazz, Function<String, E> parser, Function<E, ?> encoder) {
        SingleSearchTerm adjustedTerm = new SingleSearchTerm(singleSearchTerm.getOperation());
        for (Operand<?> operand : singleSearchTerm.getOperands()) {
            if (Operand.Type.CONSTANT.equals(operand.getType()) && clazz.isInstance(operand.getValue())) {
                /*
                 * match against encoded enum value instead
                 */
                adjustedTerm.addOperand(new ConstantOperand<Object>(encoder.apply(clazz.cast(operand.getValue()))));
            } else if (Operand.Type.CONSTANT.equals(operand.getType()) && (operand.getValue() instanceof String)) {
                /*
                 * parse operand & match against encoded enum value instead
                 */
                E value = parser.apply((String) operand.getValue());
                adjustedTerm.addOperand(new ConstantOperand<Object>(encoder.apply(value)));
            } else {
                adjustedTerm.addOperand(operand);
            }
        }
        return adjustedTerm;
    }

    /**
     * Adjusts a single search term targeting {@link EventField#ORGANIZER} to match against storage representation instead.
     *
     * @param singleSearchTerm The search term to adjust
     * @return The adjusted search term
     */
    private SingleSearchTerm adjustOrganizerTerm(SingleSearchTerm singleSearchTerm) {
        ColumnFieldOperand<EventField> adjustedColumnOperand = null;
        Operand<?> constantOperand = getFirstOperandOfType(singleSearchTerm, Operand.Type.CONSTANT);
        if (null != constantOperand && (constantOperand.getValue() instanceof String)) {
            /*
             * match against encoded organizer, trimming the X:mailto: prefix as needed
             */
            adjustedColumnOperand = new FormattingColumnFieldOperand<EventField>(EventField.ORGANIZER,
                (label) -> String.format("SUBSTRING_INDEX(LOWER(%1$s),'mailto:',-1)", label)
            );
        }
        SingleSearchTerm adjustedTerm = new SingleSearchTerm(singleSearchTerm.getOperation());
        for (Operand<?> operand : singleSearchTerm.getOperands()) {
            if (EventField.ORGANIZER.equals(operand.getValue()) && null != adjustedColumnOperand) {
                adjustedTerm.addOperand(adjustedColumnOperand);
            } else if (Operand.Type.CONSTANT.equals(operand.getType()) && (operand.getValue() instanceof Number)) {
                /*
                 * match against organizer's resource id
                 */
                String resoureceId = ResourceId.forUser(contextID, ((Number) operand.getValue()).intValue());
                adjustedTerm.addOperand(new ConstantOperand<String>(resoureceId));
            } else {
                adjustedTerm.addOperand(operand);
            }
        }
        return adjustedTerm;
    }

    private void appendOperation(Operation operation, Operand<?>[] operands, DbMapping<? extends Object, ?> mapping) {
        stringBuilder.append('(');
        for (int i = 0; i < operands.length; i++) {
            if (OperationPosition.BEFORE.equals(operation.getSqlPosition())) {
                stringBuilder.append(operation.getSqlRepresentation());
            }
            if (Operand.Type.COLUMN.equals(operands[i].getType())) {
                Entry<String, DbMapping<? extends Object, ?>> entry = getMapping(operands[i].getValue());
                appendColumnOperand(entry.getValue(), entry.getKey(), optColumnLabelFormatFunction(operands[i]));
            } else if (Operand.Type.CONSTANT.equals(operands[i].getType())) {
                appendConstantOperand(operands[i].getValue(), mapping.getSqlType());
            } else {
                throw new IllegalArgumentException("unknown type in operand: " + operands[i].getType());
            }
            if (OperationPosition.AFTER.equals(operation.getSqlPosition())) {
                stringBuilder.append(' ').append(operation.getSqlRepresentation());
            } else if (OperationPosition.BETWEEN.equals(operation.getSqlPosition()) && i != operands.length - 1) {
                //don't place an operator after the last operand here
                stringBuilder.append(' ').append(operation.getSqlRepresentation()).append(' ');
            }
        }
        stringBuilder.append(')');
    }

    private void append(CompositeSearchTerm term) throws OXException {
        stringBuilder.append(" ( ");
        if (false == appendAsInClause(term)) {
            Operation operation = term.getOperation();
            SearchTerm<?>[] terms = term.getOperands();
            if (OperationPosition.BEFORE.equals(operation.getSqlPosition())) {
                stringBuilder.append(operation.getSqlRepresentation());
            }
            for (int i = 0; i < terms.length; i++) {
                append(terms[i]);
                if (OperationPosition.AFTER.equals(operation.getSqlPosition())) {
                    stringBuilder.append(' ').append(operation.getSqlRepresentation());
                } else if (OperationPosition.BETWEEN.equals(operation.getSqlPosition()) && i != terms.length - 1) {
                    //don't place an operator after the last operand
                    stringBuilder.append(' ').append(operation.getSqlRepresentation()).append(' ');
                }
            }
        }
        stringBuilder.append(" ) ");
    }

    /**
     * Tries to interpret and append a composite term as <code>IN</code>-clause, so that a composite 'OR' term where each nested 'EQUALS'
     * operation targets the same column gets optimized to a suitable <code>column IN (value1,value2,...)</code>.
     *
     * @param compositeTerm The composite term
     * @return <code>true</code>, if the term was appended as 'IN' clause, <code>false</code>, otherwise
     */
    private boolean appendAsInClause(CompositeSearchTerm compositeTerm) {
        /*
         * check if operation & operands are applicable
         */
        if (false == CompositeOperation.OR.equals(compositeTerm.getOperation())) {
            return false; // only 'OR' composite operations
        }
        if (null == compositeTerm.getOperands() || 2 > compositeTerm.getOperands().length) {
            return false; // at least 2 operands
        }
        List<Object> constantValues = new ArrayList<Object>();
        Operand<?> commonColumnOperand = null;
        for (SearchTerm<?> term : compositeTerm.getOperands()) {
            if (false == (term instanceof SingleSearchTerm) || false == SingleOperation.EQUALS.equals(term.getOperation())) {
                return false; // only nested single search terms with 'EQUALS' operations
            }
            Operand<?> columnOperand = null;
            Object constantValue = null;
            for (Operand<?> operand : ((SingleSearchTerm) term).getOperands()) {
                if (Type.COLUMN.equals(operand.getType())) {
                    columnOperand = operand;
                } else if (Type.CONSTANT.equals(operand.getType())) {
                    constantValue = operand.getValue();
                } else {
                    return false; // only 'COLUMN' = 'CONSTANT' operations
                }
            }
            if (null == columnOperand || null == constantValue) {
                return false; // only 'COLUMN' = 'CONSTANT' operations
            }
            if (EventField.ATTENDEES.equals(columnOperand.getValue())) {
               return false; // Attendees need special "EXISTS" handling
            }
            if ((constantValue instanceof String) && StringCollection.containsWildcards((String) constantValue)) {
                return false; // no wildcard comparisons
            }
            if (null == commonColumnOperand) {
                commonColumnOperand = columnOperand; // first column value
            } else if (false == Objects.equals(commonColumnOperand.getValue(), columnOperand.getValue())) {
                return false; // only equal column value
            }
            constantValues.add(constantValue);
        }
        if (null == commonColumnOperand || 2 > constantValues.size()) {
            return false;
        }
        /*
         * all checks passed, build IN clause
         */
        Iterator<Entry<String, DbMapping<? extends Object, ?>>> iterator = getMappings(commonColumnOperand.getValue()).entrySet().iterator();
        Entry<String, DbMapping<? extends Object, ?>> firstMapping = iterator.next();
        if (false == iterator.hasNext()) {
            appendAsInClause(firstMapping.getValue(), firstMapping.getKey(), constantValues, optColumnLabelFormatFunction(commonColumnOperand));
        } else {
            stringBuilder.append('(');
            appendAsInClause(firstMapping.getValue(), firstMapping.getKey(), constantValues, optColumnLabelFormatFunction(commonColumnOperand));
            do {
                stringBuilder.append(" OR ");
                Entry<String, DbMapping<? extends Object, ?>> mapping = iterator.next();
                appendAsInClause(mapping.getValue(), mapping.getKey(), constantValues, optColumnLabelFormatFunction(commonColumnOperand));
            } while (iterator.hasNext());
            stringBuilder.append(')');
        }
        return true;
    }

    private void appendAsInClause(DbMapping<? extends Object, ?> mapping, String prefix, List<Object> constantValues, Function<String, String> optColumnLabelFormat) {
        appendColumnOperand(mapping, prefix, optColumnLabelFormat);
        stringBuilder.append(" IN (");
        appendConstantOperand(constantValues.get(0), mapping.getSqlType());
        for (int i = 1; i < constantValues.size(); i++) {
            stringBuilder.append(',');
            appendConstantOperand(constantValues.get(i), mapping.getSqlType());
        }
        stringBuilder.append(") ");
    }

    private void appendConstantOperand(Object value, int sqlType) {
        if ((value instanceof String)) {
            appendConstantOperand((String) value, sqlType);
            return;
        }
        if ((value instanceof Boolean) && Types.INTEGER == sqlType) {
            parameters.add(Integer.valueOf(Boolean.TRUE.equals(value) ? 1 : 0));
        } else if ((value instanceof Long) && Types.TIMESTAMP == sqlType) {
            parameters.add(new Timestamp(((Long) value).longValue()));
        } else if ((value instanceof Date) && Types.TIMESTAMP == sqlType) {
            parameters.add(new Timestamp(((Date) value).getTime()));
        } else if ((value instanceof Date) && Types.BIGINT == sqlType) {
            parameters.add(Long.valueOf(((Date) value).getTime()));
        } else if ((value instanceof ParticipationStatus) && Types.VARCHAR == sqlType) {
            parameters.add(((ParticipationStatus) value).getValue());
        } else if ((value instanceof Transp) && Types.VARCHAR == sqlType) {
            parameters.add(((Transp) value).getValue());
        } else if ((value instanceof RelatedTo) && Types.VARCHAR == sqlType) {
            RelatedTo relatedTo = (RelatedTo) value;
            parameters.add(null == relatedTo.getRelType() ? "" : relatedTo.getRelType() + ':' + relatedTo.getValue());
        } else {
            // default
            parameters.add(value);
        }
        stringBuilder.append('?');
    }

    private void appendConstantOperand(String value, int sqlType) {
        if (Types.INTEGER == sqlType) {
            if ("true".equalsIgnoreCase(value)) {
                // special handling for "true" string
                parameters.add(Integer.valueOf(1));
            } else if ("false".equalsIgnoreCase(value)) {
                // special handling for "false" string
                parameters.add(Integer.valueOf(0));
            } else {
                // try to parse
                parameters.add(Integer.valueOf(value));
            }
        } else {
            if (StringCollection.containsWildcards(value)) {
                // use "LIKE" search
                parameters.add(StringCollection.prepareForSearch(value, false, true));
                int index = stringBuilder.lastIndexOf("=");
                stringBuilder.replace(index, index + 1, "LIKE");
            } else {
                // use "EQUALS" search
                parameters.add(value);
            }
        }
        stringBuilder.append('?');
    }

    private void appendColumnOperand(DbMapping<? extends Object, ?> mapping, String prefix, Function<String, String> optColumnLabelFormat) {
        String columnLabel = null != prefix ? mapping.getColumnLabel(prefix) : mapping.getColumnLabel();
        if (null != optColumnLabelFormat) {
            columnLabel = optColumnLabelFormat.apply(columnLabel);
        }
        if (null != charset && Types.VARCHAR == mapping.getSqlType()) {
            stringBuilder.append("CONVERT(").append(columnLabel).append(" USING ").append(charset).append(')');
        } else {
            stringBuilder.append(columnLabel);
        }
    }

    private static Operand<?> getFirstOperandOfType(SingleSearchTerm term, Operand.Type type) {
        for (Operand<?> operand : term.getOperands()) {
            if (type.equals(operand.getType())) {
                return operand;
            }
        }
        return null;
    }

    private Map<String, DbMapping<? extends Object, ?>> getMappings(Object value) {
        if ((value instanceof EventField)) {
            DbMapping<? extends Object, ?> mapping = EventMapper.getInstance().opt((EventField) value);
            if (null == mapping) {
                throw new IllegalArgumentException("No mapping available for: " + value);
            }
            usesEvents = true;
            return Collections.<String, DbMapping<? extends Object, ?>> singletonMap(prefixEvents, mapping);
        }
        if ((value instanceof AttendeeField)) {
            DbMapping<? extends Object, Attendee> attendeeMapping = AttendeeMapper.getInstance().opt((AttendeeField) value);
            if (null == attendeeMapping) {
                throw new IllegalArgumentException("No mapping available for: " + value);
            }
            usesAttendees = true;
            return Collections.<String, DbMapping<? extends Object, ?>> singletonMap(prefixAttendees, attendeeMapping);
        }
        throw new IllegalArgumentException("No mapping available for: " + value);
    }

    private Map.Entry<String, DbMapping<? extends Object, ?>> getMapping(Object value) {
        Set<Entry<String, DbMapping<? extends Object, ?>>> entries = getMappings(value).entrySet();
        if (1 < entries.size()) {
            throw new IllegalArgumentException("Found multiple mappings for: " + value);
        }
        return entries.iterator().next();
    }

    private static Function<String, String> optColumnLabelFormatFunction(Operand<?> operand) {
        if ((operand instanceof FormattingColumnFieldOperand)) {
            return ((FormattingColumnFieldOperand<?>) operand).getColumnLabelFormatFunction();
        }
        return null;
    }

    private static final class FormattingColumnFieldOperand<E extends Enum<?>> extends ColumnFieldOperand<E> {

        private final Function<String, String> columnLabelFormatFunction;

        public FormattingColumnFieldOperand(E field, Function<String, String> columnLabelFormatFunction) {
            super(field);
            this.columnLabelFormatFunction = columnLabelFormatFunction;
        }

        public Function<String, String> getColumnLabelFormatFunction() {
            return columnLabelFormatFunction;
        }

    }

}
