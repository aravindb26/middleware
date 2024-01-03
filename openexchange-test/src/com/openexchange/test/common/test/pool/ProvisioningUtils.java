/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the OX Software GmbH. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2016-2020 OX Software GmbH
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.test.common.test.pool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.mail.internet.AddressException;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.exception.OXException;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.test.common.configuration.AJAXConfig;

/**
 * {@link ProvisioningUtils}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class ProvisioningUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ProvisioningUtils.class);

    public static final String CTX_SECRET = AJAXConfig.getProperty(AJAXConfig.Property.CONTEXT_ADMIN_PASSWORD, "secret");
    public static final String CTX_ADMIN = AJAXConfig.getProperty(AJAXConfig.Property.CONTEXT_ADMIN_USER, "oxadmin");
    public static final Credentials CONTEXT_CREDS = new Credentials(CTX_ADMIN, CTX_SECRET);
    public static final String USER_SECRET = AJAXConfig.getProperty(AJAXConfig.Property.USER_PASSWORD, "secret");
    public static final Long DEFAULT_MAX_QUOTA = Long.valueOf(-1);
    public static final String USER_NAMES = AJAXConfig.getProperty(AJAXConfig.Property.USER_NAMES, "anton,berta,caesar,dora,emil");
    public static final String[] USER_NAMES_POOL = USER_NAMES.split(",");

    protected static final String MAIL_NAME_FORMAT = "%s@context%s.ox.test";
    protected static final String CONTEXT_NAME_FORMAT = "context%s.ox.test";

    /**
     * Gets a context name for the given context id
     *
     * @param cid The context id
     * @return The context name
     */
    public static String getContextName(int cid) {
        return String.format(CONTEXT_NAME_FORMAT, String.valueOf(cid));
    }

    /**
     * Gets a mail address for the given login and context id
     *
     * @param login The login
     * @param cid The context id
     * @return The mail address
     */
    public static String getMailAddress(String login, int cid) {
        return String.format(MAIL_NAME_FORMAT, login, String.valueOf(cid));
    }

    /**
     * Creates a new {@link TestContext} with the given id and admin
     *
     * @param cid The context id
     * @param admin The admin
     * @param createdBy The class name that creates the context
     * @param users The users within the context
     * @return The new {@link TestContext}
     */
    public static TestContext toTestContext(int cid, TestUser admin, String createdBy, List<TestUser> users) {
        return new TestContext(cid, getContextName(cid), admin, createdBy, users);
    }

    /**
     * Creates a {@link TestUser} from the given informations
     *
     * @param contextName The context name
     * @param userName The name of the user
     * @param userPassword The password of the user
     * @param userId The user id
     * @param ctxId The context id
     * @return
     */
    public static TestUser userToTestUser(String contextName, String userName, String userPassword, Integer userId, Integer ctxId, String createdBy) {
        return new TestUser(userName, contextName, userPassword, userId, ctxId, createdBy);
    }

    /**
     * Creates a {@link User} object from the given data
     *
     * @param namehe name of the user
     * @param passwd The users's password
     * @param displayName The display name of the user
     * @param givenName The given name of the user
     * @param surname The surname of the user
     * @param email The email address of the user
     * @param config User attributes of the user
     * @param quota The quota for the user
     * @return The {@link User} object
     * @throws OXException In case email validation fails
     */
    public static User createUser(String name, String passwd, String displayName, String givenName, String surname, String email, Optional<Map<String, String>> config, Optional<Long> quota) throws OXException {
        Objects.requireNonNull(name);
        Objects.requireNonNull(passwd);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(givenName);
        Objects.requireNonNull(surname);
        Objects.requireNonNull(email);
        // Check for valid address
        try {
            Objects.requireNonNull(new QuotedInternetAddress(email));
        } catch (AddressException e) {
            LOG.debug("Unable to parse email address.", e);
            throw ProvisioningExceptionCode.UNABLE_TO_PARSE_ADDRESS.create(email);
        }

        User user = new User();
        user.setName(name);
        user.setPassword(passwd);
        user.setDisplay_name(displayName);
        user.setGiven_name(givenName);
        user.setSur_name(surname);
        user.setPrimaryEmail(email);
        user.setEmail1(email);
        user.setImapLogin(email);
        quota.ifPresent(q -> user.setMaxQuota(q));

        if (config.isPresent()) {
            Map<String, Map<String, String>> nsConfig = new HashMap<>(1);
            nsConfig.put("config", config.get());
            user.setUserAttributes(nsConfig);
        }
        return user;
    }

    /**
     * Creates an administrator user 
     *
     * @param cid The context of the user
     * @return The administrator as {@link User}
     * @throws OXException See {@link #createUser(String, String, String, String, String, String, Optional, Optional)}
     */
    public static User createAdminUser(int cid) throws OXException {
        return createUser(CTX_ADMIN, CTX_SECRET, CTX_ADMIN, CTX_ADMIN, CTX_ADMIN, ProvisioningUtils.getMailAddress(ProvisioningUtils.CTX_ADMIN, cid), Optional.empty(), Optional.empty());
    }

    /**
     * Generates a mail address for the admin
     *
     * @return The mail address
     */
    public static String generateAdminMailAddress() {
        return String.format(MAIL_NAME_FORMAT, ProvisioningUtils.CTX_ADMIN, RandomStringUtils.randomAlphanumeric(10));
    }

}
