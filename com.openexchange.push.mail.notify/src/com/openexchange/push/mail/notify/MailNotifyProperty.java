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
package com.openexchange.push.mail.notify;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import com.openexchange.config.lean.Property;

/**
 * {@link MailNotifyProperty}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public enum MailNotifyProperty implements Property {

    /**
     * Define if the listener should receive multicast messages.
     */
    udp_listen_multicast(Boolean.FALSE),

    /**
     * Define the hostname or interface/multicast group where the udp server should listen.
     */
    udp_listen_host("localhost"),

    /**
     * Define the port where the udp server should listen.
     */
    udp_listen_port(I(23420)),

    /**
     * Separates user and host in imap login 
     */
    imap_login_delimiter(null),

    /**
     * Whether to use the ox login name to check for a valid push event.
      The default is to only check the users aliases.
      If "mailboxname@example.com" is not contained in the list of aliases,
      set this to <code>true</code>.
      Warning:
      This won't work in multidomain setups where the same login
      might exist in different contexts!
     */
    use_ox_login(Boolean.FALSE),

    /**
     * Whether to use the full email address from aliases or just use the localpart.
       When using a multidomain setup where the imap login is an email address,
       this should be set to <code>true</code>.
       If not, login might not be unique because 'foo@example.com' and 'foo@example.net'
       might be different users.
       Note:
       Do <b>not</b> set com.openexchange.push.mail.notify.imap_login_delimiter in this case!
     */
    use_full_email_address(Boolean.FALSE),

    /**
     * The delay for pooled notifications.
     */
    delay_millis(L(5000)),
    ;

    private final static String PREFIX = "com.openexchange.push.mail.notify.";
    private final Object defaultValue;

    private MailNotifyProperty(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + name();
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
