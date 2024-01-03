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

import java.util.Collections;
import java.util.Map;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.NoTransportMailAccount;
import com.openexchange.mailaccount.TransportAuth;

/**
 * {@link NoTransportMailAccountImpl} is a {@link MailAccount} which delegates all requests to another MailAccount except the transport properties.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8
 */
public class NoTransportMailAccountImpl implements NoTransportMailAccount {

    private static final long serialVersionUID = 1277241926914134556L;

    private final MailAccount delegate;

    /**
     * Initializes a new {@link NoTransportMailAccountImpl}.
     *
     * @param delegate The underlying {@link MailAccount}
     */
    public NoTransportMailAccountImpl(MailAccount delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public String generateTransportServerURL() {
        return null;
    }

    @Override
    public int getId() {
        return delegate.getId();
    }

    @Override
    public String getLogin() {
        return delegate.getLogin();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public String getPrimaryAddress() {
        return delegate.getPrimaryAddress();
    }

    @Override
    public String getPersonal() {
        return delegate.getPersonal();
    }

    @Override
    public String getReplyTo() {
        return delegate.getReplyTo();
    }

    @Override
    public int getTransportPort() {
        return 25;
    }

    @Override
    public String getTransportProtocol() {
        return "smtp";
    }

    @Override
    public String getTransportServer() {
        return null;
    }

    @Override
    public boolean isTransportSecure() {
        return false;
    }

    @Override
    public boolean isDefaultAccount() {
        return delegate.isDefaultAccount();
    }

    @Override
    public boolean isTransportStartTls() {
        return false;
    }

    @Override
    public boolean isTransportDisabled() {
        return true;
    }

    @Override
    public boolean isSecondaryAccount() {
        return delegate.isSecondaryAccount();
    }

    @Override
    public boolean isDeactivated() {
        return delegate.isDeactivated();
    }

    @Override
    public int getUserId() {
        return delegate.getUserId();
    }

    @Override
    public String generateMailServerURL() {
        return delegate.generateMailServerURL();
    }

    @Override
    public String getMailServer() {
        return delegate.getMailServer();
    }

    @Override
    public int getMailPort() {
        return delegate.getMailPort();
    }

    @Override
    public String getMailProtocol() {
        return delegate.getMailProtocol();
    }

    @Override
    public boolean isMailSecure() {
        return delegate.isMailSecure();
    }

    @Override
    public boolean isMailOAuthAble() {
        return delegate.isMailOAuthAble();
    }

    @Override
    public int getMailOAuthId() {
        return delegate.getMailOAuthId();
    }

    @Override
    public boolean isMailDisabled() {
        return delegate.isMailDisabled();
    }

    @Override
    public TransportAuth getTransportAuth() {
        return TransportAuth.MAIL;
    }

    @Override
    public String getTransportLogin() {
        return delegate.getLogin();
    }

    @Override
    public String getTransportPassword() {
        return getPassword();
    }

    @Override
    public boolean isTransportOAuthAble() {
        return false;
    }

    @Override
    public int getTransportOAuthId() {
        return -1;
    }

    @Override
    public String getSpamHandler() {
        return delegate.getSpamHandler();
    }

    @Override
    public String getDrafts() {
        return delegate.getDrafts();
    }

    @Override
    public String getSent() {
        return delegate.getSent();
    }

    @Override
    public String getSpam() {
        return delegate.getSpam();
    }

    @Override
    public String getTrash() {
        return delegate.getTrash();
    }

    @Override
    public String getArchive() {
        return delegate.getArchive();
    }

    @Override
    public String getConfirmedHam() {
        return delegate.getConfirmedHam();
    }

    @Override
    public String getConfirmedSpam() {
        return delegate.getConfirmedSpam();
    }

    @Override
    public boolean isUnifiedINBOXEnabled() {
        return delegate.isUnifiedINBOXEnabled();
    }

    @Override
    public String getTrashFullname() {
        return delegate.getTrashFullname();
    }

    @Override
    public String getArchiveFullname() {
        return delegate.getArchiveFullname();
    }

    @Override
    public String getSentFullname() {
        return delegate.getSentFullname();
    }

    @Override
    public String getDraftsFullname() {
        return delegate.getDraftsFullname();
    }

    @Override
    public String getSpamFullname() {
        return delegate.getSpamFullname();
    }

    @Override
    public String getConfirmedSpamFullname() {
        return delegate.getConfirmedSpamFullname();
    }

    @Override
    public String getConfirmedHamFullname() {
        return delegate.getConfirmedHamFullname();
    }

    @Override
    public Map<String, String> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public void addProperty(String name, String value) {
        delegate.addProperty(name, value);
    }

    @Override
    public Map<String, String> getTransportProperties() {
        return Collections.emptyMap();
    }

    @Override
    public void addTransportProperty(String name, String value) {
        // Do nothing
    }

    @Override
    public boolean isMailStartTls() {
        return delegate.isMailStartTls();
    }

    @Override
    public String getRootFolder() {
        return delegate.getRootFolder();
    }

}
