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

package com.openexchange.webdav.protocol.osgi;

import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.BundleContext;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.NearRegistryServiceTracker;
import com.openexchange.session.SessionHolder;
import com.openexchange.webdav.protocol.WebdavProperty;
import com.openexchange.webdav.protocol.WebdavResource;
import com.openexchange.webdav.protocol.helpers.PropertyMixin;
import com.openexchange.webdav.protocol.helpers.PropertyMixinFactory;
import com.openexchange.webdav.protocol.helpers.ResourcePropertyMixin;

/**
 * {@link OSGiPropertyMixin}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class OSGiPropertyMixin implements PropertyMixin, ResourcePropertyMixin {

    private final NearRegistryServiceTracker<PropertyMixin> mixinTracker;
    private final NearRegistryServiceTracker<PropertyMixinFactory> factoryTracker;
    private final SessionHolder sessionHolder;

    public OSGiPropertyMixin(BundleContext context, SessionHolder sessionHolder) {
        this.sessionHolder = sessionHolder;

        this.mixinTracker = new NearRegistryServiceTracker<PropertyMixin>(context, PropertyMixin.class);
        mixinTracker.open();

        this.factoryTracker = new NearRegistryServiceTracker<PropertyMixinFactory>(context, PropertyMixinFactory.class);
        factoryTracker.open();
    }

    public void close() {
        mixinTracker.close();
        factoryTracker.close();
    }

    @Override
    public List<WebdavProperty> getAllProperties() throws OXException {
        return getAllProperties(null);
    }

    @Override
    public List<WebdavProperty> getAllProperties(WebdavResource resource) throws OXException {
        List<WebdavProperty> allProperties = new ArrayList<WebdavProperty>();

        List<PropertyMixin> mixins = mixinTracker.getServiceList();
        if (mixins != null && !mixins.isEmpty()) {
            for (PropertyMixin mixin : mixins) {
                if ((mixin instanceof ResourcePropertyMixin)) {
                    allProperties.addAll(((ResourcePropertyMixin) mixin).getAllProperties(resource));
                } else {
                    allProperties.addAll(mixin.getAllProperties());
                }
            }
        }

        List<PropertyMixinFactory> factories = factoryTracker.getServiceList();
        if (factories != null && !factories.isEmpty()) {
            for (PropertyMixinFactory factory : factories) {
                PropertyMixin mixin = factory.create(sessionHolder);
                if ((mixin instanceof ResourcePropertyMixin)) {
                    allProperties.addAll(((ResourcePropertyMixin) mixin).getAllProperties(resource));
                } else {
                    allProperties.addAll(mixin.getAllProperties());
                }
            }
        }

        return allProperties;
    }

    @Override
    public WebdavProperty getProperty(String namespace, String name) throws OXException {
        return getProperty(null, namespace, name);
    }

    @Override
    public WebdavProperty getProperty(WebdavResource resource, String namespace, String name) throws OXException {
        List<PropertyMixin> mixins = mixinTracker.getServiceList();
        if (mixins != null && !mixins.isEmpty()) {
            for (PropertyMixin mixin : mixins) {
                WebdavProperty property;
                if ((mixin instanceof ResourcePropertyMixin)) {
                    property = ((ResourcePropertyMixin) mixin).getProperty(resource, namespace, name);
                } else {
                    property = mixin.getProperty(namespace, name);
                }
                if (property != null) {
                    return property;
                }
            }
        }

        List<PropertyMixinFactory> factories = factoryTracker.getServiceList();
        if (factories != null && !factories.isEmpty()) {
            for (PropertyMixinFactory factory : factories) {
                PropertyMixin mixin = factory.create(sessionHolder);
                WebdavProperty property;
                if ((mixin instanceof ResourcePropertyMixin)) {
                    property = ((ResourcePropertyMixin) mixin).getProperty(resource, namespace, name);
                } else {
                    property = mixin.getProperty(namespace, name);
                }
                if (property != null) {
                    return property;
                }
            }
        }
        return null;
    }

}
