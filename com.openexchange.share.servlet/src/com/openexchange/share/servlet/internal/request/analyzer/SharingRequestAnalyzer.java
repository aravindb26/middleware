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

package com.openexchange.share.servlet.internal.request.analyzer;

import java.util.Optional;
import com.openexchange.database.ConfigDatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.RequestURL;
import com.openexchange.request.analyzer.UserInfo;
import com.openexchange.segment.SegmentMarker;
import com.openexchange.share.core.tools.ShareToken;
import com.openexchange.tools.functions.ErrorAwareSupplier;

/**
 * {@link SharingRequestAnalyzer} is a {@link RequestAnalyzer} which analyzes sharing tokens
 * to determine the marker for the request
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class SharingRequestAnalyzer implements RequestAnalyzer {

    private final String prefix;
    private final ErrorAwareSupplier<? extends ConfigDatabaseService> dbServiceSupplier;

    /**
     * Initializes a new {@link SharingRequestAnalyzer}.
     *
     * @param alias The sharing servlet alias
     * @param dbServiceSupplier A supplier for the database service for resolving the database schema name during analysis
     */
    public SharingRequestAnalyzer(String alias, ErrorAwareSupplier<? extends ConfigDatabaseService> dbServiceSupplier) {
        super();
        this.prefix = alias;
        this.dbServiceSupplier = dbServiceSupplier;
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        Optional<String> optToken = getToken(data);
        if (optToken.isEmpty()) {
            return Optional.empty();
        }
        ShareToken shareToken;
        try {
            shareToken = new ShareToken(optToken.get());
        } catch (OXException e) {
            org.slf4j.LoggerFactory.getLogger(SharingRequestAnalyzer.class).debug("Ignoring unparseable share token from URI '{}'", data.getUrl(), e);
            return Optional.of(AnalyzeResult.UNKNOWN);
        }
        int userId = shareToken.getUserID();
        int contextId = shareToken.getContextID();
        String schemaName = dbServiceSupplier.get().getSchemaName(contextId);
        UserInfo userInfo = UserInfo.builder(contextId)
                                    .withUserId(userId)
                                    .build();
        return Optional.of(new AnalyzeResult(SegmentMarker.of(schemaName), userInfo));
    }

    /**
     * Gets the share token from the request data
     *
     * @param data The request data
     * @return The optional token
     * @throws OXException
     */
    private Optional<String> getToken(RequestData data) throws OXException {
        RequestURL url = data.getParsedURL();
        Optional<String> path = url.getPath();
        if (path.isEmpty() || path.get().length() <= prefix.length() || false == path.get().startsWith(prefix)) {
            return Optional.empty();
        }
        String token = path.get().substring(prefix.length() + 1);
        String[] split = token.split("/");
        if (split.length == 0) {
            return Optional.empty();
        }

        return Optional.of(split[0]);
    }

}
