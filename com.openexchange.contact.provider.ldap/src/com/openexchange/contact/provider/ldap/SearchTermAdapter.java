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
import org.slf4j.Logger;
import com.openexchange.contact.provider.ldap.mapping.LdapMapper;
import com.openexchange.exception.OXException;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm;
import com.unboundid.ldap.sdk.Filter;

/**
 * {@link SearchAdapter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.6
 */
public class SearchTermAdapter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SearchTermAdapter.class);

    private final LdapMapper mapper;
    private final List<OXException> warnings;

    /**
     * Initializes a new {@link SearchTermAdapter}.
     *
     * @param mapper The underlying LDAP mapper
     * @param warnings A reference to a collection of warnings to use
     */
    public SearchTermAdapter(LdapMapper mapper, List<OXException> warnings) {
        super();
        this.mapper = mapper;
        this.warnings = warnings;
    }

    /**
     * Converts a search term to its LDAP filter representation.
     * 
     * @param term The search term to convert
     * @return The filter, or <code>null</code> if no LDAP representation is possible due to missing mappings
     */
    public Filter getFilter(SearchTerm<?> term) throws OXException {
        if ((term instanceof SingleSearchTerm)) {
            return optFilter((SingleSearchTerm) term);
        }
        if ((term instanceof CompositeSearchTerm)) {
            return getFilter((CompositeSearchTerm) term);
        }
        throw new IllegalArgumentException("Need either an 'SingleSearchTerm' or 'CompositeSearchTerm'.");
    }

    private List<Filter> getFilters(SearchTerm<?>[] terms) throws OXException {
        if (null == terms || 0 == terms.length) {
            return Collections.emptyList();
        }
        List<Filter> filters = new ArrayList<Filter>(terms.length);
        for (SearchTerm<?> term : terms) {
            Filter filter = getFilter(term);
            if (null == filter) {
                LOG.debug("No corresponding LDAP representation possible for term '{}', skipping.", term);
                warnings.add(LdapContactsExceptionCodes.IGNORED_TERM.create(String.valueOf(term), ""));
            } else {
                filters.add(filter);
            }
        }
        return filters.isEmpty() ? null : filters;
    }

    private Filter getFilter(CompositeSearchTerm term) throws OXException {
        SearchTerm<?>[] operands = term.getOperands();
        switch (term.getOperation()) {
            case AND:
                if (null == operands || 0 == operands.length) {
                    throw new IllegalArgumentException("Need operand(s) for AND operation");
                }
                return 1 == operands.length ? getFilter(operands[0]) : Filter.createANDFilter(getFilters(operands));
            case NOT:
                if (null == operands || 1 != operands.length) {
                    throw new IllegalArgumentException("Need excatly one operand for NOT operation");
                }
                return Filter.createNOTFilter(getFilter(operands[0]));
            case OR:
                if (null == operands || 0 == operands.length) {
                    throw new IllegalArgumentException("Need operand(s) for OR operation");
                }
                return 1 == operands.length ? getFilter(operands[0]) : Filter.createORFilter(getFilters(operands));
            default:
                throw new IllegalArgumentException("Unknown operation: " + term.getOperation());
        }
    }

    private Filter optFilter(SingleSearchTerm term) {
        try {
            return mapper.getFilter(term);
        } catch (Exception e) {
            LOG.debug("Unable to get LDAP filter for term '{}' ({}), excluding from search filter.", term, e.getMessage());
            warnings.add(LdapContactsExceptionCodes.IGNORED_TERM.create(String.valueOf(term), e.getMessage()));
            return null;
        }
    }

}
