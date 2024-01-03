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

package com.openexchange.objectusecount;

import static com.openexchange.objectusecount.ObjectUseCountService.CONTACT_MODULE;
import static com.openexchange.objectusecount.ObjectUseCountService.DEFAULT_ACCOUNT;
import com.google.common.collect.ImmutableSet;
import com.openexchange.java.Strings;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * {@link IncrementArguments} - Specifies arguments to use when incrementing use count(s).
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class IncrementArguments extends AbstractArguments {

    /**
     * Creates a builder with given user identifier.
     *
     * @param userId The user identifier
     * @return The new builder
     */
    public static Builder builderWithUserId(int userId) {
        return new Builder(userId);
    }

    /**
     * Creates a builder with given mail addresses.
     *
     * @param mailAddresses The mail addresses referencing contacts
     * @return The new builder
     */
    public static Builder builderWithMailAddresses(String... mailAddresses) {
        if (mailAddresses == null || mailAddresses.length <= 0) {
            throw new IllegalArgumentException("Mail addresses must not be null or empty");
        }
        return mailAddresses.length == 1 ? new Builder(mailAddresses[0]) : new Builder(ImmutableSet.copyOf(mailAddresses));
    }

    /**
     * Creates a builder with given mail addresses.
     *
     * @param mailAddresses The mail addresses referencing contacts
     * @return The new builder
     */
    public static Builder builderWithMailAddresses(Set<String> mailAddresses) {
        if (mailAddresses == null || mailAddresses.isEmpty()) {
            throw new IllegalArgumentException("Mail addresses must not be null or empty");
        }
        return new Builder(mailAddresses);
    }

    /**
     * Creates a builder with given internal object.
     *
     * @param objectId The object identifier
     * @param folderId The folder identifier
     * @return The new builder
     */
    public static Builder builderWithInternalObject(int objectId, int folderId) {
        return new Builder(objectId, folderId);
    }

    /**
     * Creates a builder with given object residing in specified account and folder.
     *
     * @param moduleId The module identifier
     * @param accountId The account identifier
     * @param folderId The folder identifier
     * @param objectId The object identifier
     * @return The new builders
     */
    public static Builder builderWithObject(int moduleId, int accountId, String folderId, String objectId) {
        return new Builder(moduleId, accountId, folderId, objectId);
    }

    /**
     * A builder for an {@code IncrementArguments} instance.
     */
    public static class Builder {

        private final int objectId;
        private final int folderId;
        private final int userId;
        private final Collection<String> mailAddresses;
        private final int moduleId;
        private final int accountId;
        private final String sObjectId;
        private final String sFolderId;
        private Connection con;
        private boolean throwException = false;

        /**
         * Initializes a new {@link IncrementArguments.Builder}.
         *
         * @param mailAddresses The mail addresses by which to look-up contacts to update
         */
        Builder(Set<String> mailAddresses) {
            super();
            this.mailAddresses = mailAddresses;
            this.userId = -1;
            this.objectId = -1;
            this.folderId = -1;
            this.moduleId = CONTACT_MODULE;
            this.accountId = DEFAULT_ACCOUNT;
            this.sFolderId = null;
            this.sObjectId = null;
        }

        /**
         * Initializes a new {@link IncrementArguments.Builder}.
         *
         * @param mailAddress The mail address by which to look-up the contact to update
         */
        Builder(String mailAddress) {
            super();
            this.mailAddresses = Collections.singleton(mailAddress);
            this.userId = -1;
            this.objectId = -1;
            this.folderId = -1;
            this.moduleId = CONTACT_MODULE;
            this.accountId = DEFAULT_ACCOUNT;
            this.sFolderId = null;
            this.sObjectId = null;
        }

        /**
         * Initializes a new {@link IncrementArguments.Builder}.
         *
         * @param userId The user identifier whose associated contact is supposed to be updated
         */
        Builder(int userId) {
            super();
            this.userId = userId;
            this.mailAddresses = null;
            this.objectId = -1;
            this.folderId = -1;
            this.moduleId = CONTACT_MODULE;
            this.accountId = DEFAULT_ACCOUNT;
            this.sFolderId = null;
            this.sObjectId = null;
        }

        /**
         * Initializes a new {@link IncrementArguments.Builder}.
         *
         * @param objectId The object identifier
         * @param folderId The folder identifier
         */
        Builder(int objectId, int folderId) {
            super();
            this.objectId = objectId;
            this.folderId = folderId;
            this.mailAddresses = null;
            this.userId = -1;
            this.moduleId = CONTACT_MODULE;
            this.accountId = DEFAULT_ACCOUNT;
            this.sFolderId = null;
            this.sObjectId = null;
        }

        /**
         * Initializes a new {@link Builder}.
         *
         * @param moduleId The module identifier
         * @param accountId The account identifier
         * @param folder The folder identifier
         * @param object The object identifier
         */
        Builder(int moduleId, int accountId, String folder, String object) {
            super();
            this.objectId = -1;
            this.folderId = -1;
            this.mailAddresses = null;
            this.userId = -1;
            this.moduleId = moduleId;
            this.accountId = accountId;
            this.sFolderId = folder;
            this.sObjectId = object;
        }

        /**
         * Sets whether an exception is supposed to be thrown or not. Default is <code>false</code>
         *
         * @param throwException The throwException to set
         * @return This builder
         */
        public Builder setThrowException(boolean throwException) {
            this.throwException = throwException;
            return this;
        }

        /**
         * Sets the connection
         *
         * @param con The connection to use or <code>null</code>
         * @return This builder
         */
        public Builder setCon(Connection con) {
            this.con = con;
            return this;
        }

        /**
         * Creates the appropriate {@code UpdateProperties} instance
         *
         * @return The instance
         */
        public IncrementArguments build() {
            if (DEFAULT_ACCOUNT != accountId && Strings.isNotEmpty(sFolderId) && Strings.isNotEmpty(sObjectId)) {
                return new IncrementArguments(con, moduleId, accountId, sFolderId, sObjectId, throwException);
            }
            int objectId = this.objectId;
            if (objectId < 0 && Strings.isNotEmpty(sObjectId)) {
                objectId = Strings.parsePositiveInt(sObjectId);
            }
            int folderId = this.folderId;
            if (folderId < 0 && Strings.isNotEmpty(sFolderId)) {
                folderId = Strings.parsePositiveInt(sFolderId);
            }
            return new IncrementArguments(con, mailAddresses, objectId, folderId, userId, throwException);
        }

    }

    // ----------------------------------------------------------------------------------------------------------------------- //

    private final Collection<String> mailAddresses;
    private final int objectId;
    private final int userId;
    private final int folderId;
    private final int moduleId;
    private final int accountId;
    private final String sObject;
    private final String sFolder;

    protected IncrementArguments(Connection con, Collection<String> mailAddresses, int objectId, int folderId, int userId, boolean throwException) {
        super(con, throwException);
        this.mailAddresses = mailAddresses;
        this.userId = userId;
        this.objectId = objectId;
        this.folderId = folderId;
        this.moduleId = CONTACT_MODULE;
        this.accountId = DEFAULT_ACCOUNT;
        this.sFolder = null;
        this.sObject = null;
    }

    protected IncrementArguments(Connection con, int moduleId, int accountId, String folder, String object, boolean throwException) {
        super(con, throwException);
        this.moduleId = moduleId;
        this.accountId = accountId;
        this.sFolder = folder;
        this.sObject = object;
        this.mailAddresses = null;
        this.objectId = -1;
        this.folderId = -1;
        this.userId = -1;
    }

    /**
     * Gets the mail addresses
     *
     * @return The mail addresses
     */
    public Collection<String> getMailAddresses() {
        return mailAddresses;
    }

    /**
     * Gets the object identifier
     *
     * @return The object identifier
     */
    public int getObjectId() {
        return objectId;
    }

    /**
     * Gets the folder identifier
     *
     * @return The folder identifier
     */
    public int getFolderId() {
        return folderId;
    }

    /**
     * Gets the user identifier
     *
     * @return The user identifier
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the module identifier
     *
     * @return The module identifier
     */
    public int getModuleId() {
        return moduleId;
    }

    /**
     * Gets the account identifier
     *
     * @return The account identifier
     */
    public int getAccountId() {
        return accountId;
    }

    /**
     * Gets the non-numeric folder identifier
     *
     * @return The non-numeric folder identifier
     */
    public String getFolder() {
        return sFolder;
    }

    /**
     * Gets the non-numeric object identifier
     *
     * @return The non-numeric object identifier
     */
    public String getObject() {
        return sObject;
    }

}
