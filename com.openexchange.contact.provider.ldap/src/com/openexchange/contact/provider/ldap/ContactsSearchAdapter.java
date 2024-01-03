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
import java.util.List;
import com.openexchange.contact.ContactFieldOperand;
import com.openexchange.contact.provider.ldap.mapping.LdapMapper;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactConfig;
import com.openexchange.groupware.contact.Search;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.search.ContactsSearchObject;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ConstantOperand;
import com.unboundid.ldap.sdk.Filter;

/**
 * {@link ContactsSearchAdapter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.6
 */
public class ContactsSearchAdapter {

    /** A search term to restrict the results to contacts having a non-empty image1 property */
    private static final SearchTerm<?> HAS_IMG_TERM = getEqualsTerm(ContactField.IMAGE1, "*");

    /** A search term to restrict the results to contacts having at least one non-empty email address property */
    private static final SearchTerm<?> HAS_EMAIL_TERM = new CompositeSearchTerm(CompositeOperation.OR)
        .addSearchTerm(getEqualsTerm(ContactField.EMAIL1, "*"))
        .addSearchTerm(getEqualsTerm(ContactField.EMAIL2, "*"))
        .addSearchTerm(getEqualsTerm(ContactField.EMAIL3, "*"))
    ;

    private final LdapMapper mapper;
    private final List<OXException> warnings;

    /**
     * Initializes a new {@link ContactsSearchAdapter}.
     *
     * @param mapper The underlying LDAP mapper
     * @param warnings A reference to a collection of warnings to use
     */
    public ContactsSearchAdapter(LdapMapper mapper, List<OXException> warnings) {
        super();
        this.mapper = mapper;
        this.warnings = warnings;
    }

    /**
     * Converts a search term to its LDAP filter representation.
     *
     * @param contactSearch The contacts search object to convert
     * @return The filter, or <code>null</code> if no LDAP representation is possible due to missing mappings
     */
    public Filter getFilter(ContactsSearchObject contactSearch) throws OXException {
        SearchTerm<?> searchTerm = getSearchTerm(contactSearch);
        return null != searchTerm ? new SearchTermAdapter(mapper, warnings).getFilter(searchTerm) : null;
    }

    /**
     * Converts the given {@link ContactsSearchObject} into a {@link SearchTerm}
     *
     * @param contactsSearchObject The {@link ContactsSearchObject} to convert
     * @return The {@link SearchTerm}
     * @throws OXException in case the {@link ContactsSearchObject} can't be converted to a {@link SearchTerm}
     */
    public SearchTerm<?> getSearchTerm(ContactsSearchObject contactsSearchObject) throws OXException {
        SearchTerm<?> searchTerm;
        if (null != contactsSearchObject.getPattern()) {
            /*
             * convert simple pattern based contact search
             */
            searchTerm = convertContactSearch(contactsSearchObject);
        } else {
            /*
             * convert contact search alternative
             */
            searchTerm = convertContactSearchAlternative(contactsSearchObject);
        }
        return searchTerm;
    }

    private SearchTerm<?> convertContactSearchAlternative(ContactsSearchObject contactSearch) throws OXException {
        /*
         * create terms for individual search criteria
         */
        List<SingleSearchTerm> searchTerms = new ArrayList<SingleSearchTerm>();
        boolean emailAutoComplete = contactSearch.isEmailAutoComplete();
        boolean orSearch = emailAutoComplete || contactSearch.isOrSearch();
        boolean prependWildcard = false;
        boolean appendWildcard = false;
        if (null != contactSearch.getSurname()) {
            searchTerms.add(getSearchTerm(ContactField.SUR_NAME, contactSearch.getSurname(), prependWildcard, appendWildcard));
        }
        if (null != contactSearch.getGivenName()) {
            searchTerms.add(getSearchTerm(ContactField.GIVEN_NAME, contactSearch.getGivenName(), prependWildcard, appendWildcard));
        }
        if (null != contactSearch.getDisplayName()) {
            searchTerms.add(getSearchTerm(ContactField.DISPLAY_NAME, contactSearch.getDisplayName(), prependWildcard, appendWildcard));
        }
        if (null != contactSearch.getEmail1()) {
            searchTerms.add(getSearchTerm(ContactField.EMAIL1, contactSearch.getEmail1(), prependWildcard, appendWildcard));
        }
        if (null != contactSearch.getEmail2()) {
            searchTerms.add(getSearchTerm(ContactField.EMAIL2, contactSearch.getEmail2(), prependWildcard, appendWildcard));
        }
        if (null != contactSearch.getEmail3()) {
            searchTerms.add(getSearchTerm(ContactField.EMAIL3, contactSearch.getEmail3(), prependWildcard, appendWildcard));
        }
        if (null != contactSearch.getCompany()) {
            searchTerms.add(getSearchTerm(ContactField.COMPANY, contactSearch.getCompany(), prependWildcard, appendWildcard));
        }
        if (null != contactSearch.getCatgories()) {
            searchTerms.add(getSearchTerm(ContactField.CATEGORIES, contactSearch.getCatgories(), prependWildcard, appendWildcard));
        }
        /*
         * construct composite search term
         */
        SearchTerm<?> searchTerm = null;
        if (false == searchTerms.isEmpty()) {
            CompositeSearchTerm compositeTerm = new CompositeSearchTerm(orSearch ? CompositeOperation.OR : CompositeOperation.AND);
            for (SingleSearchTerm term : searchTerms) {
                compositeTerm.addSearchTerm(term);
            }
            searchTerm = compositeTerm;
        }
        /*
         * combine with special terms as needed
         */
        if (contactSearch.isHasImage()) {
            searchTerm = getCompositeTerm(searchTerm, HAS_IMG_TERM);
        }
        if (emailAutoComplete) {
            searchTerm = getCompositeTerm(searchTerm, HAS_EMAIL_TERM);
        }
        return searchTerm;
    }

