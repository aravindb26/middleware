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

package com.openexchange.contact.provider.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import com.openexchange.contact.provider.ldap.mapping.LdapMapper;
import com.openexchange.contact.provider.ldap.mapping.LdapMapping;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.java.Collators;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.Operand;
import com.openexchange.search.Operand.Type;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;

/**
 * {@link SearchFilter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class SearchFilter {

    private final Locale locale;
    private final SearchTerm<?> term;
    private final LdapMapper mapper;

    /**
     * Initializes a new {@link SearchFilter}.
     *
     * @param term The search term
     * @param locale The locale for string comparisons, or <code>null</code> if not relevant
     * @param config The LDAP mapper to use
     */
    public SearchFilter(SearchTerm<?> term, Locale locale, LdapMapper mapper) {
        super();
        this.term = term;
        this.locale = locale;
        this.mapper = mapper;
    }

    /**
     * Creates a new collection and adds all contacts from the supplied
     * collection fulfilling the search criteria.
     *
     * @param contacts
     * @return The filtered list of contacts
     */
    public List<Contact> filter(List<Contact> contacts) {
        List<Contact> filteredContacts = null;
        if (null != contacts && null != this.term) {
            for (Contact contact : contacts) {
                if (matches(contact, this.term, this.locale)) {
                    if (filteredContacts == null) {
                        filteredContacts = new ArrayList<>();
                    }
                    filteredContacts.add(contact);
                }
            }
        }
        return filteredContacts == null ? Collections.emptyList() : filteredContacts;
    }

    private boolean matches(Contact contact, SearchTerm<?> term, Locale locale) {
        if ((term instanceof SingleSearchTerm)) {
            return matches(contact, (SingleSearchTerm) term, locale);
        } else if ((term instanceof CompositeSearchTerm)) {
            return matches(contact, (CompositeSearchTerm) term, locale);
        } else {
            throw new IllegalArgumentException("Need either a 'SingleSearchTerm' or 'CompositeSearchTerm'.");
        }
    }

    private boolean matches(Contact contact, SingleSearchTerm term, Locale locale) {
        /*
         * extract contact field mapping and constant value to compare against from term
         */
        Object operandValue = null;
        LdapMapping<? extends Object> mapping = null;
        for (Operand<?> operand : term.getOperands()) {
            if (Type.CONSTANT.equals(operand.getType())) {
                operandValue = operand.getValue();
            } else if (Type.COLUMN.equals(operand.getType())) {
                ContactField contactField = LdapMapper.getField(operand.getValue());
                if (null != contactField) {
                    mapping = mapper.opt(contactField);
                }
            }
        }
        Object contactValue = null != mapping && mapping.isSet(contact) ? mapping.get(contact) : null;
        if (null != contactValue && (operandValue instanceof String) && false == (contactValue instanceof String)) {
            // normalize to strings for comparison (numerical IDs from contact)))
            contactValue = contactValue.toString();
        }
        /*
         * compare values
         */
        switch (term.getOperation()) {
            case EQUALS:
                return 0 == compare(contactValue, operandValue, locale);
            case GREATER_OR_EQUAL:
                return 0 <= compare(contactValue, operandValue, locale);
            case GREATER_THAN:
                return 0 < compare(contactValue, operandValue, locale);
            case ISNULL:
                return null == contactValue;
            case LESS_OR_EQUAL:
                return 0 >= compare(contactValue, operandValue, locale);
            case LESS_THAN:
                return 0 > compare(contactValue, operandValue, locale);
            case NOT_EQUALS:
                return 0 != compare(contactValue, operandValue, locale);
            default:
                throw new IllegalArgumentException("Unknown operation: " + term.getOperation());
        }
    }

    private boolean matches(Contact contact, CompositeSearchTerm term, Locale locale) {
        SearchTerm<?>[] terms = term.getOperands();
        switch (term.getOperation()) {
            case AND:
                for (SearchTerm<?> searchTerm : terms) {
                    if (false == matches(contact, searchTerm, locale)) {
                        return false;
                    }
                }
                return true;
            case NOT:
                return false == matches(contact, terms[0], locale);
            case OR:
                for (SearchTerm<?> searchTerm : terms) {
                    if (matches(contact, searchTerm, locale)) {
                        return true;
                    }
                }
                return false;
            default:
                throw new IllegalArgumentException("Unknown operation: " + term.getOperation());
        }
    }

    private static int compare(Object o1, Object o2, Locale locale) {
        if (o1 == o2) {
            return 0;
        }
        if (null == o1) {
            return null != o2 ? -1 : 1;
        }
        if (null == o2) {
            return 1;
        }
        if ((o1 instanceof String) && (o2 instanceof String)) {
            String value1 = (String) o1;
            String value2 = (String) o2;
            if (value1.equals(value2) || matchesWildcard(value1.toLowerCase(), value2.toLowerCase(), locale)) {
                return 0;
            } else if (null == locale) {
                return value1.compareTo(value2);
            } else {
                return Collators.getDefaultInstance(locale).compare(value1, value2);
            }
        }
        if (o1 instanceof Comparable) {
            @SuppressWarnings("unchecked") Comparable<Object> comparable = Comparable.class.cast(o1);
            return comparable.compareTo(o2);
        }
        throw new UnsupportedOperationException("Don't know how to compare two values of class " + o1.getClass().getName());
    }

    private static boolean matchesWildcard(String value, String wildcardPattern, Locale locale) {
        return "*".equals(wildcardPattern) || matchesWildcard(value.toLowerCase(null != locale ? locale : Locale.ENGLISH), wildcardPattern.toLowerCase(null != locale ? locale : Locale.ENGLISH), 0, 0);
    }

    private static boolean matchesWildcard(String value, String wildcardPattern, int valueIndex, int patternIndex) {
        /*
         * based on http://www.java2s.com/Open-Source/Java/Development/jodd/jodd/util/Wildcard.java.htm
         */
        int patternIdx = patternIndex;
        int valueIdx = valueIndex;
        int patternLength = wildcardPattern.length();
        int valueLength = value.length();
        boolean nextIsNotWildcard = false;
        while (true) {
            // check if end of string and/or pattern occurred
            if (valueIdx >= valueLength) {   // end of string still may have pending '*' in pattern
                while (patternIdx < patternLength && '*' == wildcardPattern.charAt(patternIdx)) {
                    patternIdx++;
                }
                return patternIdx >= patternLength;
            }
            if (patternIdx >= patternLength) {         // end of pattern, but not end of the string
                return false;
            }
            char p = wildcardPattern.charAt(patternIdx);    // pattern char

            // perform logic
            if (nextIsNotWildcard == false) {
                if (p == '\\') {
                    patternIdx++;
                    nextIsNotWildcard = true;
                    continue;
                }
                if (p == '?') {
                    valueIdx++;
                    patternIdx++;
                    continue;
                }
                if (p == '*') {
                    char pnext = 0;           // next pattern char
                    if (patternIdx + 1 < patternLength) {
                        pnext = wildcardPattern.charAt(patternIdx + 1);
                    }
                    if (pnext == '*') {         // double '*' have the same effect as one '*'
                        patternIdx++;
                        continue;
                    }
                    int i;
                    patternIdx++;

                    // find recursively if there is any substring from the end of the
                    // line that matches the rest of the pattern !!!
                    for (i = value.length(); i >= valueIdx; i--) {
                        if (matchesWildcard(value, wildcardPattern, i, patternIdx) == true) {
                            return true;
                        }
                    }
                    return false;
                }
            } else {
                nextIsNotWildcard = false;
            }
            // check if pattern char and string char are equals
            if (p != value.charAt(valueIdx)) {
                return false;
            }
            // everything matches for now, continue
            valueIdx++;
            patternIdx++;
        }
    }

}
