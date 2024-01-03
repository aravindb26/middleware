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

package com.openexchange.passwordchange.impl;

import org.osgi.framework.BundleContext;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;
import com.openexchange.passwordchange.PasswordChangeEvent;
import com.openexchange.passwordchange.PasswordChangeRegistry;
import com.openexchange.passwordchange.PasswordChangeService;
import com.openexchange.passwordchange.common.PasswordChangeExceptionCodes;

/**
 * {@link PasswordChangeRegistryImpl} - Performs changing a user's password to the fitting service
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.6
 */
public class PasswordChangeRegistryImpl extends RankingAwareNearRegistryServiceTracker<PasswordChangeService> implements PasswordChangeRegistry {

    /**
     * Initializes a new {@link PasswordChangeRegistryImpl}.
     *
     * @param context The bundle context
     */
    public PasswordChangeRegistryImpl(BundleContext context) {
        super(context, PasswordChangeService.class);
    }

    @Override
    public boolean isEnabled(int contextId, int userId) {
        for (PasswordChangeService service : getServiceList()) {
            if (service.isEnabled(contextId, userId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void perform(PasswordChangeEvent event) throws OXException {
        if (null == event) {
            return;
        }
        for (PasswordChangeService service : getServiceList()) {
            if (service.isEnabled(event.getContext().getContextId(), event.getSession().getUserId())) {
                service.perform(event);
                return;
            }
        }
        throw PasswordChangeExceptionCodes.MISSING_SERVICE.create();
    }

}
