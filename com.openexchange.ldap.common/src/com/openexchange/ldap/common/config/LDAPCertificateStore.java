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

package com.openexchange.ldap.common.config;

import static com.openexchange.ldap.common.config.ConfigUtils.opt;
import static com.openexchange.java.Autoboxing.b;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;

/**
 *
 * {@link LDAPCertificateStore} is a wrapper for certificate stores
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LDAPCertificateStore {

    private final Optional<File> file;
    private final Optional<String> password;
    private final Optional<String> id;

    // keystore specific options
    private final Optional<String> alias;

    // Truststore specific options
    private final Optional<Boolean> examineValidityDates;
    private final Optional<Boolean> trustAll;

    /**
     * Creates a new {@link LDAPCertificateStore}
     *
     * @param configEntry The configuration entry
     * @return The new {@link LDAPCertificateStore}
     * @throws OXException in case the configuration is invalid
     */
    public static LDAPCertificateStore init(Map<String, Object> configEntry) throws OXException {
        // @formatter:off
        return new LDAPCertificateStore(opt(configEntry, "id"),
                                        toFile(opt(configEntry, "file")),
                                        opt(configEntry, "password"),
                                        opt(configEntry, "alias"),
                                        opt(configEntry, "examineValidityDates", Boolean.class),
                                        opt(configEntry, "trustAll", Boolean.class));
        // @formatter:on
    }

    /**
     * Initializes a new {@link LDAPCertificateStore}.
     *
     * @param id The id of the store
     * @param file The store file
     * @param password The store password
     * @param alias The certificate alias
     * @param examineValidityDates Whether to examine validity dates or not
     * @param trustAll Whether to use a trust all store
     * @throws OXException in case the configuration is invalid
     */
    private LDAPCertificateStore(String id, File file, String password, String alias, Boolean examineValidityDates, Boolean trustAll) throws OXException {
        super();
        this.id = Optional.ofNullable(id);
        this.file = Optional.ofNullable(file);
        this.password = Optional.ofNullable(password);
        this.alias = Optional.ofNullable(alias);
        this.examineValidityDates = Optional.ofNullable(examineValidityDates);
        this.trustAll = Optional.ofNullable(trustAll);
        // either a id, a file or trust all must be present
        if (false == (this.file.isPresent() || this.id.isPresent() || isTrustAll())) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create();
        }
    }

    /**
     * Checks if trust all is set to <code>true</code>
     *
     * @return <code>true</code> if trust all is present and set to <code>true</code>, <code>false</code> otherwise
     */
    private boolean isTrustAll() {
        return b(trustAll.orElse(Boolean.FALSE));
    }

    /**
     * Turns the given path to a file and checks if the file exists
     *
     * @param path The path to the file or null
     * @return The file or null in case no path was given
     * @throws OXException In case the file doesn't exists
     */
    private static File toFile(String path) throws OXException {
        if (path == null) {
            return null;
        }
        File result = new File(path);
        if (result.exists() == false) {
            throw LDAPCommonErrorCodes.INVALID_CONFIG.create();
        }
        return result;
    }

    /**
     * Gets the optional file
     *
     * @return The optional file
     */
    public Optional<File> optFile() {
        return file;
    }

    /**
     * Gets the optional password
     *
     * @return The optional password
     */
    public Optional<String> optPassword() {
        return password;
    }

    /**
     * Gets the optional alias
     *
     * @return The optional alias
     */
    public Optional<String> optAlias() {
        return alias;
    }

    /**
     * Gets the optional id
     *
     * @return The optional id
     */
    public Optional<String> optId() {
        return id;
    }

    /**
     * Gets the optional examineValidityDates
     *
     * @return The optional examineValidityDates
     */
    public Optional<Boolean> optExamineValidityDates() {
        return examineValidityDates;
    }

    /**
     * Gets the optional trustAll
     *
     * @return The optional trustAll
     */
    public Optional<Boolean> optTrustAll() {
        return trustAll;
    }


}