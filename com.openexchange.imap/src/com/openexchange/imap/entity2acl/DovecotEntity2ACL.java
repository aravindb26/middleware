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

package com.openexchange.imap.entity2acl;

/**
 * {@link DovecotEntity2ACL} - Handles the ACL entities used by Dovecot IMAP server.
 * <p>
 * The current supported identifiers are:
 * <ul>
 * <li><i>owner</i></li>
 * <li><i>anyone</i></li>
 * </ul>
 * <p>
 * Missing handling for identifiers:
 * <ul>
 * <li><i>anonymous</i> (This is a synonym from <i>anyone</i>)</li>
 * <li><i>user=loginid</i> (Rights or negative rights for IMAP account "loginid")</li>
 * <li><i>group=name</i> (Rights or negative rights for account group "name")</li>
 * <li><i>administrators</i> (This is an alias for <i>group=administrators</i>)</li>
 * </ul>
 * <p>
 * The complete implementation should be able to handle an ACL like this one:
 *
 * <pre>
 * owner aceilrstwx anyone lr user=john w -user=mary r administrators aceilrstwx
 * </pre>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class DovecotEntity2ACL extends AbstractOwnerCapableEntity2ACL {

    private static final DovecotEntity2ACL INSTANCE = new DovecotEntity2ACL();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static DovecotEntity2ACL getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    /**
     * Default constructor
     */
    private DovecotEntity2ACL() {
        super();
    }

    @Override
    protected IMAPServer getIMAPServer() {
        return IMAPServer.DOVECOT;
    }

}
