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

package com.openexchange.subscribe.osgi.tools;

import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.subscribe.SubscriptionSource;
import com.openexchange.subscribe.SubscriptionSourceDiscoveryService;

/**
 * {@link WhiteboardSubscriptionSourceDiscoveryService}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class WhiteboardSubscriptionSourceDiscoveryService implements SubscriptionSourceDiscoveryService {


    private final ServiceTracker<?, ?> tracker;

    public WhiteboardSubscriptionSourceDiscoveryService(final BundleContext context) {
        this.tracker = new ServiceTracker<>(context, SubscriptionSourceDiscoveryService.class.getName(), null);
        tracker.open();
    }

    public void close() {
        tracker.close();
    }

    @Override
    public SubscriptionSource getSource(final Context context, final int subscriptionId) throws OXException {
        return getDelegate().getSource(context, subscriptionId);
    }

    @Override
    public SubscriptionSource getSource(final String identifier) {
        return getDelegate().getSource(identifier);
    }

    @Override
    public List<SubscriptionSource> getSources() {
        return getDelegate().getSources();
    }

    @Override
    public List<SubscriptionSource> getSources(final int folderModule) {
        return getDelegate().getSources(folderModule);
    }

    @Override
    public boolean knowsSource(final String identifier) {
        return getDelegate().knowsSource(identifier);
    }

    @Override
    public SubscriptionSourceDiscoveryService filter(final int user, final int context) throws OXException {
        return getDelegate().filter(user, context);
    }

    private SubscriptionSourceDiscoveryService getDelegate() {
        return (SubscriptionSourceDiscoveryService) tracker.getService();
    }

}
