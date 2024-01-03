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
package com.openexchange.logback.json.extensions;

import java.util.HashMap;
import java.util.Map;
import com.openexchange.log.LogProperties;
import com.openexchange.logback.extensions.json.BasicJsonLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * {@link ExtendedJsonLayout} - Defines a JSON layout for {@link ILoggingEvent} to be logged
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public class ExtendedJsonLayout extends BasicJsonLayout {

    private static final String REQUEST_ID_ATTR_NAME = "requestId";
    private static final String CONTEXTID_ATTR_NAME = "contextId";
    private static final String USERID_ATTR_NAME = "userId";
    private static final String BRAND_ATTR_NAME = "brand";
    private static final String AUDIT_ATTR_NAME = "audit";

    private static final LogProperties.Name[] PROPERTIES_TO_FILTER = { LogProperties.Name.HOSTNAME, LogProperties.Name.REQUEST_TRACKING_ID, LogProperties.Name.LOCALHOST_VERSION };

    public ExtendedJsonLayout() {
        super();
    }

    @Override
    protected Map<String, Object> toJsonMap(ILoggingEvent event) {

        Map<String, Object> map = super.toJsonMap(event);

        String requestId = getPropertyFromMDC(LogProperties.Name.REQUEST_TRACKING_ID, eventMDC);
        String contextId = getPropertyFromMDC(LogProperties.Name.SESSION_CONTEXT_ID, eventMDC);
        String userId = getPropertyFromMDC(LogProperties.Name.SESSION_USER_ID, eventMDC);
        String brand = getPropertyFromMDC(LogProperties.Name.SESSION_BRAND, eventMDC);
        String sAudit = getPropertyFromMDC(LogProperties.Name.AUDIT, eventMDC);
        boolean isAudit = Boolean.parseBoolean(sAudit);

        add(REQUEST_ID_ATTR_NAME, true, requestId, map);
        add(CONTEXTID_ATTR_NAME, true, contextId, map);
        add(USERID_ATTR_NAME, true, userId, map);
        add(BRAND_ATTR_NAME, true, brand, map);
        if (isAudit) {
            add(AUDIT_ATTR_NAME, true, true, map);
        }

        Map<String, String> filteredMDCPropertyMap = filterMDC(eventMDC);
        addPropertyToDetails(THREAD_ATTR_NAME, event.getThreadName(), filteredMDCPropertyMap);
        addMap(DETAILS_ATTR_NAME, true, filteredMDCPropertyMap, map);

        return map;
    }

    @Override
    protected Map<String, String> filterMDC(Map<String, String> mdcPropertyMap) {
        if (null == mdcPropertyMap || mdcPropertyMap.isEmpty()) {
            return new HashMap<>();
        }
        for (LogProperties.Name prop : PROPERTIES_TO_FILTER) {
            mdcPropertyMap.remove(prop.getName());
        }
        return mdcPropertyMap;
    }

    private String getPropertyFromMDC(LogProperties.Name property, Map<String, String> mdc) {
        if (null == property || null == mdc || mdc.isEmpty()) {
            return null;
        }
        return mdc.get(property.getName());
    }

}
