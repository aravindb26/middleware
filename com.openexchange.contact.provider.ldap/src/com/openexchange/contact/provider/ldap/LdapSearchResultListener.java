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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.tools.functions.ErrorAwareFunction;
import com.unboundid.ldap.sdk.AsyncRequestID;
import com.unboundid.ldap.sdk.AsyncSearchResultListener;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultReference;

/**
 * {@link LdapSearchResultListener}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LdapSearchResultListener<T> implements AsyncSearchResultListener {

    private static final long serialVersionUID = 8100336214321327895L;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LdapSearchResultListener.class);

    private boolean aborted = false;
    private int referencesRead = 0;
    private int exceptionsCaught = 0;
    private int entriesRead = 0;
    private final List<T> results;
    private final ErrorAwareFunction<LdapSearchResult, Collection<T>> entryConverter;
    private final LDAPConnectionProvider connectionProvider;
    private final Function<LDAPSearchException, Boolean> errorHandler;

    /**
     * Initializes a new {@link LdapSearchResultListener}.
     *
     * @param connectionProvider The underlying connection provider for the the LDAP server
     * @param entryConverter The entry converter function
     */
    public LdapSearchResultListener(LDAPConnectionProvider connectionProvider, ErrorAwareFunction<LdapSearchResult, Collection<T>> entryConverter) {
        this(connectionProvider, entryConverter, null);
    }

    /**
     * Initializes a new {@link LdapSearchResultListener}.
     *
     * @param connectionProvider The underlying connection provider for the the LDAP server
     * @param entryConverter The entry converter function
     * @param errorHandler The error handler function, which indicates whether the search should continue afterwards or not, or <code>null</code> to use a default handling
     */
    public LdapSearchResultListener(LDAPConnectionProvider connectionProvider, ErrorAwareFunction<LdapSearchResult, Collection<T>> entryConverter, Function<LDAPSearchException, Boolean> errorHandler) {
        this(new LinkedList<T>(), connectionProvider, entryConverter, errorHandler);
    }

    /**
     * Initializes a new {@link LdapSearchResultListener}.
     *
     * @param results The result list to add converted search entries to
     * @param connectionProvider The underlying connection provider for the the LDAP server
     * @param errorHandler The error handler function, which indicates whether the search should continue afterwards or not, or <code>null</code> to use a default handling
     */
    public LdapSearchResultListener(List<T> results, LDAPConnectionProvider connectionProvider, ErrorAwareFunction<LdapSearchResult, Collection<T>> entryConverter, Function<LDAPSearchException, Boolean> errorHandler) {
        super();
        this.connectionProvider = connectionProvider;
        this.results = results;
        this.entryConverter = entryConverter;
        this.errorHandler = errorHandler;
    }

    @Override
    public void searchEntryReturned(SearchResultEntry entry) {
        if (aborted) {
            return;
        }
        entriesRead++;
        try {
            results.addAll(entryConverter.apply(new LdapSearchResult(connectionProvider, entry)));
        } catch (OXException e) {
            LOG.warn("Error converting {} to contact: {}", entry, e.getMessage(), e);
            exceptionsCaught++;
        }
    }

    @Override
    public void searchReferenceReturned(SearchResultReference searchReference) {
        if (aborted) {
            return;
        }
        referencesRead++;
        LOG.trace("Received {}, continue reading.", searchReference);
    }

    @Override
    public void searchResultReceived(AsyncRequestID requestID, SearchResult searchResult) {
        if (aborted) {
            return;
        }
        exceptionsCaught++;
        if (false == ResultCode.SUCCESS.equals(searchResult.getResultCode())) {
            LDAPSearchException e = new LDAPSearchException(searchResult);
            if (null != errorHandler) {
                aborted = Boolean.FALSE.equals(errorHandler.apply(e));
            } else {
                aborted = true;
                if (ResultCode.SIZE_LIMIT_EXCEEDED.equals(e.getResultCode())) {
                    LOG.debug("Size limit exceeded ({}), abort reading.", e.getMessage());
                } else {
                    LOG.warn("Problem encountered ({}), abort reading.", e.getMessage(), e);
                }
            }
        }
    }

    public List<T> getResults() {
        return results;
    }

    /**
     * Gets the referencesRead
     *
     * @return The referencesRead
     */
    public int getReferencesRead() {
        return referencesRead;
    }

    /**
     * Gets the exceptionsCaught
     *
     * @return The exceptionsCaught
     */
    public int getExceptionsCaught() {
        return exceptionsCaught;
    }

    /**
     * Gets the entriesRead
     *
     * @return The entriesRead
     */
    public int getEntriesRead() {
        return entriesRead;
    }

    /**
     * Increments the number of exceptions caught
     */
    public void incrementExceptionsCaugth() {
        this.exceptionsCaught++;
    }

}
