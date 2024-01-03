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
package com.openexchange.segmenter.client;

import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;
import com.openexchange.segment.SegmentMarker;

/**
 * {@link SegmenterService} - Provides site information for a given marker; usually the database schema.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
@SingletonService
public interface SegmenterService {

    /**
     * Gets the site for a given marker.
     * <p/>
     * In <i>sharded</i> environments with multiple data centers ("Active/Active"), the site serving the segment is retrieved from the
     * segmenter service. Otherwise, the local site id is returned as fallback.
     *
     * @param marker The markers to get the site for
     * @return The site that is currently responsible for the segment, falling back to the <i>local</i> site if not applicable
     * @throws OXException If site cannot be returned for given marker
     */
    Site getSite(SegmentMarker marker) throws OXException;

    /**
     * Gets the sites for the given markers.
     * <p/>
     * In <i>sharded</i> environments with multiple data centers ("Active/Active"), the sites serving the segments are retrieved from the
     * segmenter service. Otherwise, the local site id is returned as fallback for each marker.
     *
     * @param markers The markers to get the schemas for
     * @return A list of sites that are responsible for each segment, falling back to the <i>local</i> site if not applicable, in the same
     *         order as the passed list of segment markers
     * @throws OXException If sites cannot be returned for given markers
     */
    List<Site> getSites(List<SegmentMarker> markers) throws OXException;

    /**
     * Gets the <i>local</i> site where this node is deployed, defaulting the <code>default</code> site unless overridden by
     * configuration.
     *
     * @return The local site
     */
    Site getLocalSite();

    /**
     * Gets a value indicating whether a certain segment marker is currently associated with the <i>local</i> site or not.
     * 
     * @param marker The segment marker to check
     * @return <code>true</code> if the segment is currently associated with the <i>local</i> site, <code>false</code>, otherwise
     * @throws OXException If site cannot be returned for given marker
     */
    default boolean isLocal(SegmentMarker marker) throws OXException {
        return getLocalSite().matches(getSite(marker));
    }

}
