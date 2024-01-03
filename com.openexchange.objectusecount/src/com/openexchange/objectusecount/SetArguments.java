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
import java.sql.Connection;
import com.openexchange.java.Strings;

/**
 * {@link SetArguments} - Specifies arguments to use when setting use count(s).
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class SetArguments extends AbstractArguments {

    /**
     * Creates a builder with given internal object.
     *
     * @param objectId The object identifier
     * @param folderId The folder identifier
     * @param value The value to set
     * @return The new builder
     */
    public static Builder builderWithInternalObject(int objectId, int folderId, int value) {
        return new Builder(objectId, folderId, value);
    }

    /**
     * Creates a builder with given object residing in specified account and folder.
     *
     * @param moduleId The module identifier
     * @param accountId The account identifier
     * @param folder The folder identifier
     * @param object The object identifier
     * @param value The value to set
     * @return The new builder
     */
    public static Builder builderWithObject(int moduleId, int accountId, String folder, String object, int value) {
        return new Builder(moduleId, accountId, folder, object, value);
    }

    /**
     * A builder for an {@code IncrementArguments} instance.
     */
    public static class Builder {

        private final int objectId;
        private final int folderId;
        private final int moduleId;
        private final int accountId;
        private final String sObject;
        private final String sFolder;
        private final int value;
        private Connection con;
        private boolean throwException = false;

        /**
         * Initializes a new {@link SetArguments.Builder}.
         *
         * @param objectId The object identifier
         * @param folderId The folder identifier
         */
        Builder(int objectId, int folderId, int value) {
            super();
            this.objectId = objectId;
            this.folderId = folderId;
            this.moduleId = CONTACT_MODULE;
            this.accountId = DEFAULT_ACCOUNT;
            this.sFolder = null;
            this.sObject = null;
            this.value = value;
        }

        /**
         * Initializes a new {@link SetArguments.Builder}.
         *
         * @param objectId The object identifier
         * @param folderId The folder identifier
         */
        Builder(int moduleId, int accountId, String sFolder, String sObject, int value) {
            super();
            this.objectId = -1;
            this.folderId = -1;
            this.moduleId = moduleId;
            this.accountId = accountId;
            this.sFolder = sFolder;
            this.sObject = sObject;
            this.value = value;
        }

        /**
         * Sets whether an exception is supposed to be thrown or not. Default is <code>false</code>.
         *
         * @param throwException The throwException to set
         * @return This builder
         */
        public Builder setThrowException(boolean throwException) {
            this.throwException = throwException;
            return this;
        }

        /**
         * Sets the connection.
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
        public SetArguments build() {
            if (DEFAULT_ACCOUNT != accountId && Strings.isNotEmpty(sFolder) && Strings.isNotEmpty(sObject)) {
                return new SetArguments(con, moduleId, accountId, sFolder, sObject, value, throwException);
            }
            return new SetArguments(con, objectId, folderId, value, throwException);
        }

    }

    // ----------------------------------------------------------------------------------------------------------------------- //

    private final int objectId;
    private final int folderId;
    private final int module;
    private final int accountId;
    private final String sObject;
    private final String sFolder;
    private final int value;

    SetArguments(Connection con, int objectId, int folderId, int value, boolean throwException) {
        super(con, throwException);
        this.value = value;
        this.objectId = objectId;
        this.folderId = folderId;
        this.module = CONTACT_MODULE;
        this.accountId = DEFAULT_ACCOUNT;
        this.sFolder = null;
        this.sObject = null;
    }

    SetArguments(Connection con, int module, int accountId, String sFolder, String sObject, int value, boolean throwException) {
        super(con, throwException);
        this.value = value;
        this.objectId = -1;
        this.folderId = -1;
        this.module = module;
        this.accountId = accountId;
        this.sFolder = sFolder;
        this.sObject = sObject;
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
     * Gets the module identifier
     *
     * @return The module identifier
     */
    public int getModule() {
        return module;
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
     * @return The folder identifier
     */
    public String getFolder() {
        return sFolder;
    }

    /**
     * Gets the non-numeric object identifier
     *
     * @return The object identifier
     */
    public String getObject() {
        return sObject;
    }

    /**
     * Gets the value
     *
     * @return The value
     */
    public int getValue() {
        return value;
    }

}
