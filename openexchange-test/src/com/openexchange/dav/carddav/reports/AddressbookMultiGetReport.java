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

package com.openexchange.dav.carddav.reports;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.openexchange.dav.PropertyNames;

/**
 * {@link AddressbookMultiGetReport}
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class AddressbookMultiGetReport implements Report, DeltaVConstants {

    public static final ReportType ADDRESSBOOK_MULTI_GET = ReportType.register(PropertyNames.ADDRESSBOOK_MULTIGET.getName(), PropertyNames.ADDRESSBOOK_MULTIGET.getNamespace(), AddressbookMultiGetReport.class);

    @Override
    public ReportType getType() {
        return ADDRESSBOOK_MULTI_GET;
    }

    @Override
    public boolean isMultiStatusReport() {
        return true;
    }

    @Override
    public void init(DavResource dr, ReportInfo ri) throws DavException {}

    @Override
    public Element toXml(Document arg0) {
        throw new UnsupportedOperationException();
    }
}
