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
 * {@link CyrusEntity2ACL} - Handles the ACL entities used by Cyrus IMAP server.
 * <p>
 * The current supported identifiers are:
 * <ul>
 * <li><i>anyone</i> which refers to all users, including the anonymous user</li>
 * </ul>
 * <p>
 * Missing handling for identifiers:
 * <ul>
 * <li><i>anonymous</i> which refers to the anonymous, or unauthenticated user</li>
 * </ul>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CyrusEntity2ACL extends AbstractAnyoneCapableEntity2ACL {

    private static final CyrusEntity2ACL INSTANCE = new CyrusEntity2ACL();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static CyrusEntity2ACL getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Default constructor
     */
    private CyrusEntity2ACL() {
        super();
    }

    @Override
    protected IMAPServer getIMAPServer() {
        return IMAPServer.CYRUS;
    }

}
