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

package com.openexchange.groupware.update.tasks;

/**
 * {@link DropLdapIdsTableTask}<p>
 *
 * Drop table <code>ldapIds</code>.<br>
 * The table has been used along with package <code>open-xchange-contact-storage-ldap</code> until v7.10.6.
 * Due to removing this package with v8.0.0 we can drop the table as well.
 *
 * @author <a href="mailto:ralf.wurm@open-xchange.com">Ralf Wurm</a>
 * @since v8.0.0
 */
public class DropLdapIdsTableTask extends AbstractDropTableTask {

    /**
     * Initializes a new {@link DropVersionTableTask}.
     */
    public DropLdapIdsTableTask() {
        super("ldapIds");
    }

    @Override
    public String[] getDependencies() {
        return new String[] { };
    }

}
