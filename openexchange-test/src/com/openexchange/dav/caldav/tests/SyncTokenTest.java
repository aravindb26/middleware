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

package com.openexchange.dav.caldav.tests;

import java.util.Calendar;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.ReportMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.reports.SyncCollectionReportInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * {@link SyncTokenTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.2
 */
public class SyncTokenTest extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testEmptySyncToken(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        syncCollection("", StatusCodes.SC_MULTISTATUS);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testNoSyncToken(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        syncCollection(null, StatusCodes.SC_MULTISTATUS);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRegularSyncToken(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        syncCollection(String.valueOf(System.currentTimeMillis()), StatusCodes.SC_MULTISTATUS);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testOutdatedSyncToken(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -2);
        syncCollection(String.valueOf(calendar.getTimeInMillis()), StatusCodes.SC_FORBIDDEN);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMalformedSyncToken(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        syncCollection("wurstpeter", StatusCodes.SC_FORBIDDEN);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testOutdatedTruncatedSyncToken(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -2);
        String token = calendar.getTimeInMillis() + ".4";
        syncCollection(token, StatusCodes.SC_MULTISTATUS);
    }

    protected MultiStatusResponse[] syncCollection(String syncToken, int expectedResponse) throws Exception {
        String uri = getBaseUri() + Config.getPathPrefix() + "/caldav/" + encodeFolderID(getDefaultFolderID());
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.GETETAG);
        ReportInfo reportInfo = new SyncCollectionReportInfo(syncToken, props);
        ReportMethod report = null;
        try {
            report = new ReportMethod(uri, reportInfo);
            return webDAVClient.doReport(report, expectedResponse);
        } finally {
            release(report);
        }
    }

}
