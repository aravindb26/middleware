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

package com.openexchange.contact.vcard.impl.mapping;

import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import ezvcard.property.Role;

/**
 * {@link RoleMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class RoleMapping extends SimpleMapping<Role> {

    /**
     * Initializes a new {@link RoleMapping}.
     */
    public RoleMapping() {
        super(Contact.PROFESSION, Role.class, "ROLE", ContactField.PROFESSION);
    }

    @Override
    protected void exportProperty(Contact contact, Role property, List<OXException> warnings) {
        property.setValue(contact.getProfession());
    }

    @Override
    protected Role exportProperty(Contact contact, List<OXException> warnings) {
        return new Role(contact.getProfession());
    }

    @Override
    protected void importProperty(Role property, Contact contact, List<OXException> warnings) {
        contact.setProfession(property.getValue());
    }

}
