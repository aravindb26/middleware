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

package com.openexchange.mailaccount.internal;

import java.util.EnumSet;
import java.util.Set;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.mailaccount.AttributeSwitch;

/**
 * {@link UpdateMailAccountBuilder}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class UpdateMailAccountBuilder implements AttributeSwitch {

    private static final Set<Attribute> KNOWN_ATTRIBUTES = EnumSet.complementOf(EnumSet.of(
        Attribute.ID_LITERAL,
        Attribute.SECONDARY_ACCOUNT_LITERAL,
        Attribute.TRANSPORT_URL_LITERAL,
        Attribute.TRANSPORT_LOGIN_LITERAL,
        Attribute.TRANSPORT_PASSWORD_LITERAL,
        Attribute.TRANSPORT_STARTTLS_LITERAL,
        Attribute.TRANSPORT_OAUTH_LITERAL,
        Attribute.TRANSPORT_DISABLED));

    private static final Set<Attribute> PROPERTY_ATTRIBUTES = EnumSet.of(
        Attribute.POP3_DELETE_WRITE_THROUGH_LITERAL,
        Attribute.POP3_EXPUNGE_ON_QUIT_LITERAL,
        Attribute.POP3_PATH_LITERAL,
        Attribute.POP3_REFRESH_RATE_LITERAL,
        Attribute.POP3_STORAGE_LITERAL,
        Attribute.TRANSPORT_AUTH_LITERAL);

    public static boolean needsUpdate(final Set<Attribute> attributes) {
        for (final Attribute attribute : attributes) {
            if (KNOWN_ATTRIBUTES.contains(attribute)) {
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------------------------------------------

    private final StringBuilder bob;
    private boolean valid;
    private boolean injectClearingFailAuthCount;
    private boolean injectClearingDisabled;

    /**
     * Initializes a new {@link UpdateMailAccountBuilder}.
     */
    public UpdateMailAccountBuilder() {
        super();
        bob = new StringBuilder("UPDATE user_mail_account SET ");
        valid = false;
        injectClearingFailAuthCount = false;
        injectClearingDisabled = false;
    }

    /**
     * Checks if this SQL builder has valid content
     *
     * @return <code>true</code> if valid; otherwise <code>false</code>
     */
    public boolean isValid() {
        return valid || injectClearingFailAuthCount || injectClearingDisabled;
    }

    /**
     * Checks whether clearing of failed authentication counter has been injected
     *
     * @return <code>true</code> if injected; otherwise <code>false</code>
     */
    public boolean isInjectClearingFailAuthCount() {
        return injectClearingFailAuthCount;
    }

    /**
     * Checks if this SQL builder handles given attribute.
     *
     * @param attribute The attribute to check
     * @return <code>true</code> if able to handle; otherwise <code>false</code>
     */
    public boolean handles(final Attribute attribute) {
        return KNOWN_ATTRIBUTES.contains(attribute) && !PROPERTY_ATTRIBUTES.contains(attribute);
    }

    /**
     * Gets the prepared SQL statement.
     *
     * @return The prepared SQL statement
     * @see #isValid()
     */
    public String getUpdateQuery() {
        if (injectClearingFailAuthCount) {
            bob.append("failed_auth_count=0,failed_auth_date=0,");
        }
        if (injectClearingDisabled) {
            bob.append("disabled=0,");
        }
        bob.setLength(bob.length() - 1);
        bob.append(" WHERE cid = ? AND id = ? AND user = ?");
        return bob.toString();
    }

    @Override
    public String toString() {
        return bob.toString();
    }

    @Override
    public Object confirmedHam() {
        bob.append("confirmed_ham = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object confirmedSpam() {
        bob.append("confirmed_spam = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object drafts() {
        bob.append("drafts = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object id() {
        return null;
    }

    @Override
    public Object secondaryAccount() {
        return null;
    }

    @Override
    public Object deactivated() {
        bob.append("deactivated = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object login() {
        bob.append("login = ?,");
        valid = true;
        injectClearingFailAuthCount = true;
        injectClearingDisabled = true;
        return null;
    }

    @Override
    public Object mailURL() {
        bob.append("url = ?,");
        injectClearingFailAuthCount = true;
        injectClearingDisabled = true;
        valid = true;
        return null;
    }

    @Override
    public Object name() {
        bob.append("name = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object password() {
        bob.append("password = ?,");
        valid = true;
        injectClearingFailAuthCount = true;
        injectClearingDisabled = true;
        return null;
    }

    @Override
    public Object primaryAddress() {
        bob.append("primary_addr = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object personal() {
        bob.append("personal = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object replyTo() {
        bob.append("replyTo = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object sent() {
        bob.append("sent = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object spam() {
        bob.append("spam = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object spamHandler() {
        bob.append("spam_handler = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object transportURL() {
        return null;
    }

    @Override
    public Object trash() {
        bob.append("trash = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object archive() {
        bob.append("archive = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object mailPort() {
        return null;
    }

    @Override
    public Object mailProtocol() {
        return null;
    }

    @Override
    public Object mailSecure() {
        return null;
    }

    @Override
    public Object mailServer() {
        return null;
    }

    @Override
    public Object transportPort() {
        return null;
    }

    @Override
    public Object transportProtocol() {
        return null;
    }

    @Override
    public Object transportSecure() {
        return null;
    }

    @Override
    public Object transportServer() {
        return null;
    }

    @Override
    public Object transportLogin() {
        return null;
    }

    @Override
    public Object transportPassword() {
        return null;
    }

    @Override
    public Object unifiedINBOXEnabled() {
        bob.append("unified_inbox = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object confirmedHamFullname() {
        bob.append("confirmed_ham_fullname = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object confirmedSpamFullname() {
        bob.append("confirmed_spam_fullname = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object draftsFullname() {
        bob.append("drafts_fullname = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object sentFullname() {
        bob.append("sent_fullname = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object spamFullname() {
        bob.append("spam_fullname = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object trashFullname() {
        bob.append("trash_fullname = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object archiveFullname() {
        bob.append("archive_fullname = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object pop3DeleteWriteThrough() {
        return null;
    }

    @Override
    public Object pop3ExpungeOnQuit() {
        return null;
    }

    @Override
    public Object pop3RefreshRate() {
        return null;
    }

    @Override
    public Object pop3Path() {
        return null;
    }

    @Override
    public Object pop3Storage() {
        return null;
    }

    @Override
    public Object addresses() {
        return null;
    }

    @Override
    public Object transportAuth() {
        return null;
    }

    @Override
    public Object mailStartTls() {
        bob.append("starttls = ?,");
        valid = true;
        return null;
    }

    @Override
    public Object transportStartTls() {
        return null;
    }

    @Override
    public Object mailOAuth() {
        bob.append("oauth = ?,");
        valid = true;
        injectClearingFailAuthCount = true;
        injectClearingDisabled = true;
        return null;
    }

    @Override
    public Object transportOAuth() {
        return null;
    }

    @Override
    public Object rootFolder() {
        return null;
    }

    @Override
    public Object mailDisabled() {
        bob.append("disabled = ?,");
        injectClearingDisabled = false;
        valid = true;
        return null;
    }

    @Override
    public Object transportDisabled() {
        return null;
    }

}
