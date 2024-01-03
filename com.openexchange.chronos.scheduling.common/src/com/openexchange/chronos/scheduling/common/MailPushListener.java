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

package com.openexchange.chronos.scheduling.common;

import com.openexchange.exception.OXException;
import com.openexchange.mail.dataobjects.MailMessage;

/**
 * 
 * {@link MailPushListener}
 * Interface to push Mails to the calendar.
 *
 * @author <a href="mailto:martin.herfurthr@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.6
 */
public interface MailPushListener {

    /**
     * Pushes the mail to the calendar depending on the configuration and the content of the mail.
     *
     * @param mail The mail to push
     * @throws OXException
     */
    void pushMail(PushMail mail) throws OXException;

    /**
     * 
     * {@link PushMail}
     * Container class.
     *
     * @author <a href="mailto:martin.herfurthr@open-xchange.com">Martin Herfurth</a>
     * @since v7.10.6
     */
    public class PushMail {

        private MailMessage mail;
        private String event;
        private String user;

        /**
         * Gets the mail
         *
         * @return The mail or <code>null</code>
         */
        public MailMessage getMail() {
            return mail;
        }

        /**
         * Set the mail
         *
         * @param mail The mail
         */
        public void setMail(MailMessage mail) {
            this.mail = mail;
        }

        /**
         * Get the message event.
         * <p>
         * An RFC5423 event is expected. Currently only <code>MessageNew</code> is expected
         *
         * @return The message event or <code>null</code>
         */
        public String getEvent() {
            return event;
        }

        /**
         * Set the message event
         * <p>
         * An RFC5423 event. Currently only <code>MessageNew</code> is expected
         * 
         * @param event The message event
         */
        public void setEvent(String event) {
            this.event = event;
        }

        /**
         * The username of account receiving the message
         *
         * @return The username or <code>null</code>
         */
        public String getUser() {
            return user;
        }

        /**
         * Set the username of account receiving the message
         *
         * @param user The username
         */
        public void setUser(String user) {
            this.user = user;
        }

        @Override
        public String toString() {
            return "PushMail [user=" + user + ", event=" + event + "]";
        }
    }
}
