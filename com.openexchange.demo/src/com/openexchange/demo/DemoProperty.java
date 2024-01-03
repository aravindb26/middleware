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

package com.openexchange.demo;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import com.openexchange.config.lean.Property;

/**
 * {@link DemoProperty}
 *
 * @author <a href="mailto:nikolaos.tsapanidis@open-xchange.com">Nikolaos Tsapanidis</a>
 * @since v8.0.0
 */
public enum DemoProperty implements Property {

    /**
     * Whether Demo is enabled or not.
     */
    ENABLED("enabled", Boolean.FALSE),

    /**
     * Specifies the master admin username
     */
    MASTER_ADMIN_USERNAME("provisioning.masterAdminUser", "oxadminmaster"),

    /**
     * Specifies the master admin password
     */
    MASTER_ADMIN_PASSWORD("provisioning.masterAdminPass", "secret"),

    /**
     * Specifies a list of users which should be created.
     * <p>
     * <b>Note:</b>
     * If the list is empty then random user will be generated.
     * </p>
     */
    USER_LIST("provisioning.userList", ""),

    /**
     * The password of the users (even for oxadmin)
     */
    USER_PASSWORD("provisioning.userPassword", "secret"),

    /**
     * List of user locales which should be used to provision the user
     */
    USER_LOCALE("provisioning.userLocale", "en_US, de_DE"),

    /**
     * Whether a user image should be generated or not.
     */
    USER_IMAGE("provisioning.userImage", Boolean.FALSE),

    /**
     * The mail domain part
     */
    MAIL_DOMAIN("provisioning.mailDomain", "ox.test"),

    /**
     * How many users should be created per context.
     * <p>
     * <b>Note:</b> only relevant if <code>com.openexchange.demo.provisioning.randomUser</code> is enabled
     * </p>
     */
    USER_PER_CONTEXT("provisioning.userPerContext", I(1)),

    /**
     * How many context should be created.
     */
    NUMBER_OF_CONTEXT("provisioning.numberOfContext", I(1)),

    /**
     * The host or ip of the read host
     */
    DB_READ_HOST("database.readHost", ""),

    /**
     * The host or ip of the write host
     */
    DB_WRITE_HOST("database.writeHost", "localhost"),

    /**
     * The database user of the read host
     */
    DB_READ_USER("database.readUser", ""),

    /**
     * The database user of the write host
     */
    DB_WRITE_USER("database.writeUser", "openexchange"),

    /**
     * The password of the read host
     */
    DB_READ_PASSWORD("database.readPassword", ""),

    /**
     * The password of the write host
     */
    DB_WRITE_PASSWORD("database.writePassword", "secret"),

    /**
     * The name of the user db
     */
    USER_DB_NAME("database.userdb", "oxdatabase"),

    /**
     * The name of the global db
     */
    GLOBAL_DB_NAME("database.globaldb.name", "oxglobal"),

    /**
     * Whether registering global db.
     */
    GLOBAL_DB("database.globaldb.enabled", Boolean.FALSE),

    /**
     * The scheme part of the filestore URI.
     * <p>
     * Possible values:
     * <ul>
     * <li><code>file</code></li>
     * <li><code>s3</code></li>
     * <li><code>sproxyd</code></li>
     * </ul>
     */
    FILESTORE_SCHEME("filestore.scheme", "file"),

    /**
     * The filestore path
     */
    FILESTORE_PATH("filestore.path", "filestore"),

    /**
     * The size of the filestore
     */
    FILESTORE_SIZE("filestore.size", L(1000)),

    /**
     * Whether a gdprstore should be registered.
     */
    GDPRSTORE_ENABLED("gdprstore.enabled", Boolean.FALSE),

    /**
     * The gdprstore path
     */
    GDPRSTORE_PATH("gdprstore.path", "gdprstore"),

    /**
     * The size of the gdprstore
     */
    GDPRSTORE_SIZE("gdprstore.size", L(1000)),

    /**
     * Whether a fileitemstore, which is needed by the <code>FileItemService/ImageConverter</code>,
     * should be registered or not.
     */
    FILEITEMSTORE_ENABLED("fileitemstore.enabled", Boolean.FALSE),

    /**
     * The fileitemstore path
     */
    FILEITEMSTORE_PATH("fileitemstore.path", "fileitemstore"),

    /**
     * The size of the fileitemstore
     */
    FILEITEMSTORE_SIZE("fileitemstore.size", L(1000)),
    ;

    private static final String PREFIX = "com.openexchange.demo.";

    private final Object defaultValue;
    private final String fqn;

    private DemoProperty(String fqn, Object defaultValue) {
        this.fqn = PREFIX + fqn;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }
}