    private SearchTerm<?> convertContactSearch(ContactsSearchObject contactSearch) throws OXException {
        /*
         * convert single pattern based contact search, preferring a start letter if set
         */
        SearchTerm<?> searchTerm = optStartLetterTerm(contactSearch);
        if (null == searchTerm) {
            /*
             * fallback to generic display name search
             */
            searchTerm = getSearchTerm(ContactField.DISPLAY_NAME, contactSearch.getPattern(), true, true);
        }
        return searchTerm;
    }

    private SearchTerm<?> optStartLetterTerm(ContactsSearchObject contactSearch) {
        if (contactSearch.isStartLetter() && Strings.isNotEmpty(contactSearch.getPattern())) {
            String pattern = contactSearch.getPattern();
            if (".".equals(pattern) || "#".equals(pattern)) {
                /*
                 * no letter, no digit / digit - not possible in LDAP
                 */
                throw new UnsupportedOperationException("Unable to perform start-letter search for pattern " + pattern);
            }
            if (false == "all".equals(pattern)) {
                /*
                 * match start letter
                 */
                return getEqualsTerm(getStartLetterField(), prepareForSearch(pattern, false, true));
            }
        }
        /*
         * no valid pattern
         */
        return null;
    }


    private static SingleSearchTerm getEqualsTerm(ContactField field, String value) {
        return new SingleSearchTerm(SingleOperation.EQUALS).addOperand(new ContactFieldOperand(field)).addOperand(new ConstantOperand<String>(value));
    }

    private static SingleSearchTerm getSearchTerm(ContactField field, String pattern, boolean prependWildcard, boolean appendWildcard) throws OXException {
        Search.checkPatternLength(pattern);
        return getEqualsTerm(field, prepareForSearch(pattern, prependWildcard, appendWildcard));
    }

    private static String prepareForSearch(String pattern, boolean prependWildcard, boolean appendWildcard) {
        String preparedPattern = pattern;
        if (prependWildcard && false == preparedPattern.startsWith("*")) {
            preparedPattern = '*' + preparedPattern;
        }
        if (appendWildcard && false == preparedPattern.endsWith("*")) {
            preparedPattern = preparedPattern + '*';
        }
        return preparedPattern;
    }

    /**
     * Creates a new 'AND' composite search term using the supplied terms as
     * operands.
     *
     * @param term1 the first term
     * @param term2 the second term
     * @return the composite search term
     */
    private static SearchTerm<?> getCompositeTerm(SearchTerm<?> term1, SearchTerm<?> term2) {
        if (null == term1 || null == term1.getOperands() || 0 == term1.getOperands().length) {
            return term2;
        }
        if (null == term2 || null == term2.getOperands() || 0 == term2.getOperands().length) {
            return term1;
        }
        CompositeSearchTerm andTerm = new CompositeSearchTerm(CompositeOperation.AND);
        andTerm.addSearchTerm(term1);
        andTerm.addSearchTerm(term2);
        return andTerm;
    }

    private static ContactField getStartLetterField() {
        @SuppressWarnings("deprecation") ContactField field = ContactField.getByDBFieldName(ContactConfig.getInstance().getString(ContactConfig.Property.LETTER_FIELD));
        return null != field ? field : ContactField.DISPLAY_NAME;
    }

}
