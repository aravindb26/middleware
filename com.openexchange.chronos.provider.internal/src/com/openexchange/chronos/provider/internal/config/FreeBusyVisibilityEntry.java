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

package com.openexchange.chronos.provider.internal.config;

import static com.openexchange.chronos.provider.internal.Constants.ACCOUNT_ID;
import static com.openexchange.chronos.provider.internal.Constants.PROVIDER_ID;
import static com.openexchange.java.Autoboxing.I;
import java.util.Collections;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import com.openexchange.chronos.FreeBusyVisibility;
import com.openexchange.chronos.common.CalendarProperty;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.provider.CalendarAccount;
import com.openexchange.chronos.provider.account.CalendarAccountService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Enums;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link FreeBusyVisibilityEntry} - the JSlob entry for "chronos/freeBusyVisibility"
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public class FreeBusyVisibilityEntry extends ChronosJSlobEntry {

    private static final String FREE_BUSY_VISIBILITY = "freeBusyVisibility";

    /**
     * Initializes a new {@link FreeBusyVisibilityEntry}.
     *
     * @param services A service lookup reference
     */
    public FreeBusyVisibilityEntry(ServiceLookup services) {
        super(services);
    }

    @Override
    public boolean needsUpdate(Object newValue, Session session) throws OXException {
        if (super.needsUpdate(newValue, session)) {
            return true;
        }
        ServerSession serverSession = ServerSessionAdapter.valueOf(session);
        if (false == isAvailable(serverSession)) {
            return false;
        }
        CalendarAccount account = services.getService(CalendarAccountService.class).getAccount(session, ACCOUNT_ID, null);
        if (null == account) {
            throw CalendarExceptionCodes.ACCOUNT_NOT_FOUND.create(I(ACCOUNT_ID));
        }
        JSONObject userConfig = account.getUserConfiguration();
        if (null == userConfig || Strings.isEmpty(userConfig.optString(FREE_BUSY_VISIBILITY))) {
            return true;
        }
        return false;
    }

    @Override
    public String getPath() {
        return "chronos/" + FREE_BUSY_VISIBILITY;
    }

    @Override
    public boolean isWritable(Session session) throws OXException {
        return false == isProtected(session);
    }

    @Override
    public Map<String, Object> metadata(Session session) throws OXException {
        JSONArray possibleValues = new JSONArray();
        for (FreeBusyVisibility value : FreeBusyVisibility.values()) {
            if (FreeBusyVisibility.INTERNAL_ONLY.equals(value) && false == isNonInternalFreeBusyVisibilityPossible(session)) {
                continue;
            }
            possibleValues.put(value.getClientIdentifier());
        }
        return Collections.singletonMap("possibleValues", possibleValues);
    }

    @Override
    protected Object getValue(ServerSession session, JSONObject userConfig) throws OXException {
        FreeBusyVisibility freeBusyVisibility;
        if (isProtected(session)) {
            freeBusyVisibility = getDefaultFreeBusyVisibility(session);
        } else {
            String value = userConfig.optString(FREE_BUSY_VISIBILITY);
            freeBusyVisibility = Enums.parse(FreeBusyVisibility.class, value, getDefaultFreeBusyVisibility(session));
        }
        return freeBusyVisibility.getClientIdentifier();
    }

    @Override
    protected void setValue(ServerSession session, JSONObject userConfig, Object value) throws OXException {
        if (false == isWritable(session)) {
            throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(PROVIDER_ID);
        }
        if (null == value || JSONObject.NULL.equals(value)) {
            userConfig.remove(FREE_BUSY_VISIBILITY);
        } else {
            FreeBusyVisibility freeBusyVisibility;
            try {
                freeBusyVisibility = Enums.parse(FreeBusyVisibility.class, (String) value);
            } catch (Exception e) {
                throw CalendarExceptionCodes.INVALID_CONFIGURATION.create(e, FREE_BUSY_VISIBILITY);
            }
            userConfig.putSafe(FREE_BUSY_VISIBILITY, freeBusyVisibility.getClientIdentifier());
        }
    }

    private FreeBusyVisibility getDefaultFreeBusyVisibility(Session session) throws OXException {
        String value = services.getServiceSafe(LeanConfigurationService.class).getProperty(session.getUserId(), session.getContextId(), CalendarProperty.FREE_BUSY_VISIBILITY_DEFAULT);
        try {
            return Enums.parse(FreeBusyVisibility.class, value);
        } catch (Exception e) {
            throw CalendarExceptionCodes.INVALID_CONFIGURATION.create(e, CalendarProperty.FREE_BUSY_VISIBILITY_DEFAULT.getFQPropertyName());
        }
    }

    private boolean isProtected(Session session) throws OXException {
        return services.getServiceSafe(LeanConfigurationService.class).getBooleanProperty(session.getUserId(), session.getContextId(), CalendarProperty.FREE_BUSY_VISIBILITY_PROTECTED);
    }

    /**
     * Gets a value indicating whether the value {@link FreeBusyVisibility#INTERNAL_ONLY} makes sense in the context of the session user,
     * which is only the case, if "cross-context" free/busy or conflict checks are enabled.
     * 
     * @param session The requesting user's session
     * @return <code>true</code> if non-internal free/busy visibility is applicable, <code>false</code>, otherwise
     * @throws OXException
     */
    private boolean isNonInternalFreeBusyVisibilityPossible(Session session) throws OXException {
        LeanConfigurationService configurationService = services.getServiceSafe(LeanConfigurationService.class);
        if (configurationService.getBooleanProperty(session.getUserId(), session.getContextId(), CalendarProperty.ENABLE_CROSS_CONTEXT_FREE_BUSY)) {
            return true;
        }
        if (configurationService.getBooleanProperty(session.getUserId(), session.getContextId(), CalendarProperty.ENABLE_CROSS_CONTEXT_CONFLICTS)) {
            return true;
        }
        return false;
    }

}
