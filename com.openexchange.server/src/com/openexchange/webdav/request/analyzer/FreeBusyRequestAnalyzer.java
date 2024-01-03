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
package com.openexchange.webdav.request.analyzer;


import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.database.ConfigDatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.request.analyzer.AnalyzeResult;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.RequestURL;
import com.openexchange.segment.SegmentMarker;
import com.openexchange.tools.functions.ErrorAwareSupplier;
import com.openexchange.webdav.FreeBusy;

/**
 * {@link FreeBusyRequestAnalyzer} is a {@link RequestAnalyzer} which uses the users name and context
 * to determine the marker for the request
 *
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 */
public class FreeBusyRequestAnalyzer implements RequestAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeBusyRequestAnalyzer.class);

    private final ErrorAwareSupplier<? extends ConfigDatabaseService> dbServiceSupplier;

    /**
     * Initializes a new {@link FreeBusyRequestAnalyzer}.
     *
     * @param dbServiceSupplier A supplier for the database service for resolving the database schema name during analysis
     */
    public FreeBusyRequestAnalyzer(ErrorAwareSupplier<? extends ConfigDatabaseService> dbServiceSupplier) {
        super();
        this.dbServiceSupplier = dbServiceSupplier;
    }

    @Override
    public Optional<AnalyzeResult> analyze(RequestData data) throws OXException {
        RequestURL url = data.getParsedURL();
        if (url.getPath().isEmpty() || !url.getPath().get().startsWith(FreeBusy.SERVLET_PATH)) {
            return Optional.empty();
        }

        Optional<String> cid = url.optParameter(FreeBusy.PARAMETER_CONTEXTID);
        if (cid.isEmpty()) {
            return Optional.empty();
        }

        try {
            String schemaName = dbServiceSupplier.get().getSchemaName(Integer.parseInt(cid.get()));
            return Optional.of(new AnalyzeResult(SegmentMarker.of(schemaName), null));
        } catch (NumberFormatException e) {
            LOGGER.debug("The value '{}' set as contextId could not be parsed as an integer.", cid.get(), e);
            return Optional.of(AnalyzeResult.UNKNOWN);
        } catch (OXException e) {
            LOGGER.debug("No schema for context '{}' could be found.", cid.get(), e);
            return Optional.of(AnalyzeResult.UNKNOWN);
        }
    }
}
