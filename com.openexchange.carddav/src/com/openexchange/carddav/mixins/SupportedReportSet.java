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

package com.openexchange.carddav.mixins;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.openexchange.contact.provider.ContactsAccessCapability;
import com.openexchange.webdav.protocol.Protocol;
import com.openexchange.webdav.protocol.helpers.SingleXMLPropertyMixin;

/**
 * {@link SupportedReportSet}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class SupportedReportSet extends SingleXMLPropertyMixin {

    /** CARD:addressbook-multiget */
    public static final String ADDRESSBOOK_MULTIGET = "<CARD:addressbook-multiget/>";

    /** CARD:addressbook-query */
    public static final String ADDRESSBOOK_QUERY = "<CARD:addressbook-query/>";

    /** D:sync-collection */
    public static final String SYNC_COLLECTION = "<D:sync-collection/>";

    private static final String[] ALL_SUPPORTED = new String[] { ADDRESSBOOK_MULTIGET, ADDRESSBOOK_QUERY, SYNC_COLLECTION };

    private final String[] supportedReports;

    /**
     * Initializes a new {@link SupportedReportSet} with all supported reports.
     */
    public SupportedReportSet() {
        this(ALL_SUPPORTED);
    }

    /**
     * Initializes a new {@link SupportedReportSet} with specific supported reports.
     *
     * @param supportedReports The supported reports
     */
    public SupportedReportSet(String... supportedReports) {
        super(Protocol.DAV_NS.getURI(), "supported-report-set");
        this.supportedReports = supportedReports;
    }

    /**
     * Initializes a new {@link SupportedReportSet} with specific supported reports.
     *
     * @param supportedCapabilities The supported capabilities from the underlying folder
     */
    public SupportedReportSet(Set<ContactsAccessCapability> supportedCapabilities) {
        this(getSupportedReports(supportedCapabilities));
    }

    @Override
    protected String getValue() {
        StringBuilder stringBuilder = new StringBuilder();
        if (null == supportedReports) {
            return "";
        }
        for (String supportedReport : supportedReports) {
            stringBuilder.append("<D:supported-report><D:report>").append(supportedReport).append("</D:report></D:supported-report>");
        }
        return stringBuilder.toString();
    }

    private static String[] getSupportedReports(Set<ContactsAccessCapability> supportedCapabilities) {
        if (null == supportedCapabilities) {
            return ALL_SUPPORTED;
        }
        List<String> supportedReports = new LinkedList<>();
        supportedReports.add(ADDRESSBOOK_MULTIGET);
        if (supportedCapabilities.contains(ContactsAccessCapability.SEARCH)) {
            supportedReports.add(ADDRESSBOOK_QUERY);
        }
        if (supportedCapabilities.contains(ContactsAccessCapability.SYNC)) {
            supportedReports.add(SYNC_COLLECTION);
        }
        return supportedReports.toArray(new String[supportedReports.size()]);
    }
}
