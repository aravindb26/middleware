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
import com.openexchange.contact.vcard.VCardParameters;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import ezvcard.VCard;
import ezvcard.property.Revision;


/**
 * {@link RevisionMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class RevisionMapping extends AbstractMapping {

    /**
     * Initializes a new {@link RevisionMapping}.
     */
    public RevisionMapping() {
        super("REV", ContactField.LAST_MODIFIED);
    }

    @Override
    public void exportContact(Contact contact, VCard vCard, VCardParameters parameters, List<OXException> warnings) {
        Revision property = vCard.getRevision();
        if (null == property) {
            vCard.setRevision(contact.getLastModified());
        } else {
            property.setValue(contact.getLastModified());
        }
    }

    @Override
    public void importVCard(VCard vCard, Contact contact, VCardParameters parameter, List<OXException> warnings) {
        Revision revision = vCard.getRevision();
        if (null != revision) {
            contact.setLastModified(revision.getValue());
        }
    }

}
