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

package com.openexchange.ajax.request.analyzer;

import static com.openexchange.ajax.AJAXServlet.PARAMETER_ACTION;
import java.util.Optional;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.dispatcher.DispatcherPrefixService;
import com.openexchange.exception.OXException;
import com.openexchange.request.analyzer.RequestAnalyzer;
import com.openexchange.request.analyzer.RequestData;
import com.openexchange.request.analyzer.RequestURL;
import com.openexchange.server.ServiceLookup;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.tools.functions.ErrorAwareSupplier;

/**
 * {@link AbstractPrefixAwareRequestAnalyzer} is an abstract {@link RequestAnalyzer} for session based analyzer which provides utility methods.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public abstract class AbstractPrefixAwareRequestAnalyzer implements RequestAnalyzer {

    protected final ErrorAwareSupplier<SessiondService> sessiondService;
    protected final String prefix;

    /**
     * Initializes a new {@link AbstractPrefixAwareRequestAnalyzer}, using the prefix path to match from the
     * {@link DispatcherPrefixService}.
     *
     * @param services The service lookup to use, which should yield the {@link SessiondService} and the {@link DispatcherPrefixService}
     * @throws OXException If the {@link DispatcherPrefixService} is missing
     */
    protected AbstractPrefixAwareRequestAnalyzer(ServiceLookup services) throws OXException {
        this(() -> services.getServiceSafe(SessiondService.class), services.getServiceSafe(DispatcherPrefixService.class).getPrefix());
    }

    /**
     * Initializes a new {@link AbstractPrefixAwareRequestAnalyzer}.
     *
     * @param sessiondService A supplier for the {@link SessiondService} reference
     * @param prefix The prefix to match the analyzed path against
     */
    protected AbstractPrefixAwareRequestAnalyzer(ErrorAwareSupplier<SessiondService> sessiondService, String prefix) {
        super();
        this.sessiondService = sessiondService;
        this.prefix = prefix;
    }

    // --------------------- protected methods ----------------------

    /**
     * Gets the optional module and action
     *
     * @param prefix The prefix
     * @param data The request data
     * @return The optional {@link ModuleAndAction}
     * @throws OXException In case of an invalid or malformed URL
     */
    protected Optional<ModuleAndAction> optModuleAndAction(String prefix, RequestData data) throws OXException {
        RequestURL url = data.getParsedURL();
        if (url.getPath().isEmpty()) {
            return Optional.empty();
        }
        String module = AJAXRequestDataTools.getModuleFromPath(prefix, url.getPath().get());
        String action = url.optParameter(PARAMETER_ACTION).orElse(null);
        if (module == null || action == null) {
            return Optional.empty();
        }
        return Optional.of(new ModuleAndAction(module, action));
    }

    /**
     * Checks if this request contains the proper prefix
     *
     * @param data The request data
     * @return <code>true</code> if the request contains the proper prefix, <code>false</code> otherwise
     * @throws OXException In case of an invalid or malformed URL
     */
    protected boolean hasValidPrefix(RequestData data) throws OXException {
        Optional<String> path = data.getParsedURL().getPath();
        return path.isPresent() && path.get().startsWith(prefix);
    }

    // ------------------------- helper records -----------------------

    /**
     * {@link ModuleAndAction} is a wrapper for a module and action
     *
     * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
     * @param module The module name
     * @param action The action name
     */
    protected record ModuleAndAction(String module, String action) {
    }
}
