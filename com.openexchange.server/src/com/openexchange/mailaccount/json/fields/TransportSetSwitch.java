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

package com.openexchange.mailaccount.json.fields;

import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.mailaccount.AttributeSwitch;
import com.openexchange.mailaccount.TransportAccountDescription;
import com.openexchange.mailaccount.TransportAuth;

/**
 * {@link TransportSetSwitch}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class TransportSetSwitch implements AttributeSwitch {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TransportSetSwitch.class);

    private final TransportAccountDescription desc;

    private Object value;

    /**
     * Initializes a new {@link TransportSetSwitch}.
     *
     * @param desc The account description
     */
    public TransportSetSwitch(final TransportAccountDescription desc) {
        super();
        this.desc = desc;
    }

    public void setValue(final Object value) {
        this.value = value == JSONObject.NULL ? null : value;
    }

    @Override
    public Object replyTo() {
        desc.setReplyTo((String) value);
        return null;
    }

    @Override
    public Object confirmedHam() {
        return null;
    }

    @Override
    public Object confirmedSpam() {
        return null;
    }

    @Override
    public Object drafts() {
        return null;
    }

    @Override
    public Object id() {
        desc.setId(((Integer) value).intValue());
        return null;
    }

    @Override
    public Object secondaryAccount() {
        desc.setSecondaryAccount(((Boolean) value).booleanValue());
        return null;
    }

    @Override
    public Object deactivated() {
        desc.setDeactivated(((Boolean) value).booleanValue());
        return null;
    }

    @Override
    public Object login() {
        return null;
    }

    @Override
    public Object mailURL() throws OXException {
        return null;
    }

    @Override
    public Object name() {
        desc.setName((String) value);
        return null;
    }

    @Override
    public Object password() {
        return null;
    }

    @Override
    public Object primaryAddress() {
        desc.setPrimaryAddress((String) value);
        return null;
    }

    @Override
    public Object personal() {
        desc.setPersonal((String) value);
        return null;
    }

    @Override
    public Object sent() {
        return null;
    }

    @Override
    public Object spam() {
        return null;
    }

    @Override
    public Object spamHandler() {
        return null;
    }

    @Override
    public Object transportURL() throws OXException {
        desc.parseTransportServerURL((String) value);
        return null;
    }

    @Override
    public Object trash() {
        return null;
    }

    @Override
    public Object archive() {
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
        try {
            desc.setTransportPort(Integer.parseInt(value.toString()));
        } catch (NumberFormatException e) {
            LOG.debug("Transport port is not a number: {}. Setting to fallback port 25.", value,
                e);
            desc.setTransportPort(25);
        }
        return null;
    }

    @Override
    public Object transportProtocol() {
        desc.setTransportProtocol((String) value);
        return null;
    }

    @Override
    public Object transportSecure() {
        desc.setTransportSecure(Boolean.parseBoolean(value.toString()));
        return null;
    }

    @Override
    public Object transportServer() {
        desc.setTransportServer((String) value);
        return null;
    }

    @Override
    public Object transportLogin() {
        desc.setTransportLogin((String) value);
        return null;
    }

    @Override
    public Object transportPassword() {
        desc.setTransportPassword((String) value);
        return null;
    }

    @Override
    public Object unifiedINBOXEnabled() {
        return null;
    }

    @Override
    public Object confirmedHamFullname() {
        return null;
    }

    @Override
    public Object confirmedSpamFullname() {
        return null;
    }

    @Override
    public Object draftsFullname() {
        return null;
    }

    @Override
    public Object sentFullname() {
        return null;
    }

    @Override
    public Object spamFullname() {
        return null;
    }

    @Override
    public Object trashFullname() {
        return null;
    }

    @Override
    public Object archiveFullname() {
        return null;
    }

    @Override
    public Object transportAuth() {
        if (value instanceof TransportAuth) {
            desc.setTransportAuth((TransportAuth) value);
        } else {
            TransportAuth tmp = null == value ? null : TransportAuth.transportAuthFor(value.toString());
            desc.setTransportAuth(tmp);
        }
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
    public Object mailStartTls() {
        return null;
    }

    @Override
    public Object transportStartTls() {
        desc.setTransportStartTls(Boolean.parseBoolean(value.toString()));
        return null;
    }

    @Override
    public Object mailOAuth() {
        return null;
    }

    @Override
    public Object transportOAuth() {
        desc.setTransportOAuthId(value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString()));
        return null;
    }

    @Override
    public Object rootFolder() {
        return null;
    }

    @Override
    public Object mailDisabled() {
        return null;
    }

    @Override
    public Object transportDisabled() {
        desc.setTransportDisabled(Boolean.parseBoolean(value.toString()));
        return null;
    }

}
