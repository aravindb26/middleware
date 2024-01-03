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

package com.openexchange.contact.provider.composition.impl.idmangling;

import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.tools.functions.ErrorAwareFunction;

/**
 * {@link IDManglingDistributionListEntryObject}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class IDManglingDistributionListEntryObject extends DistributionListEntryObject {

    private static final long serialVersionUID = 85036226708311090L;

    public IDManglingDistributionListEntryObject(DistributionListEntryObject delegate, ErrorAwareFunction<String, String> folderIdFuntion) throws OXException {
        super();
        if (delegate.containsContactUid()) {
            super.setContactUid(delegate.getContactUid());
        }
        if (delegate.containsDisplayname()) {
            super.setDisplayname(delegate.getDisplayname());
        }
        if (delegate.containsEmailaddress()) {
            super.setEmailaddress(delegate.getEmailaddress(), false);
        }
        if (delegate.containsEmailfield()) {
            super.setEmailfield(delegate.getEmailfield());
        }
        if (delegate.containsEntryID()) {
            super.setEntryID(delegate.getEntryID());
        }
        if (delegate.containsFistname()) {
            super.setFirstname(delegate.getFirstname());
        }
        if (delegate.containsFolderld()) {
            super.setFolderID(folderIdFuntion.apply(delegate.getFolderID()));
        }
        if (delegate.containsLastname()) {
            super.setLastname(delegate.getLastname());
        }
        if (delegate.containsSortName()) {
            super.setSortName(delegate.getSortName());
        }
    }
    
    @Override
    public void setFolderID(String folderid) {
        throw new UnsupportedOperationException(); 
    }
    
}
