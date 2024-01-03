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

package com.openexchange.mail;

import com.openexchange.mail.search.SearchTerm;

/**
 * {@link MailFetchArguments}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
public class MailFetchArguments {

    /**
     * Creates a copy.
     *
     * @param fetchArguments The fetch arguments to copy from
     * @return The copy
     */
    public static MailFetchArguments copy(MailFetchArguments fetchArguments) {
        return copy(fetchArguments, fetchArguments.fields, fetchArguments.headerNames);
    }

    /**
     * Creates a copy.
     *
     * @param fetchArguments The fetch arguments to copy from
     * @param fields The originally requested fields
     * @param headerNames The originally requested header names
     * @return The copy
     */
    public static MailFetchArguments copy(MailFetchArguments fetchArguments, MailField[] fields, String[] headerNames) {
        return new MailFetchArguments(fetchArguments.folder, fetchArguments.searchTerm, fetchArguments.sortField, fetchArguments.orderDir, fields, headerNames, fetchArguments.initialFields, fetchArguments.initialHeaderNames);
    }

    /**
     * Creates a new builder.
     *
     * @param folder The folder from which mails are fetched
     * @param fields The initially requested fields
     * @param headerNames The initially requested header names
     * @return The new builder
     */
    public static Builder builder(FullnameArgument folder, MailField[] fields, String[] headerNames) {
        return new Builder(folder, fields, headerNames);
    }

    /** The builder for an instance of <code>MailFetchArguments</code> */
    public static class Builder {

        private final FullnameArgument folder;
        private MailField[] initialFields;
        private String[] initialHeaderNames;
        private SearchTerm<?> searchTerm;
        private MailSortField sortField;
        private OrderDirection orderDir;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder(FullnameArgument folder, MailField[] initialFields, String[] initialHeaderNames) {
            super();
            this.folder = folder;
            this.initialFields = initialFields;
            this.initialHeaderNames = initialHeaderNames;
        }

        /**
         * Sets the initially requested fields
         *
         * @param initialFields The fields to set
         * @return This builder
         */
        public Builder setFields(MailField[] initialFields) {
            this.initialFields = initialFields;
            return this;
        }

        /**
         * Sets the initially requested header names
         *
         * @param initialHeaderNames The header names to set
         * @return This builder
         */
        public Builder setHeaderNames(String[] initialHeaderNames) {
            this.initialHeaderNames = initialHeaderNames;
            return this;
        }

        /**
         * Sets the search term.
         *
         * @param searchTerm The search term to set
         * @return This builder
         */
        public Builder setSearchTerm(SearchTerm<?> searchTerm) {
            this.searchTerm = searchTerm;
            return this;
        }

        /**
         * Sets the sort arguments.
         *
         * @param sortField The field to sort
         * @param orderDir The order direction
         * @return This builder
         */
        public Builder setSortOptions(MailSortField sortField, OrderDirection orderDir) {
            this.sortField = sortField;
            this.orderDir = orderDir;
            return this;
        }

        /**
         * Creates the appropriate <code>MailFetchArguments</code> instance from this builder's arguments.
         *
         * @return The <code>MailFetchArguments</code> instance
         */
        public MailFetchArguments build() {
            return new MailFetchArguments(folder, searchTerm, sortField, orderDir, initialFields, initialHeaderNames, initialFields, initialHeaderNames);
        }

    }

    // -------------------------------------------------------------------------------------------------

    private final FullnameArgument folder;
    private final SearchTerm<?> searchTerm;
    private final MailSortField sortField;
    private final OrderDirection orderDir;
    private final MailField[] fields;
    private final String[] headerNames;
    private final MailField[] initialFields;
    private final String[] initialHeaderNames;

    /**
     * Initializes a new {@link MailFetchArguments}.
     */
    MailFetchArguments(FullnameArgument folder, SearchTerm<?> searchTerm, MailSortField sortField, OrderDirection orderDir, MailField[] fields, String[] headerNames, MailField[] initialFields, String[] initialHeaderNames) {
        super();
        this.folder = folder;
        this.searchTerm = searchTerm;
        this.sortField = sortField;
        this.orderDir = orderDir;
        this.fields = fields;
        this.headerNames = headerNames;
        this.initialFields = initialFields;
        this.initialHeaderNames = initialHeaderNames;
    }

    /**
     * Gets the folder from which mails are fetched.
     *
     * @return The folder from which mails are fetched
     */
    public FullnameArgument getFolder() {
        return folder;
    }

    /**
     * Gets the filtering search term or <code>null</code>.
     *
     * @return The filtering search term or <code>null</code>
     */
    public SearchTerm<?> getSearchTerm() {
        return searchTerm;
    }

    /**
     * Gets the field to sort by or <code>null</code>.
     *
     * @return The field to sort by or <code>null</code>
     */
    public MailSortField getSortField() {
        return sortField;
    }

    /**
     * Gets the order direction or <code>null</code>.
     *
     * @return The order direction or <code>null</code>
     */
    public OrderDirection getOrderDir() {
        return orderDir;
    }

    /**
     * Gets the requested fields (so far).
     *
     * @return The requested fields
     */
    public MailField[] getFields() {
        return fields;
    }

    /**
     * Gets the requested header names (so far).
     *
     * @return The requested header names
     */
    public String[] getHeaderNames() {
        return headerNames;
    }

    /**
     * Gets the initially requested fields.
     *
     * @return The initially requested fields
     */
    public MailField[] getInitialFields() {
        return initialFields;
    }

    /**
     * Gets the initially requested header names).
     *
     * @return The initially requested header names
     */
    public String[] getInitialHeaderNames() {
        return initialHeaderNames;
    }

}
