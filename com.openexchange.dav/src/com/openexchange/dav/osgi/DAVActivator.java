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

package com.openexchange.dav.osgi;

import static com.openexchange.dav.DAVTools.getInternalPath;
import org.osgi.service.http.HttpService;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.clientinfo.ClientInfoProvider;
import com.openexchange.config.Reloadable;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.contact.ContactService;
import com.openexchange.dav.DAVClientInfoProvider;
import com.openexchange.dav.DAVServlet;
import com.openexchange.dav.DAVUserAgentParser;
import com.openexchange.dav.attachments.AttachmentPerformer;
import com.openexchange.dav.mixins.AddressbookHomeSet;
import com.openexchange.dav.mixins.CalendarHomeSet;
import com.openexchange.dav.mixins.PrincipalCollectionSet;
import com.openexchange.dav.principals.PrincipalPerformer;
import com.openexchange.dav.root.RootPerformer;
import com.openexchange.dav.useragent.DAVUserAgentParserImpl;
import com.openexchange.group.GroupService;
import com.openexchange.login.Interface;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.resource.ResourceService;
import com.openexchange.tools.webdav.DAVServletPathProvider;
import com.openexchange.tools.webdav.PrefixBasedDAVServletPathProvider;
import com.openexchange.uadetector.UserAgentParser;
import com.openexchange.user.UserService;
import com.openexchange.webdav.protocol.helpers.PropertyMixin;
import com.openexchange.webdav.protocol.osgi.OSGiPropertyMixin;

/**
 * {@link DAVActivator}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.1
 */
public class DAVActivator extends HousekeepingActivator {

    private OSGiPropertyMixin mixin;

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { UserService.class, HttpService.class, ContactService.class, GroupService.class, ResourceService.class, UserAgentParser.class, ConfigViewFactory.class, CapabilityService.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        ConfigViewFactory configViewFactory = getService(ConfigViewFactory.class);
        HttpService httpService = getService(HttpService.class);
        /*
         * root
         */
        RootPerformer rootPerformer = new RootPerformer(this);
        String rootPath = getInternalPath(configViewFactory, null);
        httpService.registerServlet(rootPath, new DAVServlet(rootPerformer, Interface.CALDAV), null, null);
        registerService(DAVServletPathProvider.class, (path) -> rootPath.equals(path) || null != path && path.endsWith("/") && path.substring(0, path.length() - 1).equals(rootPath));
        /*
         * attachments
         */
        AttachmentPerformer attachmentPerformer = new AttachmentPerformer(this);
        String attachmentsPath = getInternalPath(configViewFactory, "attachments");
        httpService.registerServlet(attachmentsPath, new DAVServlet(attachmentPerformer, Interface.CALDAV), null, null);
        registerService(DAVServletPathProvider.class, new PrefixBasedDAVServletPathProvider(attachmentsPath));
        /*
         * principals
         */
        PrincipalPerformer principalPerformer = new PrincipalPerformer(this);
        String principalsPath = getInternalPath(configViewFactory, "principals");
        httpService.registerServlet(principalsPath, new DAVServlet(principalPerformer, Interface.CARDDAV), null, null);
        registerService(DAVServletPathProvider.class, new PrefixBasedDAVServletPathProvider(principalsPath));
        OSGiPropertyMixin mixin = new OSGiPropertyMixin(context, principalPerformer);
        principalPerformer.setGlobalMixins(mixin);
        this.mixin = mixin;
        /*
         * OSGi mixins
         */
        registerService(PropertyMixin.class, new PrincipalCollectionSet(configViewFactory));
        registerService(PropertyMixin.class, new CalendarHomeSet(configViewFactory));
        registerService(PropertyMixin.class, new AddressbookHomeSet(configViewFactory));
        /*
         * Initialize DAV agents
         */
        DAVUserAgentParserImpl parser = new DAVUserAgentParserImpl(this, configViewFactory);
        registerService(DAVUserAgentParser.class, parser);
        addService(DAVUserAgentParser.class, parser);
        registerService(Reloadable.class, parser);
        /*
         * DAV client info
         */
        registerService(ClientInfoProvider.class, new DAVClientInfoProvider(getService(UserAgentParser.class), parser), 0);
        openTrackers();
        /*
         * Set service lookup
         */
        Services.setServiceLookup(this);
    }

    @Override
    protected synchronized void stopBundle() throws Exception {
        OSGiPropertyMixin mixin = this.mixin;
        if (null != mixin) {
            mixin.close();
            this.mixin = null;
        }
        super.stopBundle();
    }

}
