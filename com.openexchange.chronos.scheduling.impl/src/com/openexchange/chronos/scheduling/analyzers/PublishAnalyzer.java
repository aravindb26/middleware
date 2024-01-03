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

package com.openexchange.chronos.scheduling.analyzers;

import java.util.Collections;
import java.util.List;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.ITipAction;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.scheduling.common.ITipAnnotationBuilder;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link PublishAnalyzer} - Analyzer for the iTIP method <code>PUBLISH</code>
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5546#section-3.2.1">RFC 5546 Section 3.2.1</a>
 */
public class PublishAnalyzer extends AbstractSchedulingAnalyzer {

    /**
     * Initializes a new {@link PublishAnalyzer}.
     * 
     * @param services The services
     */
    public PublishAnalyzer(ServiceLookup services) {
        super(services, SchedulingMethod.PUBLISH);
    }

    @Override
    protected List<AnalyzedChange> analyze(CalendarSession session, ObjectResourceProvider objectResourceProvider, CalendarUser originator, int targetUser) throws OXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ITipAnalysis analyze(IncomingSchedulingMessage message, CalendarSession session) throws OXException {
        return new ITipAnalysis() // @formatter:off
            .setMethod(getMethod())
            .setChanges(Collections.singletonList(new AnalyzedChange()
                .addActions(ITipAction.IGNORE)
                .addAnnotation(ITipAnnotationBuilder.newBuilder().message("Not supported").build()))); //@formatter:on
    }

}
