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
import java.util.Map;
import org.json.JSONObject;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.provider.CalendarAccount;
import com.openexchange.chronos.provider.account.CalendarAccountService;
import com.openexchange.exception.OXException;
import com.openexchange.jslob.JSlobEntry;
import com.openexchange.jslob.JSlobKey;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link ChronosJSlobEntry}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public abstract class ChronosJSlobEntry implements JSlobEntry {

    protected final ServiceLookup services;

    /**
     * Initializes a new {@link ChronosJSlobEntry}.
     *
     * @param services A service lookup reference
     */
    protected ChronosJSlobEntry(ServiceLookup services) {
        super();
        this.services = services;
    }

    protected abstract Object getValue(ServerSession session, JSONObject userConfig) throws OXException;

    protected abstract void setValue(ServerSession session, JSONObject userConfig, Object value) throws OXException;

    @Override
    public JSlobKey getKey() {
        return JSlobKey.CALENDAR;
    }

    @Override
    public Object getValue(Session session) throws OXException {
        ServerSession serverSession = ServerSessionAdapter.valueOf(session);
        if (false == isAvailable(serverSession)) {
            return JSONObject.NULL;
        }
        CalendarAccount account = services.getServiceSafe(CalendarAccountService.class).getAccount(session, ACCOUNT_ID, null);
        if (null == account) {
            throw CalendarExceptionCodes.ACCOUNT_NOT_FOUND.create(I(ACCOUNT_ID));
        }
        return getValue(serverSession, account.getUserConfiguration());
    }

    @Override
    public void setValue(Object value, Session session) throws OXException {
        CalendarAccountService accountService = services.getServiceSafe(CalendarAccountService.class);
        CalendarAccount account = accountService.getAccount(session, ACCOUNT_ID, null);
        if (null == account) {
            throw CalendarExceptionCodes.ACCOUNT_NOT_FOUND.create(I(ACCOUNT_ID));
        }
        ServerSession serverSession = ServerSessionAdapter.valueOf(session);
        if (false == isAvailable(serverSession)) {
            throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(PROVIDER_ID);
        }
        JSONObject userConfig = account.getUserConfiguration();
        setValue(serverSession, userConfig, value);
        long timestamp = null != account.getLastModified() ? account.getLastModified().getTime() : CalendarUtils.DISTANT_FUTURE;
        accountService.updateAccount(session, ACCOUNT_ID, userConfig, timestamp, null);
    }

    @Override
    public Map<String, Object> metadata(Session session) throws OXException {
        return null;
    }

    /**
     * Gets a value indicating whether chronos js lob entries are generally available or not.
     * 
     * @param session The session
     * @return <code>true</code> if genrally available, <code>false</code>, otherwise
     */
    protected static boolean isAvailable(ServerSession session) {
        if (session.isAnonymous() || session.getUser().isGuest() || false == session.getUserPermissionBits().hasCalendar()) {
            return false;
        }
        return true;
    }

}
